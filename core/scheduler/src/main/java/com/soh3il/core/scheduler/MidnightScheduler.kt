package com.soh3il.core.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.jalali.JalaliDate

object MidnightScheduler {

    const val ACTION_MIDNIGHT_TICK = "com.soh3il.calendar.ACTION_MIDNIGHT_TICK"
    const val ACTION_KEEP_ALIVE_WATCHDOG = "com.soh3il.calendar.ACTION_KEEP_ALIVE_WATCHDOG"
    private const val REQUEST_CODE_MIDNIGHT = 9999
    private const val REQUEST_CODE_WATCHDOG = 9998
    private const val DEFAULT_WATCHDOG_INTERVAL_MS = 30L * 60L * 1000L

    /**
     * Schedules the next exact midnight tick (00:00:05) to update the persistent notification,
     * widget, and launcher live icon when the date rolls over.
     * Cleanly handles DST and timezone shifts by anchoring to next day 00:00:05.
     */
    fun scheduleMidnightAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val now = System.currentTimeMillis()
        val today = JalaliDate.fromEpochMillis(now)
        val todayTick = today.toEpochMillis(0, 0, 5)
        val triggerAtMillis = if (todayTick > now) todayTick else today.plusDays(1).toEpochMillis(0, 0, 5)

        val intent = Intent(context, MidnightReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (error: SecurityException) {
            Log.w("MidnightScheduler", "Exact date alarm unavailable; scheduling an inexact date update")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Cancels any scheduled midnight rollover alarm.
     */
    fun cancelMidnightAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MidnightReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Schedules a lightweight watchdog alarm to recover the persistent notification and
     * keep-alive service if an OEM battery manager or task removal terminated them.
     */
    fun scheduleKeepAliveWatchdog(context: Context, delayMillis: Long = DEFAULT_WATCHDOG_INTERVAL_MS) {
        if (!UserPreferences.isPersistentNotificationEnabled(context)) {
            cancelKeepAliveWatchdog(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MidnightReceiver::class.java).apply {
            action = ACTION_KEEP_ALIVE_WATCHDOG
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATCHDOG,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = System.currentTimeMillis() + delayMillis.coerceAtLeast(1_000L)
        try {
            if (delayMillis <= 60_000L) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            android.util.Log.w("MidnightScheduler", "exact alarm permission revoked; keep-alive watchdog uses inexact alarm")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelKeepAliveWatchdog(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MidnightReceiver::class.java).apply {
            action = ACTION_KEEP_ALIVE_WATCHDOG
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WATCHDOG,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
