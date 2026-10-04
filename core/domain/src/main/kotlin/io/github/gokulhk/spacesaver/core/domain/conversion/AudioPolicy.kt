package io.github.gokulhk.spacesaver.core.domain.conversion

import io.github.gokulhk.spacesaver.core.model.AudioTrack
import io.github.gokulhk.spacesaver.core.model.Bitrate

/** How a video's audio track is handled during conversion (plan Section 5.3). */
enum class AudioPolicy {
    /** Copy the AAC track unchanged: no quality loss, no time spent. */
    PASS_THROUGH,

    /** Re-encode to AAC at [REENCODE_BITRATE]: MP4 players universally support AAC. */
    REENCODE_AAC,

    /** The video has no audio track. */
    NONE,
    ;

    /** Policy, rates, and selection. */
    companion object {
        /** AAC bitrate for re-encoded audio; transparent for stereo phone recordings. */
        val REENCODE_BITRATE: Bitrate = Bitrate.kbps(128)

        /** Assumed bitrate of an AAC track whose bitrate the metadata doesn't report. */
        val ASSUMED_AAC_BITRATE: Bitrate = Bitrate.kbps(128)

        /** Chooses the policy for [track]; null means no audio. */
        fun forTrack(track: AudioTrack?): AudioPolicy =
            when {
                track == null -> NONE
                track.isAac -> PASS_THROUGH
                else -> REENCODE_AAC
            }

        /** The output audio bitrate for [track] under its policy. */
        fun outputBitrate(track: AudioTrack?): Bitrate =
            when (forTrack(track)) {
                NONE -> Bitrate(0)
                PASS_THROUGH -> track?.bitrate ?: ASSUMED_AAC_BITRATE
                REENCODE_AAC -> REENCODE_BITRATE
            }
    }
}
