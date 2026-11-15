package com.soh3il.calendar.alarms

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.Reminder
import com.soh3il.core.scheduler.AlarmScheduler
import com.soh3il.core.scheduler.NotificationHelper
import com.soh3il.core.scheduler.AlarmReceiver
import com.soh3il.core.scheduler.MidnightReceiver
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.entity.ReminderEntity
import com.soh3il.core.database.preferences.UserPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, instrumentedPackages = ["com.soh3il"])
class AlarmIdentityTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val alarms get() = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    private fun reminder(id: Long) = Reminder(id = id, title = "fixture", jalaliYear = 1406,
        jalaliMonth = 1, jalaliDay = 1, hour = 9, minute = 0)

    @Before fun setup() {
        SystemClock.setCurrentTimeMillis(JalaliDate(1405, 8, 24).toEpochMillis(8))
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test fun collidingRequestCodesKeepBothAlarmsAndPreReminder() {
        val first = reminder(1).copy(preReminders = listOf(PreReminder(31, PreReminderUnit.MINUTES)))
        AlarmScheduler.scheduleReminder(context, first)
        AlarmScheduler.scheduleReminder(context, reminder(2))
        assertEquals(3, alarms.scheduledAlarms.size)
        AlarmScheduler.cancelReminder(context, reminder(2))
        assertEquals(2, alarms.scheduledAlarms.size)
    }

    @Test fun snoozeDoesNotCancelAnotherRemindersThirtyMinuteWarning() {
        val first = reminder(1).copy(preReminders = listOf(PreReminder(30, PreReminderUnit.MINUTES)))
        AlarmScheduler.scheduleReminder(context, first)
        AlarmScheduler.scheduleSnooze(context, 2)
        assertEquals(3, alarms.scheduledAlarms.size)
        AlarmScheduler.cancelReminder(context, reminder(2))
        assertEquals(2, alarms.scheduledAlarms.size)
    }

    @Test fun collidingNotificationIdsRemainIndependentlyCancelable() {
        assertTrue(NotificationHelper.showReminderNotification(context, 1, "first", "", true, 31))
        assertTrue(NotificationHelper.showReminderNotification(context, 2, "second", ""))
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        assertEquals(2, manager.activeNotifications.size)
        NotificationHelper.cancelReminderNotification(context, 2)
        assertEquals("first", manager.activeNotifications.single().notification.extras.getString("android.title")!!.removePrefix("پیش‌یادآور: "))
    }

    @Test fun collidingNotificationActionsAndContentKeepTheirOwnReminder() {
        NotificationHelper.showReminderNotification(context, 1, "first", "", true, 31)
        NotificationHelper.showReminderNotification(context, 2, "second", "")
        val notifications = (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).activeNotifications
        notifications.forEach { item ->
            val expected = if (item.notification.extras.getString("android.title")!!.contains("first")) 1L else 2L
            val intents = item.notification.actions.map { it.actionIntent } + item.notification.contentIntent
            intents.forEach { assertEquals(expected, shadowOf(it).savedIntent.getLongExtra(AlarmScheduler.EXTRA_REMINDER_ID, 0)) }
        }
    }

    @Test fun reschedulingNextOccurrenceKeepsAnIndependentSnooze() {
        AlarmScheduler.scheduleSnooze(context, 2)
        AlarmScheduler.scheduleReminder(context, reminder(2))
        assertEquals(2, alarms.scheduledAlarms.size)
    }

    @Test fun packageUpgradeReplacesLegacyAlarmsInsteadOfDoublingThem() = runBlocking {
        UserPreferences.setPersistentNotificationEnabled(context, false)
        val db = Soh3ilDatabase.getInstance(context)
        db.reminderDao().getAllRemindersSync().forEach { db.reminderDao().deleteReminderById(it.id) }
        val item = reminder(0)
        val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(item))
        val legacy = PendingIntent.getBroadcast(context, AlarmScheduler.computeAlarmRequestCode(id, 0),
            Intent(context, AlarmReceiver::class.java).setAction(AlarmScheduler.ACTION_ALARM_TRIGGER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExact(AlarmManager.RTC_WAKEUP,
            item.copy(id = id).let { JalaliDate(it.jalaliYear, it.jalaliMonth, it.jalaliDay).toEpochMillis(it.hour) }, legacy)
        MidnightReceiver.refreshNow(context, Intent.ACTION_MY_PACKAGE_REPLACED)
        val reminderAlarms = alarms.scheduledAlarms.filter {
            shadowOf(it.operation).savedIntent.action == AlarmScheduler.ACTION_ALARM_TRIGGER
        }
        assertEquals(1, reminderAlarms.size)
        assertNotNull(shadowOf(reminderAlarms.single().operation).savedIntent.data)
    }
}
