package io.github.gokulhk.spacesaver.core.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import javax.inject.Inject
import kotlin.math.roundToInt

/** The ongoing notification shown while a batch converts (plan Section 7.5). */
class BatchNotifications
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /** Foreground info for [progress]; null while the batch is starting. */
        fun foregroundInfo(progress: BatchProgress?): ForegroundInfo =
            ForegroundInfo(NOTIFICATION_ID, notification(progress), ForegroundServiceTypes.forBatches())

        private fun notification(progress: BatchProgress?): Notification {
            ensureChannel()
            val text =
                if (progress?.currentItemName == null) {
                    context.getString(R.string.batch_notification_starting)
                } else {
                    context.getString(
                        R.string.batch_notification_progress,
                        progress.completedItems + 1,
                        progress.totalItems,
                        progress.currentItemName,
                    )
                }
            return NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_spacesaver)
                .setContentTitle(context.getString(R.string.batch_notification_title))
                .setContentText(text)
                .setProgress(PERCENT, ((progress?.overall ?: 0f) * PERCENT).roundToInt(), progress == null)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentIntent(openAppIntent())
                .build()
        }

        /** Opens the app; Phase 7 deep-links to the batch progress screen. */
        private fun openAppIntent(): PendingIntent? =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        private fun ensureChannel() {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.batch_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.batch_channel_description)
                }
            manager.createNotificationChannel(channel)
        }

        /** Constants. */
        companion object {
            /** The one notification for batch progress. */
            const val NOTIFICATION_ID = 1001

            /** Low importance: progress is useful, but shouldn't make a sound. */
            const val CHANNEL_ID = "batch_progress"

            private const val PERCENT = 100
        }
    }
