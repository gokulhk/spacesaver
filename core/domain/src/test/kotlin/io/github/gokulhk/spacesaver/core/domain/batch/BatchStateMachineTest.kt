package io.github.gokulhk.spacesaver.core.domain.batch

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import org.junit.Test

class BatchStateMachineTest {
    private val machine = BatchStateMachine()

    @Test
    fun `every legal batch transition succeeds`() {
        val allTerminal = listOf(ItemStatus.ORIGINAL_DELETED, ItemStatus.OUTPUT_DISCARDED, ItemStatus.FAILED)
        val legal =
            listOf(
                Triple(BatchStatus.PLANNED, BatchEvent.StartConversion, BatchStatus.CONVERTING),
                Triple(BatchStatus.CONVERTING, BatchEvent.ConversionFinished, BatchStatus.AWAITING_REVIEW),
                Triple(BatchStatus.AWAITING_REVIEW, BatchEvent.BeginFinalizing, BatchStatus.FINALIZING),
                Triple(BatchStatus.FINALIZING, BatchEvent.FinalizingAborted, BatchStatus.AWAITING_REVIEW),
                Triple(BatchStatus.FINALIZING, BatchEvent.FinalizationFinished(allTerminal), BatchStatus.COMPLETED),
            ) +
                ACTIVE_BATCH_STATES.flatMap { state ->
                    listOf(
                        Triple(state, BatchEvent.Cancel, BatchStatus.CANCELLED),
                        Triple(state, BatchEvent.Fail, BatchStatus.FAILED),
                    )
                }

        legal.forEach { (from, event, to) ->
            assertWithMessage(
                "$from + $event",
            ).that(machine.transition(from, event)).isEqualTo(DomainResult.Success(to))
        }
    }

    @Test
    fun `illegal batch transitions fail`() {
        val illegal =
            listOf(
                BatchStatus.PLANNED to BatchEvent.ConversionFinished,
                BatchStatus.PLANNED to BatchEvent.BeginFinalizing,
                BatchStatus.CONVERTING to BatchEvent.StartConversion,
                BatchStatus.CONVERTING to BatchEvent.BeginFinalizing,
                BatchStatus.AWAITING_REVIEW to BatchEvent.ConversionFinished,
                BatchStatus.AWAITING_REVIEW to BatchEvent.FinalizingAborted,
                BatchStatus.COMPLETED to BatchEvent.StartConversion,
                BatchStatus.COMPLETED to BatchEvent.Cancel,
                BatchStatus.CANCELLED to BatchEvent.Fail,
                BatchStatus.FAILED to BatchEvent.Cancel,
            )

        illegal.forEach { (from, event) ->
            val result = machine.transition(from, event)
            assertWithMessage("$from + $event")
                .that(result.errorOrNull())
                .isInstanceOf(DomainError.InvalidTransition::class.java)
        }
    }

    @Test
    fun `cancelling the system delete dialog returns the batch to review`() {
        val result = machine.transition(BatchStatus.FINALIZING, BatchEvent.FinalizingAborted)

        assertThat(result).isEqualTo(DomainResult.Success(BatchStatus.AWAITING_REVIEW))
    }

    @Test
    fun `batch completes only when every item is terminal`() {
        val notDone = listOf(ItemStatus.ORIGINAL_DELETED, ItemStatus.ACCEPTED)

        val result = machine.transition(BatchStatus.FINALIZING, BatchEvent.FinalizationFinished(notDone))

        assertThat(result.errorOrNull()).isInstanceOf(DomainError.InvalidTransition::class.java)
    }

