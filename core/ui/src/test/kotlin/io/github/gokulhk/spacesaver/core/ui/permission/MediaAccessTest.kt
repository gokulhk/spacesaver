package io.github.gokulhk.spacesaver.core.ui.permission

import android.Manifest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class MediaAccessTest {
    private fun state(
        full: Boolean = false,
        partial: Boolean = false,
        rationale: Boolean = false,
        requested: Boolean = false,
    ) = MediaPermissionState(
        fullGranted = full,
        partialGranted = partial,
        shouldShowRationale = rationale,
        requestedBefore = requested,
    )

    @Test
    fun `access is resolved from grants, rationale, and history`() {
        val table =
            listOf(
                state(full = true) to MediaAccess.FULL,
                state(full = true, partial = true, requested = true) to MediaAccess.FULL,
                state(partial = true, requested = true) to MediaAccess.LIMITED,
                state() to MediaAccess.NOT_REQUESTED,
                state(rationale = true) to MediaAccess.DENIED,
                state(rationale = true, requested = true) to MediaAccess.DENIED,
                state(requested = true) to MediaAccess.PERMANENTLY_DENIED,
            )

        table.forEach { (input, expected) ->
            assertWithMessage(input.toString()).that(MediaAccess.resolve(input)).isEqualTo(expected)
        }
    }

    @Test
    fun `only full and limited access can browse media`() {
        assertThat(MediaAccess.entries.filter { it.canReadMedia })
            .containsExactly(MediaAccess.FULL, MediaAccess.LIMITED)
    }

    @Test
    fun `permissions depend on the API level`() {
        assertThat(MediaPermissions.forSdk(API_30).full).containsExactly(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertThat(MediaPermissions.forSdk(API_32).partial).isNull()

        val tiramisu = MediaPermissions.forSdk(API_33)
        assertThat(tiramisu.full)
            .containsExactly(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        assertThat(tiramisu.partial).isNull()
        assertThat(tiramisu.notifications).isEqualTo(Manifest.permission.POST_NOTIFICATIONS)

        val upsideDown = MediaPermissions.forSdk(API_34)
        assertThat(upsideDown.partial).isEqualTo(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        // Notifications are asked for when the first batch starts, not with media access.
        assertThat(upsideDown.toRequest())
            .containsExactly(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        assertThat(MediaPermissions.forSdk(API_32).toRequest())
            .containsExactly(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private companion object {
        const val API_30 = 30
        const val API_32 = 32
        const val API_33 = 33
        const val API_34 = 34
    }
}
