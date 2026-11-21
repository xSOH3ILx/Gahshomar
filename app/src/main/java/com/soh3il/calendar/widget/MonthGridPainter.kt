package com.soh3il.calendar.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
    private val circleStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = min(cellWidth, cellHeight) * .06f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = this@MonthGridPainter.typeface }

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
        val cx = cellWidth / 2f
        val cy = cellHeight / 2f
        val radius = min(cellWidth, cellHeight) / 2f - min(cellWidth, cellHeight) * .09f

        if (cell.isHoliday && !cell.isToday) {
            circleFill.color = withAlpha(colors.holiday, 36)
            canvas.drawCircle(cx, cy, radius, circleFill)
        }
        if (cell.isToday) {
            circleFill.color = colors.accent
            canvas.drawCircle(cx, cy, radius, circleFill)
        } else if (cell.hasReminder) {
            circleStroke.color = withAlpha(colors.secondary, 90)
            canvas.drawCircle(cx, cy, radius, circleStroke)
        }

        val number = cell.day.toPersianDigits()
        textPaint.color = when {
            cell.isToday -> colors.onAccent
            cell.isHoliday -> colors.holiday
            else -> colors.text
        }
        textPaint.textSize = min(cellWidth, cellHeight) * .48f * textScale
        val metrics = textPaint.fontMetrics
        val textWidth = textPaint.measureText(number)
        canvas.drawText(number, cx - textWidth / 2f, cy - (metrics.ascent + metrics.descent) / 2f, textPaint)

        if (cell.hasReminder && !cell.isToday) {
            dotPaint.color = colors.accent
            val dotRadius = min(cellWidth, cellHeight) * .055f
            canvas.drawCircle(cx, cy + radius * .82f, dotRadius, dotPaint)
        }
    }
}

private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
