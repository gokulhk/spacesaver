package io.github.gokulhk.spacesaver.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.gokulhk.spacesaver.core.database.entity.CalibrationEntity
import kotlinx.coroutines.flow.Flow

/** Calibration samples. */
@Dao
interface CalibrationDao {
    /** Records a sample. */
    @Insert
    suspend fun insert(sample: CalibrationEntity)

    /** Every sample, newest first. */
    @Query("SELECT * FROM calibration_samples ORDER BY recorded_at_millis DESC, id DESC")
    fun observeAll(): Flow<List<CalibrationEntity>>

    /** Deletes all but the newest [keep] samples for one key, so calibration tracks recent behavior. */
    @Query(
        """
        DELETE FROM calibration_samples
        WHERE kind = :kind AND source_format IS :sourceFormat AND target_format IS :targetFormat
          AND id NOT IN (
            SELECT id FROM calibration_samples
            WHERE kind = :kind AND source_format IS :sourceFormat AND target_format IS :targetFormat
            ORDER BY recorded_at_millis DESC, id DESC
            LIMIT :keep
          )
        """,
    )
    suspend fun pruneKeepingNewest(
        kind: String,
        sourceFormat: String?,
        targetFormat: String?,
        keep: Int,
    )
}
