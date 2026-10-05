package io.github.gokulhk.spacesaver.core.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionInput
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.domain.result.errorOrNull
import io.github.gokulhk.spacesaver.core.domain.result.getOrNull
import io.github.gokulhk.spacesaver.core.media.image.WebpImageConverter
import io.github.gokulhk.spacesaver.core.media.metadata.ExifMetadataCopier
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputGateway
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputProbe
import io.github.gokulhk.spacesaver.core.media.output.MediaStoreOutputWriter
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/** Task 5.1 support: verify, publish, discard, and list pending outputs on a real MediaStore. */
@RunWith(AndroidJUnit4::class)
class AndroidOutputGatewayTest {
    private val fixtures = MediaFixtures()
    private val writer = MediaStoreOutputWriter(fixtures.resolver, Dispatchers.IO)
    private val gateway =
        AndroidOutputGateway(
            fixtures.context,
            writer,
            AndroidOutputProbe(fixtures.resolver, Dispatchers.IO),
            Dispatchers.IO,
        )
    private val webp =
        WebpImageConverter(fixtures.context, writer, ExifMetadataCopier(fixtures.context), Dispatchers.Default)
    private val spec = ConversionSpec.Image(MediaFormat.WEBP_LOSSY, quality = 85, maxLongEdge = null)

    @After
    fun cleanUp() = fixtures.cleanUp()

    @Test
    fun verifiedOutputReportsItsSizeAndPublishingMakesItVisible() =
        runTest {
            val photo = fixtures.photoJpeg("IMG_9.jpg")
            val output =
                webp
                    .convert(
                        ConversionInput(photo),
                        spec,
                    ) {}
                    .requireSuccess()
                    .also { fixtures.track(it.outputUri) }

            val size = gateway.verify(photo, output.outputUri, spec).getOrNull()
            val published = gateway.publish(output.outputUri)

            assertThat(size).isEqualTo(output.outputSize)
            assertThat(published.displayName).isEqualTo("IMG_9.webp")
            assertThat(published.relativePath).isEqualTo(fixtures.folder)
            assertThat(published.size).isEqualTo(output.outputSize)
            assertThat(published.format).isEqualTo(MediaFormat.WEBP_LOSSY)
            assertThat(gateway.pendingOutputs()).doesNotContain(output.outputUri)
        }

    @Test
    fun pendingOutputsListsOnlyThisAppsUnpublishedOutputs() =
        runTest {
            val photo = fixtures.photoJpeg()
            val pending = writer.createPending(photo, MediaFormat.WEBP_LOSSY).also { fixtures.track(it.uri) }

            assertThat(gateway.pendingOutputs()).contains(pending.uri)
            assertThat(gateway.pendingOutputs()).doesNotContain(photo.uri)

            gateway.discard(pending.uri)

            assertThat(gateway.pendingOutputs()).doesNotContain(pending.uri)
        }

    @Test
    fun outputWithWrongDimensionsFailsVerification() =
        runTest {
            val photo = fixtures.photoJpeg()
            val output =
                webp
                    .convert(
                        ConversionInput(photo),
                        spec,
                    ) {}
                    .requireSuccess()
                    .also { fixtures.track(it.outputUri) }
            val claimedLarger =
                photo.copy(
                    resolution =
                        io.github.gokulhk.spacesaver.core.model
                            .Resolution(4000, 3000),
                )

            val result = gateway.verify(claimedLarger, output.outputUri, spec)

            assertThat(result.errorOrNull()).isInstanceOf(DomainError.OutputVerificationFailed::class.java)
        }
}
