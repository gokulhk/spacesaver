package io.github.gokulhk.spacesaver.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Formats estimated durations loosely ("about 45 min"), since they are estimates. */
class DurationTextFormatter(
    private val resources: Resources,
) {
    /** [duration] rounded to whole minutes, e.g. "about 2 h 10 min" or "less than a minute". */
    fun formatApprox(duration: Duration): String {
        if (duration < 1.minutes) return resources.getString(R.string.duration_under_a_minute)
        return duration.toComponents { hours, minutes, _, _ ->
            when {
                hours == 0L -> resources.getString(R.string.duration_approx_minutes, minutes)
                minutes == 0 -> resources.getString(R.string.duration_approx_hours, hours.toInt())
                else -> resources.getString(R.string.duration_approx_hours_minutes, hours.toInt(), minutes)
            }
        }
    }
}

/** A [DurationTextFormatter] for the current configuration. */
@Composable
fun rememberDurationTextFormatter(): DurationTextFormatter {
    val resources = LocalResources.current
    return remember(resources, resources.configuration) { DurationTextFormatter(resources) }
}
