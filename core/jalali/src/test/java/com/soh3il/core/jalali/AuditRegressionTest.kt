package com.soh3il.core.jalali

import com.soh3il.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.util.Locale

class AuditRegressionTest {
    private fun reminder() = Reminder(id = 1, title = "fixture", jalaliYear = 1406,
        jalaliMonth = 1, jalaliDay = 1, hour = 9, minute = 0)

    private fun event(rule: String = "", trigger: String = ""): Reminder {
        val alarm = if (trigger.isBlank()) "" else "BEGIN:VALARM\r\nACTION:DISPLAY\r\nTRIGGER:$trigger\r\nEND:VALARM\r\n"
        val text = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:fixture\r\nDTSTART:20270321T090000\r\nRRULE:$rule\r\n${alarm}END:VEVENT\r\nEND:VCALENDAR\r\n"
        return IcsParser.importFromIcsStream(ByteArrayInputStream(text.toByteArray())).single()
    }

    @Test fun weeklyStartMoreThanTenWeeksAwayMustStillBeScheduled() {
        val value = reminder().copy(recurrence = RecurrenceRule(RecurrenceType.WEEKLY))
        val next = JalaliRecurrenceEngine.calculateNextTrigger(value, JalaliDate(1405, 8, 24).toEpochMillis(12))
        assertEquals(JalaliDate(1406, 1, 1).toEpochMillis(9), next)
    }

    @Test fun deviceCalendarKeepsTheAnchorWeekBoundaryForMultiWeekIntervals() {
        val value = reminder().copy(recurrence = RecurrenceRule(RecurrenceType.WEEKLY, 3, daysOfWeek = setOf(3, 5)))
        assertEquals("FREQ=WEEKLY;INTERVAL=3;BYDAY=MO,WE;WKST=SU", value.toDeviceCalendarRrule())
    }

    @Test fun literalBackslashNMustSurviveIcsRoundTrip() {
        val value = reminder().copy(description = "folder\\notes")
        val result = IcsParser.importFromIcsStream(ByteArrayInputStream(IcsParser.exportToIcsString(listOf(value)).toByteArray())).single()
        assertEquals(value.description, result.description)
    }

    @Test fun lowerCaseRruleMustPreserveItsIntervalAndWeekdays() {
        val value = event("freq=weekly;interval=3;byday=mo,we")
        assertEquals(3, value.recurrence.interval)
        assertEquals(setOf(3, 5), value.recurrence.daysOfWeek)
    }

    @Test fun compoundDurationMustPreserveNinetyMinutes() {
        assertEquals(90L, event(trigger = "-PT1H30M").preReminders.single().toMinutes())
    }

    @Test fun invalidDurationMustNotBecomeAnInventedOffset() {
        assertThrows(IllegalArgumentException::class.java) { event(trigger = "-PTgarbageM") }
    }

    @Test fun afterEventDurationMustNotBeImportedAsAPreReminder() {
        assertThrows(IllegalArgumentException::class.java) { event(trigger = "PT10M") }
    }

    @Test fun exportMustUseAsciiDateDigitsUnderPersianLocale() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("fa-IR"))
            val line = IcsParser.exportToIcsString(listOf(reminder())).lineSequence()
                .single { it.startsWith("DTSTART;TZID=") }
            assertTrue("Non-ASCII DTSTART: $line", Regex("DTSTART;TZID=Asia/Tehran:[0-9]{8}T[0-9]{6}").matches(line))
        } finally { Locale.setDefault(old) }
    }

    @Test fun positiveControlCalendarRoundTripsAcrossSupportedModernYears() {
        for (year in 1390..1450) for (month in 1..12) {
            val date = JalaliDate(year, month, JalaliDate.daysInMonth(year, month))
            assertEquals(date, JalaliDate.fromJdn(date.toJdn()))
        }
    }
}
