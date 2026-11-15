package com.soh3il.calendar.alarms

import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.entity.ReminderEntity
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Reminder
import com.soh3il.core.scheduler.AlarmReceiver
import com.soh3il.core.scheduler.AlarmScheduler
import com.soh3il.core.scheduler.AlarmMigration
import com.soh3il.core.scheduler.NotificationHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBroadcastPendingResult
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter.from
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, instrumentedPackages = ["com.soh3il"])
class AlarmLifecycleTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val db get() = Soh3ilDatabase.getInstance(context)
    private val manager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarms get() = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    private var id = 0L

    @Before fun setup() = runBlocking {
        SystemClock.setCurrentTimeMillis(JalaliDate(1405, 8, 24).toEpochMillis(9))
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(Reminder(title = "fixture",
            jalaliYear = 1405, jalaliMonth = 8, jalaliDay = 24, hour = 9, minute = 0)))
    }

    private fun deliver(intent: Intent) {
        val pending = ReflectionHelpers.callStaticMethod<BroadcastReceiver.PendingResult>(
            ShadowBroadcastPendingResult::class.java, "create", from(Int::class.javaPrimitiveType, 0),
            from(String::class.java, null), from(Bundle::class.java, null), from(Boolean::class.javaPrimitiveType, false))
        val receiver = AlarmReceiver()
        ReflectionHelpers.callInstanceMethod<Void>(receiver, "setPendingResult", from(BroadcastReceiver.PendingResult::class.java, pending))
        receiver.onReceive(context, intent)
        shadowOf(pending).future.get(10, TimeUnit.SECONDS)
    }

    private fun mainAlarm() = Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmScheduler.ACTION_ALARM_TRIGGER
        putExtra(AlarmScheduler.EXTRA_REMINDER_ID, id)
    }

    private fun requestSnooze(): Intent {
        deliver(shadowOf(manager.activeNotifications.single { it.notification.actions != null }.notification.actions[1].actionIntent).savedIntent)
        return shadowOf(alarms.scheduledAlarms.single().operation).savedIntent
    }

    @Test fun oneOffSnoozeStillPostsAfterTheOriginalWasCompleted() = runBlocking {
        deliver(mainAlarm())
        assertFalse(db.reminderDao().getReminderById(id)!!.isEnabled)
        val snooze = requestSnooze()
        SystemClock.setCurrentTimeMillis(System.currentTimeMillis() + 600_001)
        deliver(snooze)
        assertTrue(manager.activeNotifications.any { it.notification.extras.getString("android.title") == "fixture" })
    }

    @Test fun deniedNotificationDoesNotCompleteTheOneOffReminder() = runBlocking {
        shadowOf(context).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        deliver(mainAlarm())
        assertTrue(db.reminderDao().getReminderById(id)!!.isEnabled)
        assertTrue(manager.activeNotifications.none { it.notification.extras.getString("android.title") == "fixture" })
    }

    @Test fun cancelingASnoozePreventsItsOldIntentFromRevivingTheReminder() = runBlocking {
        deliver(mainAlarm())
        val snooze = requestSnooze()
        AlarmScheduler.cancelReminder(context, db.reminderDao().getReminderById(id)!!.toDomain())
        SystemClock.setCurrentTimeMillis(System.currentTimeMillis() + 600_001)
        deliver(snooze)
        assertTrue(manager.activeNotifications.none { it.notification.extras.getString("android.title") == "fixture" })
    }

    @Test fun upgradeKeepsTheOriginalTimeOfAnUnambiguousLegacySnooze() = runBlocking {
        db.reminderDao().getAllRemindersSync().filter { it.id != id }.forEach { db.reminderDao().deleteReminderById(it.id) }
        db.reminderDao().updateReminder(db.reminderDao().getReminderById(id)!!.copy(isEnabled = false))
        val legacyIntent = mainAlarm().putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, true)
        val legacy = PendingIntent.getBroadcast(context, AlarmScheduler.computeAlarmRequestCode(id, AlarmScheduler.SNOOZE_OFFSET),
            legacyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val trigger = System.currentTimeMillis() + 600_000
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExact(AlarmManager.RTC_WAKEUP, trigger, legacy)
        AlarmMigration.migrate(context)
        assertEquals(trigger, alarms.scheduledAlarms.single().triggerAtTime)
        SystemClock.setCurrentTimeMillis(trigger + 1)
        deliver(legacyIntent)
        assertTrue(manager.activeNotifications.any { it.notification.extras.getString("android.title") == "fixture" })
    }
}
