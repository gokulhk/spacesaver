package io.github.gokulhk.spacesaver.core.datastore

import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode

/**
 * Settings as persisted. `:core:data` maps them to the domain's `UserSettings`.
 *
 * @property themeMode System, Light, or Dark.
 * @property imageFormat preferred photo format.
 * @property reserve the user's free-space reserve; null for the default.
 * @property chargingOnly whether batches run only while charging.
 */
data class StoredSettings(
    val themeMode: ThemeMode,
    val imageFormat: ImageFormatPreference,
    val reserve: ByteSize?,
    val chargingOnly: Boolean,
) {
    /** Defaults. */
    companion object {
        /** What an empty or unreadable store means. */
        val DEFAULT =
            StoredSettings(
                themeMode = ThemeMode.SYSTEM,
                imageFormat = ImageFormatPreference.HEIC,
                reserve = null,
                chargingOnly = false,
            )
    }
}
