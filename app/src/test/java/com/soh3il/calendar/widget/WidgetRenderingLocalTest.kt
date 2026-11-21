package com.soh3il.calendar.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.soh3il.calendar.R
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Reminder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetRenderingLocalTest {
    private val date = JalaliDate(1405, 8, 23)
    private fun context(scale: Float = 1f): Context {
        val app = RuntimeEnvironment.getApplication()
        return app.createConfigurationContext(Configuration(app.resources.configuration).apply { fontScale = scale })
    }

    private fun render(kind: WidgetKind, width: Int, height: Int, scale: Float, options: WidgetOptions = WidgetOptions(), reminders: List<Reminder> = emptyList()): View {
        val context = context(scale)
        val views = WidgetRenderer.render(context, kind, 123, WidgetSnapshot(date.toEpochMillis(10), reminders), SizeF(width.toFloat(), height.toFloat()), options)
        val root = views.apply(context, FrameLayout(context))
        val density = context.resources.displayMetrics.density
        root.measure(View.MeasureSpec.makeMeasureSpec((width * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((height * density).toInt(), View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        return root
    }

    @Test fun monthFitsBothSizesAndPersianLargeFont() {
        for ((width, height) in listOf(250 to 180, 400 to 400)) for (font in listOf(1f, 1.5f)) {
            val root = render(WidgetKind.MONTH, width, height, font)
            val density = root.resources.displayMetrics.density
            val canvas = root.findViewById<ImageView>(WidgetViewIds.monthGridCanvas)
            assertTrue("Canvas missing", canvas.width > 0 && canvas.height > 0)
            val bitmap = canvas.drawable as android.graphics.drawable.BitmapDrawable
            assertTrue("Bitmap too small at $font", bitmap.bitmap.width >= 100 && bitmap.bitmap.height >= 100)
            for (id in listOf(R.id.month_previous, R.id.month_next, R.id.widget_configure)) {
                val target = root.findViewById<View>(id)
                assertTrue(target.width >= 48 * density && target.height >= 48 * density)
            }
        }
    }

    @Test fun ordinaryOccasionIsNotColoredAsAHoliday() {
        val root = render(WidgetKind.MONTH, 400, 400, 1f)
        val friday = (1..date.daysInMonth).first { JalaliDate(date.year, date.month, it).dayOfWeek == 7 }
        val todayIndex = JalaliDate(date.year, date.month, 1).dayOfWeek - 1 + date.day - 1
        val fridayIndex = JalaliDate(date.year, date.month, 1).dayOfWeek - 1 + friday - 1
        // Per-day colors are baked into the canvas bitmap; verify by sampling pixels.
        val canvas = root.findViewById<ImageView>(WidgetViewIds.monthGridCanvas)
        val bitmap = (canvas.drawable as android.graphics.drawable.BitmapDrawable).bitmap
        val cellWidth = bitmap.width / 7f
        val rows = (JalaliDate(date.year, date.month, 1).dayOfWeek - 1 + date.daysInMonth + 6) / 7
        val cellHeight = bitmap.height / rows.toFloat()
        val todayPixel = bitmap.getPixel((((6 - (todayIndex % 7)) + 0.5f) * cellWidth).toInt(), (((todayIndex / 7) + 0.5f) * cellHeight).toInt())
        val fridayPixel = bitmap.getPixel((((6 - (fridayIndex % 7)) + 0.5f) * cellWidth).toInt(), (((fridayIndex / 7) + 0.5f) * cellHeight).toInt())
        val palette = WidgetSurface.palette(root.context, WidgetOptions())
        assertEquals(palette.holiday, fridayPixel)
        assertEquals(palette.accent, todayPixel)
    }

    @Test fun allExpiredRemindersNeverAppearAsNext() {
        val past = Reminder(1, "عنوان خصوصی", jalaliYear = date.year, jalaliMonth = date.month, jalaliDay = date.day, hour = 8, minute = 0)
        val root = render(WidgetKind.DAILY, 320, 180, 1f, reminders = listOf(past))
        assertEquals("یادآور دیگری برای امروز ندارید", root.findViewById<TextView>(R.id.widget_reminders_summary).text.toString())
    }

    @Test fun privacyOptionDoesNotExposeTheReminderTitle() {
        val future = Reminder(1, "عنوان خصوصی", jalaliYear = date.year, jalaliMonth = date.month, jalaliDay = date.day, hour = 12, minute = 30)
        val root = render(WidgetKind.DAILY, 320, 180, 1f, options = WidgetOptions(hideTitles = true), reminders = listOf(future))
        assertFalse(root.findViewById<TextView>(R.id.widget_reminders_summary).text.toString().contains("عنوان خصوصی"))
        assertFalse(root.contentDescription.toString().contains("عنوان خصوصی"))
    }

    @Test fun themeAndAccentTextHaveReadableContrast() {
        for (theme in listOf(WidgetTheme.LIGHT, WidgetTheme.DARK)) for (accent in WidgetAccent.entries) {
            val color = WidgetSurface.palette(context(), WidgetOptions(theme = theme, accent = accent))
            assertTrue(ColorUtils.calculateContrast(color.text, color.surface) >= 4.5)
            assertTrue(ColorUtils.calculateContrast(color.holiday, color.surface) >= 4.5)
            assertTrue(ColorUtils.calculateContrast(color.onAccent, color.accent) >= 4.5)
        }
    }

    @Test fun agendaIsFutureBoundedAndPrivacyAware() {
        val past = Reminder(1, "گذشته", jalaliYear = date.year, jalaliMonth = date.month, jalaliDay = date.day, hour = 8, minute = 0)
        val future = past.copy(id = 2, title = "خصوصی", hour = 11)
        val rows = WidgetAgendaRenderer.rows(WidgetSnapshot(date.toEpochMillis(10), listOf(past, future)), WidgetOptions(showOccasions = false, hideTitles = true))
        assertEquals(1, rows.size)
        assertEquals(2L, rows.single().destination.reminderId)
        assertFalse(rows.single().text.contains("خصوصی"))
    }

    @Test fun agendaCollectionRendersInTheRealHostView() {
        val context = context()
        val info = AppWidgetProviderInfo().apply {
            provider = WidgetKind.AGENDA.component(context)
            initialLayout = R.layout.widget_calendar_agenda
        }
        ReflectionHelpers.setField(info, "providerInfo", context.packageManager.getReceiverInfo(info.provider, 0))
        val host = AppWidgetHostView(context).apply { setAppWidget(123, info) }
        val source = Reminder(1, "نمونه", jalaliYear = date.year, jalaliMonth = date.month, jalaliDay = date.day, hour = 12, minute = 0)
        val views = WidgetRenderer.render(context, WidgetKind.AGENDA, 123, WidgetSnapshot(date.toEpochMillis(10), listOf(source)), SizeF(400f, 300f), WidgetOptions(showOccasions = false))
        // Robolectric's HostView shadow inflates without its parent. Collection actions
        // require the actual host parent, as supplied by Android's RemoteViews.apply.
        host.addView(views.apply(context, host))
        shadowOf(android.os.Looper.getMainLooper()).idle()
        host.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
        host.layout(0, 0, 400, 300)
        val list = host.findViewById<android.widget.ListView>(R.id.agenda_list)
        assertNotNull(list)
        assertEquals(1, list.adapter.count)
        val folder = File("/tmp/soh3il-widget-review").apply { mkdirs() }
        val bitmap = Bitmap.createBitmap(host.width, host.height, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bitmap))
        File(folder, "AGENDA-LIGHT.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun writeReviewImagesFromTheActualRemoteViews() {
        val folder = File("/tmp/soh3il-widget-review").apply { mkdirs() }
        for (kind in listOf(WidgetKind.COMPACT, WidgetKind.DAILY, WidgetKind.MONTH, WidgetKind.WEEK)) {
            for (theme in listOf(WidgetTheme.LIGHT, WidgetTheme.DARK)) {
                val width = if (kind == WidgetKind.COMPACT) 110 else 400
                val height = if (kind == WidgetKind.MONTH) 400 else if (kind == WidgetKind.COMPACT) 140 else 180
                val root = render(kind, width, height, 1f, WidgetOptions(theme = theme))
                val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
                root.draw(Canvas(bitmap))
                File(folder, "${kind.name}-${theme.name}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        assertTrue(File(folder, "MONTH-LIGHT.png").length() > 0)
    }
    @Test fun missingLunarDataIsVisibleOnMonthAndWeek() {
        val context = context()
        val future = JalaliDate(1407, 1, 2)
        val snapshot = WidgetSnapshot(future.toEpochMillis(10), emptyList())
        for ((kind, id) in listOf(WidgetKind.MONTH to R.id.month_warning, WidgetKind.WEEK to R.id.week_warning)) {
            val root = WidgetRenderer.render(context, kind, 123, snapshot, SizeF(400f, 300f), WidgetOptions())
                .apply(context, FrameLayout(context))
            val warning = root.findViewById<TextView>(id)
            assertEquals(View.VISIBLE, warning.visibility)
            assertTrue(warning.text.toString().contains("قمری"))
        }
    }
    @Test fun dailyWarningDoesNotDisappearWhenThereIsAnotherOccasion() {
        val context = context()
        val date = JalaliDate(1407, 1, 1)
        val root = WidgetRenderer.render(context, WidgetKind.DAILY, 123, WidgetSnapshot(date.toEpochMillis(10), emptyList()), SizeF(400f, 300f), WidgetOptions()).apply(context, FrameLayout(context))
        val text = root.findViewById<TextView>(R.id.widget_occasion).text.toString()
        assertTrue(text.contains("قمری"))
        assertTrue(text.contains("نوروز"))
    }

    @Test fun lunarWarningFitsTheWidgetWithLargeText() {
        val context = context(1.5f)
        val date = JalaliDate(1407, 1, 2)
        for ((kind, id) in listOf(WidgetKind.MONTH to R.id.month_warning, WidgetKind.WEEK to R.id.week_warning)) {
            val root = WidgetRenderer.render(context, kind, 123, WidgetSnapshot(date.toEpochMillis(10), emptyList()), SizeF(250f, 180f), WidgetOptions(textScale = 1.3f)).apply(context, FrameLayout(context))
            val density = context.resources.displayMetrics.density
            root.measure(View.MeasureSpec.makeMeasureSpec((250 * density).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((180 * density).toInt(), View.MeasureSpec.EXACTLY))
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            val warning = root.findViewById<TextView>(id)
            assertTrue("warning clipped for $kind", warning.layout.height <= warning.height - warning.compoundPaddingTop - warning.compoundPaddingBottom)
        }
    }

}
