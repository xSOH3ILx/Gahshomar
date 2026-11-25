package com.soh3il.calendar

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.scheduler.NotificationHelper
import com.soh3il.core.scheduler.PersistentNotificationHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, instrumentedPackages = ["com.soh3il"])
class NotificationSettingsTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val manager get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    @Test fun silentSelectionCreatesASilentChannel() {
        UserPreferences.setNotificationSound(context, null, "بی‌صدا")
        NotificationHelper.createNotificationChannel(context)
        assertNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_ID).sound)
    }
    @Test fun explicitNewSoundIsUsedWithoutRecreatingTheSameChannelId() {
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        NotificationHelper.createNotificationChannel(context)
        val sound = Uri.parse("content://fixture/custom-tone")
        UserPreferences.setNotificationSound(context, sound.toString(), "fixture")
        NotificationHelper.updateNotificationChannelSound(context, sound)
        assertTrue(NotificationHelper.showReminderNotification(context, 1, "fixture", ""))
        val notification = manager.activeNotifications.single().notification
        assertNotEquals(NotificationHelper.CHANNEL_ID, notification.channelId)
        assertEquals(sound, manager.getNotificationChannel(notification.channelId).sound)
    }
    @Test fun changingSoundMustNotBypassABlockedChannel() {
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val channel = android.app.NotificationChannel(NotificationHelper.CHANNEL_ID, "fixture", NotificationManager.IMPORTANCE_NONE)
        manager.createNotificationChannel(channel)
        NotificationHelper.updateNotificationChannelSound(context, Uri.parse("content://fixture/custom-tone"))
        assertFalse(NotificationHelper.showReminderNotification(context, 1, "fixture", ""))
    }
    @Test fun changingSoundPreservesDisabledVibrationWithAnExistingPattern() {
        val channel = android.app.NotificationChannel(NotificationHelper.CHANNEL_ID, "fixture", NotificationManager.IMPORTANCE_DEFAULT)
        channel.vibrationPattern = longArrayOf(0, 100, 200)
        channel.enableVibration(false)
        manager.createNotificationChannel(channel)
        NotificationHelper.updateNotificationChannelSound(context, Uri.parse("content://fixture/custom-tone"))
        val updated = manager.notificationChannels.single()
        assertFalse(updated.shouldVibrate())
        assertArrayEquals(channel.vibrationPattern, updated.vibrationPattern)
    }
    @Test fun disablingDynamicIconChangesTheActualDateNotificationResource() {
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        UserPreferences.setDynamicNotificationIconEnabled(context, true)
        PersistentNotificationHelper.updateNotification(context, emptyList())
        val dynamic = manager.activeNotifications.single { it.id == 2001 }.notification.smallIcon.resId
        UserPreferences.setDynamicNotificationIconEnabled(context, false)
        PersistentNotificationHelper.updateNotification(context, emptyList())
        val fixed = manager.activeNotifications.single { it.id == 2001 }.notification.smallIcon.resId
        assertNotEquals(dynamic, fixed)
    }
    @Test fun lockscreenPublicVersionDoesNotExposeDateWhenDisabled() {
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        UserPreferences.setLockscreenCalendarEnabled(context, false)
        PersistentNotificationHelper.updateNotification(context, emptyList())
        val notification = manager.activeNotifications.single { it.id == 2001 }.notification
        assertEquals(android.app.Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals(com.soh3il.core.designsystem.R.drawable.ic_stat_calendar, notification.smallIcon.resId)
        assertEquals(com.soh3il.core.designsystem.R.drawable.ic_stat_calendar, notification.publicVersion.smallIcon.resId)
        assertNull(notification.publicVersion.extras.getCharSequence("android.title"))
        assertEquals("محتوای تقویم پنهان است", notification.publicVersion.extras.getCharSequence("android.text").toString())
    }

    @Test fun lockscreenAndDynamicIconPreferenceChangesTriggerNeedsRefreshAndPreserveStatusDayNumber() {
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        UserPreferences.setPersistentNotificationEnabled(context, true)
        UserPreferences.setLockscreenCalendarEnabled(context, true)
        UserPreferences.setDynamicNotificationIconEnabled(context, true)
        PersistentNotificationHelper.updateNotification(context, emptyList())

        val active = manager.activeNotifications.single { it.id == PersistentNotificationHelper.NOTIFICATION_ID }.notification
        val day = com.soh3il.core.jalali.JalaliDate.now().day.coerceIn(1, 31)
        val expectedDayRes = context.resources.getIdentifier("ic_stat_day_$day", "drawable", context.packageName)
        assertTrue(expectedDayRes != 0)
        assertEquals(expectedDayRes, active.smallIcon.resId)
        assertEquals(expectedDayRes, active.publicVersion.smallIcon.resId)
        assertFalse(PersistentNotificationHelper.needsRefresh(context))

        UserPreferences.setLockscreenCalendarEnabled(context, false)
        assertTrue(PersistentNotificationHelper.needsRefresh(context))
        PersistentNotificationHelper.updateNotification(context, emptyList())
        assertFalse(PersistentNotificationHelper.needsRefresh(context))

        UserPreferences.setLockscreenCalendarEnabled(context, true)
        UserPreferences.setDynamicNotificationIconEnabled(context, false)
        assertTrue(PersistentNotificationHelper.needsRefresh(context))
    }

    @Test fun quietPeriodCalculationHandlesNormalAndOvernightSpans() {
        // Disabled quiet hours always returns false
        UserPreferences.setQuietHoursEnabled(context, false)
        assertFalse(UserPreferences.isQuietPeriodActive(context, 23 * 60))

        // Overnight span: 22:00 to 07:00
        UserPreferences.setQuietHoursEnabled(context, true)
        UserPreferences.setQuietStartMinute(context, 22 * 60)
        UserPreferences.setQuietEndMinute(context, 7 * 60)

        assertTrue(UserPreferences.isQuietPeriodActive(context, 23 * 60))
        assertTrue(UserPreferences.isQuietPeriodActive(context, 22 * 60))
        assertTrue(UserPreferences.isQuietPeriodActive(context, 0))
        assertTrue(UserPreferences.isQuietPeriodActive(context, 6 * 60 + 59))
        assertFalse(UserPreferences.isQuietPeriodActive(context, 7 * 60))
        assertFalse(UserPreferences.isQuietPeriodActive(context, 12 * 60))

        // Daytime span: 13:00 to 15:00
        UserPreferences.setQuietStartMinute(context, 13 * 60)
        UserPreferences.setQuietEndMinute(context, 15 * 60)
        assertFalse(UserPreferences.isQuietPeriodActive(context, 12 * 60))
        assertTrue(UserPreferences.isQuietPeriodActive(context, 13 * 60))
        assertTrue(UserPreferences.isQuietPeriodActive(context, 14 * 60))
        assertFalse(UserPreferences.isQuietPeriodActive(context, 15 * 60))
    }

    @Test fun newPreferenceAccessorsPersistAndDefaultCorrectly() {
        assertEquals(10, UserPreferences.getSnoozeMinutes(context))
        UserPreferences.setSnoozeMinutes(context, 15)
        assertEquals(15, UserPreferences.getSnoozeMinutes(context))

        assertEquals(-1, UserPreferences.getDefaultReminderMinutes(context))
        UserPreferences.setDefaultReminderMinutes(context, 30)
        assertEquals(30, UserPreferences.getDefaultReminderMinutes(context))

        assertFalse(UserPreferences.isAutoBackupEnabled(context))
        UserPreferences.setAutoBackupEnabled(context, true)
        assertTrue(UserPreferences.isAutoBackupEnabled(context))

        assertEquals(0L, UserPreferences.getAutoBackupLastAt(context))
        val now = 1700000000000L
        UserPreferences.setAutoBackupLastAt(context, now)
        assertEquals(now, UserPreferences.getAutoBackupLastAt(context))

        assertEquals("JALALI", UserPreferences.getDefaultCalendar(context))
        UserPreferences.setDefaultCalendar(context, "GREGORIAN")
        assertEquals("GREGORIAN", UserPreferences.getDefaultCalendar(context))

        assertEquals("SYSTEM", UserPreferences.getWidgetTheme(context))
        UserPreferences.setWidgetTheme(context, "DARK")
        assertEquals("DARK", UserPreferences.getWidgetTheme(context))
    }

}
