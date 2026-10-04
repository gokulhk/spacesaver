package io.github.gokulhk.spacesaver.core.domain.plan

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.sum
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class PlanSimulatorTest {
    private val simulator = PlanSimulator(BatchPlanner(BatchPlanConfig.DEFAULT))
    private val reserve = ByteSize.gigabytes(1)

    @Test
    fun `free space grows after each batch so later batches are larger`() {
        // Each item: 1,000 MB original, 250 MB output, 300 MB cost. Budget starts at 600 MB.
        val candidates = (1L..10L).map { aCandidate(id = it, original = mb(1_000), output = mb(250)) }

        val plan =
            simulator.simulate(
                candidates,
                freeSpace = mb(1_600),
                reserve = reserve,
                speed = ProcessingSpeed.DEFAULT,
            )

        // Batch 1 frees 2 x 750 MB: budget 2,100 MB fits 7. Then 1 left.
        assertThat(plan.batches.map { it.items.size }).containsExactly(2, 7, 1).inOrder()
        assertThat(plan.blocked).isEmpty()
    }

    @Test
    fun `items that never fit are reported as blocked`() {
        val candidates =
            listOf(
                aCandidate(id = 1, original = mb(400), output = mb(100)),
                aCandidate(id = 2, original = mb(12_000), output = mb(10_000)),
            )

        val plan =
            simulator.simulate(
                candidates,
                freeSpace = mb(1_500),
                reserve = reserve,
                speed = ProcessingSpeed.DEFAULT,
            )

        assertThat(
            plan.batches
                .single()
                .items
                .map { it.item.id },
        ).containsExactly(MediaId(1))
        assertThat(plan.blocked.map { it.candidate.item.id }).containsExactly(MediaId(2))
        // 10,000 MB x 1.2 + 1 GB reserve.
        assertThat(plan.blocked.single().requiredFreeSpace).isEqualTo(mb(13_000))
    }

    @Test
    fun `everything blocked from the start gives no batches`() {
        val candidates = listOf(aCandidate(id = 1, original = mb(12_000), output = mb(10_000)))

        val plan =
            simulator.simulate(
                candidates,
                freeSpace = mb(1_500),
                reserve = reserve,
                speed = ProcessingSpeed.DEFAULT,
            )

        assertThat(plan.batches).isEmpty()
        assertThat(plan.blocked).hasSize(1)
        assertThat(plan.totalEstimatedSavings).isEqualTo(ByteSize.ZERO)
    }

    @Test
    fun `totals equal the sum of the batches`() {
        val candidates = (1L..40L).map { aCandidate(id = it, original = mb(100 + it * 10), output = mb(40)) }

        val plan =
            simulator.simulate(
                candidates,
                freeSpace = ByteSize.gigabytes(3),
                reserve = reserve,
                speed = ProcessingSpeed.DEFAULT,
            )

        assertThat(plan.batches.size).isGreaterThan(1)
        assertThat(plan.totalEstimatedSavings).isEqualTo(plan.batches.map { it.estimatedSavings }.sum())
        assertThat(plan.totalEstimatedDuration).isEqualTo(
            plan.batches.fold(0.seconds) { acc, b ->
                acc +
                    b.estimatedDuration
            },
        )
        assertThat(plan.batches.sumOf { it.items.size }).isEqualTo(40)
    }

    @Test
    fun `batch savings and space needed come from its items`() {
        val candidates = listOf(aCandidate(id = 1, original = mb(1_000), output = mb(250)))

        val batch =
            simulator
                .simulate(
                    candidates,
                    ByteSize.gigabytes(10),
                    reserve,
                    ProcessingSpeed.DEFAULT,
                ).batches
                .single()

        assertThat(batch.estimatedSavings).isEqualTo(mb(750))
        assertThat(batch.spaceNeeded).isEqualTo(mb(300))
    }

    @Test
    fun `time estimate uses throughput factors per media type`() {
        val candidates =
            listOf(
                aCandidate(id = 1, original = mb(1_000), output = mb(200), type = MediaType.VIDEO, durationSec = 120),
                aCandidate(id = 2, original = mb(10), output = mb(5)),
                aCandidate(id = 3, original = mb(10), output = mb(5)),
            )
        val speed = ProcessingSpeed(videoSecondsPerFootageSecond = 0.5, secondsPerImage = 2.0)

        val plan = simulator.simulate(candidates, ByteSize.gigabytes(10), reserve, speed)

        // 120 s of footage x 0.5 + 2 images x 2 s.
        assertThat(plan.totalEstimatedDuration).isEqualTo(64.seconds)
    }

    @Test
    fun `same input gives the same plan, whatever the input order`() {
        val candidates =
            (1L..30L).map {
                aCandidate(
                    id = it,
                    original = mb(100 + (it % 7) * 50),
                    output =
                        mb(30 + (it % 3) * 10),
                )
            }

        val first = simulator.simulate(candidates, ByteSize.gigabytes(2), reserve, ProcessingSpeed.DEFAULT)
        val second =
            simulator.simulate(
                candidates.shuffled(Random(7)),
                ByteSize.gigabytes(2),
                reserve,
                ProcessingSpeed.DEFAULT,
            )

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `no candidates gives an empty plan`() {
        val plan = simulator.simulate(emptyList(), ByteSize.gigabytes(2), reserve, ProcessingSpeed.DEFAULT)

        assertThat(plan.batches).isEmpty()
        assertThat(plan.blocked).isEmpty()
        assertThat(plan.isEmpty).isTrue()
    }

    private fun mb(value: Long) = ByteSize.megabytes(value)
}
