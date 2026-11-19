package com.soh3il.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.Reminder
import java.util.Objects
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object AlarmScheduler {
    private val recoveryLock = Mutex()

    const val ACTION_ALARM_TRIGGER = "com.soh3il.calendar.ACTION_ALARM_TRIGGER"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_IS_PRE_REMINDER = "extra_is_pre_reminder"
    const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    const val EXTRA_PRE_OFFSET_MINUTES = "extra_pre_offset_minutes"
    const val EXTRA_SNOOZE_AT = "extra_snooze_at"

    /**
     * Negative discriminator so snooze never replaces the main alarm (offset 0)
     * or a pre-reminder (offset > 0).
     */
    const val SNOOZE_OFFSET = -1

    /**
     * Deterministic composite request code and notification ID generator
     * The URI and notification tag disambiguate collisions in this mandated hash.
     */
    fun computeAlarmRequestCode(reminderId: Long, preOffsetMinutes: Int): Int {
        return Objects.hash(reminderId, preOffsetMinutes)
    }

    /**
     * Notification action PendingIntents must not share the alarm request-code space.
     */
    fun actionRequestCode(reminderId: Long, preOffsetMinutes: Int, actionSlot: Int): Int {
        return Objects.hash(reminderId, preOffsetMinutes, actionSlot)
    }

    /**
     * Alias matching AGENTS.md Rule 3.4
     */
    fun alarmRequestCode(reminderId: Long, preOffsetMinutes: Int): Int =
        computeAlarmRequestCode(reminderId, preOffsetMinutes)

    /**
     * Schedules a snooze alarm for a reminder (default: 10 minutes from now).
     * Does not replace the next real occurrence.
     */
    fun scheduleSnooze(context: Context, reminderId: Long, snoozeMinutes: Int = 10) {
        cancelSnooze(context, reminderId)
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        scheduleSnoozeAt(context, reminderId, triggerAtMillis)
    }

    private fun scheduleSnoozeAt(context: Context, reminderId: Long, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            data = AlarmIdentity.uri(reminderId, SNOOZE_OFFSET, "alarm")
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
            putExtra(EXTRA_IS_SNOOZE, true)
            putExtra(EXTRA_SNOOZE_AT, triggerAtMillis)
            putExtra(EXTRA_PRE_OFFSET_MINUTES, 0)
        }

        val requestCode = computeAlarmRequestCode(reminderId, SNOOZE_OFFSET)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        AlarmIdentity.rememberSnooze(context, reminderId, triggerAtMillis)
        scheduleExact(alarmManager, maxOf(triggerAtMillis, System.currentTimeMillis() + 1_000L), pendingIntent)
    }

    suspend fun restoreSnoozes(context: Context) {
        val existingIds = com.soh3il.core.database.Soh3ilDatabase.getInstance(context).reminderDao()
            .getAllRemindersSync().map { it.id }.toSet()
        AlarmIdentity.allSnoozes(context).forEach { (id, at) ->
            if (id in existingIds) scheduleSnoozeAt(context, id, at)
            else AlarmIdentity.clearSnooze(context, id)
        }
    }

    suspend fun recoverMissed(context: Context) = recoveryLock.withLock {
        val active = com.soh3il.core.database.Soh3ilDatabase.getInstance(context).reminderDao().getAllActiveRemindersSync()
        val now = System.currentTimeMillis()
        active.forEach { entity ->
            val recorded = mainPrefs(context).getLong(entity.id.toString(), 0L)
            if (recorded == 0L) {
                // Upgrade or interrupted scheduling: seed only future alarms, never invent history.
                if (JalaliRecurrenceEngine.calculateNextTrigger(entity.toDomain(), now) != null)
                    scheduleReminder(context, entity.toDomain(), forceRecalculate = true)
            } else if (recorded <= now && NotificationHelper.canPostReminders(context)) {
                scheduleReminder(context, entity.toDomain(), forceRecalculate = true)
            }
        }
        if (NotificationHelper.canPostNotifications(context)) restoreSnoozes(context)
    }

    private fun mainPrefs(context: Context) = context.getSharedPreferences("scheduled_main_alarms", Context.MODE_PRIVATE)
    private fun rememberMain(context: Context, id: Long, at: Long) { mainPrefs(context).edit().putLong(id.toString(), at).apply() }
    fun clearMain(context: Context, id: Long) { mainPrefs(context).edit().remove(id.toString()).apply() }

    /**
     * Schedules the next exact alarm. Uses setExactAndAllowWhileIdle, never setAlarmClock,
     * so the status-bar clock cannot fire the reminder early.
     */
    fun scheduleReminder(
        context: Context,
        reminder: Reminder,
        forceRecalculate: Boolean = false,
        includePreReminders: Boolean = true
    ) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder)
            return
        }

        val now = System.currentTimeMillis()
        val calculated = if (!forceRecalculate && reminder.nextTriggerEpochMillis > now) {
            reminder.nextTriggerEpochMillis
        } else {
            JalaliRecurrenceEngine.calculateNextTrigger(reminder, now)
        }
        // Coalesce overdue occurrences into one retry. A timestamp from an earlier
        // version is not fabricated; the persisted scheduled main alarm is required.
        val recorded = mainPrefs(context).getLong(reminder.id.toString(), 0L)
        val overdue = recorded in 1..now
        val nextTrigger = when {
            overdue && NotificationHelper.canPostReminders(context) -> now + 1_000L
            overdue -> null
            else -> calculated
        }
        cancelAlarms(context, reminder.id, if (includePreReminders) reminder.preReminders.map { it.toMinutes().toInt() } + 0 else listOf(0))
        if (nextTrigger == null) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val mainRequestCode = computeAlarmRequestCode(reminder.id, 0)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            data = AlarmIdentity.uri(reminder.id, 0, "alarm")
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_IS_PRE_REMINDER, false)
            putExtra(EXTRA_IS_SNOOZE, false)
            putExtra(EXTRA_PRE_OFFSET_MINUTES, 0)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            mainRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        rememberMain(context, reminder.id, if (overdue) recorded else nextTrigger)
        scheduleExact(alarmManager, nextTrigger, pendingIntent)

        if (includePreReminders) reminder.preReminders.map { it.toMinutes() }.distinct().forEach { minutes ->
            if (minutes in 1..Int.MAX_VALUE.toLong()) schedulePreReminder(context, reminder, minutes.toInt())
        }
    }

    /** Each offset independently schedules its next future pre-alarm. */
    fun schedulePreReminder(context: Context, reminder: Reminder, preMinutes: Int) {
        if (!reminder.isEnabled || preMinutes <= 0 || reminder.preReminders.none { it.toMinutes() == preMinutes.toLong() }) return
        val preEpoch = JalaliRecurrenceEngine.calculateNextPreTrigger(reminder, preMinutes.toLong(), System.currentTimeMillis()) ?: return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            data = AlarmIdentity.uri(reminder.id, preMinutes, "alarm")
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_IS_PRE_REMINDER, true)
            putExtra(EXTRA_IS_SNOOZE, false)
            putExtra(EXTRA_PRE_OFFSET_MINUTES, preMinutes)
        }
        val pending = PendingIntent.getBroadcast(context, computeAlarmRequestCode(reminder.id, preMinutes), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        scheduleExact(manager, preEpoch, pending)
    }

    private fun scheduleExact(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminder: Reminder) {
        val offsets = reminder.preReminders.map { it.toMinutes().toInt() } + 0
        cancelReminder(context, reminder.id, offsets)
    }

    fun cancelReminder(context: Context, reminderId: Long, preOffsetMinutesList: List<Int> = emptyList()) {
        cancelAlarms(context, reminderId, preOffsetMinutesList + 0)
        cancelSnooze(context, reminderId)
        clearMain(context, reminderId)
        for (offset in (preOffsetMinutesList + 0).distinct()) {
            NotificationHelper.cancelReminderNotification(context, reminderId, offset)
        }
    }

    internal fun cancelSnooze(context: Context, reminderId: Long) {
        if (AlarmIdentity.hasSnooze(context, reminderId, AlarmIdentity.LEGACY_SNOOZE)) {
            AlarmMigration.legacyPending(context, reminderId, SNOOZE_OFFSET)?.let {
                (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(it)
                it.cancel()
            }
        }
        AlarmIdentity.clearSnooze(context, reminderId)
        cancelAlarms(context, reminderId, listOf(SNOOZE_OFFSET))
    }

    private fun cancelAlarms(context: Context, reminderId: Long, preOffsetMinutesList: List<Int>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val allOffsets = preOffsetMinutesList.distinct()
        for (offset in allOffsets) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
                data = AlarmIdentity.uri(reminderId, offset, "alarm")
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_PRE_OFFSET_MINUTES, offset)
            }
            val requestCode = computeAlarmRequestCode(reminderId, offset)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: continue
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
