package io.github.gokulhk.spacesaver.core.ui

import android.content.res.Resources
import io.github.gokulhk.spacesaver.core.domain.batch.FailureReason
import io.github.gokulhk.spacesaver.core.domain.batch.ItemFailure
import io.github.gokulhk.spacesaver.core.domain.eligibility.IneligibleReason
import io.github.gokulhk.spacesaver.core.model.MediaFormat

/** Audio and video formats people know by name; anything else is shown by its MIME subtype. */
private val KNOWN_CODECS =
    mapOf(
        "audio/ac3" to "AC-3",
        "audio/eac3" to "E-AC-3",
        "audio/eac3-joc" to "Dolby Atmos",
        "audio/vnd.dts" to "DTS",
        "audio/vnd.dts.hd" to "DTS-HD",
        "audio/true-hd" to "Dolby TrueHD",
        "audio/opus" to "Opus",
        "audio/vorbis" to "Vorbis",
        "audio/flac" to "FLAC",
        "audio/mpeg" to "MP3",
        "video/av01" to "AV1",
        "video/x-vnd.on2.vp9" to "VP9",
        "video/x-vnd.on2.vp8" to "VP8",
        "video/hevc" to "HEVC",
        "video/avc" to "H.264",
        "video/mp4v-es" to "MPEG-4",
    )

/** A codec's name as people say it: `audio/ac3` is "AC-3"; an unfamiliar one is its uppercased subtype. */
internal fun codecLabel(mime: String): String = KNOWN_CODECS[mime] ?: mime.substringAfter('/').uppercase()

/** The sentence explaining why a file in a batch wasn't converted (shown on the progress screen). */
object FailureMessages {
    /** The explanation for [failure], in the user's language. */
    fun message(
        resources: Resources,
        failure: ItemFailure,
    ): String =
        when (failure.reason) {
            FailureReason.UNSUPPORTED_AUDIO, FailureReason.UNSUPPORTED_VIDEO -> {
                codecMessage(resources, failure)
            }

            FailureReason.AUDIO_TRACKS_LOST, FailureReason.AUDIO_CHANNELS_LOST -> {
                countMessage(resources, failure)
            }

            FailureReason.ENCODER_UNAVAILABLE -> {
                encoder(resources, failure.detail)
            }

            else -> {
                resources.getString(failure.reason.plainMessage)
            }
        }

    /** The reasons whose sentence needs no detail. */
    private val FailureReason.plainMessage: Int
        get() =
            when (this) {
                FailureReason.SOURCE_UNREADABLE -> R.string.failure_source_unreadable
                FailureReason.AUDIO_MISSING -> R.string.failure_audio_missing
                FailureReason.VERIFICATION_FAILED -> R.string.failure_verification_failed
                else -> R.string.failure_unknown
            }

    /** "This phone can't read the video's sound format (AC-3)…", or the same without the name. */
    private fun codecMessage(
        resources: Resources,
        failure: ItemFailure,
    ): String {
        val audio = failure.reason == FailureReason.UNSUPPORTED_AUDIO
        val named = if (audio) R.string.failure_unsupported_audio else R.string.failure_unsupported_video
        val unnamed =
            if (audio) R.string.failure_unsupported_audio_unknown else R.string.failure_unsupported_video_unknown
        val mime = failure.detail ?: return resources.getString(unnamed)
        return resources.getString(named, codecLabel(mime))
    }

    /** "This video has 2 audio tracks…" / "…from 6 channels to 2…", pluralised by the source count. */
    private fun countMessage(
        resources: Resources,
        failure: ItemFailure,
    ): String {
        val tracks = failure.reason == FailureReason.AUDIO_TRACKS_LOST
        val unnamed =
            if (tracks) R.string.failure_audio_tracks_lost_unknown else R.string.failure_audio_channels_lost_unknown
        val (source, output) = failure.counts() ?: return resources.getString(unnamed)
        val plural = if (tracks) R.plurals.failure_audio_tracks_lost else R.plurals.failure_audio_channels_lost
        return resources.getQuantityString(plural, source, source, output)
    }

    private fun encoder(
        resources: Resources,
        detail: String?,
    ): String {
        val format = MediaFormat.entries.firstOrNull { it.name == detail }
        return if (format == null) {
            resources.getString(R.string.failure_encoder_unavailable_unknown)
        } else {
            resources.getString(R.string.failure_encoder_unavailable, resources.getString(format.label))
        }
    }
}

/** The sentence explaining why a file can't be added to the plan (shown by Browse's "Convert"). */
object IneligibleMessages {
    /**
     * The explanation for [reason].
     *
     * @param format the file's format, named in the message for formats SpaceSaver doesn't convert.
     */
    fun message(
        resources: Resources,
        reason: IneligibleReason,
        format: MediaFormat,
    ): String =
        when (reason) {
            IneligibleReason.UNSUPPORTED_FORMAT -> {
                resources.getString(R.string.ineligible_unsupported_format, resources.getString(format.label))
            }

            else -> {
                resources.getString(reason.plainMessage)
            }
        }

    /** The reasons whose sentence needs nothing from the file. */
    private val IneligibleReason.plainMessage: Int
        get() =
            when (this) {
                IneligibleReason.WRONG_MEDIA_TYPE -> R.string.ineligible_wrong_media_type
                IneligibleReason.PRODUCED_BY_SPACESAVER -> R.string.ineligible_produced_by_spacesaver
                IneligibleReason.BELOW_PRESET_RESOLUTION -> R.string.ineligible_below_preset_resolution
                IneligibleReason.MISSING_METADATA -> R.string.ineligible_missing_metadata
                IneligibleReason.SAVINGS_TOO_SMALL -> R.string.ineligible_savings_too_small
                IneligibleReason.ALREADY_HANDLED -> R.string.ineligible_already_handled
                IneligibleReason.UNSUPPORTED_FORMAT -> R.string.ineligible_wrong_media_type
            }
}
