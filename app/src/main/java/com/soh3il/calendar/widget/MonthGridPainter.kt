package com.soh3il.calendar.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import kotlin.math.min

/**
 * Canvas-based month grid renderer inspired by persian-calendar's DayPainter:
 * each cell is drawn layer by layer (today/holiday circles, day number, dot
 * indicators) instead of using per-cell TextViews.
 */
internal data class MonthCell(val day: Int, val isToday: Boolean, val isHoliday: Boolean, val hasReminder: Boolean)

internal class MonthGridPainter private constructor(    private val cellWidth: Float,
    private val cellHeight: Float,
    private val colors: WidgetPalette,
    private val typeface: Typeface,
) {
    private val circleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val circleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = this@MonthGridPainter.typeface }
    private val textBounds = Rect()

    companion object {
        fun render(
            context: Context,
            widthPx: Int,
            heightPx: Int,
            rows: Int,
            cells: List<MonthCell>,
            colors: WidgetPalette,
            textScale: Float,
        ): Bitmap {
            val typeface = ResourcesCompat.getFont(context, com.soh3il.core.designsystem.R.font.vazirmatn_bold)
                ?: Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val painter = MonthGridPainter(
                cellWidth = widthPx / 7f,
                cellHeight = heightPx / rows.toFloat(),
                colors = colors,
                typeface = typeface,
            )
            cells.forEachIndexed { index, cell ->
                val row = index / 7
                val column = index % 7
                val translationX = (6 - column) * painter.cellWidth // RTL: first column is right-most
                val translationY = row * painter.cellHeight
                val save = canvas.save()
                canvas.translate(translationX, translationY)
                painter.drawCell(canvas, cell, textScale)
                canvas.restoreToCount(save)
            }
            return bitmap
        }
    }

    private fun drawCell(canvas: Canvas, cell: MonthCell, textScale: Float) {
        if (cell.day <= 0) return
        val cx = cellWidth / 2f
        val cy = cellHeight / 2f
        val minSide = min(cellWidth, cellHeight)
        val radius = minSide * 0.40f

        if (cell.isToday) {
            circleFill.color = colors.accent
            canvas.drawCircle(cx, cy, radius, circleFill)
        } else if (cell.isHoliday) {
            circleFill.color = withAlpha(colors.holidayContainer, if (colors.isDark) 135 else 175)
            canvas.drawCircle(cx, cy, radius, circleFill)
            circleStroke.strokeWidth = (minSide * 0.03f).coerceAtLeast(1.5f)
            circleStroke.color = withAlpha(colors.holiday, if (colors.isDark) 95 else 75)
            canvas.drawCircle(cx, cy, radius - circleStroke.strokeWidth / 2f, circleStroke)
        } else if (cell.hasReminder) {
            circleFill.color = withAlpha(colors.accentContainer, if (colors.isDark) 115 else 145)
            canvas.drawCircle(cx, cy, radius, circleFill)
        }

        val number = cell.day.toPersianDigits()
        textPaint.color = when {
            cell.isToday -> colors.onAccent
            cell.isHoliday -> colors.holiday
            else -> colors.text
        }
        val dotRadius = (minSide * 0.058f).coerceAtLeast(1.2f)
        // Cap glyph height inside the circle so scaled text never touches the rim or reminder dot.
        val maxTextSize = radius * if (cell.hasReminder) 1.06f else 1.18f
        textPaint.textSize = (minSide * 0.37f * textScale).coerceAtMost(maxTextSize)
        textPaint.getTextBounds(number, 0, number.length, textBounds)
        val textWidth = textPaint.measureText(number)
        val textCenterY = if (cell.hasReminder) cy - dotRadius * 1.1f else cy
        canvas.drawText(number, cx - textWidth / 2f, textCenterY - textBounds.exactCenterY(), textPaint)

        if (cell.hasReminder) {
            dotPaint.color = when {
                cell.isToday -> colors.onAccent
                cell.isHoliday -> colors.holiday
                else -> colors.accent
            }
            val dotY = min(cy + radius * 0.74f, cellHeight - dotRadius * 1.4f)
            canvas.drawCircle(cx, dotY, dotRadius, dotPaint)
        }
    }
}

private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
