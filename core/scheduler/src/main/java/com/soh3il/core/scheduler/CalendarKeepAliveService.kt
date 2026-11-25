package com.soh3il.core.scheduler

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Keep-alive owner of the persistent date notification: a sticky foreground service
 * that re-posts the notification on TIME_TICK, SCREEN_ON, SCREEN_OFF, USER_PRESENT,
 * power-save/Doze/DND transitions, and date/time changes, so aggressive battery managers,
 * accidental swipe-dismissals, or notification sweeps cannot end the always-visible promise.
 */
class CalendarKeepAliveService : Service() {

    private val expectedActions = setOf(
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_SCREEN_ON,
        Intent.ACTION_SCREEN_OFF,
        Intent.ACTION_USER_PRESENT,
        Intent.ACTION_TIME_TICK,
        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
        PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED,
        NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED,
        NotificationManager.ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED,
        NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED
    )

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val action = intent?.action ?: return
            if (action !in expectedActions) return
            if (!UserPreferences.isPersistentNotificationEnabled(context)) {
                stop(context)
                return
            }
            when (action) {
                Intent.ACTION_DATE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED -> {
                    MidnightScheduler.scheduleMidnightAlarm(context)
                    PersistentNotificationHelper.updateNotification(context)
                }
                else -> {
                    if (PersistentNotificationHelper.needsRefresh(context)) {
                        PersistentNotificationHelper.updateNotification(context)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            expectedActions.forEach(::addAction)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!UserPreferences.isPersistentNotificationEnabled(this)) {
            running.set(false)
            MidnightScheduler.cancelKeepAliveWatchdog(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        PersistentNotificationHelper.createNotificationChannel(this)
        val notification = PersistentNotificationHelper.buildForegroundNotification(this)
            ?: minimalFallbackNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(PersistentNotificationHelper.NOTIFICATION_ID, notification, type)
        } else {
            startForeground(PersistentNotificationHelper.NOTIFICATION_ID, notification)
        }
        running.set(true)
        MidnightScheduler.scheduleKeepAliveWatchdog(this)
        if (!PersistentNotificationHelper.hasCachedReminders() || PersistentNotificationHelper.needsRefresh(this)) {
            PersistentNotificationHelper.updateNotification(this)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (UserPreferences.isPersistentNotificationEnabled(applicationContext)) {
            PersistentNotificationHelper.restoreIfMissing(applicationContext)
            MidnightScheduler.scheduleKeepAliveWatchdog(applicationContext, delayMillis = 1_500L)
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        running.set(false)
        try {
            unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
        if (UserPreferences.isPersistentNotificationEnabled(applicationContext)) {
            PersistentNotificationHelper.restoreIfMissing(applicationContext)
            MidnightScheduler.scheduleKeepAliveWatchdog(applicationContext, delayMillis = 5_000L)
        }
        super.onDestroy()
    }

    private fun minimalFallbackNotification(): Notification {
        val launchIntent = Intent().setClassName(this, "com.soh3il.calendar.MainActivity").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val deleteIntent = PendingIntent.getBroadcast(
            this,
            2002,
            Intent(this, MidnightReceiver::class.java).apply {
                action = MidnightReceiver.ACTION_NOTIFICATION_DISMISSED
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val today = JalaliDate.now()
        val showOnLockscreen = UserPreferences.isLockscreenCalendarEnabled(this)
        val dynamicIconEnabled = UserPreferences.isDynamicNotificationIconEnabled(this)
        val smallIconRes = if (showOnLockscreen && dynamicIconEnabled) {
            DateNotificationAssets.getDayDrawableRes(today.day)
        } else {
            com.soh3il.core.designsystem.R.drawable.ic_stat_calendar
        }
        val built = NotificationCompat.Builder(this, PersistentNotificationHelper.CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(if (showOnLockscreen) "${today.day.toPersianDigits()} ${today.monthNameFa} ${today.year.toPersianDigits()}" else "گاه‌شمار خورشیدی")
            .setVisibility(if (showOnLockscreen) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        built.flags = built.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        return built
    }

    companion object {
        private const val TAG = "CalendarKeepAlive"
        private val running = AtomicBoolean(false)

        fun isRunning(): Boolean = running.get()

        fun start(context: Context) {
            if (!UserPreferences.isPersistentNotificationEnabled(context)) {
                stop(context)
                return
            }
            PersistentNotificationHelper.createNotificationChannel(context)
            if (!NotificationHelper.canPostNotifications(context, PersistentNotificationHelper.CHANNEL_ID)) {
                return
            }
            MidnightScheduler.scheduleKeepAliveWatchdog(context)
            try {
                context.startForegroundService(Intent(context, CalendarKeepAliveService::class.java))
            } catch (error: Exception) {
                Log.w(TAG, "Keep-alive service start refused: ${error.javaClass.simpleName}")
            }
        }

        fun stop(context: Context) {
            running.set(false)
            MidnightScheduler.cancelKeepAliveWatchdog(context)
            try {
                context.stopService(Intent(context, CalendarKeepAliveService::class.java))
            } catch (error: Exception) {
                Log.w(TAG, "Keep-alive service stop refused: ${error.javaClass.simpleName}")
            }
        }
    }
}
