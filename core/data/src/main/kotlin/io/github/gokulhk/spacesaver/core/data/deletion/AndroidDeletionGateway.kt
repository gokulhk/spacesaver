package io.github.gokulhk.spacesaver.core.data.deletion

import android.content.ContentResolver
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionGateway
import io.github.gokulhk.spacesaver.core.domain.repository.DeletionOutcome
import javax.inject.Inject

/** Builds the system confirmation for deleting files; a seam so the gateway is JVM-testable. */
fun interface DeleteRequestFactory {
    /** An IntentSender that asks the user to permanently delete [uris]. */
    fun create(uris: List<Uri>): IntentSender
}

/** The real factory: `MediaStore.createDeleteRequest`, one dialog for any number of files (ADR-0001). */
class MediaStoreDeleteRequestFactory
    @Inject
    constructor(
        private val resolver: ContentResolver,
    ) : DeleteRequestFactory {
        override fun create(uris: List<Uri>): IntentSender = MediaStore.createDeleteRequest(resolver, uris).intentSender
    }

/**
 * Deletes files (plan Task 6.1). Files the user owns go through one system confirmation dialog, and
 * deletion is permanent, never to the trash (ADR-0003). Files SpaceSaver wrote are deleted directly.
 */
class AndroidDeletionGateway
    @Inject
    constructor(
        private val resolver: ContentResolver,
        private val factory: DeleteRequestFactory,
        private val requests: DeletionRequests,
    ) : DeletionGateway {
        override suspend fun requestUserDeletion(uris: List<String>): DeletionOutcome {
            if (uris.isEmpty()) return DeletionOutcome.DELETED
            return requests.request(factory.create(uris.map { it.toUri() }))
        }

        override suspend fun deleteOwnFiles(uris: List<String>) {
            uris.forEach { resolver.delete(it.toUri(), null) }
        }
    }
