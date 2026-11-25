package com.soh3il.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.JalaliRecurrenceEngine
import com.soh3il.core.model.Reminder

data class EventCountdownItem(
    val reminder: Reminder,
    val targetDate: JalaliDate,
    val daysRemaining: Long,
)

fun computeEventCountdowns(
    today: JalaliDate,
    reminders: List<Reminder>,
): List<EventCountdownItem> {
    val startOfToday = today.toEpochMillis(0, 0, 0) - 1L
    return reminders
        .asSequence()
        .filter { it.isEnabled && it.showCountdown }
        .mapNotNull { reminder ->
            val nextEpoch = JalaliRecurrenceEngine.calculateNextTrigger(
                reminder = reminder,
                referenceEpochMillis = startOfToday
            ) ?: return@mapNotNull null
            val targetDate = JalaliDate.fromEpochMillis(nextEpoch)
            val days = (targetDate.toJdn() - today.toJdn()).coerceAtLeast(0L)
            EventCountdownItem(
                reminder = reminder,
                targetDate = targetDate,
                daysRemaining = days
            )
        }
        .sortedWith(
            compareBy<EventCountdownItem>(
                { it.daysRemaining },
                { it.reminder.hour },
                { it.reminder.minute },
                { it.reminder.title }
            )
        )
        .toList()
}

@Composable
fun EventCountdownsCard(
    today: JalaliDate,
    countdownItems: List<EventCountdownItem>,
    selectableReminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onToggleReminderCountdown: ((Reminder) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showSelectorDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شمارش معکوس رویدادها",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (onToggleReminderCountdown != null && selectableReminders.isNotEmpty()) {
                    TextButton(
                        onClick = { showSelectorDialog = true },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "انتخاب رویدادها",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (countdownItems.isEmpty()) {
                Text(
                    text = if (selectableReminders.isEmpty()) {
                        "هنوز رویداد فعالی برای شمارش معکوس ثبت نشده است."
                    } else {
                        "برای نمایش شمارش معکوس، از دکمه «انتخاب رویدادها» یا گزینه شمارش معکوس داخل هر یادآور استفاده کنید."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    countdownItems.forEach { item ->
                        EventCountdownRow(
                            item = item,
                            onClick = { onReminderClick(item.reminder) }
                        )
                    }
                }
            }
        }
    }

    if (showSelectorDialog && onToggleReminderCountdown != null) {
        CountdownSelectorDialog(
            today = today,
            reminders = selectableReminders,
            onToggleReminder = onToggleReminderCountdown,
            onDismiss = { showSelectorDialog = false }
        )
    }
}

@Composable
private fun EventCountdownRow(
    item: EventCountdownItem,
    onClick: () -> Unit,
) {
    val indicatorColor = try {
        Color(android.graphics.Color.parseColor(item.reminder.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val dateLabel = "${item.targetDate.dayOfWeekNameFa}، ${item.targetDate.day.toPersianDigits()} ${item.targetDate.monthNameFa} ${item.targetDate.year.toPersianDigits()}"
    val remainingText = when (item.daysRemaining) {
        0L -> "امروز"
        1L -> "فردا"
        else -> "${item.daysRemaining.toPersianDigits()} روز مانده"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
        ) {
            Text(
                text = item.reminder.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (item.daysRemaining <= 1L) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = remainingText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.daysRemaining <= 1L) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        }
    }
}

@Composable
private fun CountdownSelectorDialog(
    today: JalaliDate,
    reminders: List<Reminder>,
    onToggleReminder: (Reminder) -> Unit,
    onDismiss: () -> Unit,
) {
    val startOfToday = remember(today) { today.toEpochMillis(0, 0, 0) - 1L }
    val sorted = remember(reminders, startOfToday) {
        reminders.sortedBy { reminder ->
            JalaliRecurrenceEngine.calculateNextTrigger(reminder, startOfToday) ?: Long.MAX_VALUE
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "انتخاب رویدادهای شمارش معکوس",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sorted, key = { it.id }) { reminder ->
                    val nextEpoch = JalaliRecurrenceEngine.calculateNextTrigger(reminder, startOfToday)
                    val date = nextEpoch?.let(JalaliDate::fromEpochMillis)
                        ?: JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)
                    val dateStr = "${date.day.toPersianDigits()} ${date.monthNameFa} ${date.year.toPersianDigits()}"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .semantics(mergeDescendants = true) {}
                            .clickable(role = Role.Checkbox) { onToggleReminder(reminder) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = reminder.showCountdown,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = reminder.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateStr,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("تأیید")
            }
        }
    )
}
