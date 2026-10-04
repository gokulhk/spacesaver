package io.github.gokulhk.spacesaver.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A savings ledger row (plan Section 5.2).
 *
 * @property id row ID.
 * @property type `CONVERSION` or `DELETION`.
 * @property bytesSaved space freed.
 * @property timestampMillis when, as epoch milliseconds.
 * @property mediaId the original's MediaStore ID, if known.
 */
@Entity(tableName = "savings_events", indices = [Index("timestamp_millis")])
data class SavingsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "bytes_saved") val bytesSaved: Long,
    @ColumnInfo(name = "timestamp_millis") val timestampMillis: Long,
    @ColumnInfo(name = "media_id") val mediaId: Long?,
)
