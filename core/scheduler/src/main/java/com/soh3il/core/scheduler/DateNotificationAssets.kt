package com.soh3il.core.scheduler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate

/** Raster artwork for the notification body; the status icon stays a monochrome resource. */
internal object DateNotificationAssets {
    /**
     * بَدج اختصاصی تاریخ امروز با سبک تقویم متریال ۳ جهت نمایش به عنوان LargeIcon اعلان
     */
    fun createDateBadgeBitmap(context: Context, today: JalaliDate): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (72 * density).toInt().coerceAtLeast(144)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cornerRadius = sizePx * 0.24f
        val fullRect = android.graphics.RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())

        // پس‌زمینه سرمه‌ای شب (هماهنگ با لوگوی مدار خورشید) با نوار طلایی برای کنتراست یکنواخت در تم روشن و تیره
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(fullRect, cornerRadius, cornerRadius, bgPaint)

        // نوار بالایی نام ماه — طلایی خورشیدی با متن تیره برای خوانایی بالا در هر دو تم و صفحه‌قفل
        val headerHeight = sizePx * 0.34f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(251, 191, 36)
            style = Paint.Style.FILL
        }
        val headerPath = android.graphics.Path().apply {
            addRoundRect(
                fullRect,
                floatArrayOf(
                    cornerRadius, cornerRadius,
                    cornerRadius, cornerRadius,
                    0f, 0f,
                    0f, 0f
                ),
                android.graphics.Path.Direction.CW
            )
        }
        canvas.save()
        canvas.clipRect(0f, 0f, sizePx.toFloat(), headerHeight)
        canvas.drawPath(headerPath, headerPaint)
        canvas.restore()

        // متن نام ماه خورشیدی
        val monthText = today.monthNameFa
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.soh3il.core.designsystem.R.font.vazirmatn_bold)
            textSize = headerHeight * 0.52f
            textAlign = Paint.Align.CENTER
        }
        val monthBounds = Rect()
        monthPaint.getTextBounds(monthText, 0, monthText.length, monthBounds)
        val monthY = (headerHeight / 2f) - monthBounds.exactCenterY()
        canvas.drawText(monthText, sizePx / 2f, monthY, monthPaint)

        // عدد بزرگ روز خورشیدی در بدنه کارت
        val dayText = today.day.toPersianDigits()
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, com.soh3il.core.designsystem.R.font.vazirmatn_bold)
            textSize = (sizePx - headerHeight) * 0.65f
            textAlign = Paint.Align.CENTER
        }
        val dayBounds = Rect()
        dayPaint.getTextBounds(dayText, 0, dayText.length, dayBounds)
        val bodyCenterY = headerHeight + (sizePx - headerHeight) / 2f
        val dayY = bodyCenterY - dayBounds.exactCenterY()
        canvas.drawText(dayText, sizePx / 2f, dayY, dayPaint)

        return bitmap
    }

    /** Compile-time table keeps all 31 day drawables reachable under release resource shrinking. */
    private val DAY_DRAWABLES = intArrayOf(
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_1,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_2,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_3,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_4,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_5,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_6,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_7,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_8,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_9,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_10,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_11,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_12,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_13,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_14,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_15,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_16,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_17,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_18,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_19,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_20,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_21,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_22,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_23,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_24,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_25,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_26,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_27,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_28,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_29,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_30,
        com.soh3il.core.designsystem.R.drawable.ic_stat_day_31
    )

    fun getDayDrawableRes(day: Int): Int {
        val safeDay = day.coerceIn(1, 31)
        return DAY_DRAWABLES[safeDay - 1]
    }

}
