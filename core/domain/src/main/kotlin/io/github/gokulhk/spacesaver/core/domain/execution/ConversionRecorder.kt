package io.github.gokulhk.spacesaver.core.domain.execution

import io.github.gokulhk.spacesaver.core.domain.conversion.ConversionSpec
import io.github.gokulhk.spacesaver.core.domain.estimate.ConversionPair
import io.github.gokulhk.spacesaver.core.domain.repository.BatchItem
import io.github.gokulhk.spacesaver.core.domain.repository.BatchRepository
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationSample
import io.github.gokulhk.spacesaver.core.domain.repository.PublishedOutput
import io.github.gokulhk.spacesaver.core.model.MediaType
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * Records what a completed conversion taught us: the output, so it is never suggested again
 * (plan Section 5.8 step 6), and calibration samples so later estimates improve (plan Section 5.4).
 */
class ConversionRecorder
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        private val calibrationRepository: CalibrationRepository,
    ) {
        /** Records [output] of [item] converted with [spec], which took [elapsed]. */
        suspend fun record(
            item: BatchItem,
            spec: ConversionSpec,
            output: PublishedOutput,
            elapsed: Duration,
        ) {
            batchRepository.recordConvertedFile(output, item.original.format)
            val seconds = elapsed.toDouble(DurationUnit.SECONDS)
            when (item.original.type) {
                MediaType.IMAGE -> {
                    val pair = ConversionPair(item.original.format, spec.targetFormat)
                    calibrationRepository.record(CalibrationSample.Ratio(pair, output.size.ratioTo(item.original.size)))
                    calibrationRepository.record(CalibrationSample.ImageSpeed(seconds))
                }

                MediaType.VIDEO -> {
                    val footage =
                        item.original.video
                            ?.duration
                            ?.toDouble(DurationUnit.SECONDS) ?: 0.0
                    if (footage > 0.0) calibrationRepository.record(CalibrationSample.VideoSpeed(seconds / footage))
                }
            }
        }
    }
