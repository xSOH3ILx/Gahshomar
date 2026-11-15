package com.soh3il.calendar.widget

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WidgetOptionsTest {
    @Test fun settingsBelongToEachInstanceAndSurviveStoreReads() {
        val context = RuntimeEnvironment.getApplication()
        val first = WidgetOptions(theme = WidgetTheme.LIGHT, hideTitles = true, textScale = 1.2f)
        val second = WidgetOptions(theme = WidgetTheme.DARK, opacity = .5f)
        WidgetOptionsStore.save(context, 101, first)
        WidgetOptionsStore.save(context, 102, second)
        assertEquals(first, WidgetOptionsStore.read(context, 101))
        assertEquals(second, WidgetOptionsStore.read(context, 102))
        assertNotEquals(WidgetOptionsStore.read(context, 101), WidgetOptionsStore.read(context, 102))
    }

    @Test fun restoringOverlappingIdsDoesNotLoseEitherConfiguration() {
        val context = RuntimeEnvironment.getApplication()
        val first = WidgetOptions(theme = WidgetTheme.LIGHT)
        val second = WidgetOptions(theme = WidgetTheme.DARK)
        WidgetOptionsStore.save(context, 1, first)
        WidgetOptionsStore.save(context, 2, second)
        WidgetOptionsStore.restore(context, intArrayOf(1, 2), intArrayOf(2, 3))
        assertEquals(first, WidgetOptionsStore.read(context, 2))
        assertEquals(second, WidgetOptionsStore.read(context, 3))
        assertEquals(WidgetOptions(), WidgetOptionsStore.read(context, 1))
        WidgetOptionsStore.delete(context, intArrayOf(2))
        assertEquals(WidgetOptions(), WidgetOptionsStore.read(context, 2))
        assertEquals(second, WidgetOptionsStore.read(context, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidAppearanceIsRejected() { WidgetOptions(textScale = 0f) }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSettingsAreReportedInsteadOfSilentlyReset() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("soh3il_widgets", 0).edit().putString("widget_6", "{\"version\":2}").commit()
        WidgetOptionsStore.read(context, 6)
    }
}
