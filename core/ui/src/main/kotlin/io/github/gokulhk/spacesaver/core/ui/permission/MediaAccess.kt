package io.github.gokulhk.spacesaver.core.ui.permission

/**
 * What the app may read from the photo and video library (plan Section 7.1).
 *
 * @property canReadMedia whether scanning and browsing work at all.
 */
enum class MediaAccess(
    val canReadMedia: Boolean,
) {
    /** Every photo and video. */
    FULL(canReadMedia = true),

    /** Only the photos and videos the user picked (Android 14+). */
    LIMITED(canReadMedia = true),

    /** The permission dialog has never been shown. */
    NOT_REQUESTED(canReadMedia = false),

    /** Denied, but the system will still show the dialog again. */
    DENIED(canReadMedia = false),

    /** Denied for good; only the app's system settings page can grant it now. */
    PERMANENTLY_DENIED(canReadMedia = false),
    ;

    /** Resolution. */
    companion object {
        /**
         * The access [state] amounts to. Android has no "permanently denied" flag: it is a denial
         * after an earlier request where the system no longer offers a rationale.
         */
        fun resolve(state: MediaPermissionState): MediaAccess =
            when {
                state.fullGranted -> FULL
                state.partialGranted -> LIMITED
                state.shouldShowRationale -> DENIED
                !state.requestedBefore -> NOT_REQUESTED
                else -> PERMANENTLY_DENIED
            }
    }
}

/**
 * A snapshot of the media permissions, read from the system.
 *
 * @property fullGranted all of [MediaPermissions.full] are granted.
 * @property partialGranted [MediaPermissions.partial] is granted (user-selected photos only).
 * @property shouldShowRationale the system would show the dialog again after a denial.
 * @property requestedBefore the dialog has been shown at least once (stored in settings).
 */
data class MediaPermissionState(
    val fullGranted: Boolean,
    val partialGranted: Boolean,
    val shouldShowRationale: Boolean,
    val requestedBefore: Boolean,
)
