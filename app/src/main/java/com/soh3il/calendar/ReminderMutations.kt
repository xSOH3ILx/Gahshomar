package com.soh3il.calendar

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.soh3il.calendar.widget.CalendarAppWidgetProvider
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.entity.ReminderEntity
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.model.Reminder
import com.soh3il.core.scheduler.AlarmScheduler
import com.soh3il.core.scheduler.PersistentNotificationHelper
import com.soh3il.core.scheduler.IcsHelper
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.RecurrenceType
import java.io.IOException

internal object ReminderMutations {
    suspend fun importIcs(context: Context, db: Soh3ilDatabase, uri: Uri): Result<Pair<Int, Int>> {
        val imported = try {
            IcsHelper.importFromIcsUri(context, uri)
        } catch (error: Exception) {
            if (error !is IOException && error !is IllegalArgumentException && error !is SecurityException) throw error
            return Result.failure(error)
        }
        try {
            imported.filter { it.isEnabled && it.recurrence.type != RecurrenceType.NONE }.forEach {
                requireNotNull(JalaliRecurrenceEngine.calculateNextTrigger(it)) { "قاعدهٔ تکرار یکی از رویدادها معتبر نیست" }
            }
        } catch (error: IllegalArgumentException) { return Result.failure(error) }
        // Commit the entire deduplicated batch together. Scheduling is external to
        // Room; future reminders are reconciled on app recovery after interruption.
        val created = db.withTransaction {
            val seen = db.reminderDao().getAllRemindersSync().map { importIdentity(it.toDomain()) }.toMutableSet()
            imported.mapNotNull { rem ->
                if (!seen.add(importIdentity(rem))) null else {
                    val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(rem))
                    rem.copy(id = id)
                }
            }
        }
        created.forEach { AlarmScheduler.scheduleReminder(context, it) }
        refresh(context)
        return Result.success(imported.size to created.size)
    }

    private fun importIdentity(reminder: Reminder): Pair<Reminder, List<Long>> =
        reminder.copy(id = 0L, isEnabled = true, nextTriggerEpochMillis = 0L, preReminders = emptyList()) to
            reminder.preReminders.map { it.toMinutes() }.distinct().sorted()

    suspend fun save(context: Context, db: Soh3ilDatabase, reminder: Reminder) {
        if (reminder.isEnabled && reminder.recurrence.type != RecurrenceType.NONE) {
            requireNotNull(JalaliRecurrenceEngine.calculateNextTrigger(reminder)) { "قاعدهٔ تکرار معتبر نیست" }
        }
        if (reminder.id != 0L) {
            val previous = requireNotNull(db.reminderDao().getReminderById(reminder.id)?.toDomain()) { "یادآور پیدا نشد" }
            db.reminderDao().updateReminder(ReminderEntity.fromDomain(reminder))
            try {
                AlarmScheduler.cancelReminder(context, previous)
                AlarmScheduler.scheduleReminder(context, reminder)
            } catch (error: Exception) {
                try {
                    AlarmScheduler.cancelReminder(context, reminder)
                    db.reminderDao().updateReminder(ReminderEntity.fromDomain(previous))
                    AlarmScheduler.scheduleReminder(context, previous)
                } catch (rollback: Exception) { error.addSuppressed(rollback) }
                throw error
            }
        } else {
            val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(reminder))
            val created = reminder.copy(id = id)
            try { AlarmScheduler.scheduleReminder(context, created) }
            catch (error: Exception) {
                try {
                    AlarmScheduler.cancelReminder(context, created)
                    db.reminderDao().deleteReminderById(id)
                } catch (rollback: Exception) { error.addSuppressed(rollback) }
                throw error
            }
        }
        refresh(context)
    }

    suspend fun delete(context: Context, db: Soh3ilDatabase, reminder: Reminder) {
        db.reminderDao().deleteReminderById(reminder.id)
        AlarmScheduler.cancelReminder(context, reminder)
        refresh(context)
    }

    suspend fun setCountdown(context: Context, db: Soh3ilDatabase, reminderId: Long, showCountdown: Boolean) {
        db.reminderDao().setReminderCountdown(reminderId, showCountdown)
        refresh(context)
    }

    private fun refresh(context: Context) {
        CalendarAppWidgetProvider.requestUpdate(context)
        if (UserPreferences.isPersistentNotificationEnabled(context)) {
            PersistentNotificationHelper.updateNotification(context)
        }
    }
}
