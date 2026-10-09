package io.github.gokulhk.spacesaver.core.work

import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.execution.BatchProgress
import org.junit.Test
import java.time.Instant

class BatchProgressDataTest {
    @Test
    fun `progress survives the trip through WorkManager data`() {
        val progress = BatchProgress(2, 5, 0.4f, "VID_1.mp4", Instant.parse("2026-10-09T10:00:00Z"))

        assertThat(progress.toData().toBatchProgress()).isEqualTo(progress)
    }

    @Test
    fun `progress without a current item survives too`() {
        val progress = BatchProgress(5, 5, 0f, null, Instant.parse("2026-10-09T10:00:00Z"))

        assertThat(progress.toData().toBatchProgress()).isEqualTo(progress)
    }

    @Test
    fun `data without progress is no progress`() {
        assertThat(workDataOf().toBatchProgress()).isNull()
    }
}
