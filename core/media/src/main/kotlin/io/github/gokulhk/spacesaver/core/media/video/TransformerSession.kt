package io.github.gokulhk.spacesaver.core.media.video

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.util.UnstableApi
import androidx.media3.muxer.Muxer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs one Transformer export on its own looper thread as a cancellable suspend call, polling
 * progress while it runs. Cancelling the coroutine cancels the export.
 */
@UnstableApi
internal class TransformerSession(
    private val context: Context,
    private val spec: ConversionSpec.Video,
    private val muxerFactory: Muxer.Factory,
    private val progress: MonotonicProgress,
) {
    private val thread = HandlerThread("SpaceSaverTransformer").apply { start() }
    private val handler = Handler(thread.looper)
    private val poller = ProgressPoller()
    private var transformer: Transformer? = null

    /** Exports [source]; returns when done, throws the [ExportException] on failure. */
    suspend fun export(source: Uri) {
        try {
            suspendCancellableCoroutine { continuation ->
                handler.post { start(source, continuation) }
                continuation.invokeOnCancellation {
                    handler.post {
                        handler.removeCallbacks(poller)
                        transformer?.cancel()
                    }
                }
            }
        } finally {
            thread.quitSafely()
        }
    }

    private fun start(
        source: Uri,
        continuation: CancellableContinuation<Unit>,
    ) {
        val listener = listenerFor(continuation)
        transformer =
            TransformerConfig.build(context, thread.looper, spec, muxerFactory, listener).also {
                // The muxer factory ignores this path and writes to the MediaStore descriptor.
                it.start(TransformerConfig.editedItem(source, spec), TransformerConfig.UNUSED_OUTPUT_PATH)
            }
        handler.post(poller)
    }

    private fun listenerFor(continuation: CancellableContinuation<Unit>): Transformer.Listener =
        object : Transformer.Listener {
            override fun onCompleted(
                composition: Composition,
                exportResult: ExportResult,
            ) {
                handler.removeCallbacks(poller)
                progress.report(1f)
                continuation.resume(Unit)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                handler.removeCallbacks(poller)
                continuation.resumeWithException(exportException)
            }
        }

    /** Reports Transformer's progress every [PROGRESS_POLL_MS] while the export runs. */
    private inner class ProgressPoller : Runnable {
        private val holder = ProgressHolder()

        override fun run() {
            val current = transformer ?: return
            if (current.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                progress.report(holder.progress / PERCENT)
            }
            handler.postDelayed(this, PROGRESS_POLL_MS)
        }
    }

    private companion object {
        const val PERCENT = 100f
        const val PROGRESS_POLL_MS = 250L
    }
}
