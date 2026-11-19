package com.soh3il.calendar.integrations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.designsystem.utils.toPersianDigits
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** User reviews the overwrite warning and then any cleanup candidates separately. */
@Composable
internal fun rememberCalendarSyncAction(context: Context, db: Soh3ilDatabase,
                                        requestPermission: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var confirmSync by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<CalendarCleanupPreview?>(null) }

    if (confirmSync) {
        AlertDialog(
            onDismissRequest = { confirmSync = false },
            title = { Text("تأیید به‌روزرسانی تقویم") },
            text = { Text("این عملیات عنوان، توضیح، تاریخ و تکرار رویدادهای دارای نشانگر SOH3IL را با اطلاعات برنامه جایگزین می‌کند. ویرایش‌های دستی همین موارد در تقویم دستگاه ممکن است از بین بروند. حذف رویدادهای قدیمی بعداً جداگانه نمایش داده می‌شود و تأیید مستقل می‌خواهد.") },
            dismissButton = { TextButton(onClick = { confirmSync = false }) { Text("انصراف") } },
            confirmButton = { TextButton(onClick = {
                confirmSync = false
                if (!busy) {
                    busy = true
                    scope.launch {
                        try {
                            val (count, plan) = withContext(Dispatchers.IO) {
                                val current = db.reminderDao().getAllRemindersSync().map { it.toDomain() }
                                val count = CalendarProviderSync.syncReminders(context, current).getOrThrow()
                                val active = db.reminderDao().getAllActiveRemindersSync().map { it.id }.toSet()
                                count to CalendarCleanup.preview(context, active)
                            }
                            Toast.makeText(context, "${count.toPersianDigits()} یادآور ثبت یا به‌روزرسانی شد", Toast.LENGTH_LONG).show()
                            if (plan.candidates.isNotEmpty()) preview = plan
                        } catch (error: CancellationException) { throw error }
                        catch (error: Exception) {
                            val message = if (error is IOException || error is IllegalArgumentException || error is SecurityException)
                                error.message ?: "همگام‌سازی یا بررسی حذف کامل نشد؛ تقویم دستگاه را بررسی کن."
                            else "همگام‌سازی یا بررسی حذف کامل نشد؛ تقویم دستگاه را بررسی کن."
                            Toast.makeText(context, "$message ممکن است بخشی از تغییرات ثبت شده باشد؛ تقویم را بررسی کن.", Toast.LENGTH_LONG).show()
                        } finally { busy = false }
                    }
                }
            }) { Text("ادامهٔ همگام‌سازی") } }
        )
    }
    preview?.let { plan ->
        CalendarCleanupDialog(plan, busy,
            onCancel = { if (!busy) preview = null },
            onConfirm = {
                if (!busy) {
                    busy = true
                    scope.launch {
                        try {
                            val (removed, skipped) = withContext(Dispatchers.IO) {
                                CalendarCleanup.deleteApproved(context, plan) {
                                    db.reminderDao().getAllActiveRemindersSync().map { it.id }.toSet()
                                }
                            }
                            preview = null
                            Toast.makeText(context, "${removed.toPersianDigits()} رویداد حذف شد؛ ${skipped.toPersianDigits()} مورد تغییرکرده یا فعال نادیده گرفته شد", Toast.LENGTH_LONG).show()
                        } catch (error: CancellationException) { throw error }
                        catch (error: Exception) {
                            preview = null
                            Toast.makeText(context, "حذف کامل نشد؛ ممکن است بعضی رویدادها حذف شده باشند. تقویم دستگاه را بررسی کن.", Toast.LENGTH_LONG).show()
                        } finally { busy = false }
                    }
                }
            })
    }

    return {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (!allowed) requestPermission() else if (!busy && preview == null) confirmSync = true
    }
}
