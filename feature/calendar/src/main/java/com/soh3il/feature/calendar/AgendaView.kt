package com.soh3il.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiBadge
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.utils.toEnglishDigits
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.feature.calendar.components.CalendarSearchBar
import com.soh3il.feature.calendar.components.EventCountdownsCard
import com.soh3il.feature.calendar.components.computeEventCountdowns

data class AgendaDayItem(
    val date: JalaliDate,
    val occasions: List<Occasion>,
    val reminders: List<Reminder>,
    val isToday: Boolean,
    val isTomorrow: Boolean
)

@Composable
fun AgendaView(
    today: JalaliDate,
    reminders: List<Reminder>,
    additionalOccasions: List<Occasion> = emptyList(),
    onReminderClick: (Reminder) -> Unit,
    onToggleReminderCountdown: ((Reminder) -> Unit)? = null,
    birthJdn: Long? = null,
    onSetBirthDate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryColor by remember { mutableStateOf<String?>(null) }

    val normalizedQuery = remember(searchQuery) {
        searchQuery.trim().toEnglishDigits().lowercase()
    }

    // Generate agenda items for the next 45 days with real-time query and category filtering
    val agendaItems = remember(today, reminders, additionalOccasions, normalizedQuery, selectedCategoryColor) {
        buildAgendaItems(today, reminders, additionalOccasions, normalizedQuery, selectedCategoryColor)
    }

    val selectableReminders = remember(reminders) {
        reminders.filter { it.isEnabled }
    }

    val countdownItems = remember(today, reminders, normalizedQuery, selectedCategoryColor) {
        computeEventCountdowns(today, reminders).filter { item ->
            val matchesCategory = selectedCategoryColor == null ||
                item.reminder.colorHex.equals(selectedCategoryColor, ignoreCase = true)
            val matchesQuery = normalizedQuery.isEmpty() ||
                item.reminder.title.toEnglishDigits().lowercase().contains(normalizedQuery) ||
                item.reminder.description.toEnglishDigits().lowercase().contains(normalizedQuery)
            matchesCategory && matchesQuery
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        CalendarSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedCategoryColor = selectedCategoryColor,
            onCategorySelected = { selectedCategoryColor = it }
        )
        val missingLunarYear = (0..45).map { today.plusDays(it.toLong()).year }.distinct()
            .firstOrNull { !IranianHolidays.hasOfficialLunarHolidays(it) }
        if (missingLunarYear != null && normalizedQuery.isEmpty()) {
            Text(
                text = "تعطیلات قمری سال ${missingLunarYear.toPersianDigits()} ثبت نشده است.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (agendaItems.isEmpty() && countdownItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (normalizedQuery.isNotEmpty() || selectedCategoryColor != null)
                            "هیچ نتیجه‌ای مطابق با فیلتر یا جستجو یافت نشد"
                        else
                            "هیچ رویداد یا یادآوری برای روزهای آینده ثبت نشده است",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 16.dp
                )
            ) {
                item(key = "event_countdowns_card") {
                    EventCountdownsCard(
                        today = today,
                        countdownItems = countdownItems,
                        selectableReminders = selectableReminders,
                        onReminderClick = onReminderClick,
                        onToggleReminderCountdown = onToggleReminderCountdown
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (onSetBirthDate != null && normalizedQuery.isEmpty() && selectedCategoryColor == null) {
                    item(key = "age_countdown_card") {
                        AgeCountdownCard(
                            state = remember(today, birthJdn, reminders) {
                                computeAgeCountdown(today, birthJdn, reminders)
                            },
                            onSetBirth = onSetBirthDate,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                items(agendaItems) { item ->
                    AgendaDaySection(
                        item = item,
                        onReminderClick = onReminderClick,
                        onToggleReminderCountdown = onToggleReminderCountdown
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AgendaDaySection(
    item: AgendaDayItem,
    onReminderClick: (Reminder) -> Unit,
    onToggleReminderCountdown: ((Reminder) -> Unit)? = null
) {
    val date = item.date
    val isFriday = date.dayOfWeek == 7
    val hasHoliday = item.occasions.any { it.isHoliday }
    val isDayOff = isFriday || hasHoliday

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Date Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tagText = when {
                    item.isToday -> "امروز"
                    item.isTomorrow -> "فردا"
                    else -> null
                }

                if (tagText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (item.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                }

                Text(
                    text = "${date.dayOfWeekNameFa}، ${date.day.toPersianDigits()} ${date.monthNameFa}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDayOff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Occasions under date
        item.occasions.forEach { occ ->
            FarsiBadge(
                text = occ.titleFa,
                isHoliday = occ.isHoliday,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // Reminders under date
        if (item.reminders.isEmpty()) {
            Text(
                text = "بدون یادآور",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            item.reminders.forEach { rem ->
                FarsiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { onReminderClick(rem) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val reminderColor = try {
                            Color(android.graphics.Color.parseColor(rem.colorHex))
                        } catch (_: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(reminderColor)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = rem.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (rem.isEnabled) 1f else 0.55f),
                                textDecoration = if (rem.isEnabled) null else TextDecoration.LineThrough
                            )
                            val timeStr = "\u200E${rem.hour.toPersianDigits().padStart(2, '۰')}:${rem.minute.toPersianDigits().padStart(2, '۰')}"
                            val recurrenceDetail = when (rem.recurrence.type) {
                                RecurrenceType.NONE -> null
                                RecurrenceType.DAILY -> if (rem.recurrence.interval <= 1) "روزانه" else "هر ${rem.recurrence.interval.toPersianDigits()} روز یکبار"
                                RecurrenceType.WEEKLY -> if (rem.recurrence.interval <= 1) "هفتگی" else "هر ${rem.recurrence.interval.toPersianDigits()} هفته یکبار"
                                RecurrenceType.MONTHLY_JALALI -> if (rem.recurrence.interval <= 1) "ماهانه" else "هر ${rem.recurrence.interval.toPersianDigits()} ماه یکبار"
                                RecurrenceType.YEARLY_JALALI -> if (rem.recurrence.interval <= 1) "سالانه" else "هر ${rem.recurrence.interval.toPersianDigits()} سال یکبار"
                                RecurrenceType.MONTHLY_GREGORIAN -> "هر ${rem.recurrence.interval.toPersianDigits()} ماه میلادی"
                                RecurrenceType.YEARLY_GREGORIAN -> "هر ${rem.recurrence.interval.toPersianDigits()} سال میلادی"
                            }
                            val subtitle = listOfNotNull(
                                "ساعت $timeStr",
                                recurrenceDetail,
                                if (rem.isEnabled) null else "انجام‌شده"
                            ).joinToString(" • ")
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (rem.isEnabled && onToggleReminderCountdown != null) {
                            IconButton(
                                onClick = { onToggleReminderCountdown(rem) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = if (rem.showCountdown) "حذف از شمارش معکوس" else "افزودن به شمارش معکوس",
                                    tint = if (rem.showCountdown) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش یادآور",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
