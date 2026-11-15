package com.soh3il.calendar.widget

import android.content.Context
import android.graphics.Color
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import android.util.TypedValue
import com.soh3il.calendar.R
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.CalendarOccurrences
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.designsystem.theme.FarsiHolidayRed
import androidx.compose.ui.graphics.toArgb

internal object WidgetDateRenderers {
    fun compact(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        return RemoteViews(context.packageName, R.layout.widget_calendar_compact).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.text(this, R.id.compact_day_of_week, data.today.dayOfWeekNameFa, colors.secondary, 11f, options)
            WidgetSurface.text(this, R.id.compact_day_number, data.today.day.toPersianDigits(), colors.accent, if (size.height < 110) 26f else 34f, options)
            WidgetSurface.text(this, R.id.compact_month_name, data.today.monthNameFa, colors.text, 11f, options)
            setViewVisibility(R.id.compact_day_of_week, if (size.height >= 125) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.compact_month_name, if (size.height >= 100) View.VISIBLE else View.GONE)
            setContentDescription(R.id.compact_widget_root, WidgetSurface.fullDate(data.today))
            setOnClickPendingIntent(R.id.compact_widget_root, WidgetNavigation.pending(context, id, WidgetDestination(data.today)))
        }
    }

    fun daily(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        return RemoteViews(context.packageName, R.layout.widget_calendar_m3).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.text(this, R.id.widget_day_number, data.today.day.toPersianDigits(), colors.accent, if (size.height < 130) 26f else 34f, options)
            WidgetSurface.text(this, R.id.widget_day_of_week, data.today.dayOfWeekNameFa, colors.secondary, 11f, options)
            WidgetSurface.text(this, R.id.widget_month_name, data.today.monthNameFa, colors.text, 12f, options)
            setViewVisibility(R.id.widget_day_of_week, if (size.height >= 170) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.widget_month_name, if (size.height >= 130) View.VISIBLE else View.GONE)
            WidgetSurface.text(this, R.id.widget_year_title, "${data.today.year.toPersianDigits()} خورشیدی", colors.text, 14f, options)
            val dates = buildList {
                if (options.showGregorian) add(DateConverter.getGregorianFormattedFa(data.today).toPersianDigits())
                if (options.showHijri) add(DateConverter.getHijriFormatted(data.today).toPersianDigits())
            }.joinToString(" · ")
            WidgetSurface.text(this, R.id.widget_other_dates, dates, colors.secondary, 11f, options)
            setViewVisibility(R.id.widget_other_dates, if (dates.isNotEmpty() && size.height >= 150) View.VISIBLE else View.GONE)
            val occasions = data.occasions(data.today).joinToString("، ") { it.titleFa }
            WidgetSurface.text(this, R.id.widget_occasion, occasions.ifEmpty { "امروز مناسبتی ثبت نشده" }, colors.secondary, 11f, options)
            setViewVisibility(R.id.widget_occasion, if (options.showOccasions && size.height >= 180) View.VISIBLE else View.GONE)
            val next = data.nextToday()
            val summary = next?.let {
                val title = if (options.hideTitles) "یادآور بعدی" else it.reminder.title
                "$title، ساعت ${time(it.reminder.hour, it.reminder.minute)}"
            } ?: "یادآور دیگری برای امروز ندارید"
            WidgetSurface.text(this, R.id.widget_reminders_summary, summary, colors.text, 12f, options)
            setViewVisibility(R.id.widget_reminders_summary, if (options.showReminders) View.VISIBLE else View.GONE)
            setContentDescription(R.id.widget_root, WidgetSurface.fullDate(data.today) + if (options.showReminders) "، $summary" else "")
            setOnClickPendingIntent(R.id.widget_root, WidgetNavigation.pending(context, id, WidgetDestination(data.today)))
        }
    }

    fun month(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val month = options.monthJdn?.let(JalaliDate::fromJdn) ?: data.today
        val first = JalaliDate(month.year, month.month, 1)
        val colors = WidgetSurface.palette(context, options)
        val counts = if (options.showReminders) data.occurrences(first, first.daysInMonth).groupingBy { it.date }.eachCount() else emptyMap()
        val rows = (first.dayOfWeek - 1 + first.daysInMonth + 6) / 7
        val interactive = size.width - 20 >= 7 * 48 && size.height - 76 >= rows * 48
        return RemoteViews(context.packageName, R.layout.widget_calendar_month).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.text(this, R.id.month_title, "${month.monthNameFa} ${month.year.toPersianDigits()}", colors.text, 13f, options)
            setTextViewTextSize(R.id.month_title, TypedValue.COMPLEX_UNIT_SP,
                WidgetSurface.fitText(context, "${month.monthNameFa} ${month.year.toPersianDigits()}", size.width - 164, 48f, 13f * options.textScale))
            setOnClickPendingIntent(R.id.month_widget_root, WidgetNavigation.pending(context, id, WidgetDestination(first)))
            WidgetSurface.configure(context, this, id, colors)
            setOnClickPendingIntent(R.id.month_previous, WidgetProvider.monthAction(context, id, -1))
            setOnClickPendingIntent(R.id.month_next, WidgetProvider.monthAction(context, id, 1))
            setOnClickPendingIntent(R.id.month_title, WidgetProvider.monthAction(context, id, 0))
            setInt(R.id.month_previous, "setColorFilter", colors.accent)
            setInt(R.id.month_next, "setColorFilter", colors.accent)
            for (index in 0..6) {
                setTextColor(WidgetViewIds.weekdays[index], if (index == 6) colors.holiday else colors.secondary)
            }
            WidgetViewIds.monthRows.forEachIndexed { index, view -> setViewVisibility(view, if (index < rows) View.VISIBLE else View.GONE) }
            WidgetViewIds.monthCells.forEachIndexed { index, view ->
                val day = index - first.dayOfWeek + 2
                if (day !in 1..first.daysInMonth) {
                    setTextViewText(view, "")
                    setInt(view, "setBackgroundColor", Color.TRANSPARENT)
                    setContentDescription(view, "")
                    setOnClickPendingIntent(view, null)
                } else {
                    val date = JalaliDate(month.year, month.month, day)
                    val holiday = date.dayOfWeek == 7 || data.occasions(date).any { it.isHoliday }
                    val count = counts[date] ?: 0
                    val label = day.toPersianDigits() + if (count > 0) " ·" else ""
                    setTextViewText(view, label)
                    setTextViewTextSize(view, TypedValue.COMPLEX_UNIT_SP,
                        WidgetSurface.fitText(context, label, (size.width - 20) / 7, (size.height - 76) / rows, 18f * options.textScale))
                    setInt(view, "setBackgroundColor", if (date == data.today) { if (holiday) FarsiHolidayRed.toArgb() else colors.accent } else Color.TRANSPARENT)
                    setTextColor(view, if (date == data.today) { if (holiday) Color.WHITE else colors.onAccent } else if (holiday) colors.holiday else colors.text)
                    setContentDescription(view, description(date, data.today, holiday, count))
                    setOnClickPendingIntent(view, if (interactive) WidgetNavigation.pending(context, id, WidgetDestination(date)) else null)
                }
            }
        }
    }

    fun week(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        val first = CalendarOccurrences.weekStart(data.today)
        val counts = if (options.showReminders) data.occurrences(first, 7).groupingBy { it.date }.eachCount() else emptyMap()
        val interactive = size.width - 20 >= 7 * 48 && size.height - 56 >= 48
        return RemoteViews(context.packageName, R.layout.widget_calendar_week).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.text(this, R.id.week_title, "هفتهٔ ${data.today.monthNameFa}", colors.text, 13f, options)
            WidgetSurface.configure(context, this, id, colors)
            setInt(R.id.widget_add, "setColorFilter", colors.accent)
            setOnClickPendingIntent(R.id.widget_add, WidgetNavigation.pending(context, id, WidgetDestination(data.today, add = true)))
            setOnClickPendingIntent(R.id.week_widget_root, WidgetNavigation.pending(context, id, WidgetDestination(data.today)))
            WidgetViewIds.weekCells.forEachIndexed { index, view ->
                val date = first.plusDays(index.toLong())
                val count = counts[date] ?: 0
                val holiday = date.dayOfWeek == 7 || data.occasions(date).any { it.isHoliday }
                val name = if (interactive) date.dayOfWeekNameFa else date.dayOfWeekNameFa.take(1)
                setTextViewText(view, "$name\n${date.day.toPersianDigits()}" + if (count > 0) " ·" else "")
                setTextViewTextSize(view, TypedValue.COMPLEX_UNIT_SP,
                    WidgetSurface.fitText(context, "$name\n${date.day.toPersianDigits()}" + if (count > 0) " ·" else "",
                        (size.width - 20) / 7, size.height - 56, 14f * options.textScale))
                setTextColor(view, if (date == data.today) colors.onAccent else if (holiday) colors.holiday else colors.text)
                setInt(view, "setBackgroundColor", if (date == data.today) colors.accent else Color.TRANSPARENT)
                setContentDescription(view, description(date, data.today, holiday, count))
                setOnClickPendingIntent(view, if (interactive) WidgetNavigation.pending(context, id, WidgetDestination(date)) else null)
            }
        }
    }

    fun time(hour: Int, minute: Int): String = "\u200E${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}".toPersianDigits()

    private fun description(date: JalaliDate, today: JalaliDate, holiday: Boolean, count: Int): String = buildList {
        add(WidgetSurface.fullDate(date))
        if (date == today) add("امروز")
        if (holiday) add("تعطیل")
        if (count > 0) add("دارای ${count.toPersianDigits()} یادآور")
    }.joinToString("، ")
}
