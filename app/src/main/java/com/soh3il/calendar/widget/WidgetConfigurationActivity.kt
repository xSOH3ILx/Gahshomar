package com.soh3il.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.soh3il.core.designsystem.theme.Soh3ilTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WidgetConfigurationActivity : ComponentActivity() {
    private var options by mutableStateOf<WidgetOptions?>(null)
    private var error by mutableStateOf<String?>(null)
    private var saving by mutableStateOf(false)
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val kind = WidgetKind.forId(this, widgetId)
        if (kind == null) { finish(); return }
        enableEdgeToEdge()
        try {
            options = savedInstanceState?.getString("widget_draft")?.let(WidgetOptionsStore::decode)
                ?: WidgetOptionsStore.read(this, widgetId)
        } catch (problem: Exception) {
            error = "خواندن تنظیمات این ویجت ممکن نشد. می‌توانید آن را بازنشانی کنید."
        }
        setContent {
            Soh3ilTheme {
                WidgetConfigurationScreen(kind, widgetId, options, error, saving,
                    onChange = { options = it }, onCancel = { finish() },
                    onReset = { options = WidgetOptions(); error = null }, onSave = { save() })
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        options?.let { outState.putString("widget_draft", WidgetOptionsStore.encode(it)) }
        super.onSaveInstanceState(outState)
    }

    private fun save() {
        val value = options ?: return
        if (saving) return
        saving = true
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    check(WidgetKind.forId(this@WidgetConfigurationActivity, widgetId) != null) { "Widget was removed" }
                    WidgetOptionsStore.save(this@WidgetConfigurationActivity, widgetId, value)
                    WidgetUpdates.refresh(this@WidgetConfigurationActivity)
                }
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            } catch (problem: CancellationException) {
                throw problem
            } catch (problem: Exception) {
                error = "ذخیره یا به‌روزرسانی ویجت انجام نشد. دوباره تلاش کنید."
            } finally {
                saving = false
            }
        }
    }
}
