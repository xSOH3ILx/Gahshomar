package com.soh3il.calendar.integrations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.io.IOException

/** A snapshot shown to the user before any external event may be deleted. */
internal data class CalendarCleanupCandidate(
    val eventId: Long,
    val reminderId: Long,
    val title: String,
    val startEpoch: Long,
    internal val values: List<String?>
)

internal data class CalendarCleanupPreview(
    val calendarName: String,
    val calendarId: Long,
    val candidates: List<CalendarCleanupCandidate>
)

internal object CalendarCleanup {
    private const val MAX_CANDIDATES = 20
    private val marker = Regex("\\[SOH3IL_REMINDER:([1-9][0-9]*)]$")
    // All observed values are also used in the DELETE predicate. If an observed
    // value changes between preview and confirmation, the event is skipped.
    private val columns = arrayOf(
        CalendarContract.Events._ID, CalendarContract.Events.CALENDAR_ID,
        CalendarContract.Events.TITLE, CalendarContract.Events.DESCRIPTION,
        CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND,
        CalendarContract.Events.DURATION, CalendarContract.Events.RRULE,
        CalendarContract.Events.EVENT_TIMEZONE, CalendarContract.Events.EVENT_LOCATION,
        CalendarContract.Events.ALL_DAY, CalendarContract.Events.STATUS,
        CalendarContract.Events.AVAILABILITY, CalendarContract.Events.ACCESS_LEVEL,
        CalendarContract.Events.HAS_ALARM, CalendarContract.Events.HAS_ATTENDEE_DATA,
        CalendarContract.Events.EVENT_COLOR, CalendarContract.Events.ORGANIZER
    )

    private fun requirePermission(context: Context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            throw SecurityException("مجوز تقویم صادر نشده است")
    }

    fun preview(context: Context, activeIds: Set<Long>): CalendarCleanupPreview {
        requirePermission(context)
        val resolver = context.contentResolver
        val calendarId = CalendarProviderSync.findWritableCalendarId(context)
            ?: throw IOException("تقویم قابل ویرایش پیدا نشد")
        val name = resolver.query(CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
            "${CalendarContract.Calendars._ID} = ?", arrayOf(calendarId.toString()), null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?: throw IOException("نام تقویم دستگاه خوانده نشد")
        val candidates = mutableListOf<CalendarCleanupCandidate>()
        val cursor = resolver.query(CalendarContract.Events.CONTENT_URI, columns,
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0 AND ${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(calendarId.toString(), "%[SOH3IL_REMINDER:%"), "${CalendarContract.Events._ID} ASC")
            ?: throw IOException("خواندن رویدادهای تقویم ممکن نشد")
        cursor.use {
            while (it.moveToNext() && candidates.size < MAX_CANDIDATES) {
                val values = columns.indices.map { index -> if (it.isNull(index)) null else it.getString(index) }
                val id = values[0]?.toLongOrNull()?.takeIf { id -> id > 0 } ?: continue
                if (values[1]?.toLongOrNull() != calendarId) continue
                val desc = values[3] ?: continue
                // Older exports add the marker as the final line; an embedded marker
                // in somebody else's description is not enough to nominate an event.
                val lastLine = desc.substringAfterLast('\n')
                val reminderId = marker.matchEntire(lastLine)?.groupValues?.get(1)?.toLongOrNull() ?: continue
                if (reminderId in activeIds) continue
                val start = values[4]?.toLongOrNull() ?: continue
                candidates += CalendarCleanupCandidate(id, reminderId, values[2].orEmpty(), start, values)
            }
        }
        return CalendarCleanupPreview(name, calendarId, candidates)
    }

    /** Deletes only the displayed snapshots and rechecks active reminder IDs per item. */
    suspend fun deleteApproved(context: Context, preview: CalendarCleanupPreview,
                               activeIds: suspend () -> Set<Long>): Pair<Int, Int> {
        requirePermission(context)
        var deleted = 0
        var skipped = 0
        for (candidate in preview.candidates) {
            if (candidate.reminderId in activeIds()) { skipped++; continue }
            require(candidate.values.size == columns.size && candidate.values[0] == candidate.eventId.toString() &&
                candidate.values[1] == preview.calendarId.toString()) { "فهرست تأییدشده معتبر نیست" }
            val args = mutableListOf<String>()
            val where = columns.indices.joinToString(" AND ") { index ->
                val value = candidate.values[index]
                if (value == null) "${columns[index]} IS NULL" else {
                    args += value
                    "${columns[index]} = ?"
                }
            }
            val count = context.contentResolver.delete(CalendarContract.Events.CONTENT_URI, where, args.toTypedArray())
            if (count == 1) deleted++ else if (count == 0) skipped++
            else throw IOException("حذف رویداد تقویم نتیجهٔ نامعتبر داشت")
        }
        return deleted to skipped
    }
}
