package com.soh3il.calendar

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.soh3il.calendar.widget.WidgetDestination
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ResolveReminderEntry(id: Long?, reminders: List<Reminder>?, onResolved: (WidgetDestination?) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(id, reminders) {
        if (id == null) return@LaunchedEffect
        val reminder = Soh3ilDatabase.getInstance(context).reminderDao().getReminderById(id)?.toDomain()
        withContext(Dispatchers.Main.immediate) {
            if (reminder == null) {
                Toast.makeText(context, "این یادآور دیگر در برنامه نیست", Toast.LENGTH_SHORT).show()
                onResolved(null)
            } else if (reminders != null && reminders.any { it.id == id }) {
                // The observed list can lag a committed insert; keep the entry until the editor has its target.
                onResolved(WidgetDestination(JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay), reminderId = id))
            }
        }
    }
}
