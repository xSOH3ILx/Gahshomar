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
}
