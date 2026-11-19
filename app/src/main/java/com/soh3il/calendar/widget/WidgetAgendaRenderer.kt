package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.widget.RemoteViews
import com.soh3il.calendar.MainActivity
import com.soh3il.calendar.R
import com.soh3il.core.designsystem.utils.toPersianDigits

internal data class WidgetAgendaRow(val date: String, val text: String, val destination: WidgetDestination)

internal object WidgetAgendaRenderer {
    fun rows(data: WidgetSnapshot, options: WidgetOptions): List<WidgetAgendaRow> {
        val occurrences = if (options.showReminders) data.occurrences(data.today, 7).filter { it.epochMillis >= data.now } else emptyList()
        val rows = buildList {
            if (options.showOccasions) {
                (0..6).map { data.today.plusDays(it.toLong()).year }.distinct()
                    .filterNot(com.soh3il.core.jalali.IranianHolidays::hasOfficialLunarHolidays)
                    .forEach { year -> add(WidgetAgendaRow("اطلاعات ناقص", "تعطیلات قمری سال ${year.toString().toPersianDigits()} ثبت نشده است", WidgetDestination(data.today))) }
            }
            repeat(7) { offset ->
                val date = data.today.plusDays(offset.toLong())
                if (options.showOccasions) data.occasions(date).forEach {
                    add(WidgetAgendaRow(WidgetSurface.fullDate(date), it.titleFa, WidgetDestination(date)))
                }
                occurrences.filter { it.date == date }.forEach {
                    val title = if (options.hideTitles) "یادآور" else it.reminder.title
                    val time = WidgetDateRenderers.time(it.reminder.hour, it.reminder.minute)
                    add(WidgetAgendaRow(WidgetSurface.fullDate(date), "$time  $title", WidgetDestination(date, reminderId = it.reminder.id)))
                }
            }
        }
        // Keep the Binder payload bounded; the final row explicitly opens the full calendar.
        return if (rows.size <= 200) rows else rows.take(199) + WidgetAgendaRow(
            "یادآورهای بیشتر", "دیدن همهٔ یادآورها در برنامه", WidgetDestination(data.today)
        )
    }

    fun row(context: Context, value: WidgetAgendaRow, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        return RemoteViews(context.packageName, R.layout.widget_agenda_row).apply {
            WidgetSurface.text(this, R.id.agenda_date, value.date, colors.secondary, 11f, options)
            WidgetSurface.text(this, R.id.agenda_text, value.text, colors.text, 13f, options)
            setContentDescription(R.id.agenda_row, "${value.date}، ${value.text}")
            setOnClickFillInIntent(R.id.agenda_row, WidgetNavigation.fillIn(value.destination))
        }
    }

    fun render(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        return RemoteViews(context.packageName, R.layout.widget_calendar_agenda).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.text(this, R.id.agenda_title, "هفت روز پیش رو", colors.text, 14f, options)
            WidgetSurface.text(this, R.id.agenda_empty, "در هفت روز آینده موردی برای نمایش ندارید", colors.secondary, 13f, options)
            WidgetSurface.configure(context, this, id, colors)
            setInt(R.id.widget_add, "setColorFilter", colors.accent)
            setOnClickPendingIntent(R.id.widget_add, WidgetNavigation.pending(context, id, WidgetDestination(data.today, add = true)))
            setEmptyView(R.id.agenda_list, R.id.agenda_empty)
            // Collection fill-in intents require a mutable template. Its component stays explicit.
            val template = Intent(context, MainActivity::class.java).apply {
                action = WidgetNavigation.ACTION_OPEN
                this.data = Uri.parse("soh3il-widget://$id/agenda")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            setPendingIntentTemplate(R.id.agenda_list, PendingIntent.getActivity(context, id, template,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE))
            if (Build.VERSION.SDK_INT >= 31) {
                val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(false).setViewTypeCount(1)
                rows(data, options).forEachIndexed { index, value -> items.addItem(index.toLong(), row(context, value, options)) }
                setRemoteAdapter(R.id.agenda_list, items.build())
            } else {
                val service = Intent(context, WidgetAgendaService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    this.data = Uri.parse("soh3il-widget://$id/agenda-service")
                }
                @Suppress("DEPRECATION")
                setRemoteAdapter(R.id.agenda_list, service)
            }
        }
    }
}
