package com.soh3il.calendar.widget

import com.soh3il.core.jalali.CalendarOccurrence
import com.soh3il.core.jalali.CalendarOccurrences
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.Reminder

class WidgetSnapshot(val now: Long, val reminders: List<Reminder>) {
    val today: JalaliDate = JalaliDate.fromEpochMillis(now)
    private val occasionCache = mutableMapOf<Pair<Int, Int>, List<Occasion>>()
    private val occurrenceCache = mutableMapOf<Pair<Long, Int>, List<CalendarOccurrence>>()

    fun occasions(date: JalaliDate): List<Occasion> = occasionCache.getOrPut(date.year to date.month) {
        IranianHolidays.getOccasionsForMonth(date.year, date.month)
    }.filter { it.jalaliDate == date.format("YYYY-MM-DD") }

    fun occurrences(start: JalaliDate, days: Int): List<CalendarOccurrence> =
        occurrenceCache.getOrPut(start.toJdn() to days) { CalendarOccurrences.between(reminders, start, days) }

    fun nextToday(): CalendarOccurrence? = occurrences(today, 1).firstOrNull { it.epochMillis >= now }

    companion object {
        fun preview(): WidgetSnapshot {
            val today = JalaliDate.now()
            return WidgetSnapshot(today.toEpochMillis(8), listOf(Reminder(
                id = 1, title = "یادآور نمونه", jalaliYear = today.year, jalaliMonth = today.month,
                jalaliDay = today.day, hour = 12, minute = 30
            )))
        }
    }
}
