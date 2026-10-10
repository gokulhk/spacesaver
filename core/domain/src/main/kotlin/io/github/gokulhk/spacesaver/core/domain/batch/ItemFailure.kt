package io.github.gokulhk.spacesaver.core.domain.batch

import io.github.gokulhk.spacesaver.core.domain.result.AudioProblem
import io.github.gokulhk.spacesaver.core.domain.result.DomainError

/** Why a file in a batch wasn't converted. Stored by name, so never rename or reuse a constant. */
enum class FailureReason {
    /** The original can't be read (moved, deleted, or corrupt). */
    SOURCE_UNREADABLE,

    /** This phone can't read the video's audio format. */
    UNSUPPORTED_AUDIO,

    /** This phone can't read the video's picture format. */
    UNSUPPORTED_VIDEO,

    /** This phone can't write the target format. */
    ENCODER_UNAVAILABLE,

    /** Converting would have dropped the video's sound. */
    AUDIO_MISSING,

    /** Converting would have dropped some of the video's audio tracks. */
    AUDIO_TRACKS_LOST,

    /** Converting would have reduced the audio channels. */
    AUDIO_CHANNELS_LOST,

    /** The converted file failed the safety checks, so the original was kept. */
    VERIFICATION_FAILED,

    /** Anything else, including reasons stored by a newer version. */
    UNKNOWN,
    ;

    /** Reading stored values. */
    companion object {
        /** The reason stored as [name]; [UNKNOWN] if this version doesn't know it. */
        fun fromStored(name: String): FailureReason = entries.firstOrNull { it.name == name } ?: UNKNOWN
    }
}

/**
 * Why one file failed, in a form that is stored with the item and shown on the progress screen.
 *
 * @property reason the category.
 * @property detail what explains it: a codec (`audio/ac3`), a `MediaFormat` name, or `"source/output"`
 * counts for audio tracks and channels.
 */
data class ItemFailure(
    val reason: FailureReason,
    val detail: String? = null,
) {
    /** The `source/output` numbers in [detail], for the audio reasons; null if absent or malformed. */
    fun counts(): Pair<Int, Int>? {
        val numbers = detail?.split(COUNT_SEPARATOR)?.map { it.toIntOrNull() }
        return if (numbers?.size == 2 && numbers.all { it != null }) numbers[0]!! to numbers[1]!! else null
    }

    private companion object {
        const val COUNT_SEPARATOR = "/"
    }
}

/** The stored form of this error. Errors that don't describe a single file's failure read as unknown. */
fun DomainError.toItemFailure(): ItemFailure =
    when (this) {
        is DomainError.SourceUnreadable -> {
            ItemFailure(FailureReason.SOURCE_UNREADABLE)
        }

        is DomainError.UnsupportedAudio -> {
            ItemFailure(FailureReason.UNSUPPORTED_AUDIO, codec)
        }

        is DomainError.UnsupportedVideo -> {
            ItemFailure(FailureReason.UNSUPPORTED_VIDEO, codec)
        }

        is DomainError.EncoderUnavailable -> {
            ItemFailure(
                FailureReason.ENCODER_UNAVAILABLE,
                format.name.removePrefix("WEBP_").removePrefix("MP4_"),
            )
        }

        is DomainError.OutputVerificationFailed -> {
            ItemFailure(FailureReason.VERIFICATION_FAILED)
        }

        is DomainError.AudioNotPreserved -> {
            audioFailure()
        }

        else -> {
            ItemFailure(FailureReason.UNKNOWN)
        }
    }

private fun DomainError.AudioNotPreserved.audioFailure(): ItemFailure =
    when (problem) {
        AudioProblem.MISSING -> {
            ItemFailure(FailureReason.AUDIO_MISSING)
        }

        AudioProblem.TRACKS_LOST -> {
            ItemFailure(
                FailureReason.AUDIO_TRACKS_LOST,
                "${source.trackCount}/${output.trackCount}",
            )
        }

        AudioProblem.CHANNELS_LOST -> {
            ItemFailure(
                FailureReason.AUDIO_CHANNELS_LOST,
                "${source.maxChannels}/${output.maxChannels}",
            )
        }
    }
