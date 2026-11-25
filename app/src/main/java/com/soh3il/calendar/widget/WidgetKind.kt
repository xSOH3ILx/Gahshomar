package com.soh3il.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.SizeF

enum class WidgetKind(val title: String, val provider: String, val defaultSize: SizeF) {
    COMPACT("روز فشرده", "CompactCalendarWidgetProvider", SizeF(90f, 110f)),
    DAILY("کارت تاریخ", "CalendarAppWidgetProvider", SizeF(250f, 140f)),
    MONTH("تقویم ماهانه", "MonthCalendarWidgetProvider", SizeF(250f, 180f)),
    WEEK("نمای هفته", "WeekCalendarWidgetProvider", SizeF(250f, 140f)),
    AGENDA("رویدادها", "AgendaCalendarWidgetProvider", SizeF(250f, 260f));

    fun component(context: Context) = ComponentName(context.packageName, "com.soh3il.calendar.widget.$provider")

    companion object {
        fun forId(context: Context, id: Int): WidgetKind? {
            val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id) ?: return null
            return entries.firstOrNull { it.component(context) == info.provider }
        }
    }
}
