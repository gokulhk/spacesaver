package io.github.gokulhk.spacesaver.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.gokulhk.spacesaver.core.database.entity.ConvertedFileEntity
import kotlinx.coroutines.flow.Flow

/** Files SpaceSaver produced. */
@Dao
interface ConvertedFileDao {
    /** Records a converted file; replaces an earlier row with the same output ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: ConvertedFileEntity)

    /** The record for the output with MediaStore [mediaId], or null. */
    @Query("SELECT * FROM converted_files WHERE output_media_id = :mediaId")
    suspend fun findByMediaId(mediaId: Long): ConvertedFileEntity?

    /** Fallback lookup when the MediaStore ID changed. `IS` matches a null folder too. */
    @Query(
        """
        SELECT * FROM converted_files
        WHERE relative_path IS :relativePath AND display_name = :displayName
          AND size_bytes = :sizeBytes AND date_modified_millis = :dateModifiedMillis
        LIMIT 1
        """,
    )
    suspend fun findByFingerprint(
        relativePath: String?,
        displayName: String,
        sizeBytes: Long,
        dateModifiedMillis: Long,
    ): ConvertedFileEntity?

    /** Every converted file, for marking library items. */
    @Query("SELECT * FROM converted_files")
    fun observeAll(): Flow<List<ConvertedFileEntity>>
}
