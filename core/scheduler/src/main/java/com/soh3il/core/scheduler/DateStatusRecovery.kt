package com.soh3il.core.scheduler

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.soh3il.core.jalali.JalaliDate
import java.util.concurrent.atomic.AtomicLong

/** Recover a missing or stale date when the user returns, without opening an Activity. */
object DateStatusRecovery {
    private val lastScreenRefreshDay = AtomicLong(Long.MIN_VALUE)
    private val actions = setOf(
        Intent.ACTION_DATE_CHANGED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_SCREEN_ON,
        Intent.ACTION_SCREEN_OFF,
        Intent.ACTION_USER_PRESENT,
        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
        PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED,
        NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED
    )

    fun install(context: Context) {
        ContextCompat.registerReceiver(context.applicationContext, object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                val action = intent?.action ?: return
                if (action !in actions) return
                when (action) {
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                        val day = JalaliDate.now().toJdn()
                        val alreadyRefreshedToday = lastScreenRefreshDay.getAndSet(day) == day
                        if (alreadyRefreshedToday && !PersistentNotificationHelper.needsRefresh(context)) return
                    }
                    Intent.ACTION_SCREEN_OFF,
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                    PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED,
                    NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                        if (!PersistentNotificationHelper.needsRefresh(context)) return
                    }
                }
                MidnightReceiver.enqueueFromReceiver(this, context, action)
            }
        }, IntentFilter().apply { actions.forEach(::addAction) }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
