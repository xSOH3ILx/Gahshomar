package com.soh3il.calendar.widget

import android.app.Activity
import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WidgetConfigurationTest {
    private fun intent(id: Int): Intent {
        val context = RuntimeEnvironment.getApplication()
        return Intent(context, WidgetConfigurationActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }

    private fun bind(id: Int) {
        val context = RuntimeEnvironment.getApplication()
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(id,
            AppWidgetProviderInfo().apply { provider = WidgetKind.MONTH.component(context) })
    }

    @Test fun missingWidgetIdFinishesWithoutReportingSuccess() {
        val controller = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(-1)).create()
        assertTrue(controller.get().isFinishing)
        assertEquals(Activity.RESULT_CANCELED, shadowOf(controller.get()).resultCode)
        controller.destroy()
    }

    @Test fun closingConfigurationDoesNotChangeTheInstalledInstance() {
        val context = RuntimeEnvironment.getApplication()
        bind(211)
        val existing = WidgetOptions(theme = WidgetTheme.DARK, hideTitles = true)
        WidgetOptionsStore.save(context, 211, existing)
        val controller = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(211)).create()
        controller.get().finish()
        assertEquals(Activity.RESULT_CANCELED, shadowOf(controller.get()).resultCode)
        assertEquals(existing, WidgetOptionsStore.read(context, 211))
        controller.destroy()
    }

    @Test fun recreationRetainsTheUnsavedDraftWithoutCommittingIt() {
        val context = RuntimeEnvironment.getApplication()
        bind(212)
        val original = WidgetOptions(theme = WidgetTheme.LIGHT)
        val draft = original.copy(accent = WidgetAccent.PURPLE, textScale = 1.2f)
        WidgetOptionsStore.save(context, 212, original)
        val state = Bundle().apply { putString("widget_draft", WidgetOptionsStore.encode(draft)) }
        val controller = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent(212)).create(state)
        val saved = Bundle()
        controller.saveInstanceState(saved)
        assertEquals(draft, WidgetOptionsStore.decode(requireNotNull(saved.getString("widget_draft"))))
        assertEquals(original, WidgetOptionsStore.read(context, 212))
        assertEquals(Activity.RESULT_CANCELED, shadowOf(controller.get()).resultCode)
        controller.destroy()
    }
}
