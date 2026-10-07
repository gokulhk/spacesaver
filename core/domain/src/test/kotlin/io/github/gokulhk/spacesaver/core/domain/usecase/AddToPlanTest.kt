package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.eligibility.ImageEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.MediaEligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.SavingsThresholds
import io.github.gokulhk.spacesaver.core.domain.eligibility.VideoEligibility
import io.github.gokulhk.spacesaver.core.domain.estimate.ImageSavingsEstimator
import io.github.gokulhk.spacesaver.core.domain.estimate.VideoSavingsEstimator
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.testing.FakeBatchRepository
import io.github.gokulhk.spacesaver.core.testing.FakeCalibrationRepository
import io.github.gokulhk.spacesaver.core.testing.FakeEncoderCapabilities
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import io.github.gokulhk.spacesaver.core.testing.FakePlanAdditionsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeSettingsRepository
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddToPlanTest {
    private val video4k = aVideo(id = 1, height = 2160)
    private val heic = anImage(id = 2, format = MediaFormat.HEIC)
    private val additions = FakePlanAdditionsRepository()
    private val suggestions =
        ObserveSuggestions(
            FakeMediaRepository(listOf(video4k, heic)),
            FakeSettingsRepository(),
            FakeCalibrationRepository(),
            FakeEncoderCapabilities(hardwareHevc = true, heic = true),
            MediaEligibility(
                VideoEligibility(VideoSavingsEstimator(), SavingsThresholds.DEFAULT),
                ImageEligibility(ImageSavingsEstimator(), SavingsThresholds.DEFAULT),
            ),
            FakeBatchRepository(),
        )
    private val addToPlan = AddToPlan(suggestions, additions)

    @Test
    fun `eligible files are added and the rest are reported`() =
        runTest {
            val result = addToPlan(listOf(video4k, heic))

            assertThat(result).isEqualTo(AddToPlanResult(added = 1, notEligible = 1))
            assertThat(ObservePlanAdditions(additions)().first()).containsExactly(MediaId(1))
        }

    @Test
    fun `adding again keeps earlier additions`() =
        runTest {
            additions.add(setOf(MediaId(9)))

            addToPlan(listOf(video4k))

            assertThat(ObservePlanAdditions(additions)().first()).containsExactly(MediaId(1), MediaId(9))
        }
}
