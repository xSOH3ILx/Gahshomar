package com.soh3il.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews

object WidgetRenderer {
    fun error(context: Context, id: Int): RemoteViews {
        val today = com.soh3il.core.jalali.JalaliDate.now()
        return RemoteViews(context.packageName, com.soh3il.calendar.R.layout.widget_calendar_error).apply {
            setTextViewText(com.soh3il.calendar.R.id.widget_error_date, WidgetSurface.fullDate(today))
            setTextViewText(com.soh3il.calendar.R.id.widget_error_message, "اطلاعات ویجت در دسترس نیست؛ برای بررسی برنامه را باز کنید")
            setOnClickPendingIntent(com.soh3il.calendar.R.id.widget_error_root, WidgetNavigation.pending(context, id, WidgetDestination(today)))
        }
    }

    fun render(context: Context, kind: WidgetKind, id: Int, snapshot: WidgetSnapshot, size: SizeF,
               options: WidgetOptions = WidgetOptionsStore.read(context, id)): RemoteViews = when (kind) {
        WidgetKind.COMPACT -> WidgetDateRenderers.compact(context, id, snapshot, size, options)
        WidgetKind.DAILY -> WidgetDateRenderers.daily(context, id, snapshot, size, options)
        WidgetKind.MONTH -> WidgetDateRenderers.month(context, id, snapshot, size, options)
        WidgetKind.WEEK -> WidgetDateRenderers.week(context, id, snapshot, size, options)
        WidgetKind.AGENDA -> WidgetAgendaRenderer.render(context, id, snapshot, size, options)
    }

    fun responsive(context: Context, kind: WidgetKind, id: Int, data: WidgetSnapshot): RemoteViews {
        val manager = AppWidgetManager.getInstance(context)
        val bundle = manager.getAppWidgetOptions(id)
        val options = WidgetOptionsStore.read(context, id)
        if (Build.VERSION.SDK_INT >= 31) {
            @Suppress("DEPRECATION")
            val sizes = bundle.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            if (!sizes.isNullOrEmpty()) {
                require(sizes.size <= 16 && sizes.all { it.width > 0 && it.height > 0 }) { "Invalid widget host sizes" }
                return RemoteViews(sizes.associateWith { render(context, kind, id, data, it, options) })
            }
        }
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = bundle.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = bundle.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val size = if (width > 0 && height > 0) SizeF(width.toFloat(), height.toFloat()) else kind.defaultSize
        return render(context, kind, id, data, size, options)
    }
}
