package com.soh3il.calendar

import android.app.Application
import android.net.Uri
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.jalali.IcsParser
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, instrumentedPackages = ["com.soh3il"])
class ReminderImportTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val db get() = Soh3ilDatabase.getInstance(context)
    @Before fun clearReminders() = runBlocking {
        db.reminderDao().getAllRemindersSync().forEach { db.reminderDao().deleteReminderById(it.id) }
    }
    private fun reminder() = Reminder(title = "fixture", jalaliYear = 1406, jalaliMonth = 1,
        jalaliDay = 1, hour = 9, minute = 0)
    private suspend fun import(items: List<Reminder>): Pair<Int, Int> {
        UserPreferences.setPersistentNotificationEnabled(context, false)
        val uri = Uri.parse("content://audit/events")
        shadowOf(context.contentResolver).registerInputStream(uri,
            ByteArrayInputStream(IcsParser.exportToIcsString(items).toByteArray()))
        return ReminderMutations.importIcs(context, db, uri).getOrThrow()
    }
    @Test fun duplicatesInsideOneFileAreInsertedOnlyOnce() = runBlocking {
        assertEquals(2 to 1, import(listOf(reminder(), reminder())))
        assertEquals(1, db.reminderDao().getAllRemindersSync().size)
        assertEquals(2 to 0, import(listOf(reminder(), reminder())))
    }
    @Test fun differentDescriptionsAtTheSameDateAndTitleAreNotDiscarded() = runBlocking {
        assertEquals(1 to 1, import(listOf(reminder())))
        assertEquals(1 to 1, import(listOf(reminder().copy(description = "distinct"))))
        assertEquals(2, db.reminderDao().getAllRemindersSync().size)
    }
    @Test fun databaseFailureRollsBackTheEntireImportBatch() = runBlocking {
        val sql = db.openHelper.writableDatabase
        sql.execSQL("CREATE TRIGGER fail_import BEFORE INSERT ON reminders WHEN NEW.title = 'reject' BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try {
            try {
                import(listOf(reminder().copy(title = "first"), reminder().copy(title = "reject")))
                fail("Expected an insert failure")
            } catch (expected: Exception) {
                assertTrue(db.reminderDao().getAllRemindersSync().isEmpty())
            }
        } finally {
            sql.execSQL("DROP TRIGGER IF EXISTS fail_import")
        }
    }

    @Test fun togglingCountdownPersistsAndMigrationAddsDefaultZero(): Unit = runBlocking {
        UserPreferences.setPersistentNotificationEnabled(context, false)
        val id = db.reminderDao().insertReminder(com.soh3il.core.database.entity.ReminderEntity.fromDomain(reminder()))
        assertFalse(db.reminderDao().getReminderById(id)!!.showCountdown)

        ReminderMutations.setCountdown(context, db, id, true)
        assertTrue(db.reminderDao().getReminderById(id)!!.showCountdown)

        val dbName = "migration_test_${System.nanoTime()}.db"
        val factory = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
        val v1Helper = factory.create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS reminders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, " +
                                "description TEXT NOT NULL, jalaliYear INTEGER NOT NULL, jalaliMonth INTEGER NOT NULL, " +
                                "jalaliDay INTEGER NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, " +
                                "recurrenceRule TEXT NOT NULL, preReminders TEXT NOT NULL, isEnabled INTEGER NOT NULL, " +
                                "colorHex TEXT NOT NULL, nextTriggerEpochMillis INTEGER NOT NULL)"
                        )
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        v1Helper.writableDatabase.use { v1Db ->
            v1Db.execSQL(
                "INSERT INTO reminders (title, description, jalaliYear, jalaliMonth, jalaliDay, hour, minute, recurrenceRule, preReminders, isEnabled, colorHex, nextTriggerEpochMillis) " +
                    "VALUES ('legacy', '', 1406, 1, 1, 9, 0, 'NONE|1|||SNAP_TO_LAST_DAY', '', 1, '#2563EB', 0)"
            )
            Soh3ilDatabase.MIGRATION_1_2.migrate(v1Db)
            v1Db.query("SELECT title, showCountdown FROM reminders WHERE title = 'legacy'").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("legacy", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
            }
        }
        context.deleteDatabase(dbName)
    }

}
