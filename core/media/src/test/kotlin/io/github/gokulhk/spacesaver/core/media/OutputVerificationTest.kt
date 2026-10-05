package io.github.gokulhk.spacesaver.core.media

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.DomainResult
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.media.output.OutputProbe
import io.github.gokulhk.spacesaver.core.media.output.OutputVerification
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.Resolution
import org.junit.Test

/** Plan Section 5.8 step 3: the output decodes, has the expected dimensions, and is smaller. */
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
    fun `rotated output and small encoder alignment differences pass`() {
        val rotated = OutputProbe(true, Resolution(1080, 1920), ByteSize.megabytes(3))
        val aligned = OutputProbe(true, Resolution(1920, 1088), ByteSize.megabytes(3))

        assertThat(OutputVerification.verify(rotated, fullHd, original)).isEqualTo(DomainResult.Success(Unit))
        assertThat(OutputVerification.verify(aligned, fullHd, original)).isEqualTo(DomainResult.Success(Unit))
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
