package com.soh3il.core.scheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.matchesDate
import com.soh3il.core.model.Reminder
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PersistentNotificationHelper {

    private const val LEGACY_CHANNEL_ID = "persistent_date_channel"
    const val CHANNEL_ID = "persistent_date_channel_v2"
    private const val CHANNEL_NAME = "نوار اعلان دائمی تاریخ"
    const val NOTIFICATION_ID = 2001
    const val EXTRA_DATE_JDN = "soh3il.date.jdn"
    const val EXTRA_LOCKSCREEN_ENABLED = "soh3il.date.lockscreen_enabled"
    const val EXTRA_DYNAMIC_ICON_ENABLED = "soh3il.date.dynamic_icon_enabled"
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val renderLock = Any()
    private var generation = 0L
    @Volatile
    private var lastKnownReminders: List<Reminder>? = null
    const val EXTRA_OPEN_ADD_REMINDER = "extra_open_add_reminder"
    const val ACTION_OPEN_ADD_REMINDER = "com.soh3il.calendar.action.OPEN_ADD_REMINDER"

    private val GREGORIAN_MONTHS_FA = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    private val HIJRI_MONTHS_FA = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی",
        "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان",
        "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )

    fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        val legacy = notificationManager.getNotificationChannel(LEGACY_CHANNEL_ID)
        val legacyBlocked = legacy != null && legacy.importance == NotificationManager.IMPORTANCE_NONE
        if (existing == null && legacyBlocked) {
            // Respect explicit system-level channel block from earlier versions.
            val blockedChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE
            ).apply {
                description = "نمایش پیوسته تاریخ روز خورشیدی و رویدادها در نوار وضعیت، صفحه قفل و AOD"
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(blockedChannel)
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            return
        }
        if (legacy != null) {
            notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "نمایش پیوسته تاریخ روز خورشیدی و رویدادها در نوار وضعیت، صفحه قفل و AOD"
            setShowBadge(true)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createDayNumberBitmap(context: Context, dayNumber: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (32 * density).toInt().coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val text = dayNumber.toPersianDigits()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 255, 74)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = if (text.length > 2) sizePx * 0.48f else sizePx * 0.68f
            textAlign = Paint.Align.CENTER
        }

        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)
        val x = sizePx / 2f
        val y = (sizePx / 2f) - bounds.exactCenterY()

        canvas.drawText(text, x, y, textPaint)
        return bitmap
    }

    fun updateNotification(context: Context, reminders: List<Reminder>?, summaryUnavailable: Boolean = false) {
        synchronized(renderLock) {
            generation++
            if (reminders != null && !summaryUnavailable) {
                lastKnownReminders = reminders
            }
            renderNotification(context, reminders, summaryUnavailable)
        }
    }

    fun hasCachedReminders(): Boolean = lastKnownReminders != null

    fun buildForegroundNotification(context: Context): Notification? {
        if (!UserPreferences.isPersistentNotificationEnabled(context)) return null
        createNotificationChannel(context)
        return buildNotification(context, lastKnownReminders)
    }

    fun restoreIfMissing(context: Context) {
        if (!UserPreferences.isPersistentNotificationEnabled(context)) return
        if (needsRefresh(context)) {
            synchronized(renderLock) {
                if (needsRefresh(context)) {
                    renderNotification(context.applicationContext, lastKnownReminders)
                }
            }
            if (lastKnownReminders == null) {
                updateNotification(context)
            }
        }
    }

    fun repostCachedNotification(context: Context) {
        if (!UserPreferences.isPersistentNotificationEnabled(context)) return
        synchronized(renderLock) {
            renderNotification(context.applicationContext, lastKnownReminders)
        }
    }

    private fun renderNotification(context: Context, reminders: List<Reminder>?, summaryUnavailable: Boolean = false) {
        MidnightScheduler.scheduleMidnightAlarm(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!UserPreferences.isPersistentNotificationEnabled(context)) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }
        createNotificationChannel(context)
        if (!NotificationHelper.canPostNotifications(context, CHANNEL_ID)) {
            return
        }

        val notification = buildNotification(context, reminders, summaryUnavailable)
        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (error: SecurityException) {
            Log.w("PersistentDate", "Date notification permission unavailable")
        }
    }

    private fun buildNotification(
        context: Context,
        reminders: List<Reminder>?,
        summaryUnavailable: Boolean = false
    ): Notification {
        val today = JalaliDate.now()
        val rlm = "\u200F" // نویسه اجباری راست‌چین یونیکد (Right-To-Left Mark)

        val fullTitle = "$rlm${today.dayOfWeekNameFa}، ${today.day.toPersianDigits()} ${today.monthNameFa} ${today.year.toPersianDigits()}"

        // Gregorian & Hijri equivalent
        val conv = DateConverter.fromJalali(today.year, today.month, today.day)
        val gregStr = "${conv.gregorian.dayOfMonth.toPersianDigits()} ${GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }} ${conv.gregorian.year.toPersianDigits()}"
        val hijriStr = if (conv.hijriYear > 0) {
            "${conv.hijriDay.toPersianDigits()} ${HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }} ${conv.hijriYear.toPersianDigits()}"
        } else null

        // Occasions for today
        val todayStr = today.format("YYYY-MM-DD")
        val occasions = IranianHolidays.getOccasionsForMonth(today.year, today.month)
            .filter { it.jalaliDate == todayStr }

        // Today's active reminders count (including recurring reminders)
        val todayRemindersCount = reminders?.count { it.matchesDate(today) }

        // در نمای بزرگ (InboxStyle) خط جلالی تکراری حذف می‌شود؛ عنوان بزرگ همان تاریخ جلالی است.
        val showGregorian = UserPreferences.isPersistentGregorianEnabled(context)
        val showHijri = UserPreferences.isPersistentHijriEnabled(context)
        val lineGregorian = if (showGregorian) {
            "${rlm}تقویم میلادی: $gregStr"
        } else null
        val lineHijri = if (showHijri && hijriStr != null) {
            "${rlm}تقویم هجری قمری محاسباتی: $hijriStr"
        } else null

        // خط مناسبت‌ها فقط وقتی مناسبت واقعی وجود دارد نمایش داده می‌شود (کاهش نویز).
        val lunarIncomplete = !IranianHolidays.hasOfficialLunarHolidays(today.year)
        val lineOccasions = if (occasions.isNotEmpty()) {
            "${rlm}رویدادها: " + occasions.joinToString("، ") { it.titleFa } +
                if (lunarIncomplete) " (اطلاعات قمری ناقص است)" else ""
        } else {
            null
        }

        val lineReminders = if (todayRemindersCount == null) {
            if (summaryUnavailable) "${rlm}یادآورها: خواندن اطلاعات ممکن نشد" else "${rlm}یادآورها: در حال بررسی"
        } else if (todayRemindersCount > 0) {
            "${rlm}یادآورها: ${todayRemindersCount.toPersianDigits()} یادآور برای امروز"
        } else {
            "${rlm}یادآورها: بدون یادآور برای امروز"
        }

        val notificationLines = listOfNotNull(
            lineGregorian,
            lineHijri,
            lineOccasions,
            lineReminders
        )

        // خلاصه فشرده جهت نمایش در حالت تک‌خطی اعلان (کاملاً راست‌چین با RLM)
        val dateParts = buildList {
            add("${today.day.toPersianDigits()} ${today.monthNameFa}")
            if (showGregorian) {
                add("${conv.gregorian.dayOfMonth.toPersianDigits()} ${GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }}")
            }
            if (showHijri && conv.hijriYear > 0) {
                add("${conv.hijriDay.toPersianDigits()} ${HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }}")
            }
        }
        val shortSummary = rlm + dateParts.joinToString(" • ")

        // Content intent (explicitly opens MainActivity)
        val launchIntent = Intent().setClassName(
            context,
            "com.soh3il.calendar.MainActivity"
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intent for "+ یادآور جدید" (explicitly opens MainActivity)
        val addReminderIntent = Intent().setClassName(
            context,
            "com.soh3il.calendar.MainActivity"
        ).apply {
            action = ACTION_OPEN_ADD_REMINDER
            putExtra(EXTRA_OPEN_ADD_REMINDER, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addReminderPendingIntent = PendingIntent.getActivity(
            context,
            1,
            addReminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete intent: triggers automatic restoration if swiped away while enabled
        val deleteIntent = Intent(context, MidnightReceiver::class.java).apply {
            action = MidnightReceiver.ACTION_NOTIFICATION_DISMISSED
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            2002,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The system may display the main small icon on the lock screen or AOD
        // even when it uses a publicVersion for the notification contents.
        val showOnLockscreen = UserPreferences.isLockscreenCalendarEnabled(context)
        val dynamicIconEnabled = UserPreferences.isDynamicNotificationIconEnabled(context)
        val smallIconRes = if (showOnLockscreen && dynamicIconEnabled) {
            DateNotificationAssets.getDayDrawableRes(context, today.day)
        } else {
            com.soh3il.core.designsystem.R.drawable.ic_stat_calendar
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .addExtras(
                Bundle().apply {
                    putLong(EXTRA_DATE_JDN, today.toJdn())
                    putBoolean(EXTRA_LOCKSCREEN_ENABLED, showOnLockscreen)
                    putBoolean(EXTRA_DYNAMIC_ICON_ENABLED, dynamicIconEnabled)
                }
            )
            .setContentTitle(fullTitle)
            .setContentText(shortSummary)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(null)
                    .setBigContentTitle(fullTitle)
                    .also { style -> notificationLines.forEach(style::addLine) }
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(
                if (showOnLockscreen) {
                    NotificationCompat.VISIBILITY_PUBLIC
                } else {
                    NotificationCompat.VISIBILITY_PRIVATE
                }
            )
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .addAction(com.soh3il.core.designsystem.R.drawable.ic_action_add_reminder, "افزودن یادآور", addReminderPendingIntent)

        // تنظیم تضمینی آیکون روز در نوار وضعیت با شناسه منبع استاندارد اندروید
        builder.setSmallIcon(smallIconRes)

        // نمایش بَدج گرافیکی تقویم امروز بجای آیکون پیش‌فرض برنامه در بدنه اعلان
        val dateBadgeBitmap = DateNotificationAssets.createDateBadgeBitmap(context, today)
        builder.setLargeIcon(dateBadgeBitmap)

        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (showOnLockscreen) smallIconRes else com.soh3il.core.designsystem.R.drawable.ic_stat_calendar)
            .apply { if (showOnLockscreen) setLargeIcon(dateBadgeBitmap) }
            .setContentTitle(if (showOnLockscreen) fullTitle else null)
            .setContentText(if (showOnLockscreen) shortSummary else "محتوای تقویم پنهان است")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
        publicVersion.flags = publicVersion.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        builder.setPublicVersion(publicVersion)

        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        return notification
    }

    /** A slow earlier database read must never overwrite a newer refresh. */
    fun beginRefresh(context: Context): Long = synchronized(renderLock) {
        generation++
        renderNotification(context.applicationContext, null)
        generation
    }

    fun finishRefresh(context: Context, ticket: Long, reminders: List<Reminder>?, unavailable: Boolean = false) {
        synchronized(renderLock) {
            if (ticket == generation) {
                if (reminders != null && !unavailable) {
                    lastKnownReminders = reminders
                }
                renderNotification(context.applicationContext, reminders, unavailable)
            }
        }
    }

    fun updateNotification(context: Context) {
        val app = context.applicationContext
        // Date and the next alarm must not depend on database availability.
        val ticket = beginRefresh(app)
        refreshScope.launch {
            try {
                withTimeout(8_500L) {
                    val db = com.soh3il.core.database.Soh3ilDatabase.getInstance(app)
                    val active = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
                    finishRefresh(app, ticket, active)
                }
            } catch (error: TimeoutCancellationException) {
                finishRefresh(app, ticket, null, unavailable = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("PersistentDate", "Reminder summary unavailable: ${error.javaClass.simpleName}")
                finishRefresh(app, ticket, null, unavailable = true)
            }
        }
    }

    fun needsRefresh(context: Context): Boolean {
        if (!UserPreferences.isPersistentNotificationEnabled(context)) return false
        createNotificationChannel(context)
        if (!NotificationHelper.canPostNotifications(context, CHANNEL_ID)) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val current = manager.activeNotifications.firstOrNull { it.id == NOTIFICATION_ID } ?: return true
        val notification = current.notification
        if (notification.extras?.getLong(EXTRA_DATE_JDN, Long.MIN_VALUE) != JalaliDate.now().toJdn()) return true
        if (notification.extras?.containsKey(EXTRA_LOCKSCREEN_ENABLED) != true ||
            notification.extras.getBoolean(EXTRA_LOCKSCREEN_ENABLED) != UserPreferences.isLockscreenCalendarEnabled(context)) return true
        if (notification.extras?.containsKey(EXTRA_DYNAMIC_ICON_ENABLED) != true ||
            notification.extras.getBoolean(EXTRA_DYNAMIC_ICON_ENABLED) != UserPreferences.isDynamicNotificationIconEnabled(context)) return true
        if (notification.channelId != CHANNEL_ID) return true
        if ((notification.flags and Notification.FLAG_NO_CLEAR) == 0) return true
        if (notification.deleteIntent == null) return true
        return false
    }

    fun cancelNotification(context: Context) {
        synchronized(renderLock) {
            generation++
            lastKnownReminders = null
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }
}
