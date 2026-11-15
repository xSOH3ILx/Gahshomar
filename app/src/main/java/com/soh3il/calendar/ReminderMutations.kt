package com.soh3il.calendar

import android.content.Context
import android.net.Uri
import com.soh3il.calendar.widget.CalendarAppWidgetProvider
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.entity.ReminderEntity
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.model.Reminder
import com.soh3il.core.scheduler.AlarmScheduler
import com.soh3il.core.scheduler.PersistentNotificationHelper
import com.soh3il.core.scheduler.IcsHelper
import java.io.IOException

internal object ReminderMutations {
    suspend fun importIcs(context: Context, db: Soh3ilDatabase, uri: Uri): Result<Pair<Int, Int>> {
        val imported = try {
            IcsHelper.importFromIcsUri(context, uri)
        } catch (error: Exception) {
            if (error !is IOException && error !is IllegalArgumentException && error !is SecurityException) throw error
            return Result.failure(error)
        }
        val seen = db.reminderDao().getAllRemindersSync().map { importIdentity(it.toDomain()) }.toMutableSet()
        var addedCount = 0
        imported.forEach { rem ->
            if (seen.add(importIdentity(rem))) {
                val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(rem))
                AlarmScheduler.scheduleReminder(context, rem.copy(id = id))
                addedCount++
            }
        }
        refresh(context)
        return Result.success(imported.size to addedCount)
    }

    private fun importIdentity(reminder: Reminder): Pair<Reminder, List<Long>> =
        reminder.copy(id = 0L, isEnabled = true, nextTriggerEpochMillis = 0L, preReminders = emptyList()) to
            reminder.preReminders.map { it.toMinutes() }.distinct().sorted()

    suspend fun save(context: Context, db: Soh3ilDatabase, reminder: Reminder) {
        if (reminder.id != 0L) {
            val previous = db.reminderDao().getReminderById(reminder.id)?.toDomain()
            if (previous != null) AlarmScheduler.cancelReminder(context, previous)
            db.reminderDao().updateReminder(ReminderEntity.fromDomain(reminder))
            AlarmScheduler.scheduleReminder(context, reminder)
        } else {
            val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(reminder))
            AlarmScheduler.scheduleReminder(context, reminder.copy(id = id))
        }
        refresh(context)
    }

    suspend fun delete(context: Context, db: Soh3ilDatabase, reminder: Reminder) {
        db.reminderDao().deleteReminderById(reminder.id)
        AlarmScheduler.cancelReminder(context, reminder)
        refresh(context)
    }

    private fun refresh(context: Context) {
        CalendarAppWidgetProvider.requestUpdate(context)
        if (UserPreferences.isPersistentNotificationEnabled(context)) {
            PersistentNotificationHelper.updateNotification(context)
        }
    }
}
