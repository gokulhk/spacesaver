package io.github.gokulhk.spacesaver.core.domain.plan

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import org.junit.Test

class BatchPlannerTest {
    private val planner = BatchPlanner(BatchPlanConfig.DEFAULT)
    private val reserve = ByteSize.gigabytes(1)

    /** Free space that leaves exactly [budget] after the 1 GB reserve. */
    private fun freeFor(budget: ByteSize) = budget + reserve

    @Test
    fun `1 GB budget with four 300 MB items gives a batch of 3 and defers 1`() {
        // 250 MB output x 1.2 safety factor = 300 MB cost each.
        val candidates = (1L..4L).map { aCandidate(id = it, original = mb(1_000), output = mb(250)) }

        val result = planner.planNext(candidates, freeSpace = freeFor(ByteSize.gigabytes(1)), reserve = reserve)

        result as NextBatch.Ready
        assertThat(result.items).hasSize(3)
        assertThat(result.deferred).hasSize(1)
        assertThat(result.deferred.single().requiredFreeSpace).isEqualTo(mb(300) + reserve)
    }

    @Test
    fun `single item costing more than the budget is blocked with the space it needs`() {
        val candidates = listOf(aCandidate(id = 1, original = mb(3_000), output = mb(1_000)))

        val result = planner.planNext(candidates, freeSpace = freeFor(mb(500)), reserve = reserve)

        // Cost 1,000 MB x 1.2 = 1,200 MB, plus the 1 GB reserve.
        assertThat(result).isInstanceOf(NextBatch.Blocked::class.java)
        assertThat((result as NextBatch.Blocked).requiredFreeSpace).isEqualTo(mb(1_200) + reserve)
    }

    @Test
    fun `blocked reports the smallest item's requirement`() {
        val candidates =
            listOf(
                aCandidate(id = 1, original = mb(9_000), output = mb(2_000)),
                aCandidate(id = 2, original = mb(3_000), output = mb(1_000)),
            )

        val result = planner.planNext(candidates, freeSpace = freeFor(mb(100)), reserve = reserve)

        assertThat((result as NextBatch.Blocked).requiredFreeSpace).isEqualTo(mb(1_200) + reserve)
        assertThat(result.deferred).hasSize(2)
    }

    @Test
    fun `largest savings come first`() {
        val candidates =
            listOf(
                aCandidate(id = 1, original = mb(100), output = mb(50)),
                aCandidate(id = 2, original = mb(900), output = mb(100)),
                aCandidate(id = 3, original = mb(400), output = mb(100)),
            )

        val result = planner.planNext(candidates, freeSpace = ByteSize.gigabytes(100), reserve = reserve)

        assertThat(
            (result as NextBatch.Ready).items.map {
                it.item.id
            },
        ).containsExactly(MediaId(2), MediaId(3), MediaId(1)).inOrder()
    }

    @Test
    fun `equal savings are ordered by media ID for deterministic plans`() {
        val candidates = listOf(3L, 1L, 2L).map { aCandidate(id = it, original = mb(200), output = mb(100)) }

        val result = planner.planNext(candidates, freeSpace = ByteSize.gigabytes(100), reserve = reserve)

        assertThat((result as NextBatch.Ready).items.map { it.item.id.value }).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test
    fun `a smaller item can still fill the budget after a larger one is skipped`() {
        // Highest savings first: item 1 (cost 1,200 MB) doesn't fit 1 GB, item 2 (cost 120 MB) does.
        val candidates =
            listOf(
                aCandidate(id = 1, original = mb(5_000), output = mb(1_000)),
                aCandidate(id = 2, original = mb(500), output = mb(100)),
            )

        val result = planner.planNext(candidates, freeSpace = freeFor(ByteSize.gigabytes(1)), reserve = reserve)

        result as NextBatch.Ready
        assertThat(result.items.map { it.item.id }).containsExactly(MediaId(2))
        assertThat(result.deferred.map { it.candidate.item.id }).containsExactly(MediaId(1))
    }

    @Test
    fun `max items per batch is respected`() {
        val candidates = (1L..30L).map { aCandidate(id = it, original = mb(10), output = mb(1)) }

        val result = planner.planNext(candidates, freeSpace = ByteSize.gigabytes(100), reserve = reserve)

        result as NextBatch.Ready
        assertThat(result.items).hasSize(25)
        assertThat(result.deferred).hasSize(5)
    }

    @Test
    fun `safety factor of 1_2 is applied to the estimated output`() {
        // Output 500 MB costs 600 MB: fits a 600 MB budget, not a 599 MB one.
        val candidates = listOf(aCandidate(id = 1, original = mb(2_000), output = mb(500)))

        val fits = planner.planNext(candidates, freeSpace = freeFor(mb(600)), reserve = reserve)
        val doesNotFit = planner.planNext(candidates, freeSpace = freeFor(mb(599)), reserve = reserve)

        assertThat(fits).isInstanceOf(NextBatch.Ready::class.java)
        assertThat(doesNotFit).isInstanceOf(NextBatch.Blocked::class.java)
        assertThat(planner.costOf(candidates.single())).isEqualTo(mb(600))
    }

    @Test
    fun `free space below the reserve leaves no budget`() {
        val candidates = listOf(aCandidate(id = 1, original = mb(20), output = mb(1)))

        val result = planner.planNext(candidates, freeSpace = mb(500), reserve = reserve)

        assertThat(result).isInstanceOf(NextBatch.Blocked::class.java)
    }

    @Test
    fun `empty candidate list is an empty plan, not blocked`() {
        assertThat(
            planner.planNext(emptyList(), freeSpace = mb(500), reserve = reserve),
        ).isEqualTo(NextBatch.NoCandidates)
    }

    @Test
    fun `reserve is the larger of 1 GB and 5 percent of total storage`() {
        val cases =
            mapOf(
                // 5% of 32 GB = 1.6 GB.
                ByteSize.gigabytes(32) to mb(1_600),
                ByteSize.gigabytes(128) to mb(6_400),
                ByteSize.terabytes(1) to ByteSize.gigabytes(50),
                // 5% of 16 GB = 0.8 GB, below the 1 GB floor.
                ByteSize.gigabytes(16) to ByteSize.gigabytes(1),
            )

        cases.forEach { (total, expected) ->
            assertWithMessage(
                "total $total",
            ).that(ReservePolicy.reserveFor(total, userReserve = null)).isEqualTo(expected)
        }
    }

    @Test
    fun `user reserve overrides the default but never drops below 500 MB`() {
        assertThat(ReservePolicy.reserveFor(ByteSize.gigabytes(128), userReserve = ByteSize.gigabytes(2)))
            .isEqualTo(ByteSize.gigabytes(2))
        assertThat(ReservePolicy.reserveFor(ByteSize.gigabytes(128), userReserve = mb(100))).isEqualTo(mb(500))
    }

    private fun mb(value: Long) = ByteSize.megabytes(value)
}
