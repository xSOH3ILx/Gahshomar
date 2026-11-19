package com.soh3il.calendar.integrations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate

/** Nothing is deleted until the user has reviewed and explicitly approved this list. */
@Composable
internal fun CalendarCleanupDialog(preview: CalendarCleanupPreview, busy: Boolean,
                                   onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text("بررسی حذف رویدادهای تقویم") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                Text("تقویم: ${preview.calendarName} (شناسهٔ ${preview.calendarId.toString().toPersianDigits()})")
                Text("یادآورهای متناظر این رویدادها غیرفعال یا حذف شده‌اند. نشانگر متنی به‌تنهایی مالکیت را ثابت نمی‌کند؛ فقط در صورت اطمینان از حذف این رویدادها و جزئیاتشان تأیید کن. اگر مشخصات اصلی بررسی‌شده تغییر کند یا یادآور دوباره فعال شود، مورد رد می‌شود. تغییرات جزئیات وابسته همیشه قابل تشخیص نیستند.", Modifier.padding(vertical = 8.dp))
                preview.candidates.forEach { candidate ->
                    val date = JalaliDate.fromEpochMillis(candidate.startEpoch).format().toPersianDigits()
                    val time = java.time.Instant.ofEpochMilli(candidate.startEpoch).atZone(JalaliDate.TEHRAN_ZONE)
                    val clock = "%02d:%02d".format(java.util.Locale.ROOT, time.hour, time.minute).toPersianDigits()
                    val title = candidate.title.ifBlank { "بدون عنوان" }.take(200)
                    Text("• $title — $date، $clock — شناسهٔ ${candidate.eventId.toString().toPersianDigits()}", Modifier.padding(vertical = 3.dp))
                }
                Text("حداکثر ۲۰ مورد در هر نوبت نمایش داده می‌شود؛ برای موارد بیشتر دوباره همگام‌سازی را اجرا کن.", Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !busy) { Text("حذف موارد نمایش‌داده‌شده") } },
        dismissButton = { TextButton(onClick = onCancel, enabled = !busy) { Text("انصراف") } }
    )
}
