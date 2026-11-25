package com.soh3il.feature.reminder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.utils.PersianDigitVisualTransformation
import com.soh3il.core.designsystem.utils.toEnglishDigits
import com.soh3il.core.designsystem.utils.toIntStandardOrNull
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit

@Composable
fun CustomPreReminderDialog(
    initialPreReminder: PreReminder?,
    onDismiss: () -> Unit,
    onConfirm: (PreReminder) -> Unit
) {
    var amountText by remember { mutableStateOf(initialPreReminder?.value?.toString() ?: "2") }
    var selectedUnit by remember { mutableStateOf(initialPreReminder?.unit ?: PreReminderUnit.DAYS) }
    var showError by remember { mutableStateOf(false) }
    val parsedAmount = amountText.toIntStandardOrNull()
    val isInvalid = parsedAmount == null || parsedAmount <= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (isInvalid) {
                        showError = true
                    } else {
                        onConfirm(PreReminder(parsedAmount!!, selectedUnit))
                    }
                }
            ) {
                Text("تایید و انتخاب", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
        title = {
            Text(
                text = "تنظیم پیش‌یادآور دلخواه",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "چه مدت قبل از زمان یادآور اصلی به شما هشدار داده شود؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val standard = input.toEnglishDigits()
                        if (standard.all { it.isDigit() } && standard.length <= 4) {
                            amountText = standard
                            showError = false
                        }
                    },
                    visualTransformation = PersianDigitVisualTransformation(),
                    label = { Text("مقدار زمان") },
                    isError = showError && isInvalid,
                    supportingText = if (showError && isInvalid) {
                        { Text("عدد معتبر بزرگ‌تر از صفر وارد کنید") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "واحد زمانی:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val units = listOf(
                        PreReminderUnit.MINUTES to "دقیقه",
                        PreReminderUnit.HOURS to "ساعت",
                        PreReminderUnit.DAYS to "روز"
                    )
                    units.forEach { (unit, name) ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val amountVal = parsedAmount ?: 0
                val unitStr = when (selectedUnit) {
                    PreReminderUnit.DAYS -> "روز"
                    PreReminderUnit.HOURS -> "ساعت"
                    PreReminderUnit.MINUTES -> "دقیقه"
                }
                Text(
                    text = "پیش‌نمایش: ${amountVal.toPersianDigits()} $unitStr قبل از موعد",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
