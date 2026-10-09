package io.github.gokulhk.spacesaver.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gokulhk.spacesaver.core.designsystem.theme.SpaceSaverTheme
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's theme, following the Settings choice as it changes (plan Task 7.7). Nothing is drawn
 * until the setting is read, so a saved dark theme never flashes light at start-up.
 *
 * @param themeModes the theme setting; null until read.
 */
@Composable
fun AppTheme(
    themeModes: StateFlow<ThemeMode?>,
    content: @Composable () -> Unit,
) {
    val mode by themeModes.collectAsStateWithLifecycle()
    mode?.let { SpaceSaverTheme(themeMode = it, content = content) }
}
