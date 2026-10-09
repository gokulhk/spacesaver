package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.plan.ReservePolicy
import io.github.gokulhk.spacesaver.core.domain.repository.EncoderCapabilities
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * What the Settings screen shows (plan Section 7.7).
 *
 * @property settings the stored settings.
 * @property defaultReserve the reserve used when the user hasn't chosen one: max(1 GB, 5% of storage).
 * @property heicSupported whether this phone can save HEIC; if not, photos are saved as WebP.
 */
data class SettingsOverview(
    val settings: UserSettings,
    val defaultReserve: ByteSize,
    val heicSupported: Boolean,
) {
    /** The reserve in effect. */
    val reserve: ByteSize
        get() = settings.reserveOverride?.coerceAtLeast(ReservePolicy.MIN_USER_RESERVE) ?: defaultReserve
}

/** Settings with the device facts needed to explain them. */
class ObserveSettingsOverview
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val storageRepository: StorageRepository,
        private val encoderCapabilities: EncoderCapabilities,
    ) {
        /** The overview, re-emitted when settings or storage change. */
        operator fun invoke(): Flow<SettingsOverview> =
            flow {
                val heic = encoderCapabilities.supportsHeicEncoding()
                emitAll(
                    combine(settingsRepository.settings, storageRepository.observeStorage()) { settings, storage ->
                        SettingsOverview(settings, ReservePolicy.reserveFor(storage.total, userReserve = null), heic)
                    },
                )
            }
    }

/** The theme the whole app follows; re-emitted when changed in Settings. */
class ObserveThemeMode
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) {
        /** The current theme mode. */
        operator fun invoke(): Flow<ThemeMode> = settingsRepository.settings.map { it.themeMode }.distinctUntilChanged()
    }

/** Saves changes made in Settings. */
class UpdateSettings
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
    ) {
        /** System, light, or dark. */
        suspend fun setThemeMode(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

        /** HEIC or WebP for converted photos. */
        suspend fun setImageFormat(format: ImageFormatPreference) = settingsRepository.setImageFormat(format)

        /** The free-space reserve; null restores the default. */
        suspend fun setReserve(reserve: ByteSize?) = settingsRepository.setReserveOverride(reserve)

        /** Whether batches run only while charging (applies to batches started afterwards). */
        suspend fun setChargingOnly(enabled: Boolean) = settingsRepository.setChargingOnly(enabled)
    }
