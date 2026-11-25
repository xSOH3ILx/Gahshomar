package com.soh3il.calendar.integrations

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, instrumentedPackages = ["com.soh3il"])
class CalendarExportTest {
    class Provider : ContentProvider() {
        val inserted = mutableListOf<ContentValues>()
        val updated = mutableListOf<ContentValues>()
        var existingEventId: Long? = null
        var existingDescription: String = "old\n[SOH3IL_REMINDER:2]"
        var secondEventId: Long? = null
        var updateCount = 1
        var nullEventQuery = false
        var nullInsert = false
        var eventQueryArgs: List<String> = emptyList()
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor? {
            if (uri == CalendarContract.Events.CONTENT_URI && nullEventQuery) return null
            if (uri == CalendarContract.Events.CONTENT_URI) eventQueryArgs = args?.toList().orEmpty()
            return MatrixCursor(projection).apply {
                if (uri == CalendarContract.Calendars.CONTENT_URI) addRow(arrayOf<Any>(7L, CalendarContract.Calendars.CAL_ACCESS_OWNER))
                if (uri == CalendarContract.Events.CONTENT_URI && existingEventId != null) addRow(arrayOf<Any?>(existingEventId, existingDescription))
                if (uri == CalendarContract.Events.CONTENT_URI && secondEventId != null) addRow(arrayOf<Any?>(secondEventId, existingDescription))
            }
        }
        override fun insert(uri: Uri, values: ContentValues?): Uri? {
            if (nullInsert) return null
            inserted.add(ContentValues(requireNotNull(values)))
            return Uri.withAppendedPath(uri, inserted.size.toString())
        }
        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?): Int {
            updated.add(ContentValues(requireNotNull(values)))
            return updateCount
        }
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = error("Deletion was not authorized")
        override fun getType(uri: Uri): String? = null
    }
    private val context get() = RuntimeEnvironment.getApplication()
    private fun reminder(type: RecurrenceType) = Reminder(id = 2, title = "fixture", jalaliYear = 1406,
        jalaliMonth = 1, jalaliDay = 1, hour = 9, minute = 0, recurrence = RecurrenceRule(type, interval = 3, daysOfWeek = setOf(3, 5)))
    private fun provider(): Provider {
        shadowOf(context).grantPermissions(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
        return Provider().also { ShadowContentResolver.registerProviderInternal(CalendarContract.AUTHORITY, it) }
    }
    @Test fun weeklyExportKeepsIntervalAndWeekdaysAndUsesDuration() = runBlocking {
        val provider = provider()
        assertEquals(1, CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.WEEKLY))).getOrThrow())
        val row = provider.inserted.single()
        assertEquals("FREQ=WEEKLY;INTERVAL=3;BYDAY=MO,WE;WKST=SU", row.getAsString(CalendarContract.Events.RRULE))
        assertEquals(com.soh3il.core.jalali.JalaliDate(1406, 1, 2).toEpochMillis(9), row.getAsLong(CalendarContract.Events.DTSTART))
        assertEquals("PT1H", row.getAsString(CalendarContract.Events.DURATION))
        assertNull(row.getAsLong(CalendarContract.Events.DTEND))
        assertEquals("Asia/Tehran", row.getAsString(CalendarContract.Events.EVENT_TIMEZONE))
    }
    @Test fun unsupportedJalaliRecurrenceFailsBeforeWritingAnyEvents() = runBlocking {
        val provider = provider()
        val result = CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE), reminder(RecurrenceType.MONTHLY_JALALI)))
        assertTrue(result.isFailure)
        assertTrue(provider.inserted.isEmpty())
    }

    @Test fun editingARecurringExportToOneOffClearsTheOldRuleInTheSameCalendar() = runBlocking {
        val provider = provider()
        CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.WEEKLY))).getOrThrow()
        provider.existingEventId = 1L
        assertEquals(1, CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE))).getOrThrow())
        val row = provider.updated.single()
        assertTrue(row.containsKey(CalendarContract.Events.RRULE))
        assertNull(row.getAsString(CalendarContract.Events.RRULE))
        assertNull(row.getAsString(CalendarContract.Events.DURATION))
        assertEquals(3_600_000L, row.getAsLong(CalendarContract.Events.DTEND) - row.getAsLong(CalendarContract.Events.DTSTART))
        assertEquals(listOf("7", "%[SOH3IL_REMINDER:2]%"), provider.eventQueryArgs)
        assertEquals(1, provider.inserted.size)
    }

    @Test fun clampingGregorianMonthEndsIsRejectedInsteadOfSilentlySkippingMonths() = runBlocking {
        val provider = provider()
        val date = com.soh3il.core.jalali.DateConverter.fromGregorian(2027, 1, 31).jalali
        val reminder = reminder(RecurrenceType.MONTHLY_GREGORIAN).copy(jalaliYear = date.year, jalaliMonth = date.month, jalaliDay = date.day)
        assertTrue(CalendarProviderSync.syncReminders(context, listOf(reminder)).isFailure)
        assertTrue(provider.inserted.isEmpty())
    }

    @Test fun aRejectedUpdateIsNotReportedAsASyncedEvent() = runBlocking {
        val provider = provider()
        provider.existingEventId = 1L
        provider.updateCount = 0
        assertTrue(CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE))).isFailure)
    }
    @Test fun nullProviderQueryDoesNotCauseDuplicateInsertion() = runBlocking {
        val provider = provider().apply { nullEventQuery = true }
        assertTrue(CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE))).isFailure)
        assertTrue(provider.inserted.isEmpty())
    }
    @Test fun nullInsertIsReportedAsFailure() = runBlocking {
        val provider = provider().apply { nullInsert = true }
        assertTrue(CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE))).isFailure)
        assertTrue(provider.inserted.isEmpty())
    }

    @Test fun sqlWildcardLookalikeIsNeverOverwritten() = runBlocking {
        val provider = provider().apply { existingEventId = 1L; existingDescription = "old\n[SOH3ILXREMINDER:2]" }
        assertEquals(1, CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE))).getOrThrow())
        assertTrue(provider.updated.isEmpty())
        assertEquals(1, provider.inserted.size)
    }
    @Test fun modifiedMarkerOrDuplicateMarkersAreNotOverwritten() = runBlocking {
        val provider = provider().apply { existingEventId = 1L; existingDescription = "old\n[SOH3IL_REMINDER:2]\nuser edit" }
        assertTrue(CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE))).isFailure)
        assertTrue(provider.updated.isEmpty()); assertTrue(provider.inserted.isEmpty())
        provider.existingDescription = "old\n[SOH3IL_REMINDER:2]"; provider.secondEventId = 2L
        assertTrue(CalendarProviderSync.syncReminders(context, listOf(reminder(RecurrenceType.NONE))).isFailure)
        assertTrue(provider.updated.isEmpty()); assertTrue(provider.inserted.isEmpty())
    }

}
