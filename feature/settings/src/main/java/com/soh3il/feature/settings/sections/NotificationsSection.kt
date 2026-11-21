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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.scheduler.NotificationHelper
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.feature.settings.components.SettingsSectionHeader

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
            // Persistent Status Bar Notification Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Switch) {
                        onPersistentNotificationToggled(!isPersistentNotificationEnabled)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نوار اعلان دائمی تاریخ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نمایش عدد بزرگ روز خورشیدی در نوار وضعیت، صفحه قفل و Always On Display (AOD) با مناسبت‌ها و آیکون زنده.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }

                Switch(
                    checked = isPersistentNotificationEnabled,
                    onCheckedChange = null
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Switch) {
                        onLockscreenCalendarToggled(!isLockscreenCalendarEnabled)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نمایش جزئیات تقویم روی صفحه‌قفل",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نمایش تاریخ و رویدادهای غیرحساس در صفحه‌قفل؛ در حالت خاموش، آیکون روز نیز ثابت می‌شود.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
                Switch(checked = isLockscreenCalendarEnabled, onCheckedChange = null)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Switch) {
                        onGregorianDateToggled(!isGregorianDateEnabled)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نمایش تاریخ میلادی",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تاریخ میلادی در اعلان همیشگی و صفحه‌قفل نمایش داده شود",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isGregorianDateEnabled, onCheckedChange = null)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Switch) {
                        onHijriDateToggled(!isHijriDateEnabled)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نمایش تاریخ هجری قمری",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تاریخ قمری محاسباتی در اعلان همیشگی و صفحه‌قفل نمایش داده شود",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isHijriDateEnabled, onCheckedChange = null)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Status Bar Icon Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Switch) {
                        onDynamicNotifIconToggled(!isDynamicNotifIconEnabled)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نمایش رقم زنده روز در نوار وضعیت (Status Bar)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نمایش رقم فارسی روز (مانند ۱۸) در بالای صفحه گوشی بجای آیکون پیش‌فرض برنامه.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isDynamicNotifIconEnabled,
                    onCheckedChange = null
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notification Sound Setting Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صدای زنگ اعلان یادآورها:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedSoundTitle,
                        fontSize = 14.sp,
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
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "انتخاب صدای اعلان یادآور SOH3IL")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تغییر صدا", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Snooze duration picker
            var snoozeMinutes by remember { mutableStateOf(UserPreferences.getSnoozeMinutes(context)) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "زمان پیش‌فرض تعویق (Snooze)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مدت تعویقی که از دکمه «تعویق» داخل اعلان یادآور استفاده می‌شود.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserPreferences.SNOOZE_OPTIONS_MINUTES.forEach { option ->
                        FilterChip(
                            selected = snoozeMinutes == option,
                            onClick = {
                                snoozeMinutes = option
                                UserPreferences.setSnoozeMinutes(context, option)
                            },
                            label = { Text(option.toPersianDigits()) }
                        )
                    }
                }
                Text(text = "دقیقه", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Default reminder picker
            var defaultReminder by remember { mutableStateOf(UserPreferences.getDefaultReminderMinutes(context)) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "یادآور پیش‌فرض رویدادهای جدید",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "بدون یادآور، یا هشدار چند دقیقه/ساعت قبل از رویداد به‌صورت خودکار.",
                    fontSize = 11.sp,
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
                            label = {
                                Text(
                                    when (option) {
                                        -1 -> "بدون"
                                        0 -> "زمان رویداد"
                                        else -> if (option < 60) "${option.toPersianDigits()} دقیقه قبل" else "${(option / 60).toPersianDigits()} ساعت قبل"
                                    }
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ساعات سکوت",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "در این بازه صدای هشدار یادآورها پخش نمی‌شود و اعلان بی‌صدا می‌آید.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = quietEnabled, onCheckedChange = { enabled ->
                        quietEnabled = enabled
                        UserPreferences.setQuietHoursEnabled(context, enabled)
                        NotificationHelper.refreshChannelsForQuietHours(context)
                    })
                }

                if (quietEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    QuietTimeRow(
                        label = "شروع سکوت",
                        minuteOfDay = quietStart,
                        onSelected = { quietStart = it; UserPreferences.setQuietStartMinute(context, it) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { showPicker = true }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = String.format(java.util.Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60).toPersianDigits(),
            fontSize = 13.sp,
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
            title = { Text(label) },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
