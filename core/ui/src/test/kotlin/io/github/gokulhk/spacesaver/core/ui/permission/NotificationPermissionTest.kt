package io.github.gokulhk.spacesaver.core.ui.permission

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class NotificationPermissionTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `asks for notifications until granted`() {
        assertThat(app.missingNotificationPermission()).isEqualTo(Manifest.permission.POST_NOTIFICATIONS)

        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        assertThat(app.missingNotificationPermission()).isNull()
    }

    @Test
    @Config(sdk = [API_32])
    fun `nothing to ask for before Android 13`() {
        assertThat(app.missingNotificationPermission()).isNull()
    }

    private companion object {
        const val API_32 = 32
    }
}
