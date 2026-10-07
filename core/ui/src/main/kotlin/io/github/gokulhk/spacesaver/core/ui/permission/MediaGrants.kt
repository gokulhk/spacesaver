package io.github.gokulhk.spacesaver.core.ui.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * What the system currently reports for the media permissions.
 *
 * @property fullGranted all of [MediaPermissions.full] are granted.
 * @property partialGranted [MediaPermissions.partial] is granted.
 * @property shouldShowRationale the system would show the dialog again.
 */
data class MediaGrants(
    val fullGranted: Boolean,
    val partialGranted: Boolean,
    val shouldShowRationale: Boolean,
) {
    /** The full state, given whether the dialog was ever shown. */
    fun withHistory(requestedBefore: Boolean): MediaPermissionState =
        MediaPermissionState(fullGranted, partialGranted, shouldShowRationale, requestedBefore)
}

/** Whether the app can read media at all right now (full or limited access). */
fun Context.canReadMedia(permissions: MediaPermissions = MediaPermissions.current): Boolean =
    permissions.full.all(::isGranted) || permissions.partial?.let(::isGranted) == true

/** Reads the media grants; needs an [Activity] for the rationale flag. */
fun Activity.readMediaGrants(permissions: MediaPermissions = MediaPermissions.current): MediaGrants =
    MediaGrants(
        fullGranted = permissions.full.all(::isGranted),
        partialGranted = permissions.partial?.let(::isGranted) == true,
        shouldShowRationale = permissions.full.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) },
    )

/**
 * The notification permission to request before the first batch, so its progress notification is
 * visible; null when already granted or not needed (before Android 13).
 */
fun Context.missingNotificationPermission(permissions: MediaPermissions = MediaPermissions.current): String? =
    permissions.notifications?.takeUnless(::isGranted)

/** Opens this app's page in system settings, where a permanently denied permission can be granted. */
fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
