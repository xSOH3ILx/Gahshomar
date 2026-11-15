package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.soh3il.core.designsystem.utils.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPinDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    var message by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ویجت‌های صفحهٔ اصلی", style = MaterialTheme.typography.headlineSmall)
            Text("پیش‌نمایش‌ها دادهٔ نمونه دارند. هر ویجت را می‌توانید جداگانه تنظیم کنید.")
            if (!manager.isRequestPinAppWidgetSupported) {
                Text("روی فضای خالی صفحهٔ اصلی نگه دارید، فهرست ویجت‌ها را باز کنید و SOH3IL را پیدا کنید.")
            }
            message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            WidgetKind.entries.forEach { kind ->
                Text(kind.title, style = MaterialTheme.typography.titleMedium)
                WidgetPreview(kind, WidgetOptions(), Modifier.fillMaxWidth())
                if (manager.isRequestPinAppWidgetSupported) Button(onClick = {
                    try {
                        val callback = Intent(context, WidgetPinReceiver::class.java).apply {
                            action = WidgetPinReceiver.ACTION_PINNED
                            data = Uri.parse("soh3il-widget://pin/${kind.name}/${System.nanoTime()}")
                            putExtra("widget_kind", kind.name)
                        }
                        val pending = PendingIntent.getBroadcast(context, kind.ordinal, callback,
                            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                        val accepted = manager.requestPinAppWidget(kind.component(context), null, pending)
                        message = if (accepted) "درخواست ارسال شد؛ افزودن ویجت را در پنجرهٔ گوشی تأیید کنید."
                            else "لانچر درخواست افزودن را نپذیرفت. از فهرست ویجت‌های صفحهٔ اصلی استفاده کنید."
                    } catch (error: SecurityException) {
                        message = "گوشی اجازهٔ افزودن مستقیم نداد. از فهرست ویجت‌های صفحهٔ اصلی استفاده کنید."
                    } catch (error: IllegalStateException) {
                        message = "درخواست افزودن در این وضعیت در دسترس نیست. دوباره تلاش کنید."
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("افزودن ${kind.title}") }
                manager.getAppWidgetIds(kind.component(context)).forEachIndexed { index, id ->
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(context, WidgetConfigurationActivity::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                        })
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("تنظیم نمونهٔ ${(index + 1).toPersianDigits()}")
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("بستن") }
        }
    }
}
