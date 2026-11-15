package com.soh3il.calendar.widget

import android.content.Context
import android.widget.RemoteViews
import com.soh3il.core.jalali.JalaliDate

class MonthCalendarWidgetProvider : WidgetProvider() {
    companion object {
        internal fun createRemoteViews(context: Context, appWidgetId: Int, today: JalaliDate = JalaliDate.now()): RemoteViews =
            WidgetRenderer.render(context, WidgetKind.MONTH, appWidgetId,
                WidgetSnapshot(today.toEpochMillis(8), emptyList()), WidgetKind.MONTH.defaultSize)
    }
}
