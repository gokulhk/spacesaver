package io.github.gokulhk.spacesaver.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.eligibility.ImageEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.SavingsThresholds
import io.github.gokulhk.spacesaver.core.domain.eligibility.VideoEligibility
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.repository.UserSettings
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageContent
import io.github.gokulhk.spacesaver.core.model.ImageFormatPreference
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveSuggestionsTest {
    private val capabilities = FakeEncoderCapabilities(hardwareHevc = true, heic = true)

    private fun observe(
        items: List<MediaItem>,
        settings: UserSettings = UserSettings.DEFAULT,
    ) = ObserveSuggestions(
        mediaRepository = FakeMediaRepository(items),
        settingsRepository = FakeSettingsRepository(settings),
        calibrationRepository = FakeCalibrationRepository(),
        encoderCapabilities = capabilities,
        videoEligibility = VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT),
        imageEligibility = ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT),
    )

    @Test
    fun `4K videos are grouped into one suggestion converting to Full HD by default`() =
        runTest {
            val videos = (1L..3L).map { aVideo(id = it, height = 2160) }

            observe(videos)().test {
                val suggestion = awaitItem().single()
                assertThat(suggestion.group).isEqualTo(SuggestionGroup.VIDEOS_4K)
                assertThat(suggestion.option).isEqualTo(ConversionOption.Video(VideoPreset.UHD_TO_FHD))
                assertThat(suggestion.availableOptions)
                    .containsExactly(
                        ConversionOption.Video(VideoPreset.UHD_TO_FHD),
                        ConversionOption.Video(VideoPreset.UHD_TO_HD),
                    )
                assertThat(suggestion.candidates).hasSize(3)
                assertThat(suggestion.totalSavings).isEqualTo(suggestion.candidates.sumOfSize { it.estimatedSavings })
            }
        }

    @Test
    fun `choosing 4K to HD saves more than 4K to Full HD`() =
        runTest {
            val videos = listOf(aVideo(id = 1, height = 2160))
            val toHd = mapOf(SuggestionGroup.VIDEOS_4K to ConversionOption.Video(VideoPreset.UHD_TO_HD))

            val fullHd = observe(videos)().first().single()
            val hd = observe(videos)(toHd).first().single()

            assertThat(hd.option).isEqualTo(ConversionOption.Video(VideoPreset.UHD_TO_HD))
            assertThat(hd.totalSavings).isGreaterThan(fullHd.totalSavings)
        }

    @Test
    fun `Full HD videos get their own suggestion`() =
        runTest {
            observe(listOf(aVideo(id = 1, height = 1080, size = ByteSize.megabytes(150))))().test {
                val suggestion = awaitItem().single()
                assertThat(suggestion.group).isEqualTo(SuggestionGroup.VIDEOS_FULL_HD)
                assertThat(suggestion.option).isEqualTo(ConversionOption.Video(VideoPreset.FHD_TO_HD))
            }
        }

    @Test
    fun `JPEG photos convert to HEIC when supported and preferred`() =
        runTest {
            observe(listOf(anImage(id = 1)))().test {
                val suggestion = awaitItem().single()
                assertThat(suggestion.group).isEqualTo(SuggestionGroup.JPEG_PHOTOS)
                assertThat(suggestion.option).isEqualTo(ConversionOption.Image(MediaFormat.HEIC))
                assertThat(suggestion.availableOptions)
                    .containsExactly(
                        ConversionOption.Image(MediaFormat.HEIC),
                        ConversionOption.Image(MediaFormat.WEBP_LOSSY),
                    )
            }
        }

    @Test
    fun `JPEG photos convert to WebP when the device cannot encode HEIC`() =
        runTest {
            capabilities.heic = false

            observe(listOf(anImage(id = 1)))().test {
                val suggestion = awaitItem().single()
                assertThat(suggestion.option).isEqualTo(ConversionOption.Image(MediaFormat.WEBP_LOSSY))
                assertThat(suggestion.availableOptions).containsExactly(ConversionOption.Image(MediaFormat.WEBP_LOSSY))
            }
        }

    @Test
    fun `WebP preference is the default target even when HEIC is supported`() =
        runTest {
            val settings = UserSettings.DEFAULT.copy(imageFormat = ImageFormatPreference.WEBP)

            observe(listOf(anImage(id = 1)), settings)().test {
                assertThat(awaitItem().single().option).isEqualTo(ConversionOption.Image(MediaFormat.WEBP_LOSSY))
            }
        }

    @Test
    fun `PNG photos and screenshots are separate suggestions`() =
        runTest {
            val pngs =
                listOf(
                    anImage(id = 1, format = MediaFormat.PNG, imageContent = ImageContent.PHOTO),
                    anImage(id = 2, format = MediaFormat.PNG, imageContent = ImageContent.GRAPHIC),
                    anImage(id = 3, format = MediaFormat.PNG, imageContent = ImageContent.UNKNOWN),
                )

            observe(pngs)().test {
                val byGroup = awaitItem().associateBy { it.group }
                assertThat(
                    byGroup.getValue(SuggestionGroup.PNG_PHOTOS).candidates.map { it.item.id },
                ).containsExactly(MediaId(1))
                assertThat(byGroup.getValue(SuggestionGroup.SCREENSHOTS).candidates.map { it.item.id })
                    .containsExactly(MediaId(2), MediaId(3))
                assertThat(byGroup.getValue(SuggestionGroup.SCREENSHOTS).option)
                    .isEqualTo(ConversionOption.Image(MediaFormat.WEBP_LOSSLESS))
            }
        }

    @Test
    fun `ineligible files are left out and empty groups are not suggested`() =
        runTest {
            val items =
                listOf(
                    aVideo(id = 1, height = 2160, producedBySpaceSaver = true),
                    anImage(id = 2, format = MediaFormat.HEIC),
                    aVideo(id = 3, height = 720),
                )

            observe(items)().test {
                assertThat(awaitItem()).isEmpty()
            }
        }

    @Test
    fun `suggestions are ordered by total savings`() =
        runTest {
            val items = listOf(anImage(id = 1), aVideo(id = 2, height = 2160))

            observe(items)().test {
                assertThat(
                    awaitItem().map {
                        it.group
                    },
                ).containsExactly(SuggestionGroup.VIDEOS_4K, SuggestionGroup.JPEG_PHOTOS).inOrder()
            }
        }

    @Test
    fun `H264 is used when there is no hardware HEVC encoder`() =
        runTest {
            capabilities.hardwareHevc = false

            observe(listOf(aVideo(id = 1)))().test {
                val candidate = awaitItem().single().candidates.single()
                // H.264 at 12 Mbps instead of HEVC at 8 Mbps.
                assertThat(candidate.estimatedOutput).isGreaterThan(ByteSize.megabytes(90))
            }
        }
}
