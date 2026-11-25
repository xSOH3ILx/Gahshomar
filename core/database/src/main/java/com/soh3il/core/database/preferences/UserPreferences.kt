package com.soh3il.core.database.preferences

import android.content.Context
import android.content.SharedPreferences
import com.soh3il.core.model.AppTheme

object UserPreferences {
    private const val PREFS_NAME = "soh3il_user_preferences"
    private const val KEY_THEME = "pref_theme"
    private const val KEY_AMOLED = "pref_amoled"
    private const val KEY_NOTIFICATION_SOUND_URI = "pref_sound_uri"
    private const val KEY_NOTIFICATION_SOUND_TITLE = "pref_sound_title"
    private const val KEY_NOTIFICATION_SOUND_SILENT = "pref_sound_silent"
    private const val KEY_NOTIFICATION_CHANNEL_REVISION = "pref_sound_channel_revision"
    private const val KEY_PERSISTENT_NOTIFICATION = "pref_persistent_notification"
    private const val KEY_LIVE_ICON = "pref_live_icon"
    private const val KEY_LOCKSCREEN_CALENDAR = "pref_lockscreen_calendar"
    private const val KEY_PERSISTENT_GREGORIAN = "pref_persistent_gregorian"
    private const val KEY_PERSISTENT_HIJRI = "pref_persistent_hijri"
    private const val KEY_CONTACT_BIRTHDAYS = "pref_contact_birthdays"
    private const val KEY_BIRTH_JDN = "pref_birth_jdn"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTheme(context: Context): AppTheme {
        val name = getPrefs(context).getString(KEY_THEME, AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(name ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            android.util.Log.w("UserPreferences", "unknown theme name '$name'; falling back to SYSTEM", e)
            AppTheme.SYSTEM
        }
    }

    fun setTheme(context: Context, theme: AppTheme) {
        getPrefs(context).edit().putString(KEY_THEME, theme.name).apply()
    }

    fun isAmoled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AMOLED, false)
    }

    fun setAmoled(context: Context, isAmoled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AMOLED, isAmoled).apply()
    }

    fun getNotificationSoundUri(context: Context): String? {
        return getPrefs(context).getString(KEY_NOTIFICATION_SOUND_URI, null)
    }

    fun getNotificationSoundTitle(context: Context): String {
        return getPrefs(context).getString(KEY_NOTIFICATION_SOUND_TITLE, "پیش‌فرض سیستم") ?: "پیش‌فرض سیستم"
    }

    fun setNotificationSound(context: Context, uri: String?, title: String) {
        getPrefs(context).edit()
            .putString(KEY_NOTIFICATION_SOUND_URI, uri)
            .putString(KEY_NOTIFICATION_SOUND_TITLE, title)
            .putBoolean(KEY_NOTIFICATION_SOUND_SILENT, uri == null)
            .apply()
    }

    fun isNotificationSoundSilent(context: Context): Boolean = getPrefs(context).getBoolean(
        KEY_NOTIFICATION_SOUND_SILENT, getNotificationSoundTitle(context) == "بی‌صدا")

    fun getNotificationChannelRevision(context: Context): Long =
        getPrefs(context).getLong(KEY_NOTIFICATION_CHANNEL_REVISION, 0L)

    fun setNotificationChannelRevision(context: Context, revision: Long) {
        getPrefs(context).edit().putLong(KEY_NOTIFICATION_CHANNEL_REVISION, revision).apply()
    }

    fun isPersistentNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PERSISTENT_NOTIFICATION, true)
    }

    fun setPersistentNotificationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled).apply()
    }

    fun isLiveIconEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LIVE_ICON, true)
    }

    fun setLiveIconEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LIVE_ICON, enabled).apply()
    }

    fun isDynamicNotificationIconEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("pref_dynamic_notif_icon", true)
    }

    fun setDynamicNotificationIconEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("pref_dynamic_notif_icon", enabled).apply()
    }

    fun isLockscreenCalendarEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_LOCKSCREEN_CALENDAR, true)

    fun setLockscreenCalendarEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCKSCREEN_CALENDAR, enabled).apply()
    }

    fun isPersistentGregorianEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_PERSISTENT_GREGORIAN, true)

    fun setPersistentGregorianEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERSISTENT_GREGORIAN, enabled).apply()
    }

    fun isPersistentHijriEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_PERSISTENT_HIJRI, true)

    fun setPersistentHijriEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERSISTENT_HIJRI, enabled).apply()
    }

    fun isContactBirthdaysEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_CONTACT_BIRTHDAYS, false)

    fun setContactBirthdaysEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_CONTACT_BIRTHDAYS, enabled).apply()
    }

    /** Jalali day number of the user's birth date, or null when not set. */
    fun getBirthJdn(context: Context): Long? =
        getPrefs(context).getLong(KEY_BIRTH_JDN, 0L).takeIf { it != 0L }

    fun setBirthJdn(context: Context, jdn: Long?) {
        val edit = getPrefs(context).edit()
        if (jdn == null) edit.remove(KEY_BIRTH_JDN) else edit.putLong(KEY_BIRTH_JDN, jdn)
        edit.apply()
    }

    private const val KEY_SNOOZE_MINUTES = "pref_snooze_minutes"
    private const val KEY_DEFAULT_REMINDER_MINUTES = "pref_default_reminder_minutes"
    private const val KEY_QUIET_HOURS_ENABLED = "pref_quiet_hours_enabled"
    private const val KEY_QUIET_START_MINUTE = "pref_quiet_start_minute"
    private const val KEY_QUIET_END_MINUTE = "pref_quiet_end_minute"
    private const val KEY_AUTO_BACKUP_ENABLED = "pref_auto_backup_enabled"
    private const val KEY_AUTO_BACKUP_LAST = "pref_auto_backup_last"
    private const val KEY_DEFAULT_CALENDAR = "pref_default_calendar"
    private const val KEY_WIDGET_THEME = "pref_widget_theme"

    val SNOOZE_OPTIONS_MINUTES = listOf(5, 10, 15, 30, 60)

    fun getSnoozeMinutes(context: Context): Int =
        getPrefs(context).getInt(KEY_SNOOZE_MINUTES, 10)

    fun setSnoozeMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_SNOOZE_MINUTES, minutes).apply()
    }

    val DEFAULT_REMINDER_OPTIONS_MINUTES = listOf(-1, 0, 5, 10, 15, 30, 60)

    fun getDefaultReminderMinutes(context: Context): Int =
        getPrefs(context).getInt(KEY_DEFAULT_REMINDER_MINUTES, -1)

    fun setDefaultReminderMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_DEFAULT_REMINDER_MINUTES, minutes).apply()
    }

    fun isQuietHoursEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_QUIET_HOURS_ENABLED, false)

    fun setQuietHoursEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_QUIET_HOURS_ENABLED, enabled).apply()
    }

    fun getQuietStartMinute(context: Context): Int =
        getPrefs(context).getInt(KEY_QUIET_START_MINUTE, 22 * 60)

    fun setQuietStartMinute(context: Context, minuteOfDay: Int) {
        getPrefs(context).edit().putInt(KEY_QUIET_START_MINUTE, minuteOfDay).apply()
    }

    fun getQuietEndMinute(context: Context): Int =
        getPrefs(context).getInt(KEY_QUIET_END_MINUTE, 7 * 60)

    fun setQuietEndMinute(context: Context, minuteOfDay: Int) {
        getPrefs(context).edit().putInt(KEY_QUIET_END_MINUTE, minuteOfDay).apply()
    }

    fun isAutoBackupEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, false)

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getAutoBackupLastAt(context: Context): Long =
        getPrefs(context).getLong(KEY_AUTO_BACKUP_LAST, 0L)

    fun setAutoBackupLastAt(context: Context, epochMillis: Long) {
        getPrefs(context).edit().putLong(KEY_AUTO_BACKUP_LAST, epochMillis).apply()
    }

    fun getDefaultCalendar(context: Context): String =
        getPrefs(context).getString(KEY_DEFAULT_CALENDAR, "JALALI") ?: "JALALI"

    fun setDefaultCalendar(context: Context, calendarName: String) {
        getPrefs(context).edit().putString(KEY_DEFAULT_CALENDAR, calendarName).apply()
    }

    fun getWidgetTheme(context: Context): String =
        getPrefs(context).getString(KEY_WIDGET_THEME, "SYSTEM") ?: "SYSTEM"

    fun setWidgetTheme(context: Context, themeName: String) {
        getPrefs(context).edit().putString(KEY_WIDGET_THEME, themeName).apply()
    }

    fun isQuietPeriodActive(context: Context, minuteOfDay: Int): Boolean {
        if (!isQuietHoursEnabled(context)) return false
        val start = getQuietStartMinute(context)
        val end = getQuietEndMinute(context)
        return if (start <= end) minuteOfDay in start until end else minuteOfDay >= start || minuteOfDay < end
    }
}
