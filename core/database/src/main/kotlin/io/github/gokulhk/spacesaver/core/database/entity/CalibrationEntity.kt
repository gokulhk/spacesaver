package io.github.gokulhk.spacesaver.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One calibration measurement (plan Section 5.4): an output/original ratio for a conversion pair,
 * or a processing-speed sample.
 *
 * @property id row ID.
 * @property kind `RATIO`, `VIDEO_SPEED`, or `IMAGE_SPEED`.
 * @property sourceFormat for ratios, the source `MediaFormat` name.
 * @property targetFormat for ratios, the target `MediaFormat` name.
 * @property value the measurement.
 * @property recordedAtMillis when it was measured.
 */
@Entity(tableName = "calibration_samples", indices = [Index("kind", "source_format", "target_format")])
data class CalibrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    @ColumnInfo(name = "source_format") val sourceFormat: String?,
    @ColumnInfo(name = "target_format") val targetFormat: String?,
    val value: Double,
    @ColumnInfo(name = "recorded_at_millis") val recordedAtMillis: Long,
)
