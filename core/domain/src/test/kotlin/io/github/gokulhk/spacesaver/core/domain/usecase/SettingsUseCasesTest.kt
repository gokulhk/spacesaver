package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.plan.ReservePolicy
import io.github.gokulhk.spacesaver.core.domain.repository.StorageStats
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.ThemeMode
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SettingsUseCasesTest {
    private val settings = FakeSettingsRepository()
    private val storage =
        FakeStorageRepository(
            StorageStats(
                total = ByteSize.gigabytes(128),
                free = ByteSize.gigabytes(40),
                videos = ByteSize.ZERO,
                images = ByteSize.ZERO,
            ),
        )

    private fun observe(heic: Boolean = true) =
        ObserveSettingsOverview(settings, storage, FakeEncoderCapabilities(heic = heic))

    private val update = UpdateSettings(settings)

    @Test
    fun `the overview shows the default reserve for this phone and whether HEIC can be saved`() =
        runTest {
            val overview = observe(heic = false)().first()

            assertThat(overview.defaultReserve).isEqualTo(ByteSize.megabytes(6_400))
            assertThat(overview.reserve).isEqualTo(ByteSize.megabytes(6_400))
            assertThat(overview.heicSupported).isFalse()
        }

    @Test
    fun `changes are saved and reflected`() =
        runTest {
            update.setThemeMode(ThemeMode.DARK)
            update.setImageFormat(ImageFormatPreference.WEBP)
            update.setReserve(ByteSize.gigabytes(2))
            update.setChargingOnly(true)

            val overview = observe()().first()
            assertThat(overview.settings.themeMode).isEqualTo(ThemeMode.DARK)
            assertThat(overview.settings.imageFormat).isEqualTo(ImageFormatPreference.WEBP)
            assertThat(overview.reserve).isEqualTo(ByteSize.gigabytes(2))
            assertThat(overview.settings.chargingOnly).isTrue()
            assertThat(ObserveThemeMode(settings)().first()).isEqualTo(ThemeMode.DARK)

            update.setReserve(null)
            assertThat(observe()().first().reserve).isEqualTo(ByteSize.megabytes(6_400))
        }

    @Test
    fun `reserve choices start at the minimum a user may pick`() {
        assertThat(ReservePolicy.USER_CHOICES.first()).isAtLeast(ReservePolicy.MIN_USER_RESERVE)
        assertThat(ReservePolicy.USER_CHOICES).isInStrictOrder()
    }
}
