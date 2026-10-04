package io.github.gokulhk.spacesaver.core.domain.savings

import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaId
import java.time.Instant

/** How space was freed. */
enum class SavingsType {
    /** An original was permanently deleted after a successful conversion. */
    CONVERSION,

    /** The user deleted a file from Browse. */
    DELETION,
}

/**
 * A ledger entry, recorded only when space is actually freed (plan Section 5.2).
 *
 * @property type how the space was freed.
 * @property bytesSaved space freed; always positive.
 * @property timestamp when it was freed.
 * @property mediaId the original file, if known.
 */
data class SavingsEvent(
    val type: SavingsType,
    val bytesSaved: ByteSize,
    val timestamp: Instant,
    val mediaId: MediaId?,
)

/**
 * Space saved, as shown in the banner.
 *
 * @property lifetime sum of every event since install.
 * @property today sum of events since local midnight.
 */
data class SavingsSummary(
    val lifetime: ByteSize,
    val today: ByteSize,
) {
    /** Constants. */
    companion object {
        /** Nothing saved yet. */
        val ZERO = SavingsSummary(ByteSize.ZERO, ByteSize.ZERO)
    }
}

/**
 * Sizes of one converted file.
 *
 * @property mediaId the original.
 * @property original the original's size.
 * @property output the converted output's size.
 */
data class ConvertedSizes(
    val mediaId: MediaId,
    val original: ByteSize,
    val output: ByteSize,
)
