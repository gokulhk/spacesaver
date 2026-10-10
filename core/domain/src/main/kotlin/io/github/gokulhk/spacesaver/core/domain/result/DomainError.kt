package io.github.gokulhk.spacesaver.core.domain.result

import io.github.gokulhk.spacesaver.core.model.AudioSummary
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat

/**
 * Why a domain operation failed (plan Section 4.5). ViewModels map these to user-facing
 * strings in one place (`ErrorMessageMapper`).
 */
sealed interface DomainError {
    /**
     * Not enough free space.
     *
     * @property requiredFreeSpace how much free space the operation needs.
     */
    data class InsufficientSpace(
        val requiredFreeSpace: ByteSize,
    ) : DomainError

    /**
     * The device can't encode the target format.
     *
     * @property format the unsupported target.
     */
    data class EncoderUnavailable(
        val format: MediaFormat,
    ) : DomainError

    /**
     * The original file can't be read (moved, deleted, or corrupt).
     *
     * @property uri the original's content URI.
     */
    data class SourceUnreadable(
        val uri: String,
    ) : DomainError

    /**
     * This phone can't read the video's audio format, so the video can't be converted without
     * losing its sound.
     *
     * @property codec the audio format's MIME type, e.g. `audio/ac3`, if known.
     */
    data class UnsupportedAudio(
        val codec: String?,
    ) : DomainError

    /**
     * This phone can't read the video's picture format.
     *
     * @property codec the video format's MIME type, e.g. `video/av01`, if known.
     */
    data class UnsupportedVideo(
        val codec: String?,
    ) : DomainError

    /**
     * Converting would lose part of the soundtrack, so the original is kept.
     *
     * @property problem what would be lost.
     * @property source the original's soundtrack.
     * @property output the converted file's soundtrack (or what it would have).
     */
    data class AudioNotPreserved(
        val problem: AudioProblem,
        val source: AudioSummary,
        val output: AudioSummary,
    ) : DomainError

    /**
     * A converted output failed verification (doesn't decode, wrong dimensions, or not smaller).
     *
     * @property reason a developer-facing description.
     */
    data class OutputVerificationFailed(
        val reason: String,
    ) : DomainError

    /** There is nothing to convert. */
    data object NothingToConvert : DomainError

    /**
     * No batch with this ID exists.
     *
     * @property batchId the missing batch's ID.
     */
    data class BatchNotFound(
        val batchId: Long,
    ) : DomainError

    /** Media access permission is missing. */
    data object PermissionMissing : DomainError

    /** The user or system cancelled the operation. */
    data object Cancelled : DomainError

    /**
     * A batch or item state change that the lifecycle doesn't allow (a programming error).
     *
     * @property from the current state.
     * @property event the rejected event.
     */
    data class InvalidTransition(
        val from: String,
        val event: String,
    ) : DomainError

    /**
     * Anything unexpected.
     *
     * @property cause the underlying exception.
     */
    data class Unknown(
        val cause: Throwable,
    ) : DomainError
}

/** How a converted video's soundtrack fell short of the original's. */
enum class AudioProblem {
    /** The original has sound; the converted file has none. */
    MISSING,

    /** The original has more audio tracks than the converted file. */
    TRACKS_LOST,

    /** The converted file has fewer channels (e.g. surround reduced to stereo). */
    CHANNELS_LOST,
}
