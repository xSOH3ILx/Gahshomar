package com.soh3il.feature.calendar

import com.soh3il.core.designsystem.utils.toEnglishDigits
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.visibleOn
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.Reminder

internal fun buildAgendaItems(today: JalaliDate, reminders: List<Reminder>, additionalOccasions: List<Occasion>,
    normalizedQuery: String, selectedCategoryColor: String?): List<AgendaDayItem> {
    val list = mutableListOf<AgendaDayItem>()
    val tomorrow = today.plusDays(1)

    val searching = normalizedQuery.isNotEmpty() || selectedCategoryColor != null
    val startOffset = if (searching) -365 else 0
    val endOffset = if (searching) 730 else 45
    for (i in startOffset..endOffset) {
        val date = today.plusDays(i.toLong())
        val dateStr = date.format("YYYY-MM-DD")
        val monthOccasions = IranianHolidays.getOccasionsForMonth(date.year, date.month)
        var dayOccasions = (monthOccasions + additionalOccasions).filter { it.jalaliDate == dateStr }
            .distinctBy { it.id }

        var dayReminders = reminders.filter { rem -> rem.visibleOn(date) }

        // Category filter
        if (selectedCategoryColor != null) {
            dayReminders = dayReminders.filter { it.colorHex.equals(selectedCategoryColor, ignoreCase = true) }
        }

        // Search query filter
        if (normalizedQuery.isNotEmpty()) {
            dayReminders = dayReminders.filter { rem ->
                rem.title.toEnglishDigits().lowercase().contains(normalizedQuery) ||
                    rem.description.toEnglishDigits().lowercase().contains(normalizedQuery)
            }
            dayOccasions = dayOccasions.filter { occ ->
                occ.titleFa.toEnglishDigits().lowercase().contains(normalizedQuery)
            }
        }

        // Only show days that have reminders, occasions, or (if no search) are today/tomorrow
        if (dayReminders.isNotEmpty() || dayOccasions.isNotEmpty() || (!searching && i in 0..2)) {
            list.add(
                AgendaDayItem(
                    date = date,
                    occasions = dayOccasions,
                    reminders = dayReminders,
                    isToday = date == today,
                    isTomorrow = date == tomorrow
                )
            )
        }
    }
    return list
}
