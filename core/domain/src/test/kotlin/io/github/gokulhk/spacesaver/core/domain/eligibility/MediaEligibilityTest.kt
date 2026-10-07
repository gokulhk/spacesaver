package io.github.gokulhk.spacesaver.core.domain.eligibility

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import org.junit.Test

class MediaEligibilityTest {
    private val video = VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT)
    private val image = ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT)
    private val eligibility = MediaEligibility(video, image)

    @Test
    fun `routes each option to the matching rules`() {
        val video4k = aVideo(id = 1, height = 2160)
        val jpeg = anImage(id = 2, format = MediaFormat.JPEG)
        val videoOption = ConversionOption.Video(VideoPreset.UHD_TO_FHD)
        val imageOption = ConversionOption.Image(MediaFormat.HEIC)

        assertThat(eligibility.evaluate(video4k, videoOption, VideoCodec.HEVC, CalibrationTable.EMPTY))
            .isEqualTo(video.evaluate(video4k, VideoPreset.UHD_TO_FHD, VideoCodec.HEVC))
        assertThat(eligibility.evaluate(jpeg, imageOption, VideoCodec.HEVC, CalibrationTable.EMPTY))
            .isEqualTo(image.evaluate(jpeg, MediaFormat.HEIC, CalibrationTable.EMPTY))
    }
}
