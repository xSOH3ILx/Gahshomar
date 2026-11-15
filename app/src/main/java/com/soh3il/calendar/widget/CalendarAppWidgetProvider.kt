package com.soh3il.calendar.widget

import android.content.Context

class CalendarAppWidgetProvider : WidgetProvider() {
    companion object {
        fun requestUpdate(context: Context) = WidgetUpdates.request(context)
    }
}
