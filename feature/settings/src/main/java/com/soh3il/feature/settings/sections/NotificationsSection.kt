package com.soh3il.feature.settings.sections

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.components.FarsiSwitchRow
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.scheduler.NotificationHelper
import com.soh3il.feature.settings.components.SettingsSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSection(
    context: Context,
    isPersistentNotificationEnabled: Boolean,
    onPersistentNotificationToggled: (Boolean) -> Unit,
    isLockscreenCalendarEnabled: Boolean,
    onLockscreenCalendarToggled: (Boolean) -> Unit,
    isGregorianDateEnabled: Boolean,
    onGregorianDateToggled: (Boolean) -> Unit,
    isHijriDateEnabled: Boolean,
    onHijriDateToggled: (Boolean) -> Unit,
    isDynamicNotifIconEnabled: Boolean,
    onDynamicNotifIconToggled: (Boolean) -> Unit,
    selectedSoundTitle: String,
    ringtonePickerLauncher: ActivityResultLauncher<Intent>
) {
    var isPlayingPreview by remember { mutableStateOf(false) }
    var currentRingtone by remember { mutableStateOf<Ringtone?>(null) }

    SettingsSectionHeader(
        title = "اعلان‌ها و نوار وضعیت",
        subtitle = "نوار تاریخ دائمی، صفحه قفل، AOD و صدای زنگ هشدار",
        icon = Icons.Default.NotificationsActive
    )
    Spacer(modifier = Modifier.height(6.dp))

    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            FarsiSwitchRow(
                title = "نوار اعلان دائمی تاریخ",
                subtitle = "نمایش عدد بزرگ روز خورشیدی در نوار وضعیت، صفحه قفل و Always On Display (AOD) با مناسبت‌ها و آیکون زنده.",
                checked = isPersistentNotificationEnabled,
                onCheckedChange = onPersistentNotificationToggled
            )

            Spacer(modifier = Modifier.height(10.dp))

            FarsiSwitchRow(
                title = "نمایش جزئیات تقویم روی صفحه‌قفل",
                subtitle = "نمایش تاریخ و رویدادهای غیرحساس در صفحه‌قفل؛ در حالت خاموش، آیکون روز نیز ثابت می‌شود.",
                checked = isLockscreenCalendarEnabled,
                onCheckedChange = onLockscreenCalendarToggled
            )

            Spacer(modifier = Modifier.height(10.dp))

            FarsiSwitchRow(
                title = "نمایش تاریخ میلادی",
                subtitle = "تاریخ میلادی در اعلان همیشگی و صفحه‌قفل نمایش داده شود",
                checked = isGregorianDateEnabled,
                onCheckedChange = onGregorianDateToggled
            )

            Spacer(modifier = Modifier.height(10.dp))

            FarsiSwitchRow(
                title = "نمایش تاریخ هجری قمری",
                subtitle = "تاریخ قمری محاسباتی در اعلان همیشگی و صفحه‌قفل نمایش داده شود",
                checked = isHijriDateEnabled,
                onCheckedChange = onHijriDateToggled
            )

            Spacer(modifier = Modifier.height(10.dp))

            FarsiSwitchRow(
                title = "نمایش رقم زنده روز در نوار وضعیت (Status Bar)",
                subtitle = "نمایش رقم فارسی روز (مانند ۱۸) در بالای صفحه گوشی بجای آیکون پیش‌فرض برنامه.",
                checked = isDynamicNotifIconEnabled,
                onCheckedChange = onDynamicNotifIconToggled
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notification Sound Setting Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صدای زنگ اعلان یادآورها:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedSoundTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            try {
                                if (isPlayingPreview) {
                                    currentRingtone?.stop()
                                    isPlayingPreview = false
                                } else {
                                    val uri = NotificationHelper.getEffectiveSoundUri(context)
                                    val ringtone = uri?.let { RingtoneManager.getRingtone(context, it) }
                                    currentRingtone = ringtone
                                    ringtone?.play()
                                    isPlayingPreview = ringtone?.isPlaying == true
                                }
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "پخش نمونه صدا",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val existingUri = NotificationHelper.getEffectiveSoundUri(context)
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "انتخاب صدای اعلان یادآور گاه‌شمار خورشیدی")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("تغییر صدا", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Snooze duration picker
            var snoozeMinutes by remember { mutableStateOf(UserPreferences.getSnoozeMinutes(context)) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "زمان پیش‌فرض تعویق (دقیقه)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مدت تعویقی که از دکمه «تعویق» داخل اعلان یادآور استفاده می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UserPreferences.SNOOZE_OPTIONS_MINUTES.forEachIndexed { index, option ->
                        val selected = snoozeMinutes == option
                        SegmentedButton(
                            selected = selected,
                            onClick = {
                                snoozeMinutes = option
                                UserPreferences.setSnoozeMinutes(context, option)
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = UserPreferences.SNOOZE_OPTIONS_MINUTES.size
                            ),
                            icon = {}
                        ) {
                            Text(
                                text = option.toPersianDigits(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Default reminder picker
            var defaultReminder by remember { mutableStateOf(UserPreferences.getDefaultReminderMinutes(context)) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "یادآور پیش‌فرض رویدادهای جدید",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "بدون یادآور، یا هشدار چند دقیقه/ساعت قبل از رویداد به‌صورت خودکار.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    UserPreferences.DEFAULT_REMINDER_OPTIONS_MINUTES.forEach { option ->
                        FilterChip(
                            selected = defaultReminder == option,
                            onClick = {
                                defaultReminder = option
                                UserPreferences.setDefaultReminderMinutes(context, option)
                            },
                            shape = MaterialTheme.shapes.small,
                            label = {
                                Text(
                                    text = when (option) {
                                        -1 -> "بدون"
                                        0 -> "زمان رویداد"
                                        else -> if (option < 60) "${option.toPersianDigits()} دقیقه قبل" else "${(option / 60).toPersianDigits()} ساعت قبل"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quiet hours
            var quietEnabled by remember { mutableStateOf(UserPreferences.isQuietHoursEnabled(context)) }
            var quietStart by remember { mutableStateOf(UserPreferences.getQuietStartMinute(context)) }
            var quietEnd by remember { mutableStateOf(UserPreferences.getQuietEndMinute(context)) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(bottom = if (quietEnabled) 10.dp else 0.dp)
            ) {
                FarsiSwitchRow(
                    title = "ساعات سکوت",
                    subtitle = "در این بازه صدای هشدار یادآورها پخش نمی‌شود و اعلان بی‌صدا می‌آید.",
                    checked = quietEnabled,
                    onCheckedChange = { enabled ->
                        quietEnabled = enabled
                        UserPreferences.setQuietHoursEnabled(context, enabled)
                        NotificationHelper.refreshChannelsForQuietHours(context)
                    }
                )

                if (quietEnabled) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuietTimeRow(
                            label = "شروع سکوت",
                            minuteOfDay = quietStart,
                            onSelected = { quietStart = it; UserPreferences.setQuietStartMinute(context, it) }
                        )
                        QuietTimeRow(
                            label = "پایان سکوت",
                            minuteOfDay = quietEnd,
                            onSelected = { quietEnd = it; UserPreferences.setQuietEndMinute(context, it) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuietTimeRow(
    label: String,
    minuteOfDay: Int,
    onSelected: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button) { showPicker = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = String.format(java.util.Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60).toPersianDigits(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
    if (showPicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            is24Hour = true
        )
        var useKeyboardInput by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onSelected(timePickerState.hour * 60 + timePickerState.minute)
                    showPicker = false
                }) { Text("تأیید") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("انصراف") }
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { useKeyboardInput = !useKeyboardInput }) {
                        Icon(
                            imageVector = if (useKeyboardInput) Icons.Default.AccessTime else Icons.Default.Keyboard,
                            contentDescription = if (useKeyboardInput) "نمایش ساعت" else "ورود عددی"
                        )
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    if (useKeyboardInput) {
                        TimeInput(state = timePickerState)
                    } else {
                        TimePicker(state = timePickerState)
                    }
                }
            }
        )
    }
}
