package com.soh3il.calendar.widget

import android.content.Context
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import android.util.TypedValue
import com.soh3il.calendar.R
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.CalendarOccurrences
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.IranianHolidays
import kotlin.math.roundToInt
import androidx.core.content.res.ResourcesCompat

internal object WidgetDateRenderers {
    fun compact(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        val occasionsToday = data.occasions(data.today)
        val isHolidayToday = data.today.dayOfWeek == 7 || occasionsToday.any { it.isHoliday }
        val pillBg = if (isHolidayToday) colors.holidayContainer else colors.accentContainer
        val pillInk = if (isHolidayToday) colors.onHolidayContainer else colors.onAccentContainer
        val todayCount = if (options.showReminders) data.occurrences(data.today, 1).count { it.epochMillis >= data.now } else 0

        val innerWidthDp = (size.width - 24f).coerceAtLeast(48f)
        val daySizeDesired = if (size.height < 110f) 24f else 30f
        val daySizeMax = ((size.height - 46f).coerceAtLeast(22f) * 0.48f).coerceAtLeast(18f)
        val daySizeSp = (daySizeDesired * options.textScale).coerceAtMost(daySizeMax)

        return RemoteViews(context.packageName, R.layout.widget_calendar_compact).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.tint(this, R.id.compact_pill_bg, pillBg)
            WidgetSurface.fittedText(context, this, R.id.compact_day_of_week, data.today.dayOfWeekNameFa, pillInk, innerWidthDp, 18f, 11.5f, options, minSp = 8.5f)
            setTextViewText(R.id.compact_day_number, data.today.day.toPersianDigits())
            setTextColor(R.id.compact_day_number, pillInk)
            setTextViewTextSize(R.id.compact_day_number, TypedValue.COMPLEX_UNIT_SP, daySizeSp)
            WidgetSurface.fittedText(context, this, R.id.compact_month_name, data.today.monthNameFa, pillInk, innerWidthDp, 20f, 12.5f, options, bold = true, minSp = 9f)
            setViewVisibility(R.id.compact_day_of_week, if (size.height >= 110) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.compact_month_name, if (size.height >= 96) View.VISIBLE else View.GONE)
            if (todayCount > 0 && size.height >= 132) {
                WidgetSurface.fittedText(context, this, R.id.compact_reminder_badge, "${todayCount.toPersianDigits()} یادآور", pillInk, innerWidthDp, 16f, 10.5f, options, minSp = 8.5f)
                setViewVisibility(R.id.compact_reminder_badge, View.VISIBLE)
            } else {
                setViewVisibility(R.id.compact_reminder_badge, View.GONE)
            }
            setContentDescription(R.id.compact_widget_root, description(data.today, data.today, isHolidayToday, todayCount))
            setOnClickPendingIntent(R.id.compact_widget_root, WidgetNavigation.pending(context, id, WidgetDestination(data.today)))
        }
    }

    fun daily(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        val occasionsToday = data.occasions(data.today)
        val isHolidayToday = data.today.dayOfWeek == 7 || occasionsToday.any { it.isHoliday }
        val heroBg = if (isHolidayToday) colors.holidayContainer else colors.accentContainer
        val heroInk = if (isHolidayToday) colors.onHolidayContainer else colors.onAccentContainer
        val heroTextWidthDp = 80f
        val showQuickAdd = size.width >= 265f
        val detailsWidthDp = (size.width - 128f).coerceAtLeast(96f)
        val yearTitleWidthDp = (detailsWidthDp - if (showQuickAdd) 48f else 0f).coerceAtLeast(72f)
        val heroDaySizeSp = ((if (size.height < 120f) 26f else 34f) * options.textScale)
            .coerceAtMost(((size.height - 52f).coerceAtLeast(24f) * 0.48f).coerceAtLeast(22f))

        return RemoteViews(context.packageName, R.layout.widget_calendar_m3).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.tint(this, R.id.daily_hero_bg, heroBg)
            WidgetSurface.tint(this, R.id.daily_details_bg, colors.surfaceContainerHigh)
            setTextViewText(R.id.widget_day_number, data.today.day.toPersianDigits())
            setTextColor(R.id.widget_day_number, heroInk)
            setTextViewTextSize(R.id.widget_day_number, TypedValue.COMPLEX_UNIT_SP, heroDaySizeSp)
            WidgetSurface.fittedText(context, this, R.id.widget_day_of_week, data.today.dayOfWeekNameFa, heroInk, heroTextWidthDp, 18f, 11.5f, options, bold = true, minSp = 8.5f)
            WidgetSurface.fittedText(context, this, R.id.widget_month_name, data.today.monthNameFa, heroInk, heroTextWidthDp, 20f, 13f, options, bold = true, minSp = 9f)
            setViewVisibility(R.id.widget_day_of_week, if (size.height >= 120) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.widget_month_name, if (size.height >= 100) View.VISIBLE else View.GONE)
            WidgetSurface.fittedText(context, this, R.id.widget_year_title, "${data.today.year.toPersianDigits()} خورشیدی", colors.text, yearTitleWidthDp, 24f, 15f, options, bold = true, minSp = 11f)

            setViewVisibility(R.id.daily_add_container, if (showQuickAdd) View.VISIBLE else View.GONE)
            if (showQuickAdd) {
                WidgetSurface.tint(this, R.id.daily_add_bg, colors.surfaceContainerHighest)
                WidgetSurface.tint(this, R.id.widget_add, colors.accent)
                setOnClickPendingIntent(R.id.widget_add, WidgetNavigation.pending(context, id, WidgetDestination(data.today, add = true)))
            }

            val dates = buildList {
                if (options.showGregorian) add(DateConverter.getGregorianFormattedFa(data.today).toPersianDigits())
                if (options.showHijri) add(DateConverter.getHijriFormatted(data.today).toPersianDigits())
            }.joinToString("\u200F · \u200F")
            WidgetSurface.fittedText(context, this, R.id.widget_other_dates, dates, colors.secondary, detailsWidthDp, 18f, 11.5f, options, minSp = 8.5f)
            setViewVisibility(R.id.widget_other_dates, if (dates.isNotEmpty() && size.height >= 120) View.VISIBLE else View.GONE)

            val occasions = occasionsToday.joinToString("، ") { it.titleFa }
            val incompleteLunar = !IranianHolidays.hasOfficialLunarHolidays(data.today.year)
            val occasionText = if (incompleteLunar) {
                listOf(occasions, "تعطیلات قمری این سال کامل نیست").filter { it.isNotEmpty() }.joinToString("؛ ")
            } else {
                occasions.ifEmpty { "امروز مناسبتی ثبت نشده" }
            }
            val occasionColor = if (isHolidayToday) colors.holiday else colors.secondary
            WidgetSurface.text(this, R.id.widget_occasion, occasionText, occasionColor, 11.5f, options)
            setViewVisibility(R.id.widget_occasion, if (options.showOccasions && size.height >= 135) View.VISIBLE else View.GONE)

            val next = data.nextToday()
            val summary = next?.let {
                val title = if (options.hideTitles) "یادآور بعدی" else it.reminder.title
                "$title، ساعت ${time(it.reminder.hour, it.reminder.minute)}"
            } ?: "یادآور دیگری برای امروز ندارید"
            WidgetSurface.text(this, R.id.widget_reminders_summary, summary, colors.text, 11.5f, options)
            setViewVisibility(R.id.widget_reminders_summary, if (options.showReminders) View.VISIBLE else View.GONE)
            setContentDescription(R.id.widget_root, WidgetSurface.fullDate(data.today) + if (options.showReminders) "، $summary" else "")
            setOnClickPendingIntent(R.id.widget_root, WidgetNavigation.pending(context, id, WidgetDestination(data.today)))
        }
    }

    fun month(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val month = options.monthJdn?.let(JalaliDate::fromJdn) ?: data.today
        val isCurrentMonth = month.year == data.today.year && month.month == data.today.month
        val first = JalaliDate(month.year, month.month, 1)
        val colors = WidgetSurface.palette(context, options)
        val counts = if (options.showReminders) data.occurrences(first, first.daysInMonth).groupingBy { it.date }.eachCount() else emptyMap()
        val rows = (first.dayOfWeek - 1 + first.daysInMonth + 6) / 7
        val warning = !IranianHolidays.hasOfficialLunarHolidays(first.year)
        val gridHeight = size.height - 80 - if (warning) 16 else 0
        val interactive = size.width - 20 >= 7 * 48 && gridHeight >= rows * 48
        val titleText = if (isCurrentMonth) {
            "${month.monthNameFa} ${month.year.toPersianDigits()}"
        } else {
            "${month.monthNameFa} ${month.year.toPersianDigits()} · امروز"
        }

        return RemoteViews(context.packageName, R.layout.widget_calendar_month).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.fittedText(
                context, this, R.id.month_warning, "تعطیلات قمری این سال کامل نیست",
                colors.holiday, size.width - 20, 16f, 10f, options,
            )
            setViewVisibility(R.id.month_warning, if (warning) View.VISIBLE else View.GONE)

            val titleWidthDp = (size.width - 180f).coerceAtLeast(48f)
            val titleDesired = if (isCurrentMonth) 16f else 15f
            if (isCurrentMonth) {
                setViewVisibility(R.id.month_title_bg, View.GONE)
                WidgetSurface.fittedText(context, this, R.id.month_title, titleText, colors.text, titleWidthDp, 36f, titleDesired, options, bold = true, minSp = 7.5f)
            } else {
                setViewVisibility(R.id.month_title_bg, View.VISIBLE)
                WidgetSurface.tint(this, R.id.month_title_bg, colors.accentContainer)
                WidgetSurface.fittedText(context, this, R.id.month_title, titleText, colors.onAccentContainer, titleWidthDp - 4f, 36f, titleDesired, options, bold = true, minSp = 7.5f)
            }
            setOnClickPendingIntent(R.id.month_widget_root, WidgetNavigation.pending(context, id, WidgetDestination(first)))
            WidgetSurface.configure(context, this, id, colors, bgViewId = R.id.widget_configure_bg)
            WidgetSurface.tint(this, R.id.month_previous_bg, colors.surfaceContainerHigh)
            WidgetSurface.tint(this, R.id.month_next_bg, colors.surfaceContainerHigh)
            WidgetSurface.tint(this, R.id.month_previous, colors.accent)
            WidgetSurface.tint(this, R.id.month_next, colors.accent)
            setOnClickPendingIntent(R.id.month_previous, WidgetProvider.monthAction(context, id, -1))
            setOnClickPendingIntent(R.id.month_next, WidgetProvider.monthAction(context, id, 1))
            setOnClickPendingIntent(R.id.month_title, WidgetProvider.monthAction(context, id, 0))
            val colWidthDp = ((size.width - 20f) / 7f).coerceAtLeast(24f)
            val weekdaySp = WidgetSurface.fitText(context, "پ", colWidthDp, 20f, 12.5f * options.textScale, bold = true, minSp = 9f)
            for (index in 0..6) {
                setTextColor(WidgetViewIds.weekdays[index], if (index == 6) colors.holiday else colors.secondary)
                setTextViewTextSize(WidgetViewIds.weekdays[index], TypedValue.COMPLEX_UNIT_SP, weekdaySp)
            }
            val holidayToday = data.today.month == month.month && data.today.year == month.year && data.today.day <= first.daysInMonth
            val density = context.resources.displayMetrics.density
            val bitmap = MonthGridPainter.render(
                context = context,
                widthPx = ((size.width - 20) * density).roundToInt(),
                heightPx = (gridHeight * density).roundToInt(),
                rows = rows,
                cells = List(rows * 7) { index ->
                    val day = index - first.dayOfWeek + 2
                    val date = if (day in 1..first.daysInMonth) JalaliDate(month.year, month.month, day) else null
                    MonthCell(
                        day = date?.day ?: 0,
                        isToday = date != null && holidayToday && date == data.today,
                        isHoliday = date != null && (date.dayOfWeek == 7 || data.occasions(date).any { it.isHoliday }),
                        hasReminder = (counts[date] ?: 0) > 0,
                    )
                },
                colors = colors,
                textScale = options.textScale,
            )
            setImageViewBitmap(R.id.month_grid_canvas, bitmap)
            for (row in 0 until rows) setViewVisibility(WidgetViewIds.monthRows[row], if (interactive) View.VISIBLE else View.GONE)
            if (interactive) {
                for (row in 0 until rows) for (column in 0..6) {
                    val day = row * 7 + column - first.dayOfWeek + 2
                    val overlay = WidgetViewIds.monthCells[row * 7 + column]
                    if (day in 1..first.daysInMonth) {
                        val date = JalaliDate(month.year, month.month, day)
                        val holiday = date.dayOfWeek == 7 || data.occasions(date).any { it.isHoliday }
                        setContentDescription(overlay, description(date, data.today, holiday, counts[date] ?: 0))
                        setOnClickPendingIntent(overlay, WidgetNavigation.pending(context, id, WidgetDestination(date)))
                    } else {
                        setContentDescription(overlay, "")
                        setOnClickPendingIntent(overlay, null)
                    }
                }
            }
        }
    }

    fun week(context: Context, id: Int, data: WidgetSnapshot, size: SizeF, options: WidgetOptions): RemoteViews {
        val colors = WidgetSurface.palette(context, options)
        val first = CalendarOccurrences.weekStart(data.today)
        val last = first.plusDays(6)
        val counts = if (options.showReminders) data.occurrences(first, 7).groupingBy { it.date }.eachCount() else emptyMap()
        val warning = (0..6).any { !IranianHolidays.hasOfficialLunarHolidays(first.plusDays(it.toLong()).year) }
        val weekdayRowHeight = 22
        val gridHeight = size.height - 60 - weekdayRowHeight - if (warning) 16 else 0
        val interactive = size.width - 20 >= 7 * 48 && gridHeight >= 48
        val weekLabel = if (first.month == last.month) {
            "هفتهٔ ${first.day.toPersianDigits()} تا ${last.day.toPersianDigits()} ${last.monthNameFa}"
        } else {
            "هفتهٔ ${first.monthNameFa} - ${last.monthNameFa}"
        }

        return RemoteViews(context.packageName, R.layout.widget_calendar_week).apply {
            WidgetSurface.apply(context, this, size, options)
            WidgetSurface.fittedText(
                context, this, R.id.week_warning, "تعطیلات قمری این هفته کامل نیست",
                colors.holiday, size.width - 20, 16f, 10f, options,
            )
            setViewVisibility(R.id.week_warning, if (warning) View.VISIBLE else View.GONE)
            WidgetSurface.fittedText(
                context, this, R.id.week_title, weekLabel, colors.text,
                (size.width - 124f).coerceAtLeast(72f), 40f, 15f, options, bold = true, minSp = 10.5f,
            )
            WidgetSurface.configure(context, this, id, colors, bgViewId = R.id.widget_configure_bg)
            WidgetSurface.tint(this, R.id.week_add_bg, colors.surfaceContainerHigh)
            WidgetSurface.tint(this, R.id.widget_add, colors.accent)
            setOnClickPendingIntent(R.id.widget_add, WidgetNavigation.pending(context, id, WidgetDestination(data.today, add = true)))
            setOnClickPendingIntent(R.id.week_widget_root, WidgetNavigation.pending(context, id, WidgetDestination(data.today)))
            val colWidthDp = ((size.width - 20f) / 7f).coerceAtLeast(24f)
            val fullWeekdaySp = WidgetSurface.fitText(context, "چهارشنبه", colWidthDp, 22f, 12f * options.textScale, bold = true, minSp = 4f)
            val useFullWeekdays = fullWeekdaySp >= 9f
            val weekdaySp = if (useFullWeekdays) {
                fullWeekdaySp
            } else {
                WidgetSurface.fitText(context, "پ", colWidthDp, 22f, 12f * options.textScale, bold = true, minSp = 8.5f)
            }
            for (index in 0..6) {
                val date = first.plusDays(index.toLong())
                val label = if (useFullWeekdays) date.dayOfWeekNameFa else date.dayOfWeekNameFa.first().toString()
                setTextViewText(WidgetViewIds.weekWeekdays[index], label)
                setTextColor(WidgetViewIds.weekWeekdays[index], if (index == 6) colors.holiday else colors.secondary)
                setTextViewTextSize(WidgetViewIds.weekWeekdays[index], TypedValue.COMPLEX_UNIT_SP, weekdaySp)
            }
            val density = context.resources.displayMetrics.density
            val bitmap = MonthGridPainter.render(
                context = context,
                widthPx = ((size.width - 20) * density).roundToInt(),
                heightPx = (gridHeight * density).roundToInt(),
                rows = 1,
                cells = (0..6).map { offset ->
                    val date = first.plusDays(offset.toLong())
                    MonthCell(
                        day = date.day,
                        isToday = date == data.today,
                        isHoliday = date.dayOfWeek == 7 || data.occasions(date).any { it.isHoliday },
                        hasReminder = (counts[date] ?: 0) > 0,
                    )
                },
                colors = colors,
                textScale = options.textScale,
            )
            setImageViewBitmap(WidgetViewIds.weekGridCanvas, bitmap)
            setViewVisibility(R.id.week_cells, if (interactive) View.VISIBLE else View.GONE)
            if (interactive) {
                WidgetViewIds.weekCells.forEachIndexed { index, overlay ->
                    val date = first.plusDays(index.toLong())
                    val holiday = date.dayOfWeek == 7 || data.occasions(date).any { it.isHoliday }
                    setContentDescription(overlay, description(date, data.today, holiday, counts[date] ?: 0))
                    setOnClickPendingIntent(overlay, WidgetNavigation.pending(context, id, WidgetDestination(date)))
                }
            } else {
                WidgetViewIds.weekCells.forEach { overlay ->
                    setContentDescription(overlay, "")
                    setOnClickPendingIntent(overlay, null)
                }
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
