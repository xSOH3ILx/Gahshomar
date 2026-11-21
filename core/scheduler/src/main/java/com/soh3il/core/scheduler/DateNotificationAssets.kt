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

        // پس‌زمینه برند اپ (سبز فسفری SOH3IL) با کنتراست یکنواخت در تم روشن و تیره
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 39, 43)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(fullRect, cornerRadius, cornerRadius, bgPaint)

        // نوار بالایی نام ماه — بدنه تیره برند با نوار روشن برای کنتراست بالا در هر دو تم
        val headerHeight = sizePx * 0.34f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(215, 255, 74)
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
            color = Color.rgb(35, 39, 43)
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
            color = Color.rgb(215, 255, 74)
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

    @Suppress("DiscouragedApi")
    fun getDayDrawableRes(context: Context, day: Int): Int {
        val safeDay = day.coerceIn(1, 31)
        val resName = "ic_stat_day_$safeDay"
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        if (resId != 0) return resId

        val dsResId = context.resources.getIdentifier(resName, "drawable", "com.soh3il.core.designsystem")
        if (dsResId != 0) return dsResId

        return android.R.drawable.ic_menu_today
    }

}
