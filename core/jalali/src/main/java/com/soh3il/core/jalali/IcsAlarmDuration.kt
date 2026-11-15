package com.soh3il.core.jalali

import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import java.time.Duration
import java.time.format.DateTimeParseException
import java.util.Locale

internal object IcsAlarmDuration {
    fun parse(trigger: String): PreReminder {
        require(trigger.startsWith("-P", ignoreCase = true)) {
            "فقط هشدار نسبی پیش از رویداد پشتیبانی می‌شود"
        }
        val value = trigger.substring(1).uppercase(Locale.ROOT)
        val duration = try {
            if (Regex("P[0-9]+W").matches(value)) {
                Duration.ofDays(Math.multiplyExact(value.substring(1, value.length - 1).toLong(), 7L))
            } else Duration.parse(value)
        } catch (error: DateTimeParseException) {
            throw IllegalArgumentException("مدت هشدار تقویم معتبر نیست", error)
        } catch (error: ArithmeticException) {
            throw IllegalArgumentException("مدت هشدار تقویم بیش از حد مجاز است", error)
        }
        require(!duration.isNegative && duration.nano == 0 && duration.seconds % 60 == 0L) {
            "مدت هشدار باید بر حسب دقیقهٔ کامل باشد"
        }
        val minutes = duration.toMinutes()
        require(minutes <= Int.MAX_VALUE) { "مدت هشدار تقویم بیش از حد مجاز است" }
        return when {
            minutes > 0 && minutes % 1440 == 0L -> PreReminder((minutes / 1440).toInt(), PreReminderUnit.DAYS)
            minutes > 0 && minutes % 60 == 0L -> PreReminder((minutes / 60).toInt(), PreReminderUnit.HOURS)
            else -> PreReminder(minutes.toInt(), PreReminderUnit.MINUTES)
        }
    }
}
