package io.github.gokulhk.spacesaver.core.domain.usecase

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionOption
import io.github.gokulhk.spacesaver.core.domain.conversion.TargetSelection
import io.github.gokulhk.spacesaver.core.domain.eligibility.Eligibility
import io.github.gokulhk.spacesaver.core.domain.eligibility.MediaEligibility
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.plan.PlanCandidate
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import io.github.gokulhk.spacesaver.core.domain.repository.EncoderCapabilities
import io.github.gokulhk.spacesaver.core.domain.repository.MediaRepository
import io.github.gokulhk.spacesaver.core.domain.repository.SettingsRepository
import io.github.gokulhk.spacesaver.core.model.ByteSize
import io.github.gokulhk.spacesaver.core.model.ImageContent
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import io.github.gokulhk.spacesaver.core.model.MediaItem
import io.github.gokulhk.spacesaver.core.model.MediaType
import io.github.gokulhk.spacesaver.core.model.VideoCodec
import io.github.gokulhk.spacesaver.core.model.VideoPreset
import io.github.gokulhk.spacesaver.core.model.sumOfSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** A kind of savings opportunity shown as one card on the home screen. */
enum class SuggestionGroup {
    /** 4K videos. */
    VIDEOS_4K,

    /** Full HD videos. */
    VIDEOS_FULL_HD,

    /** JPEG photos. */
    JPEG_PHOTOS,

    /** Photo-like PNGs. */
    PNG_PHOTOS,

    /** Screenshots and other PNG graphics (including unclassified PNGs). */
    SCREENSHOTS,
}

/**
 * A savings opportunity, e.g. "23 videos in 4K can be converted to Full HD to save ~10.8 GB".
 *
 * @property group what kind of files.
 * @property option the selected conversion.
 * @property availableOptions every option the user can pick in the preset sheet.
 * @property candidates eligible files under [option].
 * @property totalSavings estimated savings of all candidates.
 */
data class Suggestion(
    val group: SuggestionGroup,
    val option: ConversionOption,
    val availableOptions: List<ConversionOption>,
    val candidates: List<PlanCandidate>,
    val totalSavings: ByteSize,
)

/**
 * Smart suggestions (plan Section 1.3): groups eligible files by kind, applies the user's
 * chosen option per group (or the default), and orders groups by savings. Files already in an
 * unfinished batch are left out, so nothing is converted twice.
 */
