package io.github.gokulhk.spacesaver.core.model

/**
 * What a video's soundtrack consists of, measured from the file itself.
 *
 * @property trackCount how many audio tracks it has.
 * @property maxChannels the most channels in any one track (1 mono, 2 stereo, 6 for 5.1).
 */
data class AudioSummary(
    val trackCount: Int,
    val maxChannels: Int,
) {
    /** Whether there is any sound at all. */
    val hasAudio: Boolean get() = trackCount > 0

    /** Constants. */
    companion object {
        /** No audio track. */
        val NONE = AudioSummary(trackCount = 0, maxChannels = 0)
    }
}
