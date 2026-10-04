package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.MediaSort
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.testing.FakeMediaRepository
import org.junit.Test

class ObserveMediaBySizeTest {
    private val repository = FakeMediaRepository()

    @Test
    fun `defaults to largest first`() {
        ObserveMediaBySize(repository)(MediaType.VIDEO)

        assertThat(repository.pagedRequests).containsExactly(MediaType.VIDEO to MediaSort.SIZE_DESCENDING)
    }

    @Test
    fun `passes the chosen sort through`() {
        ObserveMediaBySize(repository)(MediaType.IMAGE, MediaSort.DATE_DESCENDING)

        assertThat(repository.pagedRequests).containsExactly(MediaType.IMAGE to MediaSort.DATE_DESCENDING)
    }
}
