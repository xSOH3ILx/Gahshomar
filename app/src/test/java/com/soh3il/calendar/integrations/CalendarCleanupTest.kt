package com.soh3il.calendar.integrations

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Events as EventFields
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
class CalendarCleanupTest {
    class Provider : ContentProvider() {
        val db = SQLiteDatabase.create(null)
        var deleted = 0
        init {
            val fields = listOf(EventFields.CALENDAR_ID, EventFields.TITLE, EventFields.DESCRIPTION, EventFields.DTSTART, EventFields.DTEND, EventFields.DURATION,
                EventFields.RRULE, EventFields.EVENT_TIMEZONE, EventFields.EVENT_LOCATION, EventFields.ALL_DAY, EventFields.STATUS, EventFields.AVAILABILITY,
                EventFields.ACCESS_LEVEL, EventFields.HAS_ALARM, EventFields.HAS_ATTENDEE_DATA, EventFields.EVENT_COLOR, EventFields.ORGANIZER, EventFields.DELETED)
            db.execSQL("CREATE TABLE events (_id INTEGER PRIMARY KEY, " + fields.joinToString(", ") { "$it TEXT" } + ")")
        }
        fun add(id: Long, title: String, description: String) {
            db.insertOrThrow("events", null, ContentValues().apply {
                put(EventFields._ID, id); put(EventFields.CALENDAR_ID, 7); put(EventFields.TITLE, title); put(EventFields.DESCRIPTION, description)
                put(EventFields.DTSTART, 1_800_000_000_000L); put(EventFields.DELETED, "0")
            })
        }
        override fun onCreate() = true
        override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
            if (uri == CalendarContract.Calendars.CONTENT_URI) return MatrixCursor(projection).apply {
                if (projection?.size == 2) addRow(arrayOf<Any>(7L, CalendarContract.Calendars.CAL_ACCESS_OWNER))
                else addRow(arrayOf("Private calendar"))
            }
            return db.query("events", projection, selection, args, null, null, sort)
        }
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int = db.delete("events", selection, args).also { deleted += it }
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
        override fun getType(uri: Uri): String? = null
    }
    private val context get() = RuntimeEnvironment.getApplication()
    private fun setup(): Provider {
        shadowOf(context).grantPermissions(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR)
        return Provider().also { ShadowContentResolver.registerProviderInternal(CalendarContract.AUTHORITY, it) }
    }
    private fun marker(id: Long) = "details\n[SOH3IL_REMINDER:$id]"

    @Test fun previewDoesNotDeleteAndOnlyIncludesMatchingInactiveEvents() = runBlocking {
        val p = setup()
        p.add(1, "active", marker(10)); p.add(2, "disabled", marker(20)); p.add(3, "unrelated", "no marker")
        p.add(4, "embedded", marker(40) + "\nmore text")
        val preview = CalendarCleanup.preview(context, setOf(10))
        assertEquals("Private calendar", preview.calendarName)
        assertEquals(listOf(2L), preview.candidates.map { it.eventId })
        assertEquals(0, p.deleted)
        assertEquals(1 to 0, CalendarCleanup.deleteApproved(context, preview) { setOf(10) })
        assertEquals(1, p.deleted)
        assertEquals(3L, android.database.DatabaseUtils.queryNumEntries(p.db, "events"))
    }
    @Test fun changedEventOrReenabledReminderIsNotDeleted() = runBlocking {
        val p = setup(); p.add(1, "one", marker(10)); p.add(2, "two", marker(20))
        val preview = CalendarCleanup.preview(context, emptySet())
        p.db.update("events", ContentValues().apply { put(CalendarContract.Events.EVENT_LOCATION, "edited") }, "_id=?", arrayOf("1"))
        assertEquals(0 to 2, CalendarCleanup.deleteApproved(context, preview) { setOf(20) })
        assertEquals(0, p.deleted)
    }
    @Test fun previewIsBoundedToOnlyTwentyDisplayedEvents() = runBlocking {
        val p = setup(); (1L..25L).forEach { p.add(it, "event $it", marker(it)) }
        val preview = CalendarCleanup.preview(context, emptySet())
        assertEquals(20, preview.candidates.size)
        assertEquals(20 to 0, CalendarCleanup.deleteApproved(context, preview) { emptySet() })
        assertEquals(5L, android.database.DatabaseUtils.queryNumEntries(p.db, "events"))
    }
    @Test fun permissionDeniedNeverDeletesAnything() = runBlocking {
        val p = setup(); p.add(1, "one", marker(10))
        val preview = CalendarCleanup.preview(context, emptySet())
        shadowOf(context).denyPermissions(android.Manifest.permission.WRITE_CALENDAR)
        try { CalendarCleanup.deleteApproved(context, preview) { emptySet() }; fail("permission required") }
        catch (expected: SecurityException) { assertEquals(0, p.deleted) }
    }
}
