package io.github.gokulhk.spacesaver.core.work

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import org.junit.Test

class BatchDeepLinkTest {
    @Test
    fun `a batch link round-trips`() {
        assertThat(BatchDeepLink.uri(BatchId(42))).isEqualTo("spacesaver://batch/42")
        assertThat(BatchDeepLink.parse("spacesaver://batch/42")).isEqualTo(BatchId(42))
    }

    @Test
    fun `other links are not batch links`() {
        listOf(null, "", "spacesaver://batch/", "spacesaver://batch/abc", "https://batch/1", "spacesaver://review/1")
            .forEach { assertThat(BatchDeepLink.parse(it)).isNull() }
    }
}
