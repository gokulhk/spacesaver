package io.github.gokulhk.spacesaver.core.work

import io.github.gokulhk.spacesaver.core.domain.repository.BatchId

/**
 * The link that opens a batch's progress screen, e.g. from its notification: `spacesaver://batch/42`.
 * `MainActivity` declares a matching intent filter; navigation reads the ID back with [parse].
 */
object BatchDeepLink {
    private const val PREFIX = "spacesaver://batch/"

    /** The link for [batchId]. */
    fun uri(batchId: BatchId): String = PREFIX + batchId.value

    /** The batch in [uri], or null if it isn't a batch link. */
    fun parse(uri: String?): BatchId? =
        uri
            ?.removePrefix(PREFIX)
            ?.takeIf { it != uri }
            ?.toLongOrNull()
            ?.let(::BatchId)
}
