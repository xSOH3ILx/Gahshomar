package com.soh3il.feature.calendar

import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.feature.calendar.components.computeEventCountdowns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AgeCountdownTest {

    @Test
    fun computeAgeCountdown_whenNoBirthDate_returnsNullAge() {
        val today = JalaliDate(1404, 5, 10)
        val state = computeAgeCountdown(today, null, emptyList())
        assertNull(state.ageYears)
        assertNull(state.ageMonths)
        assertNull(state.ageDays)
        assertNull(state.daysToBirthday)
    }

    @Test
    fun computeAgeCountdown_exactBirthdayToday() {
        val birth = JalaliDate(1375, 5, 10)
        val today = JalaliDate(1404, 5, 10)
        val state = computeAgeCountdown(today, birth.toJdn(), emptyList())
        assertEquals(29, state.ageYears)
        assertEquals(0, state.ageMonths)
        assertEquals(0, state.ageDays)
        assertEquals(0, state.daysToBirthday)
    }

    @Test
    fun computeAgeCountdown_borrowingDaysAndMonthsCorrectly() {
        val birth = JalaliDate(1375, 7, 20)
        val today = JalaliDate(1404, 1, 5)
        val state = computeAgeCountdown(today, birth.toJdn(), emptyList())
        assertNotNull(state.ageYears)
        assertEquals(28, state.ageYears)
        assertEquals(5, state.ageMonths)
        assertEquals(15, state.ageDays)
    }

    @Test
    fun computeAgeCountdown_nextUpcomingReminderIdentified() {
        val today = JalaliDate(1404, 1, 10)
        val reminder1 = Reminder(
            id = 1,
            title = "جلسه کاری",
            jalaliYear = 1404,
            jalaliMonth = 1,
            jalaliDay = 15,
            hour = 10,
            minute = 0,
            isEnabled = true
        )
        val reminder2 = Reminder(
            id = 2,
            title = "سفر",
            jalaliYear = 1404,
            jalaliMonth = 2,
            jalaliDay = 1,
            hour = 8,
            minute = 0,
            isEnabled = true
        )
        val state = computeAgeCountdown(today, null, listOf(reminder2, reminder1))
        assertEquals("جلسه کاری", state.nextReminderTitle)
        assertEquals(5L, state.nextReminderDays)
        assertEquals(JalaliDate(1404, 1, 15), state.nextReminderDate)
    }

    @Test
    fun computeAgeCountdown_recurringReminderIdentified() {
        val today = JalaliDate(1404, 1, 10)
        val recurringReminder = Reminder(
            id = 3,
            title = "تولد دوست",
            jalaliYear = 1400,
            jalaliMonth = 1,
            jalaliDay = 12,
            hour = 12,
            minute = 0,
            recurrence = RecurrenceRule(type = RecurrenceType.YEARLY_JALALI),
            isEnabled = true
        )
        val state = computeAgeCountdown(today, null, listOf(recurringReminder))
        assertEquals("تولد دوست", state.nextReminderTitle)
        assertEquals(2L, state.nextReminderDays)
        assertEquals(JalaliDate(1404, 1, 12), state.nextReminderDate)
    }

    @Test
    fun computeEventCountdowns_onlyIncludesEnabledSelectedRemindersSortedByDate() {
        val today = JalaliDate(1404, 1, 10)
        val unselected = Reminder(
            id = 1,
            title = "عدم انتخاب",
            jalaliYear = 1404,
            jalaliMonth = 1,
            jalaliDay = 11,
            hour = 9,
            minute = 0,
            isEnabled = true,
            showCountdown = false
        )
        val disabledSelected = Reminder(
            id = 2,
            title = "غیرفعال",
            jalaliYear = 1404,
            jalaliMonth = 1,
            jalaliDay = 12,
            hour = 9,
            minute = 0,
            isEnabled = false,
            showCountdown = true
        )
        val farEvent = Reminder(
            id = 3,
            title = "سفر تابستان",
            jalaliYear = 1404,
            jalaliMonth = 4,
            jalaliDay = 1,
            hour = 8,
            minute = 0,
            isEnabled = true,
            showCountdown = true
        )
        val nearRecurring = Reminder(
            id = 4,
            title = "سالگرد",
            jalaliYear = 1400,
            jalaliMonth = 1,
            jalaliDay = 14,
            hour = 10,
            minute = 0,
            recurrence = RecurrenceRule(type = RecurrenceType.YEARLY_JALALI),
            isEnabled = true,
            showCountdown = true
        )

        val result = computeEventCountdowns(today, listOf(farEvent, unselected, disabledSelected, nearRecurring))
        assertEquals(listOf("سالگرد", "سفر تابستان"), result.map { it.reminder.title })
        assertEquals(4L, result[0].daysRemaining)
        assertEquals(JalaliDate(1404, 1, 14), result[0].targetDate)
        assertEquals(JalaliDate(1404, 4, 1), result[1].targetDate)
    }
}
