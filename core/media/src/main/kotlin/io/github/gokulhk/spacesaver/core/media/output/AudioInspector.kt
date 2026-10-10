package io.github.gokulhk.spacesaver.core.media.output

import android.content.ContentResolver
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import io.github.gokulhk.spacesaver.core.model.AudioSummary
import java.io.IOException

/** Reads a video file's real soundtrack: how many audio tracks it has and how many channels. */
internal object AudioInspector {
    /** The soundtrack of the file at [uri]; [AudioSummary.NONE] if it has none or can't be read. */
    fun inspect(
        resolver: ContentResolver,
        uri: Uri,
    ): AudioSummary {
        val extractor = MediaExtractor()
        return try {
            resolver.openFileDescriptor(uri, "r")?.use { extractor.setDataSource(it.fileDescriptor) }
                ?: return AudioSummary.NONE
            val channels =
                (0 until extractor.trackCount)
                    .map { extractor.getTrackFormat(it) }
                    .filter { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                    // A track that doesn't say how many channels it has counts as one.
                    .map {
                        if (it.containsKey(
                                MediaFormat.KEY_CHANNEL_COUNT,
                            )
                        ) {
                            it.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        } else {
                            1
                        }
                    }
            AudioSummary(trackCount = channels.size, maxChannels = channels.maxOrNull() ?: 0)
        } catch (_: IOException) {
            AudioSummary.NONE
        } catch (_: RuntimeException) {
            // MediaExtractor reports unreadable media with IllegalArgument/IllegalState.
            AudioSummary.NONE
        } finally {
            extractor.release()
        }
    }
}
