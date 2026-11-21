package com.soh3il.feature.settings.sections

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.feature.settings.components.SettingsSectionHeader

@Composable
fun ToolsSection(
    onOpenDateConverter: () -> Unit,
    onExportIcsClick: (() -> Unit)?,
    importIcsLauncher: ActivityResultLauncher<String>,
    onSyncCalendarClick: (() -> Unit)? = null,
    onWidgetsClick: (() -> Unit)? = null,
    onAutoBackupToggled: ((Boolean) -> Unit)? = null,
    onDefaultCalendarSelected: ((String) -> Unit)? = null
) {
    SettingsSectionHeader(
        title = "ابزارها و گاه‌شماری",
        subtitle = "مبدل سه‌گانه تاریخ و پشتیبان‌گیری استاندارد جهانی تقویم",
        icon = Icons.Default.DateRange
    )
    Spacer(modifier = Modifier.height(6.dp))

    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (onWidgetsClick != null) {
                OutlinedButton(onClick = onWidgetsClick, modifier = Modifier.fillMaxWidth()) {
                    Text("افزودن ویجت به صفحهٔ اصلی")
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Date Converter Tool
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مبدل پیشرفته تاریخ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تبدیل بلادرنگ تاریخ‌های خورشیدی، میلادی و قمری، تحلیل کبیسه و حیوان سال.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }

                Button(
                    onClick = onOpenDateConverter,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("باز کردن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Universal Calendar Backup & Transfer (ICS)
            Text(
                text = "پشتیبان‌گیری و انتقال تقویم (فرمت جهانی iCalendar / ICS):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "تکرار خورشیدی در فایل مخصوص SOH3IL حفظ می‌شود؛ سایر تقویم‌ها فقط نوبت اول آن را می‌بینند. بعضی قوانین ICS خارجی پشتیبانی نمی‌شوند.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onExportIcsClick?.invoke() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("صدور ICS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { importIcsLauncher.launch("text/calendar") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ورود ICS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto backup toggle
            val context = androidx.compose.ui.platform.LocalContext.current
            var autoBackup by remember { mutableStateOf(UserPreferences.isAutoBackupEnabled(context)) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "پشتیبان‌گیری خودکار روزانه",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "هر ۲۴ ساعت یک فایل ICS در پوشه اختصاصی اپ (Android/data) ذخیره می‌شود؛ ۷ نسخه آخر نگه داشته می‌شود.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                Switch(
                    checked = autoBackup,
                    onCheckedChange = { enabled ->
                        autoBackup = enabled
                        onAutoBackupToggled?.invoke(enabled)
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Default calendar for new reminders
            var defaultCalendar by remember { mutableStateOf(UserPreferences.getDefaultCalendar(context)) }
            Text(
                text = "تقویم پیش‌فرض رویدادهای جدید:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                listOf("JALALI" to "جلالی", "GREGORIAN" to "میلادی", "HIJRI" to "قمری").forEach { (name, label) ->
                    FilterChip(
                        selected = defaultCalendar == name,
                        onClick = {
                            defaultCalendar = name
                            onDefaultCalendarSelected?.invoke(name)
                        },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "همگام‌سازی با تقویم دستگاه و Google Calendar",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "یادآورهای فعال را ثبت یا به‌روزرسانی می‌کند؛ ویرایش‌های دستی همان رویدادها ممکن است جایگزین شوند. موارد حذف‌شده یا غیرفعال برای تأیید حذف نمایش داده می‌شوند.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )
            OutlinedButton(
                onClick = { onSyncCalendarClick?.invoke() },
                enabled = onSyncCalendarClick != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("همگام‌سازی اکنون", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
