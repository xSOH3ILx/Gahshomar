package com.soh3il.calendar.rollover

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.scheduler.MidnightScheduler
import com.soh3il.core.scheduler.PersistentNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.TimeZone
import android.app.Notification
import android.app.NotificationChannel
import android.content.Intent
import android.os.PowerManager
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.scheduler.CalendarKeepAliveService
import com.soh3il.core.scheduler.MidnightReceiver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.robolectric.Robolectric

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, instrumentedPackages = ["com.soh3il"])
class DateRolloverTest {
    @Test fun midnightAlarmUsesTheSameTehranDayAsTheDisplayedDate() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            SystemClock.setCurrentTimeMillis(Instant.parse("2026-11-15T19:00:00Z").toEpochMilli())
            val context = RuntimeEnvironment.getApplication()
            MidnightScheduler.scheduleMidnightAlarm(context)
            val alarm = requireNotNull(shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).nextScheduledAlarm)
            assertEquals(Instant.parse("2026-11-15T20:30:05Z").toEpochMilli(), alarm.triggerAtTime)
        } finally {
            TimeZone.setDefault(previous)
        }
    }

    @Test fun enablingDateAtProcessStartupDoesNotRequireOpeningTheActivity() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        // Application startup is the real boundary reached when an alarm starts a dead process.
        com.soh3il.calendar.Soh3ilApp().also { app ->
            org.robolectric.util.ReflectionHelpers.callInstanceMethod<Void>(
                Application::class.java, app, "attach",
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(Context::class.java, context)
            )
            app.onCreate()
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertNotNull(manager.activeNotifications.firstOrNull { it.id == 2001 })
    }

    @Test fun dateAndAlarmSurviveDatabaseFailureOnTheNewSolarMonth() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        SystemClock.setCurrentTimeMillis(JalaliDate(1405, 12, 29).toEpochMillis(23, 59))
        PersistentNotificationHelper.updateNotification(context, emptyList())
        SystemClock.setCurrentTimeMillis(JalaliDate(1406, 1, 1).toEpochMillis(0, 0, 6))
        try {
            MidnightReceiver.refreshNow(context, MidnightReceiver.ACTION_MIDNIGHT_TICK) {
                throw java.io.IOException("database unavailable")
            }
            fail("The detail read failure must stay observable")
        } catch (expected: java.io.IOException) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = manager.activeNotifications.single { it.id == 2001 }.notification
            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
            assertTrue("Expected the first day of Farvardin, actual title=$title", title.contains("۱ فروردین"))
            assertEquals(JalaliDate(1406, 1, 1).toJdn(), notification.extras.getLong(PersistentNotificationHelper.EXTRA_DATE_JDN))
            assertEquals(com.soh3il.core.designsystem.R.drawable.ic_stat_day_1, notification.smallIcon.resId)
            val alarms = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).scheduledAlarms
            val nextTick = JalaliDate(1406, 1, 2).toEpochMillis(0, 0, 5)
            assertTrue("Expected next tick $nextTick, scheduled ${alarms.map { it.triggerAtTime }}", alarms.any { it.triggerAtTime == nextTick })
        }
    }

    @Test fun disabledDateIsNotResurrectedByARefresh() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, false)
        PersistentNotificationHelper.updateNotification(context, null)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertTrue(manager.activeNotifications.none { it.id == 2001 })
    }

    @Test fun screenUnlockRecoversAMissingDateWithoutOpeningTheActivity() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        com.soh3il.core.scheduler.DateStatusRecovery.install(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(2001)
        val signal = Intent(Intent.ACTION_USER_PRESENT)
        val receivers = shadowOf(context).getReceiversForIntent(signal)
        assertTrue(receivers.isNotEmpty())
        receivers.forEach { it.onReceive(context, signal) }
        val deadline = System.nanoTime() + 2_000_000_000L
        while (manager.activeNotifications.none { it.id == 2001 } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(manager.activeNotifications.any { it.id == 2001 })
    }

    @Test fun statusResourcesExistForEveryPossibleSolarDay() {
        val context = RuntimeEnvironment.getApplication()
        for (day in 1..31) assertTrue("Missing status icon for day $day",
            context.resources.getIdentifier("ic_stat_day_$day", "drawable", context.packageName) != 0)
    }
    @Test fun slowOlderRolloverCannotOverwriteNewerReminderSummary() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        val today = JalaliDate.now()
        val oldReminder = com.soh3il.core.model.Reminder(id = 991, title = "old",
            jalaliYear = today.year, jalaliMonth = today.month, jalaliDay = today.day, hour = 10, minute = 0)
        val started = kotlinx.coroutines.CompletableDeferred<Unit>()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val work = async(kotlinx.coroutines.Dispatchers.Default) {
            MidnightReceiver.refreshNow(context, MidnightReceiver.ACTION_MIDNIGHT_TICK) {
                started.complete(Unit); release.await(); listOf(oldReminder)
            }
        }
        started.await()
        PersistentNotificationHelper.updateNotification(context, emptyList())
        release.complete(Unit)
        work.await()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = manager.activeNotifications.single { it.id == 2001 }.notification
        val lines = n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).orEmpty().joinToString(" ")
        assertTrue(lines.contains("بدون یادآور"))
    }

    @Test fun startupHealsAStaleButSinglyEnabledLiveIcon() {
        val context = RuntimeEnvironment.getApplication()
        SystemClock.setCurrentTimeMillis(JalaliDate(1405, 8, 24).toEpochMillis(10))
        UserPreferences.setLiveIconEnabled(context, true)
        val pm = context.packageManager
        val old = android.content.ComponentName(context.packageName, "com.soh3il.calendar.Day23")
        val current = android.content.ComponentName(context.packageName, "com.soh3il.calendar.Day24")
        val default = android.content.ComponentName(context.packageName, "com.soh3il.calendar.DefaultLauncherAlias")
        pm.setComponentEnabledSetting(old, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(current, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(default, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        com.soh3il.core.scheduler.LiveIconHelper.validateAndHealLauncherAliases(context)
        assertEquals(android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, pm.getComponentEnabledSetting(current))
        assertEquals(android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, pm.getComponentEnabledSetting(old))
    }

    @Test fun persistentNotificationUsesNoClearSilentDefaultChannelAndImmediateBehavior() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        PersistentNotificationHelper.updateNotification(context, emptyList())

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(PersistentNotificationHelper.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, channel.importance)
        assertNull(channel.sound)
        assertFalse(channel.shouldVibrate())
        assertEquals(Notification.VISIBILITY_PUBLIC, channel.lockscreenVisibility)

        val notification = manager.activeNotifications.single { it.id == PersistentNotificationHelper.NOTIFICATION_ID }.notification
        assertTrue((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0)
        assertTrue((notification.flags and Notification.FLAG_NO_CLEAR) != 0)
        assertNotNull(notification.deleteIntent)
        assertEquals(PersistentNotificationHelper.CHANNEL_ID, notification.channelId)
    }

    @Test fun legacyChannelMigrationUpgradesNormalAndRespectsBlockedChannel() {
        val context = RuntimeEnvironment.getApplication()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.deleteNotificationChannel(PersistentNotificationHelper.CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel("persistent_date_channel", "old", NotificationManager.IMPORTANCE_LOW)
        )
        PersistentNotificationHelper.createNotificationChannel(context)
        assertNull(manager.getNotificationChannel("persistent_date_channel"))
        assertEquals(
            NotificationManager.IMPORTANCE_DEFAULT,
            manager.getNotificationChannel(PersistentNotificationHelper.CHANNEL_ID).importance
        )

        manager.deleteNotificationChannel(PersistentNotificationHelper.CHANNEL_ID)
        manager.createNotificationChannel(
            NotificationChannel("persistent_date_channel", "old", NotificationManager.IMPORTANCE_NONE)
        )
        PersistentNotificationHelper.createNotificationChannel(context)
        assertEquals(
            NotificationManager.IMPORTANCE_NONE,
            manager.getNotificationChannel(PersistentNotificationHelper.CHANNEL_ID).importance
        )
    }

    @Test fun dismissedPersistentNotificationRestoresWhenEnabledEvenDuringQuietHoursAndStaysOffWhenDisabled(): Unit = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        UserPreferences.setQuietHoursEnabled(context, true)
        UserPreferences.setQuietStartMinute(context, 0)
        UserPreferences.setQuietEndMinute(context, 23 * 60 + 59)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(PersistentNotificationHelper.NOTIFICATION_ID)
        MidnightReceiver.refreshNow(context, MidnightReceiver.ACTION_NOTIFICATION_DISMISSED) { emptyList() }
        assertNotNull(manager.activeNotifications.firstOrNull { it.id == PersistentNotificationHelper.NOTIFICATION_ID })

        UserPreferences.setPersistentNotificationEnabled(context, false)
        manager.cancel(PersistentNotificationHelper.NOTIFICATION_ID)
        MidnightReceiver.refreshNow(context, MidnightReceiver.ACTION_NOTIFICATION_DISMISSED) { emptyList() }
        assertTrue(manager.activeNotifications.none { it.id == PersistentNotificationHelper.NOTIFICATION_ID })
    }

    @Test fun keepAliveServicePostsRealNotificationAndWatchdogRecoversMissingNotification(): Unit = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)

        val controller = Robolectric.buildService(CalendarKeepAliveService::class.java).create().startCommand(0, 0)
        val service = controller.get()
        val foregroundNotification = shadowOf(service).lastForegroundNotification
        assertNotNull(foregroundNotification)
        assertEquals(JalaliDate.now().toJdn(), foregroundNotification.extras.getLong(PersistentNotificationHelper.EXTRA_DATE_JDN))
        assertTrue((foregroundNotification.flags and Notification.FLAG_NO_CLEAR) != 0)
        assertNotNull(foregroundNotification.deleteIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(PersistentNotificationHelper.NOTIFICATION_ID)
        MidnightReceiver.refreshNow(context, MidnightReceiver.ACTION_KEEP_ALIVE_WATCHDOG) { emptyList() }
        assertNotNull(manager.activeNotifications.firstOrNull { it.id == PersistentNotificationHelper.NOTIFICATION_ID })

        UserPreferences.setPersistentNotificationEnabled(context, false)
        manager.cancel(PersistentNotificationHelper.NOTIFICATION_ID)
        MidnightReceiver.refreshNow(context, MidnightReceiver.ACTION_KEEP_ALIVE_WATCHDOG) { emptyList() }
        assertTrue(manager.activeNotifications.none { it.id == PersistentNotificationHelper.NOTIFICATION_ID })
        controller.destroy()
    }

    @Test fun powerSaveAndDndChangesRecoverMissingPersistentNotification() {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        com.soh3il.core.scheduler.DateStatusRecovery.install(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED
        ).forEach { action ->
            manager.cancel(PersistentNotificationHelper.NOTIFICATION_ID)
            val signal = Intent(action)
            shadowOf(context).getReceiversForIntent(signal).forEach { it.onReceive(context, signal) }
            val deadline = System.nanoTime() + 2_000_000_000L
            while (manager.activeNotifications.none { it.id == PersistentNotificationHelper.NOTIFICATION_ID } && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }
            assertTrue("Missing notification after $action", manager.activeNotifications.any { it.id == PersistentNotificationHelper.NOTIFICATION_ID })
        }
    }

}
