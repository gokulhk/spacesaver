package io.github.gokulhk.spacesaver.core.ui

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.designsystem.component.SizeText
import io.github.gokulhk.spacesaver.core.domain.result.AudioProblem
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.AudioSummary
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class FormattersTest {
    private val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val sizes = SizeTextFormatter(resources)
    private val durations = DurationTextFormatter(resources)

    @Test
    fun `sizes are shown with SI units and spoken in full words`() {
        assertThat(sizes.format(ByteSize(12_400_000_000))).isEqualTo(SizeText("12.4 GB", "12.4 gigabytes"))
        assertThat(sizes.format(ByteSize.kilobytes(512))).isEqualTo(SizeText("512 KB", "512 kilobytes"))
        assertThat(sizes.format(ByteSize.kilobytes(1))).isEqualTo(SizeText("1 KB", "1 kilobyte"))
        assertThat(sizes.format(ByteSize(999))).isEqualTo(SizeText("999 B", "999 bytes"))
        assertThat(sizes.format(ByteSize.terabytes(2))).isEqualTo(SizeText("2.0 TB", "2.0 terabytes"))
    }

    @Test
    fun `approximate sizes say so on screen and aloud`() {
        assertThat(sizes.formatApprox(ByteSize(10_800_000_000))).isEqualTo(SizeText("~10.8 GB", "about 10.8 gigabytes"))
    }

    @Test
    fun `durations are approximate and human`() {
        assertThat(durations.formatApprox(30.seconds)).isEqualTo("less than a minute")
        assertThat(durations.formatApprox(1.minutes)).isEqualTo("about 1 min")
        assertThat(durations.formatApprox(45.minutes)).isEqualTo("about 45 min")
        assertThat(durations.formatApprox(2.hours + 10.minutes)).isEqualTo("about 2 h 10 min")
        assertThat(durations.formatApprox(3.hours)).isEqualTo("about 3 h")
    }

    @Test
    fun `every domain error has a user-facing message`() {
        val errors =
            listOf(
                DomainError.InsufficientSpace(ByteSize.gigabytes(2)),
                DomainError.EncoderUnavailable(MediaFormat.HEIC),
                DomainError.SourceUnreadable("content://x"),
                DomainError.OutputVerificationFailed("bad"),
                DomainError.UnsupportedAudio("audio/ac3"),
                DomainError.UnsupportedVideo(null),
                DomainError.AudioNotPreserved(AudioProblem.MISSING, AudioSummary(1, 2), AudioSummary.NONE),
                DomainError.NothingToConvert,
                DomainError.BatchNotFound(1),
                DomainError.PermissionMissing,
                DomainError.Cancelled,
                DomainError.InvalidTransition("A", "B"),
                DomainError.Unknown(IllegalStateException()),
            )

        val messages = errors.associateWith { ErrorMessageMapper.message(resources, sizes, it) }

        assertThat(messages.values.none { it.isBlank() }).isTrue()
        assertThat(messages.getValue(DomainError.InsufficientSpace(ByteSize.gigabytes(2))))
            .isEqualTo("Not enough free space. Free up 2.0 GB and try again.")
        assertThat(messages.getValue(DomainError.PermissionMissing))
            .isEqualTo("SpaceSaver needs access to your photos and videos.")
    }
}
