package com.soh3il.feature.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.Reminder
import kotlin.math.abs

/** Age since birth date and countdown to the next upcoming reminder. */
data class AgeCountdownState(
    val ageYears: Int? = null,
    val ageMonths: Int? = null,
    val ageDays: Int? = null,
    val daysToBirthday: Int? = null,
    val nextReminderTitle: String? = null,
    val nextReminderDays: Long? = null,
    val nextReminderDate: JalaliDate? = null,
)

fun computeAgeCountdown(
    today: JalaliDate,
    birthJdn: Long?,
    reminders: List<Reminder>,
): AgeCountdownState {
    val birth = birthJdn?.let(JalaliDate::fromJdn)
    var state = AgeCountdownState()
    if (birth != null && today >= birth) {
        var years = today.year - birth.year
        var months = today.month - birth.month
        var days = today.day - birth.day
        if (days < 0) {
            months -= 1
            val prevYear = if (today.month == 1) today.year - 1 else today.year
            val prevMonth = if (today.month == 1) 12 else today.month - 1
            days += JalaliDate.daysInMonth(prevYear, prevMonth)
        }
        if (months < 0) {
            years -= 1
            months += 12
        }
        if (years < 0) years = 0

        val maxDaysThisYear = JalaliDate.daysInMonth(today.year, birth.month)
        var nextBirthday = JalaliDate(today.year, birth.month, minOf(birth.day, maxDaysThisYear))
        if (nextBirthday < today) {
            val nextYear = today.year + 1
            val maxDaysNextYear = JalaliDate.daysInMonth(nextYear, birth.month)
            nextBirthday = JalaliDate(nextYear, birth.month, minOf(birth.day, maxDaysNextYear))
        }
        state = state.copy(
            ageYears = years,
            ageMonths = months,
            ageDays = days,
            daysToBirthday = abs(nextBirthday.toJdn() - today.toJdn()).toInt(),
        )
    }

    val upcoming = reminders
        .filter { it.isEnabled }
        .mapNotNull { reminder ->
            val nextEpoch = JalaliRecurrenceEngine.calculateNextTrigger(
                reminder = reminder,
                referenceEpochMillis = today.toEpochMillis(0, 0, 0) - 1
            ) ?: return@mapNotNull null
            val nextDate = JalaliDate.fromEpochMillis(nextEpoch)
            nextDate to reminder
        }
        .minByOrNull { (date, _) -> date.toJdn() }

    if (upcoming != null) {
        val (date, reminder) = upcoming
        state = state.copy(
            nextReminderTitle = reminder.title,
            nextReminderDays = abs(date.toJdn() - today.toJdn()),
            nextReminderDate = date,
        )
    }
    return state
}

@Composable
fun AgeCountdownCard(
    state: AgeCountdownState,
    onSetBirth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.ageYears != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSetBirth)
                ) {
                    Column {
                        Text(
                            text = "سن",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val yearsText = state.ageYears.toPersianDigits()
                        val monthsText = (state.ageMonths ?: 0).toPersianDigits()
                        val daysText = (state.ageDays ?: 0).toPersianDigits()
                        Text(
                            text = "$yearsText سال و $monthsText ماه و $daysText روز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if ((state.daysToBirthday ?: 0) > 0) {
                        Text(
                            text = "${state.daysToBirthday?.toPersianDigits()} روز تا تولد بعدی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            } else {
                TextButton(onClick = onSetBirth) {
                    Text("ثبت تاریخ تولد")
                }
                Text(
                    text = "تاریخ تولد خود را ثبت کنید تا سن و روزشمار تولد بعدی در این بخش نمایش داده شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
