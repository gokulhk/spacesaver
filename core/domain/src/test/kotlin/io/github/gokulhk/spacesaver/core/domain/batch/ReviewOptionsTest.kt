package io.github.gokulhk.spacesaver.core.domain.batch

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanConfig
import io.github.gokulhk.spacesaver.core.domain.plan.BatchPlanner
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.aCandidate
import org.junit.Test

class ReviewOptionsTest {
    private val options = ReviewOptions(BatchPlanner(BatchPlanConfig.DEFAULT))
    private val reserve = ByteSize.gigabytes(1)
    private val next =
        listOf(aCandidate(id = 9, original = ByteSize.megabytes(1_000), output = ByteSize.megabytes(250)))

    @Test
    fun `keep both is offered when the next item still fits`() {
        val actions = options.availableActions(freeSpace = ByteSize.gigabytes(2), reserve = reserve, remaining = next)

        assertThat(actions).containsExactly(
            ReviewAction.DELETE_ORIGINALS_AND_CONTINUE,
            ReviewAction.KEEP_BOTH_AND_CONTINUE,
            ReviewAction.STOP_HERE,
        )
    }

    @Test
    fun `keep both is unavailable when the budget fits nothing`() {
        val actions =
            options.availableActions(
                freeSpace = ByteSize.megabytes(1_200),
                reserve = reserve,
                remaining = next,
            )

        assertThat(actions).containsExactly(ReviewAction.DELETE_ORIGINALS_AND_CONTINUE, ReviewAction.STOP_HERE)
    }

    @Test
    fun `keep both is unavailable when nothing is left to convert`() {
        val actions =
            options.availableActions(
                freeSpace = ByteSize.gigabytes(50),
                reserve = reserve,
                remaining = emptyList(),
            )

        assertThat(actions).doesNotContain(ReviewAction.KEEP_BOTH_AND_CONTINUE)
    }
}
