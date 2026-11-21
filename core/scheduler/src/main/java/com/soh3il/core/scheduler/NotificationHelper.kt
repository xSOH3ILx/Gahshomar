package com.soh3il.core.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.utils.toPersianDigits

object NotificationHelper {

    const val CHANNEL_ID = "soh3il_reminders_channel"
    const val CHANNEL_NAME = "یادآورهای تقویم خورشیدی"

    const val ACTION_MARK_DONE = "com.soh3il.calendar.ACTION_MARK_DONE"
    const val ACTION_DISMISS_REMINDER = "com.soh3il.calendar.ACTION_DISMISS_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.soh3il.calendar.ACTION_SNOOZE_REMINDER"
    const val ACTION_SNOOZE_15 = "com.soh3il.calendar.ACTION_SNOOZE_15"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    private fun selectedSoundUri(context: Context): Uri? {
        if (UserPreferences.isNotificationSoundSilent(context)) return null
        val saved = UserPreferences.getNotificationSoundUri(context)
        return if (!saved.isNullOrEmpty()) {
            Uri.parse(saved)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
    }

    private fun channelId(context: Context): String {
        val revision = UserPreferences.getNotificationChannelRevision(context)
        return if (revision == 0L) CHANNEL_ID else "$CHANNEL_ID.$revision"
    }

    fun getEffectiveSoundUri(context: Context): Uri? {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(channelId(context))
        return if (channel != null) channel.sound else selectedSoundUri(context)
    }

    fun createNotificationChannel(context: Context, customSoundUri: Uri? = selectedSoundUri(context)) {
        createChannel(context, channelId(context), customSoundUri)
    }

    private fun createChannel(context: Context, id: String, soundUri: Uri?, previous: NotificationChannel? = null) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()

        val channel = NotificationChannel(
            id,
            CHANNEL_NAME,
            previous?.importance ?: NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان‌های زنده و فوری یادآورهای تقویم SOH3IL"
            previous?.vibrationPattern?.let { vibrationPattern = it }
            enableVibration(previous?.shouldVibrate() ?: true)
            previous?.let {
                enableLights(it.shouldShowLights())
                lightColor = it.lightColor
                setShowBadge(it.canShowBadge())
                lockscreenVisibility = it.lockscreenVisibility
            }
            setSound(soundUri, audioAttributes)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun refreshChannelsForQuietHours(context: Context) {
        updateNotificationChannelSound(context, UserPreferences.getNotificationSoundUri(context)?.let { Uri.parse(it) })
    }

    fun isQuietPeriodActiveNow(context: Context): Boolean {
        if (!UserPreferences.isQuietHoursEnabled(context)) return false
        val calendar = java.util.Calendar.getInstance()
        val minuteOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        return UserPreferences.isQuietPeriodActive(context, minuteOfDay)
    }

    fun canPostNotifications(context: Context, channelId: String? = null): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!manager.areNotificationsEnabled()) return false
        if (channelId != null && manager.getNotificationChannel(channelId)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canPostReminders(context: Context): Boolean {
        createNotificationChannel(context)
        return canPostNotifications(context, channelId(context))
    }

    fun updateNotificationChannelSound(context: Context, newSoundUri: Uri?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val previous = manager.getNotificationChannel(channelId(context))
        val revision = Math.addExact(UserPreferences.getNotificationChannelRevision(context), 1L)
        val nextId = "$CHANNEL_ID.$revision"
        // Android restores deleted channel settings. A new ID is used only on explicit sound selection.
        createChannel(context, nextId, newSoundUri, previous)
        UserPreferences.setNotificationChannelRevision(context, revision)
        val activeChannels = manager.activeNotifications.map { it.notification.channelId }.toSet()
        manager.notificationChannels.filter {
            (it.id == CHANNEL_ID || it.id.startsWith("$CHANNEL_ID.")) && it.id != nextId && it.id !in activeChannels
        }.forEach { manager.deleteNotificationChannel(it.id) }
    }

    fun cancelReminderNotification(context: Context, reminderId: Long, preOffsetMinutes: Int = 0) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = AlarmScheduler.computeAlarmRequestCode(reminderId, preOffsetMinutes)
        notificationManager.cancel(AlarmIdentity.notificationTag(reminderId, preOffsetMinutes), notificationId)
    }

    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        description: String,
        isPreReminder: Boolean = false,
        preOffsetMinutes: Int = 0
    ): Boolean {
        createNotificationChannel(context)
        if (!canPostNotifications(context, channelId(context))) return false
        val soundUri = getEffectiveSoundUri(context)
        val snoozeMinutes = UserPreferences.getSnoozeMinutes(context)
        val quietNow = isQuietPeriodActiveNow(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = AlarmScheduler.computeAlarmRequestCode(reminderId, preOffsetMinutes)

        // Explicit Intent to launch MainActivity
        val launchIntent = Intent().setClassName(
            context,
            "com.soh3il.calendar.MainActivity"
        ).apply {
            data = AlarmIdentity.uri(reminderId, preOffsetMinutes, "open")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Dismiss / Mark Done
        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_REMINDER
            data = AlarmIdentity.uri(reminderId, preOffsetMinutes, "done")
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmScheduler.EXTRA_PRE_OFFSET_MINUTES, preOffsetMinutes)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.actionRequestCode(reminderId, preOffsetMinutes, 1),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            data = AlarmIdentity.uri(reminderId, preOffsetMinutes, "snooze")
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(AlarmScheduler.EXTRA_PRE_OFFSET_MINUTES, preOffsetMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmScheduler.actionRequestCode(reminderId, preOffsetMinutes, 2),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayTitle = if (isPreReminder) "پیش‌یادآور: $title" else title

        val builder = NotificationCompat.Builder(context, channelId(context))
            .setSmallIcon(com.soh3il.core.designsystem.R.drawable.ic_stat_calendar)
            .setContentTitle(displayTitle)
            .setContentText(description.ifEmpty { "زمان سررسید این رویداد فرا رسیده است." })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .apply {
                if (quietNow) {
                    setVibrate(longArrayOf(0))
                } else {
                    setSound(soundUri)
                }
            }
            .setContentIntent(contentPendingIntent)
            .addAction(0, "انجام شد", donePendingIntent)
            .addAction(0, "تعویق ${snoozeMinutes.toString().toPersianDigits()} دقیقه‌ای", snoozePendingIntent)

        return try {
            notificationManager.notify(AlarmIdentity.notificationTag(reminderId, preOffsetMinutes), notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
