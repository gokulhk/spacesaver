package io.github.gokulhk.spacesaver.core.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.batch.FailureReason
import io.github.gokulhk.spacesaver.core.domain.batch.ItemFailure
import io.github.gokulhk.spacesaver.core.domain.eligibility.IneligibleReason
import io.github.gokulhk.spacesaver.core.domain.result.AudioProblem
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.AudioSummary
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import org.junit.Test
import org.junit.runner.RunWith

/** Real-device finding: failures and rejections said "failed" without saying why. */
@RunWith(AndroidJUnit4::class)
class ReasonMessagesTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val sizes = SizeTextFormatter(resources)

    private fun failure(
        reason: FailureReason,
        detail: String? = null,
    ) = FailureMessages.message(resources, ItemFailure(reason, detail))

    @Test
    fun `an unsupported soundtrack names its format in words people know`() {
        val ac3 = failure(FailureReason.UNSUPPORTED_AUDIO, "audio/ac3")
        assertThat(ac3).startsWith("This phone can't read the video's sound format (AC-3)")
        assertThat(ac3).endsWith("without losing the sound.")
        assertThat(failure(FailureReason.UNSUPPORTED_AUDIO, "audio/vnd.dts")).contains("DTS")
        assertThat(failure(FailureReason.UNSUPPORTED_AUDIO, "audio/x-something")).contains("X-SOMETHING")
        assertThat(failure(FailureReason.UNSUPPORTED_AUDIO)).doesNotContain("(")
    }

    @Test
    fun `an unsupported picture format is named too`() {
        assertThat(failure(FailureReason.UNSUPPORTED_VIDEO, "video/av01")).contains("AV1")
        assertThat(failure(FailureReason.UNSUPPORTED_VIDEO, "video/dolby-vision")).contains("DOLBY-VISION")
    }

    @Test
    fun `lost sound reasons say what would have been lost`() {
        assertThat(failure(FailureReason.AUDIO_TRACKS_LOST, "2/1"))
            .startsWith("This video has 2 audio tracks and compression would keep only 1")
        assertThat(failure(FailureReason.AUDIO_CHANNELS_LOST, "6/2"))
            .startsWith("Compression would reduce the sound from 6 channels to 2")
        assertThat(failure(FailureReason.AUDIO_MISSING)).contains("lose the sound")
    }

    @Test
    fun `every stored reason has a message`() {
        FailureReason.entries.forEach { assertThat(failure(it, "1/2")).isNotEmpty() }
        assertThat(failure(FailureReason.ENCODER_UNAVAILABLE, "HEIC")).isEqualTo("This phone can't save HEIC files.")
        assertThat(failure(FailureReason.ENCODER_UNAVAILABLE, "NOT_A_FORMAT")).isNotEmpty()
    }

    @Test
    fun `every reason a file can't be added has a message`() {
        IneligibleReason.entries.forEach {
            assertThat(IneligibleMessages.message(resources, it, MediaFormat.HEIC)).isNotEmpty()
        }
        assertThat(IneligibleMessages.message(resources, IneligibleReason.UNSUPPORTED_FORMAT, MediaFormat.HEIC))
            .isEqualTo("SpaceSaver doesn't convert HEIC files: they're already compact.")
    }

    @Test
    fun `audio errors reach the user through the general error mapper`() {
        val error = DomainError.AudioNotPreserved(AudioProblem.TRACKS_LOST, AudioSummary(2, 2), AudioSummary(1, 2))

        assertThat(ErrorMessageMapper.message(resources, sizes, error)).contains("2 audio tracks")
        assertThat(
            ErrorMessageMapper.message(resources, sizes, DomainError.UnsupportedAudio("audio/ac3")),
        ).contains("AC-3")
    }
}
