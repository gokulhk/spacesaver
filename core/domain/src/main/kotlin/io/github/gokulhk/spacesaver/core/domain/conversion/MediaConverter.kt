package io.github.gokulhk.spacesaver.core.domain.conversion

import io.github.gokulhk.spacesaver.core.domain.result.DomainError
import io.github.gokulhk.spacesaver.core.model.Bitrate
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem

/** Everything a converter needs to know about the requested output (plan Section 4.4). */
sealed interface ConversionSpec {
    /** The output format. */
    val targetFormat: MediaFormat

    /**
     * Video output.
     *
     * @property targetShortEdge output short edge in pixels; aspect ratio is kept.
     * @property videoBitrate target video bitrate.
     * @property audio how the audio track is handled.
     */
    data class Video(
        override val targetFormat: MediaFormat,
        val targetShortEdge: Int,
        val videoBitrate: Bitrate,
        val audio: AudioPolicy,
    ) : ConversionSpec

    /**
     * Image output.
     *
     * @property quality encoder quality 0–100; ignored for lossless targets.
     * @property maxLongEdge optional downscale limit; null keeps the original dimensions.
     */
    data class Image(
        override val targetFormat: MediaFormat,
        val quality: Int,
        val maxLongEdge: Int?,
    ) : ConversionSpec
}

/**
 * The file to convert.
 *
 * @property item the original.
 */
data class ConversionInput(
    val item: MediaItem,
)

/** Outcome of one conversion. */
sealed interface ConversionResult {
    /**
     * Converted into a **pending** MediaStore entry (hidden from other apps). The batch runner then
     * verifies it and publishes it, or discards it (plan Section 5.8).
     *
     * @property outputUri the output's content URI.
     * @property outputSize the output's size on disk.
     */
    data class Success(
        val outputUri: String,
        val outputSize: ByteSize,
    ) : ConversionResult

    /**
     * Conversion failed; any partial output was deleted.
     *
     * @property error why.
     */
    data class Failure(
        val error: DomainError,
    ) : ConversionResult

    /** Cancelled; any partial output was deleted. */
    data object Cancelled : ConversionResult
}

/** Strategy: converts one input into one output (plan Section 4.4). */
interface MediaConverter {
    /** Whether this converter handles [source] to [target]. */
    fun supports(
        source: MediaFormat,
        target: MediaFormat,
    ): Boolean

    /** Converts [input] according to [spec], reporting progress from 0 to 1. */
    suspend fun convert(
        input: ConversionInput,
        spec: ConversionSpec,
        onProgress: (Float) -> Unit,
    ): ConversionResult
}

/**
 * Registry: finds the converter for a (source, target) pair. Adding a format later means adding
 * a converter and registering it; batch processing and post-MVP per-file conversion both go
 * through here (plan Section 4.4).
 */
interface ConverterRegistry {
    /** The converter for [source] to [target], or null if none is registered. */
    fun converterFor(
        source: MediaFormat,
        target: MediaFormat,
    ): MediaConverter?

    /** Every target format some converter supports for [source]. */
    fun targetsFor(source: MediaFormat): Set<MediaFormat>
}
