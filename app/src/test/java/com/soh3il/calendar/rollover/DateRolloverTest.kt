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
import android.content.Intent
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.scheduler.MidnightReceiver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

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
}