    @Test
    fun `every legal item transition succeeds`() {
        val legal =
            listOf(
                Triple(ItemStatus.QUEUED, ItemEvent.START_CONVERSION, ItemStatus.CONVERTING),
                Triple(ItemStatus.QUEUED, ItemEvent.SKIPPED_NO_SPACE, ItemStatus.SKIPPED_NO_SPACE),
                Triple(ItemStatus.QUEUED, ItemEvent.CANCEL, ItemStatus.CANCELLED),
                Triple(ItemStatus.CONVERTING, ItemEvent.CONVERSION_SUCCEEDED, ItemStatus.CONVERTED),
                Triple(ItemStatus.CONVERTING, ItemEvent.CONVERSION_FAILED, ItemStatus.FAILED),
                Triple(ItemStatus.CONVERTING, ItemEvent.SKIPPED_NO_SPACE, ItemStatus.SKIPPED_NO_SPACE),
                Triple(ItemStatus.CONVERTING, ItemEvent.CANCEL, ItemStatus.CANCELLED),
                Triple(ItemStatus.CONVERTING, ItemEvent.RESET, ItemStatus.QUEUED),
                Triple(ItemStatus.CONVERTED, ItemEvent.ACCEPT, ItemStatus.ACCEPTED),
                Triple(ItemStatus.CONVERTED, ItemEvent.REJECT, ItemStatus.REJECTED),
                Triple(ItemStatus.ACCEPTED, ItemEvent.REJECT, ItemStatus.REJECTED),
                Triple(ItemStatus.REJECTED, ItemEvent.ACCEPT, ItemStatus.ACCEPTED),
                Triple(ItemStatus.ACCEPTED, ItemEvent.ORIGINAL_DELETED, ItemStatus.ORIGINAL_DELETED),
                Triple(ItemStatus.ACCEPTED, ItemEvent.KEPT_BOTH, ItemStatus.KEPT_BOTH),
                Triple(ItemStatus.REJECTED, ItemEvent.OUTPUT_DISCARDED, ItemStatus.OUTPUT_DISCARDED),
            )

        legal.forEach { (from, event, to) ->
            assertWithMessage(
                "$from + $event",
            ).that(machine.transition(from, event)).isEqualTo(DomainResult.Success(to))
        }
    }

    @Test
    fun `illegal item transitions fail`() {
        val illegal =
            listOf(
                ItemStatus.QUEUED to ItemEvent.CONVERSION_SUCCEEDED,
                ItemStatus.QUEUED to ItemEvent.ACCEPT,
                ItemStatus.CONVERTING to ItemEvent.ACCEPT,
                ItemStatus.CONVERTED to ItemEvent.ORIGINAL_DELETED,
                ItemStatus.REJECTED to ItemEvent.ORIGINAL_DELETED,
                ItemStatus.REJECTED to ItemEvent.KEPT_BOTH,
                ItemStatus.ACCEPTED to ItemEvent.OUTPUT_DISCARDED,
                ItemStatus.FAILED to ItemEvent.START_CONVERSION,
                ItemStatus.ORIGINAL_DELETED to ItemEvent.REJECT,
                ItemStatus.CANCELLED to ItemEvent.START_CONVERSION,
                ItemStatus.CONVERTED to ItemEvent.RESET,
                ItemStatus.QUEUED to ItemEvent.RESET,
            )

        illegal.forEach { (from, event) ->
            assertWithMessage("$from + $event")
                .that(machine.transition(from, event).errorOrNull())
                .isInstanceOf(DomainError.InvalidTransition::class.java)
        }
    }

    @Test
    fun `terminal and active states`() {
        assertThat(ItemStatus.entries.filter { it.isTerminal })
            .containsExactly(
                ItemStatus.ORIGINAL_DELETED,
                ItemStatus.KEPT_BOTH,
                ItemStatus.OUTPUT_DISCARDED,
                ItemStatus.FAILED,
                ItemStatus.SKIPPED_NO_SPACE,
                ItemStatus.CANCELLED,
            )
        assertThat(BatchStatus.entries.filter { it.isActive }).containsExactlyElementsIn(ACTIVE_BATCH_STATES)
    }

    private companion object {
        val ACTIVE_BATCH_STATES =
            listOf(BatchStatus.PLANNED, BatchStatus.CONVERTING, BatchStatus.AWAITING_REVIEW, BatchStatus.FINALIZING)
    }
}
