package com.soh3il.feature.calendar

import com.soh3il.core.jalali.JalaliDate
import org.junit.Assert.*
import org.junit.Test

class AgendaItemsTest {
    @Test fun everyOccasionBelongsToTheDateOfItsAgendaRow() {
        val items = buildAgendaItems(JalaliDate(1405, 1, 1), emptyList(), emptyList(), "", null)
        assertTrue(items.isNotEmpty())
        items.forEach { row -> row.occasions.forEach { occasion ->
            assertEquals(row.date.format("YYYY-MM-DD"), occasion.jalaliDate)
        } }
    }
    @Test fun searchingForAnOccasionDoesNotRepeatItOnOtherDaysOfItsMonth() {
        val items = buildAgendaItems(JalaliDate(1405, 1, 1), emptyList(), emptyList(), "نوروز", null)
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { row -> row.occasions.all { it.jalaliDate == row.date.format("YYYY-MM-DD") } })
    }
}
