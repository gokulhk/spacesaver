package io.github.gokulhk.spacesaver.core.domain.batch

/** Lifecycle of a batch (plan Section 5.7). */
enum class BatchStatus(
    /** Whether the batch can still change; CANCELLED and FAILED are reachable from these. */
    val isActive: Boolean,
) {
    /** Created, not started. */
    PLANNED(isActive = true),

    /** Items are being converted. */
    CONVERTING(isActive = true),

    /** Conversion finished; waiting for the user to review results. Also the "Stop here" state. */
    AWAITING_REVIEW(isActive = true),

    /** Applying the review: deleting originals or discarded outputs. */
    FINALIZING(isActive = true),

    /** Every item reached a terminal state. */
    COMPLETED(isActive = false),

    /** Cancelled by the user. */
    CANCELLED(isActive = false),

    /** Stopped by an unrecoverable error. */
    FAILED(isActive = false),
}

/** Lifecycle of one item in a batch (plan Section 5.7). */
enum class ItemStatus(
    /** Whether the item is finished and won't change again. */
    val isTerminal: Boolean,
) {
    /** Waiting to be converted. */
    QUEUED(isTerminal = false),

    /** Being converted. */
    CONVERTING(isTerminal = false),

    /** Converted and verified; not yet reviewed. */
    CONVERTED(isTerminal = false),

    /** The user accepted the output (the default). */
    ACCEPTED(isTerminal = false),

    /** The user rejected the output. */
    REJECTED(isTerminal = false),

    /** Output kept, original permanently deleted; space saved. */
    ORIGINAL_DELETED(isTerminal = true),

    /** Output and original both kept; no space saved. */
    KEPT_BOTH(isTerminal = true),

    /** Rejected output deleted; original untouched. */
    OUTPUT_DISCARDED(isTerminal = true),

    /** Conversion or verification failed; original untouched. */
    FAILED(isTerminal = true),

    /** Skipped because free space fell below the reserve; original untouched. */
    SKIPPED_NO_SPACE(isTerminal = true),

    /** The batch was cancelled before this item finished; original untouched. */
    CANCELLED(isTerminal = true),
}

/** Something that happens to a batch. */
sealed interface BatchEvent {
    /** Conversion work starts. */
    data object StartConversion : BatchEvent

    /** Every item has been processed. */
    data object ConversionFinished : BatchEvent

    /** The user picked "Delete originals" or "Keep both". */
    data object BeginFinalizing : BatchEvent

    /** The user cancelled the system delete dialog: nothing changed, back to review. */
    data object FinalizingAborted : BatchEvent

    /**
     * Finalization finished.
     *
     * @property itemStatuses every item's status; all must be terminal to complete.
     */
    data class FinalizationFinished(
        val itemStatuses: Collection<ItemStatus>,
    ) : BatchEvent

    /** The user cancelled the batch. */
    data object Cancel : BatchEvent

    /** An unrecoverable error occurred. */
    data object Fail : BatchEvent
}

/** Something that happens to an item. */
enum class ItemEvent {
    /** Its conversion starts. */
    START_CONVERSION,

    /** Converted and verified. */
    CONVERSION_SUCCEEDED,

    /** Conversion or verification failed. */
    CONVERSION_FAILED,

    /** Free space fell below the reserve. */
    SKIPPED_NO_SPACE,

    /** The batch was cancelled. */
    CANCEL,

    /** The user accepted the output. */
    ACCEPT,

    /** The user rejected the output. */
    REJECT,

    /** The original was permanently deleted. */
    ORIGINAL_DELETED,

    /** The user chose to keep both files. */
    KEPT_BOTH,

    /** The rejected output was deleted. */
    OUTPUT_DISCARDED,
}
