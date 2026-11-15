package com.soh3il.calendar

import android.app.Application
import com.soh3il.core.scheduler.LiveIconHelper
import com.soh3il.core.scheduler.NotificationHelper

class Soh3ilApp : Application() {

    override fun onCreate() {
        super.onCreate()
        com.soh3il.core.scheduler.DateStatusRecovery.install(this)
        com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this, null)
        NotificationHelper.createNotificationChannel(this)
        LiveIconHelper.validateAndHealLauncherAliases(this)
    }
}
