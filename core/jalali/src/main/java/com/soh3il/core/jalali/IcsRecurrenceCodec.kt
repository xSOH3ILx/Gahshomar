package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ShortMonthPolicy
import java.util.Locale

internal object IcsRecurrenceCodec {
    private val ICS_DAY_TO_PERSIAN = mapOf(
        "SA" to 1, "SU" to 2, "MO" to 3, "TU" to 4, "WE" to 5, "TH" to 6, "FR" to 7
    )
    private val PERSIAN_TO_ICS_DAY = ICS_DAY_TO_PERSIAN.entries.associate { (ics, day) -> day to ics }

    fun standardRrule(rem: Reminder): String? {
        val interval = rem.recurrence.interval.coerceAtLeast(1)
        return when (rem.recurrence.type) {
            RecurrenceType.DAILY -> "RRULE:FREQ=DAILY;INTERVAL=$interval"
            RecurrenceType.WEEKLY -> {
                val days = rem.recurrence.daysOfWeek.sorted().mapNotNull { PERSIAN_TO_ICS_DAY[it] }
                val byDay = if (days.isEmpty()) "" else ";BYDAY=${days.joinToString(",")}"
                val weekStart = rem.recurrence.weekStart ?: JalaliDate(rem.jalaliYear, rem.jalaliMonth, rem.jalaliDay).dayOfWeek
                "RRULE:FREQ=WEEKLY;INTERVAL=$interval$byDay;WKST=${PERSIAN_TO_ICS_DAY.getValue(weekStart)}"
            }
            // A Gregorian RRULE would silently move solar occurrences in other apps.
            RecurrenceType.MONTHLY_JALALI, RecurrenceType.YEARLY_JALALI -> null
            RecurrenceType.MONTHLY_GREGORIAN -> "RRULE:FREQ=MONTHLY;INTERVAL=$interval"
            RecurrenceType.YEARLY_GREGORIAN -> "RRULE:FREQ=YEARLY;INTERVAL=$interval"
            RecurrenceType.NONE -> null
        }
    }

    fun xRecurrence(rem: Reminder): String {
        val days = rem.recurrence.daysOfWeek.sorted().joinToString(",")
        val day = rem.recurrence.dayOfMonth?.toString().orEmpty()
        return "${rem.recurrence.type.name};INTERVAL=${rem.recurrence.interval.coerceAtLeast(1)};DAY=$day;WEEKDAYS=$days;POLICY=${rem.recurrence.shortMonthPolicy.name};WEEKSTART=${rem.recurrence.weekStart ?: ""}"
    }

    fun parseXRecurrence(value: String, strict: Boolean = false): RecurrenceRule {
        val parts = value.split(";")
        val parsedType = runCatching { RecurrenceType.valueOf(parts.first()) }.getOrNull()
        if (strict) require(parsedType != null) { "نوع تکرار SOH3IL ناشناخته است" }
        val type = parsedType ?: RecurrenceType.NONE
        fun field(name: String): String? = parts.firstOrNull { it.startsWith("$name=") }?.substringAfter("=")
        val intervalValue = field("INTERVAL")
        if (strict && intervalValue != null) require((intervalValue.toIntOrNull() ?: 0) in 1..999) { "بازهٔ تکرار معتبر نیست" }
        val interval = intervalValue?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val dayOfMonth = field("DAY")?.toIntOrNull()
        require(dayOfMonth == null || dayOfMonth in 1..31) { "روز تکرار تقویم معتبر نیست" }
        val daysOfWeek = field("WEEKDAYS")
            ?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
        val policy = field("POLICY")?.let {
            runCatching { ShortMonthPolicy.valueOf(it) }.getOrNull()
        } ?: ShortMonthPolicy.SNAP_TO_LAST_DAY
        val weekStart = field("WEEKSTART")?.takeIf { it.isNotBlank() }?.toIntOrNull()
        require(weekStart == null || weekStart in 1..7) { "شروع هفته معتبر نیست" }
        require(daysOfWeek.all { it in 1..7 }) { "روز هفته معتبر نیست" }
        return RecurrenceRule(type, interval, dayOfMonth, daysOfWeek, policy, weekStart)
    }

    fun parseStandardRrule(rrule: String, strict: Boolean = false): RecurrenceRule {
        val normalized = rrule.uppercase(Locale.ROOT)
        if (strict && normalized.isNotBlank()) {
            val parts = normalized.split(";").map { it.substringBefore("=") to it.substringAfter("=", "") }
            require(parts.all { it.first in setOf("FREQ", "INTERVAL", "BYDAY", "WKST") && it.second.isNotBlank() } &&
                parts.map { it.first }.distinct().size == parts.size) { "این قانون تکرار ICS به‌طور کامل پشتیبانی نمی‌شود" }
            val freq = parts.firstOrNull { it.first == "FREQ" }?.second
            require(freq in setOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) { "نوع تکرار ICS پشتیبانی نمی‌شود" }
            val byDay = parts.firstOrNull { it.first == "BYDAY" }?.second
            require(byDay == null || (freq == "WEEKLY" && byDay.split(",").all { it in ICS_DAY_TO_PERSIAN })) { "روزهای تکرار ICS پشتیبانی نمی‌شوند" }
            val interval = parts.firstOrNull { it.first == "INTERVAL" }?.second
            require(interval == null || (interval.toIntOrNull() ?: 0) in 1..999) { "بازهٔ تکرار ICS معتبر نیست" }
        }
        var recType = RecurrenceType.NONE
        var recInterval = 1
        if (normalized.contains("FREQ=DAILY", ignoreCase = true)) {
            recType = RecurrenceType.DAILY
        } else if (normalized.contains("FREQ=WEEKLY", ignoreCase = true)) {
            recType = RecurrenceType.WEEKLY
        } else if (normalized.contains("FREQ=MONTHLY", ignoreCase = true)) {
            recType = RecurrenceType.MONTHLY_GREGORIAN
        } else if (normalized.contains("FREQ=YEARLY", ignoreCase = true)) {
            recType = RecurrenceType.YEARLY_GREGORIAN
        }

        if (normalized.contains("INTERVAL=", ignoreCase = true)) {
            val intervalStr = normalized.substringAfter("INTERVAL=").substringBefore(";")
            recInterval = intervalStr.toIntOrNull() ?: 1
        }
        val daysOfWeek = if (normalized.contains("BYDAY=", ignoreCase = true)) {
            normalized.substringAfter("BYDAY=")
                .substringBefore(";")
                .split(",")
                .mapNotNull { ICS_DAY_TO_PERSIAN[it.trim().uppercase()] }
                .toSet()
        } else {
            emptySet()
        }
        return RecurrenceRule(
            type = recType,
            interval = recInterval.coerceAtLeast(1),
            daysOfWeek = daysOfWeek,
            shortMonthPolicy = ShortMonthPolicy.SKIP_MONTH,
            weekStart = if (recType == RecurrenceType.WEEKLY) {
                val key = normalized.split(";").firstOrNull { it.startsWith("WKST=") }?.substringAfter("=") ?: "MO"
                requireNotNull(ICS_DAY_TO_PERSIAN[key]) { "آغاز هفته معتبر نیست" }
            } else null
        )
    }

}
