package com.soh3il.core.scheduler

import android.content.Context
import android.net.Uri

/** Extras and hash codes alone do not distinguish PendingIntents. */
internal object AlarmIdentity {
    const val LEGACY_SNOOZE = 1L
    fun uri(reminderId: Long, offset: Int, kind: String): Uri =
        Uri.parse("soh3il://reminder/$reminderId/$kind/$offset")

    fun notificationTag(reminderId: Long, offset: Int) = "reminder:$reminderId:$offset"

    private fun prefs(context: Context) = context.getSharedPreferences("pending_snoozes", Context.MODE_PRIVATE)

    fun rememberSnooze(context: Context, reminderId: Long, triggerAt: Long) {
        prefs(context).edit().putLong(reminderId.toString(), triggerAt).apply()
    }

    fun hasSnooze(context: Context, reminderId: Long, triggerAt: Long): Boolean =
        triggerAt > 0 && prefs(context).getLong(reminderId.toString(), 0L) == triggerAt

    fun allSnoozes(context: Context): Map<Long, Long> = prefs(context).all.mapNotNull { (key, value) ->
        val id = key.toLongOrNull()
        if (id != null && value is Long && value > LEGACY_SNOOZE) id to value else null
    }.toMap()

    fun clearSnooze(context: Context, reminderId: Long) {
        prefs(context).edit().remove(reminderId.toString()).apply()
    }
}
