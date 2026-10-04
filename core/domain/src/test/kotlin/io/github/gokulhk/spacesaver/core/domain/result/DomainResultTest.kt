package io.github.gokulhk.spacesaver.core.domain.result

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DomainResultTest {
    private val success: DomainResult<Int> = DomainResult.Success(2)
    private val failure: DomainResult<Int> = DomainResult.Failure(DomainError.Cancelled)

    @Test
    fun `map transforms a success and passes a failure through`() {
        assertThat(success.map { it * 10 }).isEqualTo(DomainResult.Success(20))
        assertThat(failure.map { it * 10 }).isEqualTo(failure)
    }

    @Test
    fun `flatMap chains results`() {
        assertThat(success.flatMap { DomainResult.Success(it + 1) }).isEqualTo(DomainResult.Success(3))
        assertThat(success.flatMap { DomainResult.Failure(DomainError.PermissionMissing) })
            .isEqualTo(DomainResult.Failure(DomainError.PermissionMissing))
        assertThat(failure.flatMap { DomainResult.Success(it + 1) }).isEqualTo(failure)
    }

    @Test
    fun `getOrNull and errorOrNull`() {
        assertThat(success.getOrNull()).isEqualTo(2)
        assertThat(failure.getOrNull()).isNull()
        assertThat(success.errorOrNull()).isNull()
        assertThat(failure.errorOrNull()).isEqualTo(DomainError.Cancelled)
    }
}
