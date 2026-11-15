package com.soh3il.core.scheduler

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.soh3il.core.database.Soh3ilDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One-time transition from hash-only identities, without leaving duplicate alarms behind. */
object AlarmMigration {
    private val lock = Mutex()

    suspend fun migrate(context: Context) = lock.withLock {
        val prefs = context.getSharedPreferences("alarm_identity_migration", Context.MODE_PRIVATE)
        if (prefs.getBoolean("uri_identity", false)) return@withLock
        val reminders = Soh3ilDatabase.getInstance(context).reminderDao().getAllRemindersSync().map { it.toDomain() }
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val legacyOffsets = reminders.associate { it.id to (it.preReminders.map { pre -> pre.toMinutes().toInt() } + 0).distinct() }
        val normalCodes = legacyOffsets.flatMap { (id, offsets) -> offsets.map { AlarmScheduler.computeAlarmRequestCode(id, it) } }.toSet()
        reminders.forEach { reminder ->
            // An unambiguous legacy snooze keeps its original OS trigger time.
            val snoozeCode = AlarmScheduler.computeAlarmRequestCode(reminder.id, AlarmScheduler.SNOOZE_OFFSET)
            if (snoozeCode !in normalCodes && legacyPending(context, reminder.id, AlarmScheduler.SNOOZE_OFFSET) != null) {
                AlarmIdentity.rememberSnooze(context, reminder.id, AlarmIdentity.LEGACY_SNOOZE)
            }
            legacyOffsets.getValue(reminder.id).forEach { offset ->
                legacyPending(context, reminder.id, offset)?.let { alarms.cancel(it); it.cancel() }
                val code = AlarmScheduler.computeAlarmRequestCode(reminder.id, offset)
                if (notifications.activeNotifications.any { it.tag == null && it.id == code && it.notification.category == Notification.CATEGORY_ALARM }) {
                    notifications.cancel(code)
                }
            }
        }
        reminders.filter { it.isEnabled }.forEach { AlarmScheduler.scheduleReminder(context, it) }
        prefs.edit().putBoolean("uri_identity", true).apply()
    }

    internal fun legacyPending(context: Context, id: Long, offset: Int): PendingIntent? =
        PendingIntent.getBroadcast(context, AlarmScheduler.computeAlarmRequestCode(id, offset),
            Intent(context, AlarmReceiver::class.java).setAction(AlarmScheduler.ACTION_ALARM_TRIGGER),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
}
