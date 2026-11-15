package com.soh3il.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object WidgetUpdates {
    const val ACTION_REFRESH = "com.soh3il.calendar.widget.REFRESH"
    private val updateLock = Mutex()
    @Volatile private var lastSnapshot: WidgetSnapshot? = null

    fun request(context: Context) {
        context.sendBroadcast(Intent(context, CalendarAppWidgetProvider::class.java).setAction(ACTION_REFRESH))
    }

    fun receive(receiver: BroadcastReceiver, context: Context, beforeRefresh: (() -> Unit)? = null) {
        val pending = receiver.goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_500L) {
                    refresh(app, beforeRefresh = beforeRefresh)
                }
            } catch (error: TimeoutCancellationException) {
                Log.w("WidgetUpdates", "Widget refresh timed out")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("WidgetUpdates", "Widget refresh failed: ${error.javaClass.simpleName}")
            } finally {
                pending?.finish()
            }
        }
    }

    suspend fun refresh(
        context: Context,
        beforeRefresh: (() -> Unit)? = null,
        readReminders: suspend () -> List<Reminder> = { read(context) }
    ) = withContext(Dispatchers.IO) {
        updateLock.withLock {
            beforeRefresh?.invoke()
            val manager = AppWidgetManager.getInstance(context)
            val installed = WidgetKind.entries.associateWith { manager.getAppWidgetIds(it.component(context)) }
            if (installed.values.all { it.isEmpty() }) return@withLock
            val reminders = try {
                readReminders()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                installed.values.forEach { ids -> ids.forEach { id -> manager.updateAppWidget(id, WidgetRenderer.error(context, id)) } }
                lastSnapshot = null
                throw error
            }
            val data = WidgetSnapshot(System.currentTimeMillis(), reminders.toList())
            lastSnapshot = data
            installed.forEach { (kind, ids) ->
                ids.forEach { id ->
                    try {
                        manager.updateAppWidget(id, WidgetRenderer.responsive(context, kind, id, data))
                        if (kind == WidgetKind.AGENDA && android.os.Build.VERSION.SDK_INT < 31) {
                            @Suppress("DEPRECATION")
                            manager.notifyAppWidgetViewDataChanged(id, com.soh3il.calendar.R.id.agenda_list)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        // A damaged instance must be reported without blocking the remaining instances.
                        Log.e("WidgetUpdates", "Widget $id could not be rendered: ${error.javaClass.simpleName}")
                        manager.updateAppWidget(id, WidgetRenderer.error(context, id))
                    }
                }
            }
        }
    }

    internal suspend fun collectionSnapshot(context: Context): WidgetSnapshot = updateLock.withLock {
        lastSnapshot?.takeIf { it.today == JalaliDate.now() }
            ?: WidgetSnapshot(System.currentTimeMillis(), read(context)).also { lastSnapshot = it }
    }

    private suspend fun read(context: Context): List<Reminder> =
        Soh3ilDatabase.getInstance(context).reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
}
