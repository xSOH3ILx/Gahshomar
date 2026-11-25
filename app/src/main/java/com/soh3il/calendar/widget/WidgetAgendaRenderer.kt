package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.soh3il.calendar.MainActivity
import com.soh3il.calendar.R
import com.soh3il.core.designsystem.utils.toPersianDigits

internal data class WidgetAgendaRow(
    val date: String,
    val text: String,
    val destination: WidgetDestination,
    val isHoliday: Boolean = false,
    val countdownLabel: String? = null,
)

internal object WidgetAgendaRenderer {
    fun rows(data: WidgetSnapshot, options: WidgetOptions): List<WidgetAgendaRow> {
        val occurrences = if (options.showReminders) data.occurrences(data.today, 7).filter { it.epochMillis >= data.now } else emptyList()
        val todayJdn = data.today.toJdn()
        val rows = buildList {
            if (options.showOccasions) {
                (0..6).map { data.today.plusDays(it.toLong()).year }.distinct()
                    .filterNot(com.soh3il.core.jalali.IranianHolidays::hasOfficialLunarHolidays)
                    .forEach { year ->
                        add(
                            WidgetAgendaRow(
                                date = "اطلاعات ناقص",
                                text = "تعطیلات قمری سال ${year.toString().toPersianDigits()} ثبت نشده است",
                                destination = WidgetDestination(data.today),
                                isHoliday = true,
                            ),
                        )
                    }
            }
            repeat(7) { offset ->
                val date = data.today.plusDays(offset.toLong())
                val dateLabel = when (offset) {
                    0 -> "امروز · ${WidgetSurface.fullDate(date)}"
                    1 -> "فردا · ${WidgetSurface.fullDate(date)}"
                    else -> WidgetSurface.fullDate(date)
                }
                if (options.showOccasions) {
                    data.occasions(date).forEach { occasion ->
                        add(
                            WidgetAgendaRow(
                                date = dateLabel,
                                text = occasion.titleFa,
                                destination = WidgetDestination(date),
                                isHoliday = occasion.isHoliday,
                            ),
                        )
                    }
                }
                occurrences.filter { it.date == date }.forEach { occ ->
                    val title = if (options.hideTitles) "یادآور" else occ.reminder.title
                    val time = WidgetDateRenderers.time(occ.reminder.hour, occ.reminder.minute)
                    val countdownLabel = if (occ.reminder.showCountdown) {
                        val diffDays = (date.toJdn() - todayJdn).toInt()
                        if (diffDays <= 0) "امروز" else "${diffDays.toPersianDigits()} روز"
                    } else {
                        null
                    }
                    add(
                        WidgetAgendaRow(
                            date = dateLabel,
                            text = "$time  $title",
                            destination = WidgetDestination(date, reminderId = occ.reminder.id),
                            isHoliday = false,
                            countdownLabel = countdownLabel,
                        ),
                    )
                }
            }
        }
        // Keep the Binder payload bounded; the final row explicitly opens the full calendar.
        return if (rows.size <= 200) rows else rows.take(199) + WidgetAgendaRow(
            "یادآورهای بیشتر", "دیدن همهٔ رویدادها در برنامه", WidgetDestination(data.today),
        )
    }

    fun row(context: Context, value: WidgetAgendaRow, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        return RemoteViews(context.packageName, R.layout.widget_agenda_row).apply {
            WidgetSurface.tint(this, R.id.agenda_row_bg, colors.surfaceContainerHigh)
            WidgetSurface.tint(this, R.id.agenda_indicator, if (value.isHoliday) colors.holiday else colors.accent)
            WidgetSurface.text(this, R.id.agenda_date, value.date, if (value.isHoliday) colors.holiday else colors.secondary, 11.5f, options)
            WidgetSurface.text(this, R.id.agenda_text, value.text, colors.text, 13f, options)
            if (value.countdownLabel != null) {
                setViewVisibility(R.id.agenda_countdown_container, View.VISIBLE)
                WidgetSurface.tint(this, R.id.agenda_countdown_bg, colors.accentContainer)
                WidgetSurface.text(this, R.id.agenda_countdown_text, value.countdownLabel, colors.onAccentContainer, 10.5f, options)
            } else {
                setViewVisibility(R.id.agenda_countdown_container, View.GONE)
            }
            val desc = buildString {
                append("${value.date}، ${value.text}")
                if (value.countdownLabel != null) append("، ${value.countdownLabel}")
            }
            setContentDescription(R.id.agenda_row, desc)
            setOnClickFillInIntent(R.id.agenda_row, WidgetNavigation.fillIn(value.destination))
        }
    }

    fun render(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        return RemoteViews(context.packageName, R.layout.widget_calendar_agenda).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.fittedText(
                context, this, R.id.agenda_title, "رویدادهای ۷ روز پیش رو",
                colors.text, (size.width - 126f).coerceAtLeast(80f), 40f, 15f, options, bold = true, minSp = 8f,
            )
            WidgetSurface.text(this, R.id.agenda_empty, "در هفت روز آینده رویدادی برای نمایش ندارید", colors.secondary, 13f, options)
            WidgetSurface.configure(context, this, id, colors, bgViewId = R.id.widget_configure_bg)
            WidgetSurface.tint(this, R.id.agenda_add_bg, colors.surfaceContainerHigh)
            WidgetSurface.tint(this, R.id.widget_add, colors.accent)
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
