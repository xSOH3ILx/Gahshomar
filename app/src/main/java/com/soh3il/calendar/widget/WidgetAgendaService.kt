package com.soh3il.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

/** Collection adapter for Android 8–11; Android 12+ receives collection items directly. */
class WidgetAgendaService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))

    private inner class Factory(val id: Int) : RemoteViewsFactory {
        private var options = WidgetOptions()
        @Volatile private var rows = emptyList<WidgetAgendaRow>()
        override fun onCreate() = Unit
        override fun onDestroy() { rows = emptyList() }
        override fun onDataSetChanged() {
            if (WidgetKind.forId(applicationContext, id) != WidgetKind.AGENDA) return
            try {
                options = WidgetOptionsStore.read(applicationContext, id)
                val data = runBlocking { WidgetUpdates.collectionSnapshot(applicationContext) }
                rows = WidgetAgendaRenderer.rows(data, options)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("WidgetAgenda", "Agenda unavailable: ${error.javaClass.simpleName}")
                rows = listOf(WidgetAgendaRow("دستور کار", "خواندن یادآورها ممکن نشد؛ برنامه را باز کنید", WidgetDestination(com.soh3il.core.jalali.JalaliDate.now())))
            }
        }
        override fun getCount() = rows.size
        override fun getViewAt(position: Int): RemoteViews? = rows.getOrNull(position)?.let {
            WidgetAgendaRenderer.row(applicationContext, it, options)
        }
        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false
    }
}
