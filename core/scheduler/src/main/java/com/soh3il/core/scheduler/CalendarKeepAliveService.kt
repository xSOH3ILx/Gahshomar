package com.soh3il.core.scheduler

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.soh3il.core.database.preferences.UserPreferences

/**
 * Keep-alive owner of the persistent date notification, mirroring persian-calendar's
 * ApplicationService: a sticky foreground service that re-posts the notification on
 * TIME_TICK, SCREEN_ON and date/time changes, so aggressive battery managers or a
 * system-wide notification sweep cannot end the always-visible promise.
 */
class CalendarKeepAliveService : Service() {

    private val expectedActions = setOf(
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_SCREEN_ON,
        Intent.ACTION_TIME_TICK
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
                Intent.ACTION_TIMEZONE_CHANGED -> PersistentNotificationHelper.updateNotification(context)
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_TIME_TICK ->
                    if (PersistentNotificationHelper.needsRefresh(context)) {
                        PersistentNotificationHelper.updateNotification(context)
                    }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            expectedActions.forEach(::addAction)
        }
        registerReceiver(receiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!UserPreferences.isPersistentNotificationEnabled(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        PersistentNotificationHelper.createNotificationChannel(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(PersistentNotificationHelper.NOTIFICATION_ID, placeholderNotification(), type)
        } else {
            startForeground(PersistentNotificationHelper.NOTIFICATION_ID, placeholderNotification())
        }
        PersistentNotificationHelper.updateNotification(this)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    private fun placeholderNotification(): Notification {
        val contentIntent = packageManager.getLaunchIntentForPackage(packageName)
        return NotificationCompat.Builder(this, PersistentNotificationHelper.CHANNEL_ID)
            .setSmallIcon(com.soh3il.core.designsystem.R.drawable.ic_stat_calendar)
            .setContentIntent(contentIntent?.let {
                PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            })
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    companion object {
        private const val TAG = "CalendarKeepAlive"

        fun start(context: Context) {
            if (!UserPreferences.isPersistentNotificationEnabled(context)) {
                stop(context)
                return
            }
            try {
                context.startForegroundService(Intent(context, CalendarKeepAliveService::class.java))
            } catch (error: Exception) {
                Log.w(TAG, "Keep-alive service start refused: ${error.javaClass.simpleName}")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, CalendarKeepAliveService::class.java))
            } catch (error: Exception) {
                Log.w(TAG, "Keep-alive service stop refused: ${error.javaClass.simpleName}")
            }
        }
    }
}
