package io.github.gokulhk.spacesaver.core.domain.batch

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.result.AudioProblem
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.AudioSummary
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import org.junit.Test

class ItemFailureTest {
    private val stereo = AudioSummary(trackCount = 1, maxChannels = 2)

    @Test
    fun `every error that can fail a file maps to a stored reason with the detail that explains it`() {
        val table =
            listOf(
                DomainError.SourceUnreadable("content://x") to ItemFailure(FailureReason.SOURCE_UNREADABLE),
                DomainError.UnsupportedAudio("audio/ac3") to ItemFailure(FailureReason.UNSUPPORTED_AUDIO, "audio/ac3"),
                DomainError.UnsupportedVideo(
                    "video/av01",
                ) to ItemFailure(FailureReason.UNSUPPORTED_VIDEO, "video/av01"),
                DomainError.EncoderUnavailable(MediaFormat.HEIC) to
                    ItemFailure(FailureReason.ENCODER_UNAVAILABLE, "HEIC"),
                DomainError.AudioNotPreserved(AudioProblem.TRACKS_LOST, AudioSummary(2, 2), stereo) to
                    ItemFailure(FailureReason.AUDIO_TRACKS_LOST, "2/1"),
                DomainError.AudioNotPreserved(AudioProblem.CHANNELS_LOST, AudioSummary(1, 6), stereo) to
                    ItemFailure(FailureReason.AUDIO_CHANNELS_LOST, "6/2"),
                DomainError.AudioNotPreserved(AudioProblem.MISSING, stereo, AudioSummary.NONE) to
                    ItemFailure(FailureReason.AUDIO_MISSING),
                DomainError.OutputVerificationFailed(
                    "developer text",
                ) to ItemFailure(FailureReason.VERIFICATION_FAILED),
                DomainError.InsufficientSpace(ByteSize.gigabytes(1)) to ItemFailure(FailureReason.UNKNOWN),
                DomainError.Unknown(IllegalStateException("boom")) to ItemFailure(FailureReason.UNKNOWN),
            )

        table.forEach { (error, expected) -> assertThat(error.toItemFailure()).isEqualTo(expected) }
    }

    @Test
    fun `a count pair in the detail can be read back`() {
        assertThat(ItemFailure(FailureReason.AUDIO_TRACKS_LOST, "2/1").counts()).isEqualTo(2 to 1)
        assertThat(ItemFailure(FailureReason.AUDIO_TRACKS_LOST, "nonsense").counts()).isNull()
        assertThat(ItemFailure(FailureReason.UNKNOWN).counts()).isNull()
    }

    @Test
    fun `stored reason names that no longer exist read as unknown`() {
        assertThat(FailureReason.fromStored("UNSUPPORTED_AUDIO")).isEqualTo(FailureReason.UNSUPPORTED_AUDIO)
        assertThat(FailureReason.fromStored("RETIRED_REASON")).isEqualTo(FailureReason.UNKNOWN)
    }
}
