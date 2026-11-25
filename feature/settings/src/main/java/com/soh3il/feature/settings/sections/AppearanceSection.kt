package com.soh3il.feature.settings.sections

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.components.FarsiSwitchRow
import com.soh3il.core.model.AppTheme
import com.soh3il.feature.settings.components.SettingsSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSection(
    context: Context,
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    isAmoled: Boolean,
    onAmoledToggled: (Boolean) -> Unit,
    isLiveIconEnabled: Boolean,
    onLiveIconToggled: (Boolean) -> Unit
) {
    SettingsSectionHeader(
        title = "پوسته و ظاهر",
        subtitle = "شخصی‌سازی رنگ‌بندی، حالت شب و صرفه‌جویی باتری",
        icon = Icons.Default.ColorLens
    )
    Spacer(modifier = Modifier.height(6.dp))

    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val themeOptions = listOf(
                AppTheme.SYSTEM to "خودکار",
                AppTheme.LIGHT to "روشن",
                AppTheme.DARK to "تاریک"
            )

            Text(
                text = "حالت نمایش برنامه",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (theme, label) ->
                    val selected = currentTheme == theme
                    SegmentedButton(
                        selected = selected,
                        onClick = { onThemeSelected(theme) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                        icon = {}
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FarsiSwitchRow(
                title = "مشکی خالص برای نمایشگرهای AMOLED",
                subtitle = if (currentTheme == AppTheme.LIGHT) {
                    "در حالت روشن غیرفعال است؛ در زمان تاریک بودن سیستم یا برنامه اعمال می‌شود."
                } else {
                    "پس‌زمینه مشکی خالص (#000000) برای خاموش شدن پیکسل‌ها و کاهش مصرف باتری."
                },
                checked = isAmoled,
                onCheckedChange = onAmoledToggled
            )

            Spacer(modifier = Modifier.height(10.dp))

            FarsiSwitchRow(
                title = "آیکون زنده تقویم در صفحه اصلی (Launcher Live Icon)",
                subtitle = "نمایش روز جاری خورشیدی به عنوان آیکون برنامه در صفحه برنامه‌های گوشی شما.",
                checked = isLiveIconEnabled,
                onCheckedChange = onLiveIconToggled
            )
        }
    }
}
