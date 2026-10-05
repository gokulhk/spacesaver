package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.database.dao.CalibrationDao
import io.github.gokulhk.spacesaver.core.database.entity.CalibrationEntity
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ConversionPair
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.domain.estimate.RatioCalibrator
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationSample
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/** Calibration from stored samples, reduced with [RatioCalibrator] (plan Section 5.4). */
class CalibrationRepositoryImpl
    @Inject
    constructor(
        private val dao: CalibrationDao,
        private val calibrator: RatioCalibrator,
        private val clock: Clock,
    ) : CalibrationRepository {
        override fun observeCalibration(): Flow<CalibrationTable> =
            dao.observeAll().map { samples ->
                val ratios =
                    samples
                        .filter { it.kind == KIND_RATIO }
                        .groupBy({ it.pairOrNull() }, { it.value })
                        .mapNotNull { (pair, values) ->
                            pair?.let { key -> calibrator.calibrate(values)?.let { key to it } }
                        }.toMap()
                CalibrationTable(ratios)
            }

        override fun observeProcessingSpeed(): Flow<ProcessingSpeed> =
            dao.observeAll().map { samples ->
                ProcessingSpeed.calibrated(
                    videoFactors = samples.filter { it.kind == KIND_VIDEO_SPEED }.map { it.value },
                    imageSeconds = samples.filter { it.kind == KIND_IMAGE_SPEED }.map { it.value },
                    calibrator = calibrator,
                )
            }

        override suspend fun record(sample: CalibrationSample) {
            val entity =
                when (sample) {
                    is CalibrationSample.Ratio -> {
                        sampleEntity(
                            KIND_RATIO,
                            sample.ratio,
                            sample.pair.source.name,
                            sample.pair.target.name,
                        )
                    }

                    is CalibrationSample.VideoSpeed -> {
                        sampleEntity(KIND_VIDEO_SPEED, sample.factor)
                    }

                    is CalibrationSample.ImageSpeed -> {
                        sampleEntity(KIND_IMAGE_SPEED, sample.seconds)
                    }
                }
            dao.insert(entity)
            dao.pruneKeepingNewest(entity.kind, entity.sourceFormat, entity.targetFormat, SAMPLES_PER_KEY)
        }

        private fun sampleEntity(
            kind: String,
            value: Double,
            source: String? = null,
            target: String? = null,
        ) = CalibrationEntity(
            kind = kind,
            sourceFormat = source,
            targetFormat = target,
            value = value,
            recordedAtMillis = clock.millis(),
        )

        private fun CalibrationEntity.pairOrNull(): ConversionPair? {
            val source = MediaFormat.entries.firstOrNull { it.name == sourceFormat }
            val target = MediaFormat.entries.firstOrNull { it.name == targetFormat }
            return if (source != null && target != null) ConversionPair(source, target) else null
        }

        /** Sample kinds stored in [CalibrationEntity.kind]. */
        companion object {
            /** Output/original size ratio for a conversion pair. */
            const val KIND_RATIO = "RATIO"

            /** Seconds of processing per second of video. */
            const val KIND_VIDEO_SPEED = "VIDEO_SPEED"

            /** Seconds of processing per image. */
            const val KIND_IMAGE_SPEED = "IMAGE_SPEED"

            /**
             * Samples kept per key. Twenty recent conversions give a stable median while still
             * following changes, e.g. after an OS update brings a faster encoder.
             */
            const val SAMPLES_PER_KEY = 20
        }
    }
