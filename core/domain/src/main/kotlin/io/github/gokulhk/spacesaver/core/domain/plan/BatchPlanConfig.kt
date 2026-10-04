package io.github.gokulhk.spacesaver.core.domain.plan

import io.github.gokulhk.spacesaver.core.model.ByteSize

/**
 * Batch planning parameters (plan Section 5.5).
 *
 * @property safetyFactor multiplier on the estimated output when reserving space for it.
 * @property maxItemsPerBatch cap on items per batch.
 */
data class BatchPlanConfig(
    val safetyFactor: Double,
    val maxItemsPerBatch: Int,
) {
    init {
        require(safetyFactor >= 1.0) { "Safety factor must be at least 1: $safetyFactor" }
        require(maxItemsPerBatch > 0) { "Batches must hold at least one item: $maxItemsPerBatch" }
    }

    /** Defaults. */
    companion object {
        /**
         * Estimates can be off (especially uncalibrated ones, ±15%), and the encoder needs scratch
         * space; 20% headroom keeps a batch from running out of space mid-way.
         */
        private const val DEFAULT_SAFETY_FACTOR = 1.2

        /** 25 items keeps the review screen manageable in one sitting. */
        private const val DEFAULT_MAX_ITEMS = 25

        /** The plan's defaults. */
        val DEFAULT = BatchPlanConfig(DEFAULT_SAFETY_FACTOR, DEFAULT_MAX_ITEMS)
    }
}

/**
 * The free space SpaceSaver never uses, so the phone keeps working while batches run
 * (plan Section 5.5).
 */
object ReservePolicy {
    /** Absolute floor of the default reserve. */
    private val DEFAULT_MIN_RESERVE = ByteSize.gigabytes(1)

    /** Lowest reserve a user may choose in Settings. */
    val MIN_USER_RESERVE: ByteSize = ByteSize.megabytes(500)

    /** Share of total storage kept free by default; Android itself warns at around 5–10%. */
    private const val DEFAULT_RESERVE_SHARE = 0.05

    /**
     * The reserve for a device with [totalStorage]: the user's choice if set (at least
     * [MIN_USER_RESERVE]), otherwise `max(1 GB, 5% of total)`.
     */
    fun reserveFor(
        totalStorage: ByteSize,
        userReserve: ByteSize?,
    ): ByteSize =
        userReserve?.coerceAtLeast(MIN_USER_RESERVE)
            ?: maxOf(DEFAULT_MIN_RESERVE, totalStorage * DEFAULT_RESERVE_SHARE)
}
