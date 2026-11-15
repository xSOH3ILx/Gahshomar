package com.soh3il.calendar

import android.app.Application
import com.soh3il.core.scheduler.LiveIconHelper
import com.soh3il.core.scheduler.NotificationHelper
import com.soh3il.core.scheduler.AlarmMigration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class Soh3ilApp : Application() {

    override fun onCreate() {
        super.onCreate()
        com.soh3il.core.scheduler.DateStatusRecovery.install(this)
        com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this, null)
        NotificationHelper.createNotificationChannel(this)
        LiveIconHelper.validateAndHealLauncherAliases(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_500L) { AlarmMigration.migrate(this@Soh3ilApp) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                android.util.Log.e("AlarmMigration", "Alarm migration failed: ${error.javaClass.simpleName}")
            }
        }
    }
}
