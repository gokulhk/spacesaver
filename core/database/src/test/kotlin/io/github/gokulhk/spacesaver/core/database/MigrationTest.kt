package io.github.gokulhk.spacesaver.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Migration harness (plan Task 3.1): every exported schema opens, the chain of migrations reaches
 * the current version, and data written by an older version survives each step.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SpaceSaverDatabase::class.java)

    @Test
    fun `version 1 schema can be created from the exported schema`() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            val tables = mutableListOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
            assertThat(
                tables,
            ).containsAtLeast("savings_events", "batches", "batch_items", "converted_files", "calibration_samples")
        }
    }

    @Test
    fun `version 1 history survives the move to version 2, with no failure recorded for old items`() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO batches (id, status, created_at_millis) VALUES (1, 'COMPLETED', 1000)")
            db.execSQL(
                """
                INSERT INTO batch_items (id, batch_id, position, media_id, uri, display_name, relative_path, format,
                    size_bytes, width, height, duration_millis, date_taken_millis, date_modified_millis, option_kind,
                    option_value, estimated_output_bytes, status, output_uri, output_size_bytes)
                VALUES (7, 1, 0, 42, 'content://media/42', 'VID_1.mp4', 'DCIM/Camera/', 'MP4_H264',
                    900000000, 3840, 2160, 60000, NULL, 1000, 'VIDEO', 'UHD_TO_FHD', 200000000, 'FAILED', NULL, NULL)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, *Migrations.ALL).use { db ->
            val sql = "SELECT display_name, status, failure_reason, failure_detail FROM batch_items WHERE id = 7"
            db.query(sql).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("VID_1.mp4")
                assertThat(cursor.getString(1)).isEqualTo("FAILED")
                assertThat(cursor.isNull(2)).isTrue()
                assertThat(cursor.isNull(3)).isTrue()
            }
        }
    }

    @Test
    fun `every migration leads to the current schema`() {
        helper.createDatabase(TEST_DB, 1).close()

        val database =
            Room
                .databaseBuilder(ApplicationProvider.getApplicationContext(), SpaceSaverDatabase::class.java, TEST_DB)
                .addMigrations(*Migrations.ALL)
                .build()

        database.openHelper.writableDatabase.close()
        assertThat(database.openHelper.readableDatabase.version).isEqualTo(SpaceSaverDatabase.VERSION)
        database.close()
    }

    @Test
    fun `migrations form a contiguous chain to the current version`() {
        val steps = Migrations.ALL.map { it.startVersion to it.endVersion }

        assertThat(steps).isEqualTo((1 until SpaceSaverDatabase.VERSION).map { it to it + 1 })
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
