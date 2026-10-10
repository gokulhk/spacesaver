package io.github.gokulhk.spacesaver.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A batch of conversions.
 *
 * @property id row ID.
 * @property status a `BatchStatus` name.
 * @property createdAtMillis when it was planned, as epoch milliseconds.
 */
@Entity(tableName = "batches", indices = [Index("status")])
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: String,
    @ColumnInfo(name = "created_at_millis") val createdAtMillis: Long,
)

/**
 * One file in a batch, with a snapshot of the original's metadata so a batch can be reviewed
 * even if MediaStore changes in the meantime.
 *
 * @property id row ID.
 * @property batchId owning batch; items are deleted with it.
 * @property position order within the batch.
 * @property mediaId the original's MediaStore ID.
 * @property uri the original's content URI.
 * @property displayName the original's file name.
 * @property relativePath the original's folder.
 * @property format a `MediaFormat` name.
 * @property sizeBytes the original's size.
 * @property width the original's width, if known.
 * @property height the original's height, if known.
 * @property durationMillis video duration, if a video.
 * @property dateTakenMillis capture time, if known.
 * @property dateModifiedMillis last modification time.
 * @property optionKind `VIDEO` or `IMAGE`.
 * @property optionValue a `VideoPreset` or target `MediaFormat` name.
 * @property estimatedOutputBytes the planning estimate.
 * @property status an `ItemStatus` name.
 * @property outputUri the converted file, once written.
 * @property outputSizeBytes the converted file's size, once written.
 */
@Entity(
    tableName = "batch_items",
    foreignKeys = [
        ForeignKey(
            entity = BatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["batch_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("batch_id")],
)
data class BatchItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "batch_id") val batchId: Long,
    val position: Int,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "relative_path") val relativePath: String?,
    val format: String,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long?,
    @ColumnInfo(name = "date_taken_millis") val dateTakenMillis: Long?,
    @ColumnInfo(name = "date_modified_millis") val dateModifiedMillis: Long,
    @ColumnInfo(name = "option_kind") val optionKind: String,
    @ColumnInfo(name = "option_value") val optionValue: String,
    @ColumnInfo(name = "estimated_output_bytes") val estimatedOutputBytes: Long,
    val status: String,
    @ColumnInfo(name = "output_uri") val outputUri: String?,
    @ColumnInfo(name = "output_size_bytes") val outputSizeBytes: Long?,
    @ColumnInfo(name = "failure_reason") val failureReason: String? = null,
    @ColumnInfo(name = "failure_detail") val failureDetail: String? = null,
)

/**
 * A batch with its items (in no particular order; sort by [BatchItemEntity.position]).
 *
 * @property batch the batch row.
 * @property items its item rows.
 */
data class BatchWithItems(
    @Embedded val batch: BatchEntity,
    @Relation(parentColumn = "id", entityColumn = "batch_id") val items: List<BatchItemEntity>,
)
