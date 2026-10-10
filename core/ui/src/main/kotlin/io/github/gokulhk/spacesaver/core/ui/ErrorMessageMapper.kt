package io.github.gokulhk.spacesaver.core.ui

import android.content.res.Resources
import io.github.gokulhk.spacesaver.core.domain.batch.toItemFailure
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.MediaFormat

/** The one place where a [DomainError] becomes text the user reads (plan Section 4.5). */
object ErrorMessageMapper {
    /** A user-facing message for [error]. Developer details (reasons, causes) are never shown. */
    fun message(
        resources: Resources,
        sizes: SizeTextFormatter,
        error: DomainError,
    ): String =
        when (error) {
            is DomainError.InsufficientSpace -> {
                resources.getString(R.string.error_insufficient_space, sizes.format(error.requiredFreeSpace).display)
            }

            // Everything that can fail a single file reads the same here as on the progress screen.
            is DomainError.EncoderUnavailable,
            is DomainError.SourceUnreadable,
            is DomainError.UnsupportedAudio,
            is DomainError.UnsupportedVideo,
            is DomainError.AudioNotPreserved,
            is DomainError.OutputVerificationFailed,
            -> {
                FailureMessages.message(resources, error.toItemFailure())
            }

            DomainError.NothingToConvert -> {
                resources.getString(R.string.error_nothing_to_convert)
            }

            is DomainError.BatchNotFound -> {
                resources.getString(R.string.error_batch_not_found)
            }

            DomainError.PermissionMissing -> {
                resources.getString(R.string.error_permission_missing)
            }

            DomainError.Cancelled -> {
                resources.getString(R.string.error_cancelled)
            }

            is DomainError.InvalidTransition, is DomainError.Unknown -> {
                resources.getString(R.string.error_unknown)
            }
        }
}

/** The format's name as people know it ("HEIC", "WebP", "H.264"). */
val MediaFormat.label: Int
    get() =
        when (this) {
            MediaFormat.JPEG -> R.string.format_jpeg
            MediaFormat.PNG -> R.string.format_png
            MediaFormat.WEBP_LOSSY, MediaFormat.WEBP_LOSSLESS -> R.string.format_webp
            MediaFormat.HEIC -> R.string.format_heic
            MediaFormat.AVIF -> R.string.format_avif
            MediaFormat.GIF -> R.string.format_gif
            MediaFormat.MP4_H264 -> R.string.format_h264
            MediaFormat.MP4_HEVC -> R.string.format_hevc
            MediaFormat.VIDEO_OTHER -> R.string.format_video_other
        }
