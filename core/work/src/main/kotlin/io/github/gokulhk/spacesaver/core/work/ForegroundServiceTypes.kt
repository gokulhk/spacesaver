package io.github.gokulhk.spacesaver.core.work

import android.annotation.SuppressLint
import android.content.pm.ServiceInfo
import android.os.Build

/** Chooses the foreground service type for batch work (findings in `docs/spikes/foreground-work.md`). */
object ForegroundServiceTypes {
    /**
     * `mediaProcessing` (API 35+) is the type made for transcoding. Earlier versions lack it, and
     * `dataSync` is the closest allowed type there.
     *
     * `InlinedApi` is suppressed: the API 35 constant is inlined at compile time and only returned
     * when [sdkInt] is at least 35; lint can't follow the check through the parameter, which exists
     * for testability.
     */
    @SuppressLint("InlinedApi")
    fun forBatches(sdkInt: Int = Build.VERSION.SDK_INT): Int =
        if (sdkInt >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
}
