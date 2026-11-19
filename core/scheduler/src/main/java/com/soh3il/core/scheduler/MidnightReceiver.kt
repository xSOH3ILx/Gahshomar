package com.soh3il.core.scheduler

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.soh3il.core.database.Soh3ilDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException

/**
 * Handles in-app midnight and notification-dismiss signals.
 * System broadcasts are accepted only by [SystemEventReceiver], which delegates here.
 */
class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in INTERNAL_ACTIONS) return
        enqueueFromReceiver(this, context, action)
    }

    companion object {
        private const val TAG = "MidnightReceiver"
        const val ACTION_MIDNIGHT_TICK = "com.soh3il.calendar.ACTION_MIDNIGHT_TICK"
        const val ACTION_NOTIFICATION_DISMISSED = "com.soh3il.calendar.ACTION_NOTIFICATION_DISMISSED"
        const val ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

        private val INTERNAL_ACTIONS = setOf(
            ACTION_MIDNIGHT_TICK,
            ACTION_NOTIFICATION_DISMISSED
        )

        private val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON
        )

        fun enqueueFromReceiver(receiver: BroadcastReceiver, context: Context, action: String) {
            val pendingResult = receiver.goAsync()
            refresh(context.applicationContext, action, pendingResult)
        }

        private fun refresh(
            context: Context,
            action: String,
            pendingResult: BroadcastReceiver.PendingResult?
        ) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    withTimeout(8_500L) {
                        refreshNow(context, action)
                    }
                } catch (error: TimeoutCancellationException) {
                    Log.w(TAG, "Calendar details timed out; date and next alarm were already refreshed")
                } catch (error: CancellationException) {
                    throw error
                } catch (e: Exception) {
                    Log.e(TAG, "calendar refresh failed: ${e.javaClass.simpleName}")
                } finally {
                    pendingResult?.finish()
                }
            }
        }

        suspend fun refreshNow(
            context: Context,
            action: String,
            readReminders: suspend () -> List<com.soh3il.core.model.Reminder> = {
                Soh3ilDatabase.getInstance(context).reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
            }
        ) {
            MidnightScheduler.scheduleMidnightAlarm(context)
            val notificationTicket = PersistentNotificationHelper.beginRefresh(context)
            requestWidgetUpdate(context)
            val activeReminders = try {
                readReminders()
            } catch (error: CancellationException) {
                PersistentNotificationHelper.finishRefresh(context, notificationTicket, null, unavailable = true)
                throw error
            } catch (error: Exception) {
                PersistentNotificationHelper.finishRefresh(context, notificationTicket, null, unavailable = true)
                throw error
            }

            PersistentNotificationHelper.finishRefresh(context, notificationTicket, activeReminders)
            if (action == ACTION_NOTIFICATION_DISMISSED) return

            if (action in RESCHEDULE_ACTIONS) {
                AlarmMigration.migrate(context)
                activeReminders.forEach { reminder ->
                    AlarmScheduler.scheduleReminder(
                        context = context,
                        reminder = reminder,
                        forceRecalculate = true
                    )
                }
                AlarmScheduler.restoreSnoozes(context)
            }

            LiveIconHelper.updateLiveIcon(context)
        }

        fun requestWidgetUpdate(context: Context) {
            context.sendBroadcast(Intent("com.soh3il.calendar.widget.REFRESH").apply {
                component = ComponentName(context.packageName, "com.soh3il.calendar.widget.CalendarAppWidgetProvider")
            })
        }
    }
}