class ObserveSuggestions
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
        private val settingsRepository: SettingsRepository,
        private val calibrationRepository: CalibrationRepository,
        private val encoderCapabilities: EncoderCapabilities,
        private val eligibility: MediaEligibility,
        private val batchRepository: BatchRepository,
    ) {
        /**
         * Suggestions, recomputed when the library, settings, or calibration change.
         *
         * @param selections options the user picked per group; other groups use their default.
         */
        operator fun invoke(selections: Map<SuggestionGroup, ConversionOption> = emptyMap()): Flow<List<Suggestion>> =
            flow {
                val codec = TargetSelection.videoCodec(encoderCapabilities.hasHardwareHevcEncoder())
                val heicSupported = encoderCapabilities.supportsHeicEncoding()
                val suggestions =
                    combine(
                        mediaRepository.observeMedia(MediaType.VIDEO),
                        mediaRepository.observeMedia(MediaType.IMAGE),
                        settingsRepository.settings,
                        calibrationRepository.observeCalibration(),
                        batchRepository
                            .observeActiveBatches()
                            .map { batches -> batches.flatMap { batch -> batch.items.map { it.original.id } }.toSet() }
                            .distinctUntilChanged(),
                    ) { allVideos, allImages, settings, calibration, reserved ->
                        val videos = allVideos.filter { it.id !in reserved }
                        val images = allImages.filter { it.id !in reserved }
                        val jpegTarget = TargetSelection.jpegTarget(settings.imageFormat, heicSupported)
                        val context = Context(codec, jpegTarget, heicSupported, calibration, selections)
                        (videoGroups(videos) + imageGroups(images))
                            .mapNotNull { (group, items) -> suggestionFor(group, items, context) }
                            .sortedByDescending { it.totalSavings }
                    }
                emitAll(suggestions)
            }

        private fun suggestionFor(
            group: SuggestionGroup,
            items: List<MediaItem>,
            context: Context,
        ): Suggestion? {
            val options = context.optionsFor(group)
            val option = context.selections[group]?.takeIf { it in options } ?: options.first()
            val candidates = items.mapNotNull { candidateFor(it, option, context) }
            if (candidates.isEmpty()) return null
            return Suggestion(group, option, options, candidates, candidates.sumOfSize { it.estimatedSavings })
        }

        private fun candidateFor(
            item: MediaItem,
            option: ConversionOption,
            context: Context,
        ): PlanCandidate? {
            val result = eligibility.evaluate(item, option, context.codec, context.calibration)
            return (result as? Eligibility.Eligible)?.let { PlanCandidate(item, option, it.estimate) }
        }

        private fun videoGroups(videos: List<MediaItem>): List<Pair<SuggestionGroup, List<MediaItem>>> {
            val byGroup =
                videos.groupBy { video ->
                    val shortEdge = video.resolution?.shortEdge ?: 0
                    when {
                        shortEdge >= VideoPreset.UHD_TO_FHD.minSourceShortEdge -> SuggestionGroup.VIDEOS_4K
                        shortEdge >= VideoPreset.FHD_TO_HD.minSourceShortEdge -> SuggestionGroup.VIDEOS_FULL_HD
                        else -> null
                    }
                }
            return byGroup.mapNotNull { (group, items) -> group?.let { it to items } }
        }

        private fun imageGroups(images: List<MediaItem>): List<Pair<SuggestionGroup, List<MediaItem>>> {
            val byGroup =
                images.groupBy { image ->
                    when {
                        image.format == MediaFormat.JPEG -> SuggestionGroup.JPEG_PHOTOS
                        image.format != MediaFormat.PNG -> null
                        image.imageContent == ImageContent.PHOTO -> SuggestionGroup.PNG_PHOTOS
                        else -> SuggestionGroup.SCREENSHOTS
                    }
                }
            return byGroup.mapNotNull { (group, items) -> group?.let { it to items } }
        }

        /** Inputs shared by every group in one computation. */
        private data class Context(
            val codec: VideoCodec,
            val jpegTarget: MediaFormat,
            val heicSupported: Boolean,
            val calibration: CalibrationTable,
            val selections: Map<SuggestionGroup, ConversionOption>,
        ) {
            /** Options for [group], default first. */
            fun optionsFor(group: SuggestionGroup): List<ConversionOption> =
                when (group) {
                    SuggestionGroup.VIDEOS_4K -> {
                        listOf(
                            VideoPreset.UHD_TO_FHD,
                            VideoPreset.UHD_TO_HD,
                        ).map(ConversionOption::Video)
                    }

                    SuggestionGroup.VIDEOS_FULL_HD -> {
                        listOf(ConversionOption.Video(VideoPreset.FHD_TO_HD))
                    }

                    SuggestionGroup.JPEG_PHOTOS -> {
                        jpegOptions()
                    }

                    SuggestionGroup.PNG_PHOTOS -> {
                        listOf(ConversionOption.Image(MediaFormat.WEBP_LOSSY))
                    }

                    SuggestionGroup.SCREENSHOTS -> {
                        listOf(ConversionOption.Image(MediaFormat.WEBP_LOSSLESS))
                    }
                }

            private fun jpegOptions(): List<ConversionOption> {
                val supported =
                    if (heicSupported) {
                        listOf(
                            MediaFormat.HEIC,
                            MediaFormat.WEBP_LOSSY,
                        )
                    } else {
                        listOf(MediaFormat.WEBP_LOSSY)
                    }
                return (listOf(jpegTarget) + supported).distinct().map(ConversionOption::Image)
            }
        }
    }
