package io.github.gokulhk.spacesaver.core.ui.permission

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build

/**
 * The runtime permissions SpaceSaver asks for on a given Android version.
 *
 * @property full permissions that together grant access to every photo and video.
 * @property partial the Android 14+ permission for user-selected photos only; null before that.
 * @property notifications the notification permission (Android 13+), for batch progress; null
 * before that, when notifications need no permission. Requested when the first batch starts (see
 * [missingNotificationPermission]), not with media access.
 *
 * `InlinedApi` is suppressed because permission names are compile-time strings that are only
 * returned for API levels that define them (see [forSdk]).
 */
@SuppressLint("InlinedApi")
data class MediaPermissions(
    val full: List<String>,
    val partial: String?,
    val notifications: String?,
) {
    /** The media permissions to request together on onboarding. */
    fun toRequest(): List<String> = full + listOfNotNull(partial)

    /** Per-version permission sets. */
    companion object {
        /** Permissions for API level [sdkInt]. */
        fun forSdk(sdkInt: Int): MediaPermissions =
            when {
                sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    MediaPermissions(
                        full = GRANULAR_MEDIA,
                        partial = Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                        notifications = Manifest.permission.POST_NOTIFICATIONS,
                    )
                }

                sdkInt >= Build.VERSION_CODES.TIRAMISU -> {
                    MediaPermissions(GRANULAR_MEDIA, partial = null, Manifest.permission.POST_NOTIFICATIONS)
                }

                else -> {
                    MediaPermissions(
                        listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        partial = null,
                        notifications = null,
                    )
                }
            }

        /** Permissions for the running device. */
        val current: MediaPermissions get() = forSdk(Build.VERSION.SDK_INT)

        private val GRANULAR_MEDIA = listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    }
}
