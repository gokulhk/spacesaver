package io.github.gokulhk.spacesaver.core.media.di

import androidx.media3.common.util.UnstableApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.gokulhk.spacesaver.core.domain.conversion.ConverterRegistry
import io.github.gokulhk.spacesaver.core.domain.conversion.MediaConverter
import io.github.gokulhk.spacesaver.core.domain.repository.EncoderCapabilities
import io.github.gokulhk.spacesaver.core.domain.repository.OutputGateway
import io.github.gokulhk.spacesaver.core.media.DefaultConverterRegistry
import io.github.gokulhk.spacesaver.core.media.capabilities.AndroidEncoderCapabilities
import io.github.gokulhk.spacesaver.core.media.image.HeicImageConverter
import io.github.gokulhk.spacesaver.core.media.image.WebpImageConverter
import io.github.gokulhk.spacesaver.core.media.output.AndroidOutputGateway
import io.github.gokulhk.spacesaver.core.media.video.Media3VideoConverter

/** Binds the media engine. New formats are added by binding another converter `@IntoSet`. */
@Module
@InstallIn(SingletonComponent::class)
@UnstableApi
interface MediaModule {
    /** Encoder capability detection. */
    @Binds
    fun encoderCapabilities(impl: AndroidEncoderCapabilities): EncoderCapabilities

    /** Output verification and publishing. */
    @Binds
    fun outputGateway(impl: AndroidOutputGateway): OutputGateway

    /** Converter lookup. */
    @Binds
    fun converterRegistry(impl: DefaultConverterRegistry): ConverterRegistry

    /** Videos to H.264 or HEVC MP4. */
    @Binds
    @IntoSet
    fun videoConverter(impl: Media3VideoConverter): MediaConverter

    /** JPEG and PNG to WebP. */
    @Binds
    @IntoSet
    fun webpConverter(impl: WebpImageConverter): MediaConverter

    /** JPEG to HEIC. */
    @Binds
    @IntoSet
    fun heicConverter(impl: HeicImageConverter): MediaConverter
}
