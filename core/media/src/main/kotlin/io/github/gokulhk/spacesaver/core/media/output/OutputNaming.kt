package io.github.gokulhk.spacesaver.core.media.output

import io.github.gokulhk.spacesaver.core.model.MediaFormat

/**
 * Names converted outputs (plan Section 5.8 step 1): the original's base name with the target
 * extension, in SpaceSaver's output folder; `_compressed` (then `_compressed_2`, ...) on collision.
 */
object OutputNaming {
    private const val COLLISION_SUFFIX = "_compressed"

    /** First numbered suffix after the plain one, so the third name is `_compressed_2`. */
    private const val FIRST_NUMBERED_SUFFIX = 2

    /**
     * The output file name for [originalName] converted to [target], avoiding [existing] names in
     * the folder. Comparison is case-insensitive, like Android's shared storage.
     */
    fun nameFor(
        originalName: String,
        target: MediaFormat,
        existing: Set<String>,
    ): String {
        val taken = existing.map { it.lowercase() }.toSet()
        val base = originalName.substringBeforeLast('.', missingDelimiterValue = originalName)
        val extension = extensionFor(target)
        val candidates =
            sequence {
                yield("$base.$extension")
                yield("$base$COLLISION_SUFFIX.$extension")
                generateSequence(
                    FIRST_NUMBERED_SUFFIX,
                ) { it + 1 }.forEach { yield("$base${COLLISION_SUFFIX}_$it.$extension") }
            }
        return candidates.first { it.lowercase() !in taken }
    }

    /** File extension for [format]. */
    fun extensionFor(format: MediaFormat): String =
        when (format) {
            MediaFormat.JPEG -> "jpg"
            MediaFormat.PNG -> "png"
            MediaFormat.WEBP_LOSSY, MediaFormat.WEBP_LOSSLESS -> "webp"
            MediaFormat.HEIC -> "heic"
            MediaFormat.AVIF -> "avif"
            MediaFormat.GIF -> "gif"
            MediaFormat.MP4_H264, MediaFormat.MP4_HEVC, MediaFormat.VIDEO_OTHER -> "mp4"
        }
}
