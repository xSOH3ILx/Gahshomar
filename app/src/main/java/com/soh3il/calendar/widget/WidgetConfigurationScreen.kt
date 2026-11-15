package com.soh3il.calendar.widget

import android.view.MotionEvent
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.soh3il.core.designsystem.utils.toPersianDigits

@Composable
internal fun WidgetConfigurationScreen(
    kind: WidgetKind, id: Int, options: WidgetOptions?, error: String?, saving: Boolean,
    onChange: (WidgetOptions) -> Unit, onCancel: () -> Unit, onReset: () -> Unit, onSave: () -> Unit
) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("تنظیم ${kind.title}", style = MaterialTheme.typography.headlineSmall)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            if (options == null) {
                Button(onClick = onReset) { Text("بازنشانی تنظیمات") }
                TextButton(onClick = onCancel) { Text("انصراف") }
                return@Column
            }
            Text("پیش‌نمایش با دادهٔ نمونه", style = MaterialTheme.typography.labelLarge)
            WidgetPreview(kind, options, Modifier.fillMaxWidth())
            Text("پوسته", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(WidgetTheme.SYSTEM to "سیستم", WidgetTheme.LIGHT to "روشن", WidgetTheme.DARK to "تیره").forEach { (theme, label) ->
                    FilterChip(selected = options.theme == theme, onClick = { onChange(options.copy(theme = theme)) }, label = { Text(label) }, enabled = !saving)
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                WidgetToggle("رنگ‌های پس‌زمینهٔ گوشی", options.dynamicColors, !saving) { onChange(options.copy(dynamicColors = it)) }
            }
            if (!options.dynamicColors) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(WidgetAccent.BLUE to "آبی", WidgetAccent.GREEN to "سبز", WidgetAccent.PURPLE to "بنفش").forEach { (accent, label) ->
                    FilterChip(selected = options.accent == accent, onClick = { onChange(options.copy(accent = accent)) }, label = { Text(label) }, enabled = !saving)
                }
            }
            Text("تیرگی پس‌زمینه: ${(options.opacity * 100).toInt().toPersianDigits()}٪")
            Slider(value = options.opacity, onValueChange = { onChange(options.copy(opacity = it)) }, valueRange = .25f..1f, enabled = !saving)
            Text("اندازهٔ متن: ${(options.textScale * 100).toInt().toPersianDigits()}٪")
            Slider(value = options.textScale, onValueChange = { onChange(options.copy(textScale = it)) }, valueRange = .85f..1.3f, enabled = !saving)
            if (kind == WidgetKind.DAILY) {
                WidgetToggle("تاریخ میلادی", options.showGregorian, !saving) { onChange(options.copy(showGregorian = it)) }
                WidgetToggle("تاریخ قمری", options.showHijri, !saving) { onChange(options.copy(showHijri = it)) }
            }
            if (kind == WidgetKind.DAILY || kind == WidgetKind.AGENDA) {
                WidgetToggle("مناسبت‌ها", options.showOccasions, !saving) { onChange(options.copy(showOccasions = it)) }
            }
            if (kind != WidgetKind.COMPACT) {
                WidgetToggle("یادآورها", options.showReminders, !saving) { onChange(options.copy(showReminders = it)) }
            }
            if (kind == WidgetKind.DAILY || kind == WidgetKind.AGENDA) {
                WidgetToggle("پنهان‌کردن عنوان یادآورها", options.hideTitles, !saving) { onChange(options.copy(hideTitles = it)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSave, enabled = !saving, modifier = Modifier.weight(1f)) { Text(if (saving) "در حال ذخیره" else "ذخیره") }
                TextButton(onClick = onCancel, enabled = !saving) { Text("انصراف") }
            }
        }
    }
}

@Composable
private fun WidgetToggle(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
internal fun WidgetPreview(kind: WidgetKind, options: WidgetOptions, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val data = remember { WidgetSnapshot.preview() }
    AndroidView(
        factory = { object : AppWidgetHostView(it) {
            override fun onInterceptTouchEvent(event: MotionEvent?) = true
        }.apply {
            val info = AppWidgetManager.getInstance(context).installedProviders.first { it.provider == kind.component(context) }
            setAppWidget(-1, info)
            setPadding(0, 0, 0, 0)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            setOnTouchListener { _, _ -> true }
        } },
        update = { frame ->
            val preview = WidgetRenderer.render(context, kind, -1, data, kind.defaultSize, options)
            frame.updateAppWidget(preview)
        },
        modifier = modifier.height(kind.defaultSize.height.dp)
    )
}
