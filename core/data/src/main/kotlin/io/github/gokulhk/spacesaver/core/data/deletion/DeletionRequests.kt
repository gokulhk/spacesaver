package io.github.gokulhk.spacesaver.core.data.deletion

import android.content.IntentSender
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A system delete dialog waiting to be shown.
 *
 * @property intentSender launches `MediaStore.createDeleteRequest`'s confirmation.
 */
class PendingDeletion internal constructor(
    val intentSender: IntentSender,
    private val result: CompletableDeferred<DeletionOutcome>,
) {
    /** Whether the requester is still waiting; skip showing the dialog otherwise. */
    val isActive: Boolean get() = result.isActive

    /** Reports the user's answer: [approved] when the dialog returned `RESULT_OK`. */
    fun complete(approved: Boolean) {
        result.complete(if (approved) DeletionOutcome.DELETED else DeletionOutcome.DECLINED)
    }
}

/**
 * Connects [AndroidDeletionGateway] with the activity that can show system dialogs
 * (plan Task 6.1). The gateway [request]s and suspends; the activity collects [pending], launches
 * each dialog, and completes it with the result.
 */
@Singleton
class DeletionRequests
    @Inject
    constructor() {
        private val channel = Channel<PendingDeletion>(Channel.UNLIMITED)

        /** Dialogs to show, in order. */
        val pending: Flow<PendingDeletion> = channel.receiveAsFlow()

        /** Asks the user via [intentSender] and suspends until they answer. */
        suspend fun request(intentSender: IntentSender): DeletionOutcome {
            val result = CompletableDeferred<DeletionOutcome>()
            channel.send(PendingDeletion(intentSender, result))
            return try {
                result.await()
            } finally {
                // If the caller is cancelled first, the dialog is skipped when it comes up.
                result.cancel()
            }
        }
    }
