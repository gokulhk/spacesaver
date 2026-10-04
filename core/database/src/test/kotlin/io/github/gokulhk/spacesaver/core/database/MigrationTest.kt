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
 * Migration harness (plan Task 3.1). With only version 1 it checks the exported schema opens; when
 * version 2 arrives, add a `MIGRATION_1_2` to [Migrations.ALL] and a test that creates version 1,
 * inserts data, and calls `runMigrationsAndValidate`.
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
