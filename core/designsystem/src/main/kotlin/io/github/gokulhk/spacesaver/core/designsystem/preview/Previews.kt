package io.github.gokulhk.spacesaver.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/** Light and dark previews. Wrap preview content in `SpaceSaverTheme()` so it follows uiMode. */
@Preview(name = "Light", showBackground = true)
@Preview(
    name = "Dark",
    showBackground = true,
    backgroundColor = 0xFF0E1514,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
annotation class PreviewLightDark

/** Light, dark, and 200% font scale previews for shared components (plan Section 6.5). */
@PreviewLightDark
@Preview(name = "Large font", showBackground = true, fontScale = 2f)
annotation class PreviewComponents
