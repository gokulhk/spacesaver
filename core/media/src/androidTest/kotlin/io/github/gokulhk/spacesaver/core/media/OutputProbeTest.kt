package io.github.gokulhk.spacesaver.core.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputProbe
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.media.output.OutputVerification
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Task 4.6: the probe decodes real files; truncated ones fail verification. */
@RunWith(AndroidJUnit4::class)
class OutputProbeTest {
    private val fixtures = MediaFixtures()
    private val probe = AndroidOutputProbe(fixtures.resolver, Dispatchers.IO)
    private val writer = MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO)

    @After
    fun cleanUp() = fixtures.cleanUp()

    @Test
    fun validImageDecodesWithItsDimensions() =
        runTest {
            val photo = fixtures.photoJpeg()

            val result = probe.probe(photo.uri, MediaType.IMAGE)

            assertThat(result.decodes).isTrue()
            assertThat(result.resolution).isEqualTo(Resolution(1600, 1200))
            assertThat(result.size).isEqualTo(photo.size)
        }

    @Test
    fun validVideoDecodesItsFirstFrame() =
        runTest {
            val video = fixtures.video1080p()

            val result = probe.probe(video.uri, MediaType.VIDEO)

            assertThat(result.decodes).isTrue()
            assertThat(result.resolution).isEqualTo(Resolution(1920, 1080))
        }

    @Test
    fun truncatedImageFailsVerification() =
        runTest {
            val photo = fixtures.photoJpeg()
            val truncated = truncatedCopy(photo.uri, MediaFormat.JPEG, photo)

            val result = probe.probe(truncated, MediaType.IMAGE)

            assertThat(OutputVerification.verify(result, Resolution(1600, 1200), photo.size).errorOrNull())
                .isInstanceOf(DomainError.OutputVerificationFailed::class.java)
        }

    @Test
    fun truncatedVideoFailsVerification() =
        runTest {
            val video = fixtures.video1080p()
            val truncated = truncatedCopy(video.uri, MediaFormat.MP4_H264, video)

            val result = probe.probe(truncated, MediaType.VIDEO)

            assertThat(OutputVerification.verify(result, Resolution(1920, 1080), video.size).errorOrNull())
                .isInstanceOf(DomainError.OutputVerificationFailed::class.java)
        }

    @Test
    fun outputLargerThanTheOriginalFailsVerification() =
        runTest {
            val photo = fixtures.photoJpeg()

            val result = probe.probe(photo.uri, MediaType.IMAGE)

            assertThat(
                OutputVerification.verify(result, Resolution(1600, 1200), photo.size - ByteSize(1)).errorOrNull(),
            ).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
        }

    /** Writes the first 40% of [uri]'s bytes to a new pending output. */
    private suspend fun truncatedCopy(
        uri: String,
        format: MediaFormat,
        original: io.github.gokulhk.spacesaver.core.model.MediaItem,
    ): String {
        val bytes = fixtures.resolver.openInputStream(android.net.Uri.parse(uri))!!.use { it.readBytes() }
        val output = writer.createPending(original, format).also { fixtures.track(it.uri) }
        writer.openForWrite(output).use { it.write(bytes.copyOf(bytes.size * 2 / 5)) }
        return output.uri
    }
}
