package io.github.gokulhk.spacesaver.core.ui.comparison

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ComparisonGeometryTest {
    @Test
    fun `the crop is the view's size, centred in the image`() {
        assertThat(centerCrop(imageWidth = 4000, imageHeight = 3000, viewWidth = 1000, viewHeight = 800))
            .isEqualTo(CropRect(left = 1500, top = 1100, right = 2500, bottom = 1900))
    }

    @Test
    fun `an image smaller than the view is shown whole`() {
        assertThat(centerCrop(imageWidth = 600, imageHeight = 400, viewWidth = 1000, viewHeight = 800))
            .isEqualTo(CropRect(left = 0, top = 0, right = 600, bottom = 400))
    }

    @Test
    fun `videos are compared at a quarter, half, and three quarters`() {
        assertThat(comparisonTimestamps(60.seconds)).containsExactly(15.seconds, 30.seconds, 45.seconds).inOrder()
    }
}
