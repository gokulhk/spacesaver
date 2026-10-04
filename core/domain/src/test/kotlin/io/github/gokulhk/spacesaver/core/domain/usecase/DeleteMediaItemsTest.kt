package io.github.gokulhk.spacesaver.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsType
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.testing.FakeDeletionGateway
import io.github.gokulhk.spacesaver.core.testing.FakeSavingsRepository
import io.github.gokulhk.spacesaver.core.testing.FakeStorageRepository
import io.github.gokulhk.spacesaver.core.testing.TestClock
import io.github.gokulhk.spacesaver.core.testing.aVideo
import io.github.gokulhk.spacesaver.core.testing.anImage
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteMediaItemsTest {
    private val gateway = FakeDeletionGateway()
    private val savings = FakeSavingsRepository()
    private val storage = FakeStorageRepository()
    private val delete = DeleteMediaItems(gateway, savings, storage, SavingsCalculator(TestClock()))
    private val items =
        listOf(aVideo(id = 1, size = ByteSize.gigabytes(2)), anImage(id = 2, size = ByteSize.megabytes(5)))

    @Test
    fun `approved deletion records the full size of each file and refreshes storage`() =
        runTest {
            val outcome = delete(items)

            assertThat(outcome).isEqualTo(DeletionOutcome.DELETED)
            assertThat(gateway.userDeletionRequests).containsExactly(items.map { it.uri })
            assertThat(savings.events.map { it.type }.distinct()).containsExactly(SavingsType.DELETION)
            assertThat(
                savings.events.map { it.bytesSaved },
            ).containsExactly(ByteSize.gigabytes(2), ByteSize.megabytes(5))
            assertThat(storage.refreshCount).isEqualTo(1)
        }

    @Test
    fun `declined deletion records nothing`() =
        runTest {
            gateway.outcome = DeletionOutcome.DECLINED

            val outcome = delete(items)

            assertThat(outcome).isEqualTo(DeletionOutcome.DECLINED)
            assertThat(savings.events).isEmpty()
            assertThat(storage.refreshCount).isEqualTo(0)
        }

    @Test
    fun `deleting nothing shows no dialog`() =
        runTest {
            assertThat(delete(emptyList())).isEqualTo(DeletionOutcome.DELETED)
            assertThat(gateway.userDeletionRequests).isEmpty()
        }
}
