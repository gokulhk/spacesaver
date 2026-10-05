package io.github.gokulhk.spacesaver.core.media.video

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.Mp4Muxer
import androidx.media3.muxer.Muxer
import androidx.media3.muxer.MuxerUtil
import androidx.media3.muxer.SeekableMuxerOutput
import com.google.common.collect.ImmutableList
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * A muxer factory that writes MP4 to an already-open [stream] (a pending MediaStore entry) and
 * ignores the path Transformer passes. Media3's own factories open the path with exclusive create,
 * which fails because MediaStore has already created the file, and `/proc/self/fd` paths are
 * blocked by SELinux for writes.
 *
 * Like Media3's `InAppMp4Muxer`, unsupported metadata entries are dropped instead of failing the
 * export; location and creation time are supported and kept.
 */
@UnstableApi
class StreamMp4MuxerFactory(
    private val stream: FileOutputStream,
) : Muxer.Factory {
    override fun create(path: String): Muxer = FilteringMuxer(Mp4Muxer.Builder(SeekableMuxerOutput.of(stream)).build())

    override fun getSupportedSampleMimeTypes(trackType: Int): ImmutableList<String> =
        when (trackType) {
            C.TRACK_TYPE_VIDEO -> Mp4Muxer.SUPPORTED_VIDEO_SAMPLE_MIME_TYPES
            C.TRACK_TYPE_AUDIO -> Mp4Muxer.SUPPORTED_AUDIO_SAMPLE_MIME_TYPES
            else -> ImmutableList.of()
        }

    /** Delegates to [delegate], skipping metadata the MP4 muxer can't write. */
    private class FilteringMuxer(
        private val delegate: Mp4Muxer,
    ) : Muxer {
        override fun addTrack(format: Format): Int = delegate.addTrack(format)

        override fun writeSampleData(
            trackId: Int,
            data: ByteBuffer,
            bufferInfo: BufferInfo,
        ) = delegate.writeSampleData(trackId, data, bufferInfo)

        override fun addMetadataEntry(metadataEntry: Metadata.Entry) {
            if (MuxerUtil.isMetadataSupported(metadataEntry)) delegate.addMetadataEntry(metadataEntry)
        }

        override fun close() = delegate.close()
    }
}
