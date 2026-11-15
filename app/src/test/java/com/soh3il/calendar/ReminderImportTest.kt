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
}
