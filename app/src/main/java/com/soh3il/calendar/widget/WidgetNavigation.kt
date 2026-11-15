package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.soh3il.calendar.MainActivity
import com.soh3il.core.jalali.JalaliDate

data class WidgetDestination(val date: JalaliDate, val add: Boolean = false, val reminderId: Long? = null)

object WidgetNavigation {
    const val ACTION_OPEN = "com.soh3il.calendar.widget.OPEN"
    private const val EXTRA_DATE = "widget_date_jdn"
    private const val EXTRA_ADD = "widget_add"
    private const val EXTRA_REMINDER = "widget_reminder_id"

    fun intent(context: Context, widgetId: Int, destination: WidgetDestination): Intent =
        Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN
            data = Uri.parse("soh3il-widget://$widgetId/${destination.date.toJdn()}/${destination.add}/${destination.reminderId ?: 0}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtras(fillIn(destination))
        }

    fun pending(context: Context, widgetId: Int, destination: WidgetDestination): PendingIntent =
        PendingIntent.getActivity(context, widgetId, intent(context, widgetId, destination),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun fillIn(destination: WidgetDestination): Intent = Intent().apply {
        putExtra(EXTRA_DATE, destination.date.toJdn())
        putExtra(EXTRA_ADD, destination.add)
        destination.reminderId?.let { putExtra(EXTRA_REMINDER, it) }
    }

    fun parse(intent: Intent?): WidgetDestination? {
        if (intent?.action != ACTION_OPEN || !intent.hasExtra(EXTRA_DATE)) return null
        val jdn = intent.getLongExtra(EXTRA_DATE, Long.MIN_VALUE)
        if (jdn !in JalaliDate(1, 1, 1).toJdn()..JalaliDate(9999, 12, JalaliDate.daysInMonth(9999, 12)).toJdn()) return null
        val date = JalaliDate.fromJdn(jdn)
        if (date.toJdn() != jdn) return null
        val reminderId = intent.getLongExtra(EXTRA_REMINDER, 0).takeIf { it > 0 }
        return WidgetDestination(date, intent.getBooleanExtra(EXTRA_ADD, false), reminderId)
    }
}
