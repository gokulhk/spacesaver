package io.github.gokulhk.spacesaver.feature.home

import io.github.gokulhk.spacesaver.core.domain.batch.BatchStatus
import io.github.gokulhk.spacesaver.core.domain.batch.ItemStatus
import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.estimate.SizeEstimate
import io.github.gokulhk.spacesaver.core.domain.plan.ConversionPlan
import io.github.gokulhk.spacesaver.core.domain.plan.DeferredCandidate
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.plan.PlannedBatch
import io.github.gokulhk.spacesaver.core.domain.repository.Batch
import io.github.gokulhk.spacesaver.core.domain.repository.BatchId
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItemId
import io.github.gokulhk.spacesaver.core.domain.savings.SavingsSummary
import io.github.gokulhk.spacesaver.core.domain.usecase.PendingReview
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanStatus
import io.github.gokulhk.spacesaver.core.domain.usecase.PlanSuggestion
import io.github.gokulhk.spacesaver.core.domain.usecase.StorageOverview
import io.github.gokulhk.spacesaver.core.domain.usecase.Suggestion
import io.github.gokulhk.spacesaver.core.domain.usecase.SuggestionGroup
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaId
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/** Realistic home states for previews, UI tests, and screenshots. */
@Suppress("MagicNumber") // Sample figures.
internal object HomePreviewData {
    /** The batch in [ready]'s pending review card. */
    val PENDING_BATCH = BatchId(3)

    /** 23 4K videos converting to Full HD. */
    val videos4k =
        suggestion(
            SuggestionGroup.VIDEOS_4K,
            ConversionOption.Video(VideoPreset.UHD_TO_FHD),
            listOf(VideoPreset.UHD_TO_FHD, VideoPreset.UHD_TO_HD).map(ConversionOption::Video),
            candidates(
                count = 23,
                MediaFormat.MP4_H264,
                original = ByteSize.megabytes(620),
                output = ByteSize.megabytes(150),
            ),
        )

    private val jpegPhotos =
        suggestion(
            SuggestionGroup.JPEG_PHOTOS,
            ConversionOption.Image(MediaFormat.HEIC),
            listOf(MediaFormat.HEIC, MediaFormat.WEBP_LOSSY).map(ConversionOption::Image),
            candidates(
                count = 412,
                MediaFormat.JPEG,
                original = ByteSize.kilobytes(4_200),
                output = ByteSize.kilobytes(2_100),
            ),
        )

    private val screenshots =
        suggestion(
            SuggestionGroup.SCREENSHOTS,
            ConversionOption.Image(MediaFormat.WEBP_LOSSLESS),
            listOf(ConversionOption.Image(MediaFormat.WEBP_LOSSLESS)),
            candidates(
                count = 120,
                MediaFormat.PNG,
                original = ByteSize.kilobytes(900),
                output = ByteSize.kilobytes(600),
            ),
        )

    private val plan =
        ConversionPlan(
            batches =
                listOf(
                    batch(videos4k.candidates.take(12), duration = 20),
                    batch(videos4k.candidates.drop(12), duration = 18),
                    batch(jpegPhotos.candidates.take(25), duration = 7),
                ),
            blocked = emptyList(),
            totalEstimatedSavings = ByteSize.gigabytes(13) + ByteSize.megabytes(400),
            totalEstimatedDuration = 45.minutes,
        )

    private val content =
        HomeUiState.Content(
            savings = SavingsSummary(lifetime = ByteSize.megabytes(12_400), today = ByteSize.megabytes(1_200)),
            storage =
                StorageOverview(
                    ByteSize.gigabytes(128),
                    free = ByteSize.gigabytes(44),
                    ByteSize.gigabytes(42),
                    ByteSize.gigabytes(12),
                ),
            pendingReviews =
                listOf(
                    PendingReview(PENDING_BATCH, itemCount = 4, potentialSavings = ByteSize.megabytes(1_600)),
                ),
            runningBatch = null,
            plan = PlanStatus.Ready(plan),
            suggestions = listOf(videos4k, jpegPhotos, screenshots).map { PlanSuggestion(it, included = true) },
            planExpanded = false,
            presetSheet = null,
            isStarting = false,
        )

    /** A typical first visit with a ready plan and a batch awaiting review. */
    val ready = content

    /** Nothing fits above the reserve. */
    val blocked =
        content.copy(
            pendingReviews = emptyList(),
            plan =
                PlanStatus.Blocked(
                    ConversionPlan(
                        batches = emptyList(),
                        blocked = videos4k.candidates.map { DeferredCandidate(it, ByteSize.gigabytes(8)) },
                        totalEstimatedSavings = ByteSize.ZERO,
                        totalEstimatedDuration = 0.minutes,
                    ),
                    freeUpAtLeast = ByteSize.megabytes(2_100),
                ),
        )

    /** Nothing worth converting. */
    val empty = content.copy(pendingReviews = emptyList(), plan = PlanStatus.Empty, suggestions = emptyList())

    /** The batch in [running]. */
    val RUNNING_BATCH = BatchId(4)

    /** A batch converting: 2 of 5 files done. */
    val running =
        content.copy(
            runningBatch =
                Batch(
                    RUNNING_BATCH,
                    BatchStatus.CONVERTING,
                    videos4k.candidates.take(5).mapIndexed { index, candidate ->
                        BatchItem(
                            id = BatchItemId(index.toLong()),
                            original = candidate.item,
                            option = candidate.option,
                            estimatedOutput = candidate.estimatedOutput,
                            status = if (index < 2) ItemStatus.CONVERTED else ItemStatus.QUEUED,
                        )
                    },
                    Instant.EPOCH,
                ),
        )

    /** The plan behind [ready], for Plan detail. */
    val readyOverview = PlanOverview(ready.suggestions, plan.batches.flatMap { it.items }, ready.plan)

    /** The plan behind [blocked]. */
    val blockedOverview = PlanOverview(blocked.suggestions, videos4k.candidates, blocked.plan)

    /** No plan at all. */
    val emptyOverview = PlanOverview(suggestions = emptyList(), candidates = emptyList(), status = PlanStatus.Empty)

    private fun suggestion(
        group: SuggestionGroup,
        option: ConversionOption,
        options: List<ConversionOption>,
        candidates: List<PlanCandidate>,
    ) = Suggestion(group, option, options, candidates, ByteSize(candidates.sumOf { it.estimatedSavings.bytes }))

    private fun batch(
        items: List<PlanCandidate>,
        duration: Int,
    ) = PlannedBatch(
        items = items,
        estimatedSavings = ByteSize(items.sumOf { it.estimatedSavings.bytes }),
        spaceNeeded = ByteSize(items.sumOf { it.estimatedOutput.bytes }) * SAFETY_FACTOR,
        estimatedDuration = duration.minutes,
    )

    private fun candidates(
        count: Int,
        format: MediaFormat,
        original: ByteSize,
        output: ByteSize,
    ): List<PlanCandidate> =
        (1..count).map { id ->
            val item =
                MediaItem(
                    id = MediaId(id.toLong()),
                    uri = "content://media/$id",
                    displayName = "item_$id",
                    relativePath = null,
                    format = format,
                    size = original,
                    resolution = null,
                    dateTaken = null,
                    dateModified = Instant.EPOCH,
                )
            val option =
                if (format == MediaFormat.MP4_H264) {
                    ConversionOption.Video(VideoPreset.UHD_TO_FHD)
                } else {
                    ConversionOption.Image(MediaFormat.HEIC)
                }
            PlanCandidate(item, option, SizeEstimate.exact(output))
        }

    private const val SAFETY_FACTOR = 1.2
}
