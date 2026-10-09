package io.github.gokulhk.spacesaver.core.work

import io.github.gokulhk.spacesaver.core.domain.repository.BatchId

/**
 * The link that opens a batch's progress screen, e.g. from its notification: `spacesaver://batch/42`.
 * `MainActivity` declares a matching intent filter, and the navigation graph's deep link for batch
 * progress is built from [BASE_PATH].
 */
object BatchDeepLink {
    /** The link without the batch ID; navigation appends `/{batchId}` to match it. */
    const val BASE_PATH = "spacesaver://batch"

    private const val PREFIX = "$BASE_PATH/"

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
