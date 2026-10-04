package io.github.gokulhk.spacesaver.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A file SpaceSaver wrote, so it is never suggested for conversion again (plan Section 5.8).
 * MediaStore IDs can change (e.g. after a backup restore), so the file is also identified by its
 * path, size, and modification time.
 *
 * @property id row ID.
 * @property outputMediaId the output's MediaStore ID.
 * @property relativePath the output's folder.
 * @property displayName the output's file name.
 * @property sizeBytes the output's size.
 * @property dateModifiedMillis the output's modification time.
 * @property sourceFormat the original's `MediaFormat` name.
 * @property targetFormat the output's `MediaFormat` name.
 * @property createdAtMillis when it was converted.
 */
@Entity(
    tableName = "converted_files",
    indices = [
        Index("output_media_id", unique = true),
        Index("relative_path", "display_name", "size_bytes", "date_modified_millis"),
    ],
)
data class ConvertedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "output_media_id") val outputMediaId: Long?,
    @ColumnInfo(name = "relative_path") val relativePath: String?,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "date_modified_millis") val dateModifiedMillis: Long,
    @ColumnInfo(name = "source_format") val sourceFormat: String,
    @ColumnInfo(name = "target_format") val targetFormat: String,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
)
