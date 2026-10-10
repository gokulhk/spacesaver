package io.github.gokulhk.spacesaver.core.media.video

import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.muxer.Muxer
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import io.github.gokulhk.spacesaver.core.domain.conversion.AudioPolicy
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.model.MediaFormat

/** Turns a [ConversionSpec.Video] into Transformer configuration (plan Section 5.3). */
@UnstableApi
internal object TransformerConfig {
    /** Output formats the video converter produces. */
    val TARGETS = setOf(MediaFormat.MP4_H264, MediaFormat.MP4_HEVC)

    /** Transformer requires an output path; [StreamMp4MuxerFactory] ignores it. */
    const val UNUSED_OUTPUT_PATH = "unused.mp4"

    /** Plan Section 5.3: keep the source frame rate, capped at 60 fps. */
    private const val MAX_FRAME_RATE = 60f

    /** A Transformer for [spec] running on [looper], writing through [muxerFactory]. */
    fun build(
        context: Context,
        looper: Looper,
        spec: ConversionSpec.Video,
        muxerFactory: Muxer.Factory,
        listener: Transformer.Listener,
    ): Transformer =
        Transformer
            .Builder(context)
            .setLooper(looper)
            .setVideoMimeType(videoMimeType(spec.targetFormat))
            // An AAC source is copied unchanged (no audio effects); anything else becomes AAC.
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory(context, spec))
            .setMuxerFactory(muxerFactory)
            .addListener(listener)
            .build()

    /**
     * The input with downscaling and the frame rate cap applied.
     *
     * Audio is never removed here, whatever `spec.audio` says: the plan's audio details are a
     * snapshot that can't see the file's real tracks (a batch item rebuilt from the database has
     * none, which reads as "no audio"). Transformer keeps the tracks the file really has and copes
     * with a video that has none.
     */
    fun editedItem(
        source: Uri,
        spec: ConversionSpec.Video,
    ): EditedMediaItem {
        val videoEffects =
            listOf(
                Presentation.createForShortSide(spec.targetShortEdge),
                FrameDropEffect.createDefaultFrameDropEffect(MAX_FRAME_RATE),
            )
        return EditedMediaItem
            .Builder(MediaItem.fromUri(source))
            .setEffects(Effects(emptyList(), videoEffects))
            .build()
    }

    private fun encoderFactory(
        context: Context,
        spec: ConversionSpec.Video,
    ): DefaultEncoderFactory {
        val video = VideoEncoderSettings.Builder().setBitrate(spec.videoBitrate.bitsPerSecond.toInt()).build()
        val audio =
            AudioEncoderSettings
                .Builder()
                .setBitrate(
                    AudioPolicy.REENCODE_BITRATE.bitsPerSecond.toInt(),
                ).build()
        return DefaultEncoderFactory
            .Builder(context)
            .setRequestedVideoEncoderSettings(video)
            .setRequestedAudioEncoderSettings(audio)
            .setEnableFallback(true)
            .build()
    }

    private fun videoMimeType(target: MediaFormat): String =
        if (target == MediaFormat.MP4_HEVC) MimeTypes.VIDEO_H265 else MimeTypes.VIDEO_H264
}
