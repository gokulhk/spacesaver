package io.github.gokulhk.spacesaver.core.domain.repository

import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * The user's settings (plan Section 7.7).
 *
 * @property themeMode System, Light, or Dark.
 * @property imageFormat preferred format for converted photos.
 * @property reserveOverride the user's free-space reserve, or null for the default.
 * @property chargingOnly whether batches run only while charging.
 */
data class UserSettings(
    val themeMode: ThemeMode,
    val imageFormat: ImageFormatPreference,
    val reserveOverride: ByteSize?,
    val chargingOnly: Boolean,
) {
    /** Defaults. */
    companion object {
        /** Settings before the user changes anything. */
        val DEFAULT =
            UserSettings(
                themeMode = ThemeMode.SYSTEM,
                imageFormat = ImageFormatPreference.HEIC,
                reserveOverride = null,
                chargingOnly = false,
            )
    }
}

/** Port: settings persisted with DataStore. */
interface SettingsRepository {
    /** Current settings, re-emitted on every change. */
    val settings: Flow<UserSettings>

    /** Sets the theme mode. */
    suspend fun setThemeMode(mode: ThemeMode)

    /** Sets the preferred photo format. */
    suspend fun setImageFormat(format: ImageFormatPreference)

    /** Sets the reserve; null restores the default. */
    suspend fun setReserveOverride(reserve: ByteSize?)

    /** Sets whether batches run only while charging. */
    suspend fun setChargingOnly(enabled: Boolean)
}
