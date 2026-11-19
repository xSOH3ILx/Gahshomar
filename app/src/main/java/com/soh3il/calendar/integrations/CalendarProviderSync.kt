package com.soh3il.calendar.integrations

import android.annotation.SuppressLint
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.jalali.toDeviceCalendarRrule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object CalendarProviderSync {
    suspend fun syncReminders(context: Context, reminders: List<Reminder>): Result<Int> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext Result.failure(SecurityException("مجوز تقویم صادر نشده است"))
        }

        try {
            // Validate the whole selection before writing any event.
            val export = reminders.filter { it.id != 0L && it.isEnabled }.map { reminder ->
                val rule = reminder.toDeviceCalendarRrule()
                val anchor = JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)
                    .toEpochMillis(reminder.hour, reminder.minute, 0, JalaliDate.TEHRAN_ZONE)
                val start = if (reminder.recurrence.type == RecurrenceType.WEEKLY) {
                    requireNotNull(JalaliRecurrenceEngine.calculateNextTrigger(reminder, anchor - 1L)) {
                        "قاعدهٔ تکرار هفتگی تاریخ معتبری ندارد"
                    }
                } else anchor
                Triple(reminder, rule, start)
            }
            val calendarId = findWritableCalendarId(context) ?:
                return@withContext Result.failure(IllegalStateException("تقویم قابل ویرایش پیدا نشد"))
            var synced = 0
            export.forEach { (reminder, rrule, start) ->
                val marker = "[SOH3IL_REMINDER:${reminder.id}]"
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, reminder.title)
                    put(CalendarContract.Events.DESCRIPTION, "${reminder.description}\n$marker")
                    put(CalendarContract.Events.DTSTART, start)
                    put(CalendarContract.Events.EVENT_TIMEZONE, JalaliDate.TEHRAN_ZONE.id)
                    if (rrule == null) {
                        put(CalendarContract.Events.DTEND, start + 60 * 60 * 1000L)
                        putNull(CalendarContract.Events.RRULE)
                        putNull(CalendarContract.Events.DURATION)
                    } else {
                        put(CalendarContract.Events.RRULE, rrule)
                        put(CalendarContract.Events.DURATION, "PT1H")
                        putNull(CalendarContract.Events.DTEND)
                    }
                }

                val existingId = findEventId(context, calendarId, marker)
                val uri = if (existingId != null) {
                    val updated = context.contentResolver.update(
                        CalendarContract.Events.CONTENT_URI,
                        values,
                        "${CalendarContract.Events._ID} = ?",
                        arrayOf(existingId.toString())
                    )
                    if (updated != 1) throw IOException("رویداد تقویم به‌روزرسانی نشد؛ دوباره تلاش کنید.")
                    CalendarContract.Events.CONTENT_URI.buildUpon().appendPath(existingId.toString()).build()
                } else {
                    context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                }
                if (uri == null) throw IOException("رویداد در تقویم دستگاه ثبت نشد؛ دوباره تلاش کنید.")
                synced++
            }
            Result.success(synced)
        } catch (error: SecurityException) {
            Result.failure(SecurityException("دسترسی به تقویم قطع شده است؛ مجوز آن را در تنظیمات گوشی بررسی کنید.", error))
        } catch (error: IllegalArgumentException) {
            Result.failure(error)
        } catch (error: IOException) {
            Result.failure(error)
        }
    }

    @SuppressLint("Recycle")
    internal fun findWritableCalendarId(context: Context): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            "${CalendarContract.Calendars._ID} ASC"
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    @SuppressLint("Recycle")
    private fun findEventId(context: Context, calendarId: Long, marker: String): Long? {
        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID, CalendarContract.Events.DESCRIPTION),
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(calendarId.toString(), "%$marker%"),
            null
        ) ?: throw IOException("خواندن رویدادهای تقویم دستگاه ممکن نشد؛ دوباره تلاش کنید.")
        var match: Long? = null
        cursor.use {
            while (it.moveToNext()) {
                val description = it.getString(1).orEmpty()
                if (description.substringAfterLast('\n') == marker) {
                    if (match != null) throw IOException("چند رویداد با نشانگر یکسان وجود دارد؛ تقویم دستگاه را بررسی کنید.")
                    match = it.getLong(0)
                } else if (description.contains(marker)) {
                    throw IOException("نشانگر رویداد تغییر کرده است؛ تقویم دستگاه را بررسی کنید.")
                }
            }
        }
        return match
    }
}
