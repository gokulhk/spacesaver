package io.github.gokulhk.spacesaver.core.data.repository

import io.github.gokulhk.spacesaver.core.database.dao.CalibrationDao
import io.github.gokulhk.spacesaver.core.database.entity.CalibrationEntity
import io.github.gokulhk.spacesaver.core.domain.estimate.CalibrationTable
import io.github.gokulhk.spacesaver.core.domain.estimate.ConversionPair
import io.github.gokulhk.spacesaver.core.domain.estimate.ProcessingSpeed
import io.github.gokulhk.spacesaver.core.domain.estimate.RatioCalibrator
import io.github.gokulhk.spacesaver.core.domain.repository.CalibrationRepository
import io.github.gokulhk.spacesaver.core.model.MediaFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Calibration from stored samples, reduced with [RatioCalibrator] (plan Section 5.4). */
class CalibrationRepositoryImpl
    @Inject
    constructor(
        private val dao: CalibrationDao,
        private val calibrator: RatioCalibrator,
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
        }
    }
