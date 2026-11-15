package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.scheduler.MidnightScheduler

abstract class WidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in ACTIONS) return
        when (intent.action) {
            WidgetUpdates.ACTION_REFRESH -> WidgetUpdates.receive(this, context)
            ACTION_MONTH -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                val delta = intent.getIntExtra(EXTRA_DELTA, Int.MIN_VALUE)
                if (WidgetKind.forId(context, id) != WidgetKind.MONTH || delta !in -1..1) return
                WidgetUpdates.receive(this, context) {
                    val options = WidgetOptionsStore.read(context, id)
                    val date = options.monthJdn?.let(JalaliDate::fromJdn) ?: JalaliDate.now()
                    val first = JalaliDate(date.year, date.month, 1)
                    val next = if (delta == 0) null else first.plusMonths(delta).toJdn()
                    WidgetOptionsStore.save(context, id, options.copy(monthJdn = next))
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        MidnightScheduler.scheduleMidnightAlarm(context)
        WidgetUpdates.receive(this, context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        WidgetUpdates.receive(this, context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetUpdates.receive(this, context) { WidgetOptionsStore.delete(context, appWidgetIds) }
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        WidgetUpdates.receive(this, context) { WidgetOptionsStore.restore(context, oldWidgetIds, newWidgetIds) }
    }

    companion object {
        private const val ACTION_MONTH = "com.soh3il.calendar.widget.MONTH"
        private const val EXTRA_DELTA = "month_delta"
        private val ACTIONS = setOf(
            WidgetUpdates.ACTION_REFRESH, ACTION_MONTH, AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED, AppWidgetManager.ACTION_APPWIDGET_DISABLED,
            AppWidgetManager.ACTION_APPWIDGET_DELETED, AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_RESTORED
        )

        fun monthAction(context: Context, id: Int, delta: Int): PendingIntent {
            val intent = Intent(context, MonthCalendarWidgetProvider::class.java).apply {
                action = ACTION_MONTH
                data = Uri.parse("soh3il-widget://$id/month/$delta")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(EXTRA_DELTA, delta)
            }
            return PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
