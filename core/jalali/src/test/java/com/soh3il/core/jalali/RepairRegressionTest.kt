package com.soh3il.core.jalali

import com.soh3il.core.model.*
import java.io.ByteArrayInputStream
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RepairRegressionTest {
    private fun import(text: String, strict: Boolean = true) =
        IcsParser.importFromIcsStream(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)), strict = strict)
    private fun event(rule: String, start: String = "20261201T090000") =
        "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:test\r\nDTSTART:$start\r\nRRULE:$rule\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
    private fun sample(): Reminder {
        val d = DateConverter.fromGregorian(2026, 12, 15).jalali
        return Reminder(title = "test", jalaliYear = d.year, jalaliMonth = d.month, jalaliDay = d.day, hour = 9, minute = 0)
    }

    @Test fun weeklyIcsRespectsWeekStartAndBiweeklyBoundary() {
        val r = import(event("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE;WKST=MO")).single()
        assertTrue(JalaliRecurrenceEngine.matchesDate(r, DateConverter.fromGregorian(2026,12,2).jalali))
        assertFalse(JalaliRecurrenceEngine.matchesDate(r, DateConverter.fromGregorian(2026,12,7).jalali))
        assertTrue(JalaliRecurrenceEngine.matchesDate(r, DateConverter.fromGregorian(2026,12,14).jalali))
        assertEquals(r.recurrence, import(IcsParser.exportToIcsString(listOf(r))).single().recurrence)
    }
    @Test fun monthlyNeverSchedulesBeforeAnchorDate() {
        val r = sample().copy(recurrence = RecurrenceRule(RecurrenceType.MONTHLY_JALALI, dayOfMonth = 1))
        val reference = JalaliDate(r.jalaliYear, r.jalaliMonth, 1).toEpochMillis()
        val next = JalaliDate.fromEpochMillis(JalaliRecurrenceEngine.calculateNextTrigger(r, reference)!!)
        assertTrue(next >= JalaliDate(r.jalaliYear, r.jalaliMonth, r.jalaliDay))
        assertTrue(JalaliRecurrenceEngine.matchesDate(r, next))
    }
    @Test fun preReminderOffsetCanExceedRecurrenceInterval() {
        val r = sample().copy(recurrence = RecurrenceRule(RecurrenceType.DAILY))
        val now = JalaliDate(r.jalaliYear, r.jalaliMonth, r.jalaliDay).toEpochMillis(8)
        assertEquals(JalaliDate(r.jalaliYear, r.jalaliMonth, r.jalaliDay).toEpochMillis(9),
            JalaliRecurrenceEngine.calculateNextPreTrigger(r, 2L*1440, now))
    }
    @Test fun rejectUnsupportedRecurrenceAndExceptionsInStrictImport() {
        for (rule in listOf("FREQ=DAILY;COUNT=2", "FREQ=MONTHLY;BYMONTHDAY=15", "FREQ=MONTHLY;BYDAY=1MO"))
            assertThrows(IllegalArgumentException::class.java) { import(event(rule)) }
        assertThrows(IllegalArgumentException::class.java) { import(event("FREQ=DAILY").replace("RRULE:", "EXDATE:20261202T090000\r\nRRULE:")) }
        assertThrows(IllegalArgumentException::class.java) { import(event("FREQ=DAILY").replace("RRULE:", "EXRULE:FREQ=DAILY\r\nRRULE:")) }
        assertThrows(IllegalArgumentException::class.java) { import(event("FREQ=DAILY").replace("RRULE:", "STATUS:CANCELLED\r\nRRULE:")) }
    }
    @Test fun foreignTimezoneRecurrenceIsNotSilentlyAltered() {
        val input = event("FREQ=WEEKLY;BYDAY=TU").replace("DTSTART:", "DTSTART;TZID=America/New_York:")
        assertThrows(IllegalArgumentException::class.java) { import(input) }
    }
    @Test fun roundTripPreservesDisabledColorAndAstralCharacter() {
        val r = sample().copy(title = "a".repeat(66)+"😀", isEnabled = false, colorHex = "#FF0000")
        val restored = import(IcsParser.exportToIcsString(listOf(r))).single()
        assertEquals(r.title, restored.title)
        assertFalse(restored.isEnabled)
        assertEquals(r.colorHex, restored.colorHex)
    }
    @Test fun solarRecurrenceNeverPretendsToBeGregorianRrule() {
        val r = sample().copy(recurrence = RecurrenceRule(RecurrenceType.MONTHLY_JALALI))
        val ics = IcsParser.exportToIcsString(listOf(r))
        assertFalse(ics.contains("RRULE:FREQ=MONTHLY"))
        assertEquals(r.recurrence, import(ics).single().recurrence)
    }
    @Test fun strictModeReportsTooManyEvents() {
        val input = "BEGIN:VCALENDAR\r\n" + (1..3).joinToString("\r\n") { "BEGIN:VEVENT\r\nSUMMARY:x$it\r\nDTSTART:20261201T090000\r\nEND:VEVENT" } + "\r\nEND:VCALENDAR"
        assertThrows(IllegalArgumentException::class.java) {
            IcsParser.importFromIcsStream(ByteArrayInputStream(input.toByteArray()), 2, strict = true)
        }
    }
    @Test fun skipShortMonthKeepsTheDirectionOfTravel() {
        val date = JalaliDate(1405, 1, 31)
        assertEquals(JalaliDate(1404, 6, 31), date.plusMonths(-1, ShortMonthPolicy.SKIP_MONTH))
    }
    @Test fun skipShortYearDoesNotClampLeapDay() {
        val leapYear = (1390..1410).first { JalaliDate.isLeap(it) && !JalaliDate.isLeap(it + 1) }
        val date = JalaliDate(leapYear, 12, 30)
        val next = date.plusYears(1, ShortMonthPolicy.SKIP_MONTH)
        assertTrue(next.year > leapYear + 1)
        assertEquals(30, next.day)
    }

    @Test fun futureAnchorCannotProduceEarlierMonthlyOrYearlyDate() {
        val base = JalaliDate(1407, 9, 24)
        val beforeBaseYear = JalaliDate(1406, 1, 1).toEpochMillis()
        for (type in listOf(RecurrenceType.MONTHLY_JALALI, RecurrenceType.YEARLY_JALALI)) {
            val reminder = Reminder(title = "future", jalaliYear = base.year, jalaliMonth = base.month,
                jalaliDay = base.day, hour = 9, minute = 0, recurrence = RecurrenceRule(type, dayOfMonth = 1))
            val next = JalaliDate.fromEpochMillis(JalaliRecurrenceEngine.calculateNextTrigger(reminder, beforeBaseYear)!!)
            assertTrue("$type produced $next before $base", next >= base)
            assertTrue(JalaliRecurrenceEngine.matchesDate(reminder, next))
        }
    }
    @Test fun exportIncludesHistoricalTehranTransitions() {
        val transition = java.time.ZoneId.of("Asia/Tehran").rules.transitions.first {
            it.dateTimeBefore.year == 2018 && it.offsetAfter.totalSeconds == 4 * 3600 + 30 * 60
        }
        val ics = IcsParser.exportToIcsString(listOf(sample()))
        val local = transition.dateTimeBefore.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
        assertTrue(ics.contains("DTSTART:$local\r\nTZOFFSETFROM:+0330\r\nTZOFFSETTO:+0430"))
        assertTrue(ics.contains("TZOFFSETFROM:+0430\r\nTZOFFSETTO:+0330"))
    }

}
