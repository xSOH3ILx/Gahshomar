package com.soh3il.feature.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.components.FarsiSwitchRow
import com.soh3il.feature.settings.components.SettingsSectionHeader

@Composable
fun IntegrationsSection(
    contactBirthdaysEnabled: Boolean,
    contactsPermissionGranted: Boolean,
    onContactBirthdaysToggled: (Boolean) -> Unit
) {
    SettingsSectionHeader(
        title = "مخاطبان و تقویم‌های بیرونی",
        subtitle = "تولد مخاطبان و تقویم دستگاه، از جمله حساب Google همگام‌شده",
        icon = Icons.Default.Contacts
    )
    Spacer(modifier = Modifier.height(6.dp))
    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            FarsiSwitchRow(
                title = "تولد مخاطبان",
                subtitle = if (contactsPermissionGranted) {
                    "تولدها به‌صورت محلی و سالانه در تقویم خورشیدی نمایش داده می‌شوند."
                } else {
                    "برای خواندن تولدها، مجوز مخاطبان لازم است."
                },
                checked = contactBirthdaysEnabled,
                onCheckedChange = onContactBirthdaysToggled,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        }
    }
}
