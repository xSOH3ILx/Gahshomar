package com.soh3il.calendar.widget

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun WidgetNavigationPrompt(
    destination: WidgetDestination?, hasDraft: Boolean, ready: Boolean,
    onProceed: (WidgetDestination) -> Unit, onCancel: () -> Unit
) {
    if (destination == null || !ready) return
    LaunchedEffect(destination, hasDraft) {
        if (!hasDraft) onProceed(destination)
    }
    if (hasDraft) AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("پیش‌نویس باز دارید") },
        text = { Text("برای بازکردن تاریخ یا یادآور، پیش‌نویس فعلی کنار گذاشته می‌شود.") },
        confirmButton = { TextButton(onClick = { onProceed(destination) }) { Text("کنارگذاشتن و ادامه") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("ماندن در پیش‌نویس") } }
    )
}
