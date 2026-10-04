package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.repository.DeletionGateway
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import io.github.gokulhk.spacesaver.core.domain.repository.SavingsRepository
import io.github.gokulhk.spacesaver.core.domain.repository.StorageRepository
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsCalculator
import io.github.gokulhk.spacesaver.core.model.MediaItem
import javax.inject.Inject

/**
 * Permanently deletes files chosen in Browse after the system confirmation, and records each
 * file's full size as a DELETION saving (plan Sections 5.2 and 7.3).
 */
class DeleteMediaItems
    @Inject
    constructor(
        private val deletionGateway: DeletionGateway,
        private val savingsRepository: SavingsRepository,
        private val storageRepository: StorageRepository,
        private val savingsCalculator: SavingsCalculator,
    ) {
        /** Deletes [items]; records savings only if the user approves the system dialog. */
        suspend operator fun invoke(items: List<MediaItem>): DeletionOutcome {
            if (items.isEmpty()) return DeletionOutcome.DELETED
            val outcome = deletionGateway.requestUserDeletion(items.map { it.uri })
            if (outcome == DeletionOutcome.DELETED) {
                savingsRepository.record(items.map { savingsCalculator.deletionEvent(it.id, it.size) })
                storageRepository.refresh()
            }
            return outcome
        }
    }
