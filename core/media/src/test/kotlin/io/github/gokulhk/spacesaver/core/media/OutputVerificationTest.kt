package io.github.gokulhk.spacesaver.core.media

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.result.AudioProblem
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.media.output.OutputProbe
import io.github.gokulhk.spacesaver.core.media.output.OutputVerification
import io.github.gokulhk.spacesaver.core.model.AudioSummary
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Resolution
import org.junit.Test

/** Plan Section 5.8 step 3: the output decodes, keeps its orientation, size and sound, and is smaller. */
class OutputVerificationTest {
    private val original = ByteSize.megabytes(10)
    private val fullHd = Resolution(1920, 1080)

    @Test
    fun `a decodable, smaller output with the expected dimensions passes`() {
        val probe = OutputProbe(decodes = true, resolution = fullHd, size = ByteSize.megabytes(3))

        assertThat(
            OutputVerification.verify(probe, expected = fullHd, original = original),
        ).isEqualTo(DomainResult.Success(Unit))
    }

    @Test
    fun `an output that does not decode fails`() {
        val probe = OutputProbe(decodes = false, resolution = null, size = ByteSize.megabytes(3))

        assertFails(OutputVerification.verify(probe, fullHd, original), "decode")
    }

    @Test
    fun `an output that is not smaller fails`() {
        assertFails(OutputVerification.verify(OutputProbe(true, fullHd, original), fullHd, original), "smaller")
        assertFails(
            OutputVerification.verify(OutputProbe(true, fullHd, original + ByteSize(1)), fullHd, original),
            "smaller",
        )
    }

    @Test
    fun `wrong dimensions fail`() {
        val probe = OutputProbe(decodes = true, resolution = Resolution(1280, 720), size = ByteSize.megabytes(3))

        assertFails(OutputVerification.verify(probe, fullHd, original), "dimensions")
    }

    @Test
    fun `small encoder alignment differences pass`() {
        val aligned = OutputProbe(true, Resolution(1920, 1088), ByteSize.megabytes(3))

        assertThat(OutputVerification.verify(aligned, fullHd, original)).isEqualTo(DomainResult.Success(Unit))
    }

    @Test
    fun `a portrait video that comes out landscape fails, and so does the reverse`() {
        val portrait = Resolution(1080, 1920)
        val landscapeOut = OutputProbe(true, fullHd, ByteSize.megabytes(3))
        val portraitOut = OutputProbe(true, portrait, ByteSize.megabytes(3))

        assertFails(OutputVerification.verify(landscapeOut, expected = portrait, original = original), "orientation")
        assertFails(OutputVerification.verify(portraitOut, expected = fullHd, original = original), "orientation")
        assertThat(OutputVerification.verify(portraitOut, portrait, original)).isEqualTo(DomainResult.Success(Unit))
    }

    @Test
    fun `square videos have no orientation to get wrong`() {
        val square = Resolution(1080, 1080)

        assertThat(OutputVerification.verify(OutputProbe(true, square, ByteSize.megabytes(3)), square, original))
            .isEqualTo(DomainResult.Success(Unit))
    }

    private data class AudioCase(
        val source: AudioSummary,
        val output: AudioSummary,
        val problem: AudioProblem,
    )

    @Test
    fun `the soundtrack must come through intact`() {
        val stereo = AudioSummary(trackCount = 1, maxChannels = 2)
        val table =
            listOf(
                AudioCase(source = stereo, output = AudioSummary.NONE, problem = AudioProblem.MISSING),
                AudioCase(source = AudioSummary(2, 2), output = AudioSummary(1, 2), problem = AudioProblem.TRACKS_LOST),
                AudioCase(source = AudioSummary(1, 6), output = stereo, problem = AudioProblem.CHANNELS_LOST),
            )

        table.forEach { case ->
            val probe = OutputProbe(true, fullHd, ByteSize.megabytes(3), audio = case.output)

            val error = OutputVerification.verify(probe, fullHd, original, sourceAudio = case.source).errorOrNull()

            assertThat(error).isEqualTo(DomainError.AudioNotPreserved(case.problem, case.source, case.output))
        }
    }

    @Test
    fun `an intact soundtrack, a silent video, and a gained channel all pass`() {
        val stereo = AudioSummary(1, 2)
        val ok =
            listOf(
                OutputProbe(true, fullHd, ByteSize.megabytes(3), audio = stereo) to stereo,
                OutputProbe(true, fullHd, ByteSize.megabytes(3), audio = AudioSummary.NONE) to AudioSummary.NONE,
                OutputProbe(true, fullHd, ByteSize.megabytes(3), audio = stereo) to AudioSummary(1, 1),
            )

        ok.forEach { (probe, source) ->
            assertThat(
                OutputVerification.verify(probe, fullHd, original, sourceAudio = source),
            ).isEqualTo(DomainResult.Success(Unit))
        }
    }

    @Test
    fun `unknown expected dimensions skip the dimension check`() {
        val probe = OutputProbe(decodes = true, resolution = null, size = ByteSize.megabytes(3))

        assertThat(
            OutputVerification.verify(probe, expected = null, original = original),
        ).isEqualTo(DomainResult.Success(Unit))
    }

    private fun assertFails(
        result: DomainResult<Unit>,
        reasonContains: String,
    ) {
        val error = result.errorOrNull()
        assertThat(error).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
        assertThat((error as DomainError.OutputVerificationFailed).reason).contains(reasonContains)
    }
}
