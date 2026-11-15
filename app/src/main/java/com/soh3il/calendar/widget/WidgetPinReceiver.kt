package com.soh3il.calendar.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_PINNED) return
        if (WidgetKind.entries.none { it.name == intent.getStringExtra("widget_kind") }) return
        WidgetUpdates.request(context)
        Toast.makeText(context, "ویجت به صفحهٔ اصلی افزوده شد", Toast.LENGTH_SHORT).show()
    }

    companion object { const val ACTION_PINNED = "com.soh3il.calendar.widget.PINNED" }
}
