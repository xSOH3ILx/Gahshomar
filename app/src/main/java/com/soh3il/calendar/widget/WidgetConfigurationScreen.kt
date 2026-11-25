package com.soh3il.calendar.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.Build
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.components.FarsiSwitchRow
import com.soh3il.core.designsystem.theme.FarsiPrimary
import com.soh3il.core.designsystem.theme.WidgetAmberLight
import com.soh3il.core.designsystem.theme.WidgetGreenLight
import com.soh3il.core.designsystem.theme.WidgetPurpleLight
import com.soh3il.core.designsystem.utils.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetConfigurationScreen(
    kind: WidgetKind,
    id: Int,
    options: WidgetOptions?,
    error: String?,
    saving: Boolean,
    onChange: (WidgetOptions) -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "تنظیم ویجت ${kind.title}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !saving) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (options != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = onSave,
                            enabled = !saving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (saving) "در حال ذخیره" else "ذخیره تغییرات")
                        }
                        TextButton(onClick = onCancel, enabled = !saving) {
                            Text("انصراف")
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            if (options == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onReset) { Text("بازنشانی تنظیمات") }
                    TextButton(onClick = onCancel) { Text("انصراف") }
                }
                return@Column
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "پیش‌نمایش روی صفحه اصلی",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF1E3A8A),
                                    Color(0xFF334155),
                                ),
                            ),
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    WidgetPreview(kind, options, Modifier.fillMaxWidth())
                }
            }

            FarsiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "ظاهر و رنگ‌بندی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    val themeOptions = listOf(
                        WidgetTheme.SYSTEM to "سیستم",
                        WidgetTheme.LIGHT to "روشن",
                        WidgetTheme.DARK to "تیره",
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (theme, label) ->
                            val selected = options.theme == theme
                            SegmentedButton(
                                selected = selected,
                                onClick = { onChange(options.copy(theme = theme)) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                                enabled = !saving,
                                icon = {},
                            ) {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 31) {
                        FarsiSwitchRow(
                            title = "رنگ‌های پویای سیستم (Material You)",
                            subtitle = "هماهنگی خودکار رنگ ویجت با تصویر پس‌زمینه گوشی",
                            checked = options.dynamicColors,
                            onCheckedChange = { if (!saving) onChange(options.copy(dynamicColors = it)) },
                        )
                    }

                    if (!options.dynamicColors) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "رنگ تأکید",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                val swatches = listOf(
                                    Triple(WidgetAccent.BLUE, "آبی", FarsiPrimary),
                                    Triple(WidgetAccent.AMBER, "خورشیدی", WidgetAmberLight),
                                    Triple(WidgetAccent.GREEN, "سبز", WidgetGreenLight),
                                    Triple(WidgetAccent.PURPLE, "بنفش", WidgetPurpleLight),
                                )
                                swatches.forEach { (accent, label, color) ->
                                    val selected = options.accent == accent
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .selectable(
                                                selected = selected,
                                                enabled = !saving,
                                                role = Role.RadioButton,
                                                onClick = { onChange(options.copy(accent = accent)) },
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .then(
                                                    if (selected) {
                                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("شفافیت پس‌زمینه", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${(options.opacity * 100).toInt().toPersianDigits()}٪",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = options.opacity,
                            onValueChange = { onChange(options.copy(opacity = it)) },
                            valueRange = .25f..1f,
                            enabled = !saving,
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("اندازهٔ متن", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${(options.textScale * 100).toInt().toPersianDigits()}٪",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Slider(
                            value = options.textScale,
                            onValueChange = { onChange(options.copy(textScale = it)) },
                            valueRange = .85f..1.3f,
                            enabled = !saving,
                        )
                    }
                }
            }

            FarsiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "محتوا و حریم خصوصی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (kind == WidgetKind.DAILY) {
                        FarsiSwitchRow(
                            title = "نمایش تاریخ میلادی",
                            checked = options.showGregorian,
                            onCheckedChange = { if (!saving) onChange(options.copy(showGregorian = it)) },
                        )
                        FarsiSwitchRow(
                            title = "نمایش تاریخ قمری",
                            checked = options.showHijri,
                            onCheckedChange = { if (!saving) onChange(options.copy(showHijri = it)) },
                        )
                    }
                    if (kind == WidgetKind.DAILY || kind == WidgetKind.AGENDA) {
                        FarsiSwitchRow(
                            title = "نمایش مناسبت‌ها و تعطیلات",
                            checked = options.showOccasions,
                            onCheckedChange = { if (!saving) onChange(options.copy(showOccasions = it)) },
                        )
                    }
                    FarsiSwitchRow(
                        title = if (kind == WidgetKind.COMPACT) "نشانگر تعداد یادآورهای امروز" else "نمایش یادآورها",
                        checked = options.showReminders,
                        onCheckedChange = { if (!saving) onChange(options.copy(showReminders = it)) },
                    )
                    if (kind == WidgetKind.DAILY || kind == WidgetKind.AGENDA) {
                        FarsiSwitchRow(
                            title = "مخفی‌سازی عنوان یادآورها",
                            subtitle = "جلوگیری از نمایش عنوان خصوصی روی صفحه اصلی",
                            checked = options.hideTitles,
                            onCheckedChange = { if (!saving) onChange(options.copy(hideTitles = it)) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun WidgetPreview(kind: WidgetKind, options: WidgetOptions, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data = remember { WidgetSnapshot.preview() }
    AndroidView(
        factory = {
            object : AppWidgetHostView(it) {
                override fun onInterceptTouchEvent(event: MotionEvent?) = true
            }.apply {
                val info = AppWidgetManager.getInstance(context).installedProviders.first { p ->
                    p.provider == kind.component(context)
                }
                setAppWidget(-1, info)
                setPadding(0, 0, 0, 0)
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                setOnTouchListener { _, _ -> true }
            }
        },
        update = { frame ->
            val preview = WidgetRenderer.render(context, kind, -1, data, kind.defaultSize, options)
            frame.updateAppWidget(preview)
        },
        modifier = modifier.height(kind.defaultSize.height.dp),
    )
}
