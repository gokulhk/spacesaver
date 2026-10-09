package io.github.gokulhk.spacesaver.core.work

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class BatchNotificationsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `tapping the notification opens the batch's progress in this app`() {
        val notification = BatchNotifications(context).foregroundInfo(BatchId(7), progress = null).notification

        val intent = shadowOf(notification.contentIntent).savedIntent

        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.dataString).isEqualTo("spacesaver://batch/7")
        assertThat(intent.`package`).isEqualTo(context.packageName)
    }
}
