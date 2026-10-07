package io.github.gokulhk.spacesaver.core.data.media

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.content.ContentResolver as Resolver

/**
 * A stand-in for MediaStore backed by in-memory SQLite. It honors the same `Bundle` query
 * arguments as the real provider on API 30+ (selection, sort order, limit, offset), so sorting
 * and paging are exercised through real SQL rather than reimplemented in the test.
 */
class FakeMediaStoreProvider : ContentProvider() {
    private lateinit var db: SQLiteDatabase

    /** When true, every query throws as if media permission was revoked. */
    var denyAccess = false

    /** Every `Bundle` passed to a query, for asserting on query arguments. */
    val queries = mutableListOf<Bundle?>()

    override fun onCreate(): Boolean {
        db = SQLiteDatabase.create(null)
        TABLES.forEach { table ->
            db.execSQL(
                """
                CREATE TABLE $table (
                    _id INTEGER PRIMARY KEY, _display_name TEXT, relative_path TEXT, mime_type TEXT,
                    _size INTEGER, width INTEGER, height INTEGER, datetaken INTEGER, date_modified INTEGER,
                    duration INTEGER, bitrate INTEGER, capture_framerate REAL
                )
                """.trimIndent(),
            )
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor {
        if (denyAccess) throw SecurityException("Permission denial: reading ${uri.authority}")
        queries += queryArgs
        val sql =
            buildString {
                append("SELECT ${projection?.joinToString() ?: "*"} FROM ${tableFor(uri)}")
                queryArgs?.getString(Resolver.QUERY_ARG_SQL_SELECTION)?.let { append(" WHERE $it") }
                queryArgs?.getString(Resolver.QUERY_ARG_SQL_SORT_ORDER)?.let { append(" ORDER BY $it") }
                if (queryArgs?.containsKey(Resolver.QUERY_ARG_LIMIT) == true) {
                    append(" LIMIT ${queryArgs.getInt(Resolver.QUERY_ARG_LIMIT)}")
                    append(" OFFSET ${queryArgs.getInt(Resolver.QUERY_ARG_OFFSET, 0)}")
                }
            }
        return db.rawQuery(sql, queryArgs?.getStringArray(Resolver.QUERY_ARG_SQL_SELECTION_ARGS))
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = error("SpaceSaver must use Bundle query arguments")

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri {
        val id = db.insertOrThrow(tableFor(uri), null, values)
        context!!.contentResolver.notifyChange(uri, null)
        return Uri.withAppendedPath(uri, id.toString())
    }

    override fun shutdown() {
        db.close()
    }

    override fun getType(uri: Uri): String? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        val id = uri.lastPathSegment?.toLongOrNull() ?: return 0
        val deleted = db.delete(tableFor(uri), "_id = ?", arrayOf(id.toString()))
        if (deleted > 0) context!!.contentResolver.notifyChange(uri, null)
        return deleted
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun tableFor(uri: Uri): String =
        when (uri.pathSegments.getOrNull(1)) {
            "video" -> "video"
            "images" -> "images"
            else -> error("Unexpected URI $uri")
        }

    /** Helpers for inserting rows. */
    companion object {
        private val TABLES = listOf("video", "images")

        /** The external video collection. */
        val VIDEOS: Uri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        /** The external images collection. */
        val IMAGES: Uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }
}

/** Inserts a MediaStore-like row; null values become SQL NULL. */
@Suppress("LongParameterList") // Mirrors MediaStore's columns.
fun Resolver.insertMedia(
    collection: Uri,
    id: Long,
    size: Long?,
    mime: String? = if (collection == FakeMediaStoreProvider.VIDEOS) "video/mp4" else "image/jpeg",
    name: String? = "file_$id",
    path: String? = "DCIM/Camera/",
    width: Int? = 3840,
    height: Int? = 2160,
    dateTakenMillis: Long? = 1_700_000_000_000,
    dateModifiedSeconds: Long = 1_700_000_100,
    durationMillis: Long? = null,
    bitrate: Long? = null,
    frameRate: Float? = null,
) {
    val values =
        ContentValues().apply {
            put("_id", id)
            put("_display_name", name)
            put("relative_path", path)
            put("mime_type", mime)
            put("_size", size)
            put("width", width)
            put("height", height)
            put("datetaken", dateTakenMillis)
            put("date_modified", dateModifiedSeconds)
            put("duration", durationMillis)
            put("bitrate", bitrate)
            put("capture_framerate", frameRate)
        }
    insert(collection, values)
}
