package com.soh3il.core.jalali

import com.soh3il.core.model.Reminder
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarOccurrencesTest {
    private fun reminder(id: Long, day: Int, hour: Int = 8) = Reminder(
        id = id, title = "یادآور", jalaliYear = 1405, jalaliMonth = 1, jalaliDay = day,
        hour = hour, minute = 0
    )

    @Test fun nextReminderExcludesElapsedAndDisabledOccurrences() {
        val today = JalaliDate(1405, 1, 3)
        val records = listOf(reminder(1, 3), reminder(2, 3, 11).copy(isEnabled = false), reminder(3, 3, 12))
        val now = today.toEpochMillis(10)
        val upcoming = CalendarOccurrences.between(records, today, 1, now)
        assertEquals(listOf(3L), upcoming.map { it.reminder.id })
        assertTrue(CalendarOccurrences.between(records, today, 1, today.toEpochMillis(13)).isEmpty())
    }

    @Test fun agendaProjectsMonthlyAnchorInsteadOfTheOriginalStoredDay() {
        val source = reminder(10, 31).copy(recurrence = RecurrenceRule(RecurrenceType.MONTHLY_JALALI, dayOfMonth = 31))
        val occurrences = CalendarOccurrences.between(listOf(source), JalaliDate(1405, 7, 28), 7)
        assertEquals(listOf(JalaliDate(1405, 7, 30)), occurrences.map { it.date })
    }

    @Test fun weekStartsSaturdayAcrossTheSolarYearBoundary() {
        val today = JalaliDate(1405, 1, 1)
        val start = CalendarOccurrences.weekStart(today)
        assertEquals(1, start.dayOfWeek)
        assertTrue(start.toJdn() <= today.toJdn())
        assertTrue(today.toJdn() - start.toJdn() < 7)
    }

    @Test fun occurrenceAtTheCurrentInstantIsStillVisible() {
        val today = JalaliDate(1405, 1, 3)
        assertEquals(1, CalendarOccurrences.between(listOf(reminder(1, 3)), today, 1, today.toEpochMillis(8)).size)
        assertTrue(CalendarOccurrences.between(listOf(reminder(1, 3)), today, 1, today.toEpochMillis(8) + 1).isEmpty())
    }
}
