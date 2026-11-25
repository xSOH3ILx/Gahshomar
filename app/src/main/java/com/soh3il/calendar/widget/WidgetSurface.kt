package com.soh3il.calendar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.util.SizeF
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.content.res.ResourcesCompat
import com.soh3il.calendar.R
import com.soh3il.core.designsystem.theme.WidgetAmberDark
import com.soh3il.core.designsystem.theme.WidgetAmberLight
import com.soh3il.core.designsystem.theme.WidgetGreenDark
import com.soh3il.core.designsystem.theme.WidgetGreenLight
import com.soh3il.core.designsystem.theme.WidgetPurpleDark
import com.soh3il.core.designsystem.theme.WidgetPurpleLight
import com.soh3il.core.designsystem.theme.soh3ilWidgetColorScheme
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import kotlin.math.roundToInt

data class WidgetPalette(
    val surface: Int,
    val text: Int,
    val secondary: Int,
    val accent: Int,
    val onAccent: Int,
    val holiday: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val accentContainer: Int,
    val onAccentContainer: Int,
    val holidayContainer: Int,
    val onHolidayContainer: Int,
    val outlineVariant: Int,
    val isDark: Boolean,
)

internal object WidgetSurface {
    fun fitText(context: Context, text: String, widthDp: Float, heightDp: Float, desiredSp: Float): Float {
        val density = context.resources.displayMetrics.density
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, com.soh3il.core.designsystem.R.font.vazirmatn_regular)
        }
        var size = desiredSp
        val lines = text.lines()
        while (size > 4f) {
            paint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, context.resources.displayMetrics)
            val metrics = paint.fontMetricsInt
            if (lines.maxOf { paint.measureText(it) } <= (widthDp - 2) * density &&
                (metrics.descent - metrics.ascent) * lines.size <= (heightDp - 2) * density) break
            size -= .25f
        }
        return size
    }

    private fun bestInkOn(backgroundArgb: Int, preferredLight: Int, preferredDark: Int): Int =
        if (ColorUtils.calculateContrast(preferredLight, backgroundArgb) >=
            ColorUtils.calculateContrast(preferredDark, backgroundArgb)
        ) preferredLight else preferredDark

    fun palette(context: Context, options: WidgetOptions): WidgetPalette {
        val dark = when (options.theme) {
            WidgetTheme.LIGHT -> false
            WidgetTheme.DARK -> true
            WidgetTheme.SYSTEM -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        val scheme = soh3ilWidgetColorScheme(context, dark, options.dynamicColors)
        val surfaceArgb = scheme.surfaceContainer.toArgb()
        val surfaceHighArgb = scheme.surfaceContainerHigh.toArgb()
        val surfaceHighestArgb = scheme.surfaceContainerHighest.toArgb()
        val textArgb = scheme.onSurface.toArgb()
        val secondaryArgb = scheme.onSurfaceVariant.toArgb()
        val holidayArgb = scheme.error.toArgb()
        val holidayContainerArgb = scheme.errorContainer.toArgb()
        val onHolidayContainerArgb = scheme.onErrorContainer.toArgb()
        val outlineVariantArgb = scheme.outlineVariant.toArgb()

        val useDynamic = options.dynamicColors && Build.VERSION.SDK_INT >= 31
        val accentArgb = if (useDynamic) {
            scheme.primary.toArgb()
        } else {
            when (options.accent) {
                WidgetAccent.BLUE -> scheme.primary.toArgb()
                WidgetAccent.GREEN -> (if (dark) WidgetGreenDark else WidgetGreenLight).toArgb()
                WidgetAccent.PURPLE -> (if (dark) WidgetPurpleDark else WidgetPurpleLight).toArgb()
                WidgetAccent.AMBER -> (if (dark) WidgetAmberDark else WidgetAmberLight).toArgb()
            }
        }
        val white = android.graphics.Color.WHITE
        val black = android.graphics.Color.BLACK
        val onAccentArgb = bestInkOn(accentArgb, white, black)

        val accentContainerArgb = if (useDynamic || options.accent == WidgetAccent.BLUE) {
            scheme.primaryContainer.toArgb()
        } else {
            ColorUtils.blendARGB(surfaceArgb, accentArgb, if (dark) 0.28f else 0.16f)
        }
        val onAccentContainerArgb = if (useDynamic || options.accent == WidgetAccent.BLUE) {
            scheme.onPrimaryContainer.toArgb()
        } else {
            bestInkOn(accentContainerArgb, white, ColorUtils.blendARGB(accentArgb, black, 0.35f))
        }

        return WidgetPalette(
            surface = surfaceArgb,
            text = textArgb,
            secondary = secondaryArgb,
            accent = accentArgb,
            onAccent = onAccentArgb,
            holiday = holidayArgb,
            surfaceContainerHigh = surfaceHighArgb,
            surfaceContainerHighest = surfaceHighestArgb,
            accentContainer = accentContainerArgb,
            onAccentContainer = onAccentContainerArgb,
            holidayContainer = holidayContainerArgb,
            onHolidayContainer = onHolidayContainerArgb,
            outlineVariant = outlineVariantArgb,
            isDark = dark,
        )
    }

    fun apply(context: Context, views: RemoteViews, size: SizeF, options: WidgetOptions) {
        val color = palette(context, options)
        val density = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val widthPx = (size.width * density).roundToInt().coerceIn(1, 1400)
        val heightPx = (size.height * density).roundToInt().coerceIn(1, 1400)
        val radiusPx = 28f * density
        val strokeWidth = 1f * density
        val inset = strokeWidth / 2f

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = ColorUtils.setAlphaComponent(color.surface, (255 * options.opacity).roundToInt())
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            this.color = ColorUtils.setAlphaComponent(color.outlineVariant, ((if (color.isDark) 130 else 150) * options.opacity).roundToInt())
        }
        canvas.drawRoundRect(0f, 0f, widthPx.toFloat(), heightPx.toFloat(), radiusPx, radiusPx, fillPaint)
        canvas.drawRoundRect(inset, inset, widthPx - inset, heightPx - inset, radiusPx - inset, radiusPx - inset, strokePaint)
        views.setImageViewBitmap(R.id.widget_background, bitmap)
    }

    fun tint(views: RemoteViews, viewId: Int, color: Int) {
        views.setInt(viewId, "setColorFilter", color)
    }

    fun text(views: RemoteViews, id: Int, text: String, color: Int, sp: Float, options: WidgetOptions) {
        views.setTextViewText(id, text)
        views.setTextColor(id, color)
        views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp * options.textScale)
    }

    fun fullDate(date: JalaliDate): String = "${date.dayOfWeekNameFa}، ${date.day.toPersianDigits()} ${date.monthNameFa} ${date.year.toPersianDigits()}"

    fun configure(context: Context, views: RemoteViews, id: Int, colors: WidgetPalette, bgViewId: Int? = null) {
        val intent = Intent(context, WidgetConfigurationActivity::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        }
        bgViewId?.let { tint(views, it, colors.surfaceContainerHigh) }
        tint(views, R.id.widget_configure, colors.secondary)
        views.setOnClickPendingIntent(
            R.id.widget_configure,
            PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE),
        )
    }
}
