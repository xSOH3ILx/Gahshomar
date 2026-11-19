package com.soh3il.calendar.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.widget.TextView
import com.soh3il.calendar.R
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.designsystem.utils.toPersianDigits
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WidgetUpdateTest {
    private fun bind(context: Context, id: Int, kind: WidgetKind) {
        val manager = AppWidgetManager.getInstance(context)
        shadowOf(manager).addBoundWidget(id, AppWidgetProviderInfo().apply { provider = kind.component(context) })
    }

    @Test fun oneReadSuppliesEveryInstalledWidget() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        bind(context, 101, WidgetKind.DAILY)
        bind(context, 102, WidgetKind.MONTH)
        bind(context, 103, WidgetKind.COMPACT)
        var reads = 0
        WidgetUpdates.refresh(context) { reads++; emptyList() }
        assertEquals(1, reads)
        val shadow = shadowOf(AppWidgetManager.getInstance(context))
        assertEquals(JalaliDate.now().day.toPersianDigits(),
            shadow.getViewFor(101).findViewById<TextView>(R.id.widget_day_number).text.toString())
        assertNotNull(shadow.getViewFor(102).findViewById<TextView>(R.id.month_title))
        assertNotNull(shadow.getViewFor(103).findViewById<TextView>(R.id.compact_day_number))
    }

    @Test fun failedReadShowsAnExplicitErrorInsteadOfAnEmptySuccessfulAgenda() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        bind(context, 104, WidgetKind.AGENDA)
        try {
            WidgetUpdates.refresh(context) { throw java.io.IOException("unavailable") }
            fail("Read errors must remain observable")
        } catch (expected: java.io.IOException) {
            val root = shadowOf(AppWidgetManager.getInstance(context)).getViewFor(104)
            assertTrue(root.findViewById<TextView>(R.id.widget_error_message).text.toString().contains("در دسترس نیست"))
            assertTrue(root.findViewById<TextView>(R.id.widget_error_date).text.isNotEmpty())
        }
    }

    @Test fun damagedInstanceDoesNotPreventOtherInstancesFromRefreshing() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        bind(context, 105, WidgetKind.DAILY)
        bind(context, 106, WidgetKind.DAILY)
        context.getSharedPreferences("soh3il_widgets", 0).edit().putString("widget_105", "{\"version\":2}").commit()
        WidgetUpdates.refresh(context) { emptyList() }
        val shadow = shadowOf(AppWidgetManager.getInstance(context))
        assertNotNull(shadow.getViewFor(105).findViewById<TextView>(R.id.widget_error_message))
        assertNotNull(shadow.getViewFor(106).findViewById<TextView>(R.id.widget_day_number))
    }

    @Test fun navigationPreservesTheExactDateAndReminderForEachInstance() {
        val context = RuntimeEnvironment.getApplication()
        val first = WidgetDestination(JalaliDate(1405, 8, 7), reminderId = 21)
        val second = WidgetDestination(JalaliDate(1405, 9, 2), add = true)
        val firstIntent = WidgetNavigation.intent(context, 1, first)
        val secondIntent = WidgetNavigation.intent(context, 2, second)
        assertEquals(first, WidgetNavigation.parse(firstIntent))
        assertEquals(second, WidgetNavigation.parse(secondIntent))
        assertNotEquals(firstIntent.data, secondIntent.data)
        assertNotEquals(WidgetNavigation.pending(context, 1, first), WidgetNavigation.pending(context, 2, second))
        assertNull(WidgetNavigation.parse(Intent("unknown")))
    }
    @Test fun timedOutRefreshShowsAnErrorInsteadOfStaleWidgetData() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        bind(context, 112, WidgetKind.DAILY)
        try {
            WidgetUpdates.refreshWithinDeadline(context, timeoutMs = 50) {
                kotlinx.coroutines.delay(500); emptyList()
            }
            fail("Expected timeout")
        } catch (expected: kotlinx.coroutines.TimeoutCancellationException) {
            val root = shadowOf(AppWidgetManager.getInstance(context)).getViewFor(112)
            assertTrue(root.findViewById<TextView>(R.id.widget_error_message).text.toString().contains("در دسترس نیست"))
        }
    }

}
