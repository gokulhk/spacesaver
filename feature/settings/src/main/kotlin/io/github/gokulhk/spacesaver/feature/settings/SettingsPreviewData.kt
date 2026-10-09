package io.github.gokulhk.spacesaver.feature.settings

import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.domain.usecase.SettingsOverview
import io.github.gokulhk.spacesaver.core.model.ByteSize

/** Settings states for previews, UI tests, and screenshots. */
@Suppress("MagicNumber") // Sample figures.
internal object SettingsPreviewData {
    /** Defaults on a 128 GB phone with HEIC support. */
    val content =
        SettingsUiState.Content(
            SettingsOverview(UserSettings.DEFAULT, ByteSize.megabytes(6_400), heicSupported = true),
            dialog = null,
        )

    /** A phone without a HEIC encoder. */
    val noHeic = content.copy(overview = content.overview.copy(heicSupported = false))
}
