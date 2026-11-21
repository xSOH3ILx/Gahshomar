package com.soh3il.feature.reminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.mapSaver
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder

internal class ReminderDraftState(initialDate: JalaliDate, existingReminder: Reminder?) {
    private val initialPreMinutes = existingReminder?.preReminders?.firstOrNull()?.toMinutes()?.toInt()
    var selectedDate by mutableStateOf(existingReminder?.let { JalaliDate(it.jalaliYear, it.jalaliMonth, it.jalaliDay) } ?: initialDate)
    var title by mutableStateOf(existingReminder?.title ?: "")
    var description by mutableStateOf(existingReminder?.description ?: "")
    var reminderEnabled by mutableStateOf(existingReminder?.isEnabled ?: true)
    var selectedHour by mutableIntStateOf(existingReminder?.hour ?: 9)
    var selectedMinute by mutableIntStateOf(existingReminder?.minute ?: 0)
    var selectedColorHex by mutableStateOf(existingReminder?.colorHex ?: "#2563EB")
    var showTimePickerDialog by mutableStateOf(false)
    var recurrenceType by mutableStateOf(existingReminder?.recurrence?.type ?: RecurrenceType.NONE)
    var recurrenceInterval by mutableIntStateOf(existingReminder?.recurrence?.interval ?: 1)
    var showCustomRecurrenceDialog by mutableStateOf(false)
    var recurrenceExpanded by mutableStateOf(false)
    var selectedPreReminderMinutes by mutableStateOf(initialPreMinutes?.takeIf { it in listOf(15, 30, 60, 1440, 2880) })
    var customPreReminder by mutableStateOf(existingReminder?.preReminders?.firstOrNull()?.takeIf { it.toMinutes().toInt() !in listOf(15, 30, 60, 1440, 2880) })
    var isCustomPreReminderSelected by mutableStateOf(customPreReminder != null)
    var showCustomPreReminderDialog by mutableStateOf(false)
    var preReminderExpanded by mutableStateOf(false)
    var showDeleteConfirmDialog by mutableStateOf(false)

    fun preRemindersForSave(existingReminder: Reminder?): List<PreReminder> {
        val selected = when {
            isCustomPreReminderSelected && customPreReminder != null -> {
                listOf(customPreReminder!!)
            }
            selectedPreReminderMinutes != null -> {
                val mins = selectedPreReminderMinutes!!
                when {
                    mins % 1440 == 0 -> listOf(PreReminder(mins / 1440, PreReminderUnit.DAYS))
                    mins % 60 == 0 -> listOf(PreReminder(mins / 60, PreReminderUnit.HOURS))
                    else -> listOf(PreReminder(mins, PreReminderUnit.MINUTES))
                }
            }
            else -> emptyList()
        }
        val original = existingReminder?.preReminders.orEmpty()
        return if (original.isNotEmpty() && selected.singleOrNull()?.toMinutes() == original.first().toMinutes()) {
            original
        } else {
            selected
        }
    }

    companion object {
        val Saver = mapSaver<ReminderDraftState>(
            save = {
                mapOf(
                    "selectedDate" to it.selectedDate.toJdn(),
                    "title" to it.title,
                    "description" to it.description,
                    "reminderEnabled" to it.reminderEnabled,
                    "selectedHour" to it.selectedHour,
                    "selectedMinute" to it.selectedMinute,
                    "selectedColorHex" to it.selectedColorHex,
                    "showTimePickerDialog" to it.showTimePickerDialog,
                    "recurrenceType" to it.recurrenceType.name,
                    "recurrenceInterval" to it.recurrenceInterval,
                    "showCustomRecurrenceDialog" to it.showCustomRecurrenceDialog,
                    "recurrenceExpanded" to it.recurrenceExpanded,
                    "selectedPreReminderMinutes" to it.selectedPreReminderMinutes,
                    "customPreReminder" to it.customPreReminder?.let { pre -> "${pre.value}|${pre.unit.name}" },
                    "isCustomPreReminderSelected" to it.isCustomPreReminderSelected,
                    "showCustomPreReminderDialog" to it.showCustomPreReminderDialog,
                    "preReminderExpanded" to it.preReminderExpanded,
                    "showDeleteConfirmDialog" to it.showDeleteConfirmDialog
                )
            },
            restore = { values ->
                @Suppress("UNCHECKED_CAST")
                ReminderDraftState(
                    JalaliDate.fromJdn((values["selectedDate"] as? Long) ?: JalaliDate.now().toJdn()),
                    null
                ).apply {
                    selectedDate = JalaliDate.fromJdn((values["selectedDate"] as? Long) ?: JalaliDate.now().toJdn())
                    title = values["title"] as? String ?: ""
                    description = values["description"] as? String ?: ""
                    reminderEnabled = values["reminderEnabled"] as? Boolean ?: true
                    selectedHour = values["selectedHour"] as? Int ?: 9
                    selectedMinute = values["selectedMinute"] as? Int ?: 0
                    selectedColorHex = values["selectedColorHex"] as? String ?: "#2563EB"
                    showTimePickerDialog = values["showTimePickerDialog"] as? Boolean ?: false
                    recurrenceType = (values["recurrenceType"] as? String)
                        ?.let { runCatching { RecurrenceType.valueOf(it) }.getOrNull() } ?: RecurrenceType.NONE
                    recurrenceInterval = values["recurrenceInterval"] as? Int ?: 1
                    showCustomRecurrenceDialog = values["showCustomRecurrenceDialog"] as? Boolean ?: false
                    recurrenceExpanded = values["recurrenceExpanded"] as? Boolean ?: false
                    selectedPreReminderMinutes = values["selectedPreReminderMinutes"] as Int?
                    customPreReminder = (values["customPreReminder"] as String?)
                        ?.split('|')
                        ?.takeIf { it.size == 2 && it[0].toIntOrNull() != null }
                        ?.let { PreReminder(it[0].toInt(), runCatching { PreReminderUnit.valueOf(it[1]) }.getOrDefault(PreReminderUnit.MINUTES)) }
                    isCustomPreReminderSelected = values["isCustomPreReminderSelected"] as? Boolean ?: (customPreReminder != null)
                    showCustomPreReminderDialog = values["showCustomPreReminderDialog"] as? Boolean ?: false
                    preReminderExpanded = values["preReminderExpanded"] as? Boolean ?: false
                    showDeleteConfirmDialog = values["showDeleteConfirmDialog"] as? Boolean ?: false
                }
            }
        )
    }
}
