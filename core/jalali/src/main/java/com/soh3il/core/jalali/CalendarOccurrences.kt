package com.soh3il.core.jalali

import com.soh3il.core.model.Reminder

data class CalendarOccurrence(val reminder: Reminder, val date: JalaliDate, val epochMillis: Long)

/** Projects records onto a bounded calendar range using the canonical recurrence engine. */
object CalendarOccurrences {
    fun weekStart(date: JalaliDate): JalaliDate = date.plusDays(1L - date.dayOfWeek)

    fun between(
        reminders: List<Reminder>,
        start: JalaliDate,
        days: Int,
        notBeforeEpochMillis: Long = Long.MIN_VALUE
    ): List<CalendarOccurrence> {
        require(days in 1..366) { "Occurrence range must contain 1..366 days" }
        return buildList {
            repeat(days) { offset ->
                val date = start.plusDays(offset.toLong())
                reminders.forEach { reminder ->
                    if (reminder.matchesDate(date)) {
                        val time = date.toEpochMillis(reminder.hour, reminder.minute)
                        if (time >= notBeforeEpochMillis) add(CalendarOccurrence(reminder, date, time))
                    }
                }
            }
        }.sortedWith(compareBy({ it.epochMillis }, { it.reminder.id }))
    }
}
