package com.soh3il.core.jalali

import com.soh3il.core.model.Occasion

/**
 * مرجع رسمی تعطیلات و مناسبت‌های تقویم رسمی جمهوری اسلامی ایران
 * استخراج‌شده بر اساس تقویم مصوب شورای مرکز تقویم مؤسسه ژئوفیزیک دانشگاه تهران و شورای فرهنگ عمومی.
 *
 * در این کلاس تفکیک کامل بین:
 * ۱. «تعطیلات رسمی» (isHoliday = true) که رنگ قرمز دارند.
 * ۲. «مناسبت‌های ملی، باستانی و فرهنگی غیرتعطیل» (isHoliday = false) که فقط به صورت بج خاکستری نمایش می‌یابند و روز را تعطیل نمی‌کنند.
 */
object IranianHolidays {

    internal data class HolidayEntry(
        val month: Int,
        val day: Int,
        val title: String,
        val isHoliday: Boolean
    )

    // ۱. تعطیلات رسمی ثابت خورشیدی (در تمامی سال‌ها تعطیل رسمی هستند)
    private val FIXED_SOLAR_HOLIDAYS = listOf(
        HolidayEntry(1, 1, "آغاز سال نو / عید نوروز", isHoliday = true),
        HolidayEntry(1, 2, "عید نوروز", isHoliday = true),
        HolidayEntry(1, 3, "عید نوروز", isHoliday = true),
        HolidayEntry(1, 4, "عید نوروز", isHoliday = true),
        HolidayEntry(1, 12, "روز جمهوری اسلامی ایران", isHoliday = true),
        HolidayEntry(1, 13, "روز طبیعت (سیزده‌بدر)", isHoliday = true),
        HolidayEntry(3, 14, "رحلت حضرت امام خمینی (ره)", isHoliday = true),
        HolidayEntry(3, 15, "قیام خونین ۱۵ خرداد", isHoliday = true),
        HolidayEntry(11, 22, "سالروز پیروزی انقلاب اسلامی ایران", isHoliday = true),
        HolidayEntry(12, 29, "روز ملی شدن صنعت نفت ایران", isHoliday = true)
    )

    // ۲. مناسبت‌های ملی، باستانی و فرهنگی خورشیدی (غیرتعطیل - isHoliday = false)
    private val SOLAR_CULTURAL_OCCASIONS = listOf(
        HolidayEntry(1, 6, "روز امید و شادباش‌نویسی / زادروز زرتشت", isHoliday = false),
        HolidayEntry(1, 18, "روز سلامت (روز جهانی بهداشت)", isHoliday = false),
        HolidayEntry(1, 19, "جشن فروردینگان", isHoliday = false),
        HolidayEntry(1, 25, "روز بزرگداشت عطار نیشابوری", isHoliday = false),
        HolidayEntry(1, 29, "روز ارتش جمهوری اسلامی ایران", isHoliday = false),
        HolidayEntry(2, 1, "روز بزرگداشت سعدی", isHoliday = false),
        HolidayEntry(2, 2, "روز زمین پاک", isHoliday = false),
        HolidayEntry(2, 3, "جشن اردیبهشت‌گان و روز بزرگداشت شیخ بهایی", isHoliday = false),
        HolidayEntry(2, 10, "روز ملی خلیج فارس", isHoliday = false),
        HolidayEntry(2, 12, "روز معلم (شهادت استاد مرتضی مطهری)", isHoliday = false),
        HolidayEntry(2, 15, "جشن میانهٔ بهار (بهاربد) و روز شیراز", isHoliday = false),
        HolidayEntry(2, 22, "زادروز مریم میرزاخانی و روز جهانی زن در ریاضیات", isHoliday = false),
        HolidayEntry(2, 25, "روز بزرگداشت حکیم ابوالقاسم فردوسی و پاسداشت زبان فارسی", isHoliday = false),
        HolidayEntry(2, 28, "روز بزرگداشت حکیم عمر خیام", isHoliday = false),
        HolidayEntry(3, 1, "روز بزرگداشت ملاصدرا", isHoliday = false),
        HolidayEntry(3, 3, "فتح خرمشهر و روز مقاومت، ایثار و پیروزی", isHoliday = false),
        HolidayEntry(3, 6, "جشن خردادگان", isHoliday = false),
        HolidayEntry(4, 1, "جشن آب‌پاشونک (آغاز تابستان)", isHoliday = false),
        HolidayEntry(4, 10, "روز صنعت و معدن و بزرگداشت صائب تبریزی", isHoliday = false),
        HolidayEntry(4, 13, "جشن تیرگان و بزرگداشت آرش کمانگیر", isHoliday = false),
        HolidayEntry(4, 14, "روز قلم", isHoliday = false),
        HolidayEntry(4, 22, "زادروز محمد خوارزمی و روز فناوری اطلاعات", isHoliday = false),
        HolidayEntry(5, 7, "جشن امردادگان", isHoliday = false),
        HolidayEntry(5, 8, "روز بزرگداشت شیخ شهاب‌الدین سهروردی", isHoliday = false),
        HolidayEntry(5, 14, "سالروز صدور فرمان مشروطیت", isHoliday = false),
        HolidayEntry(5, 17, "روز خبرنگار", isHoliday = false),
        HolidayEntry(6, 1, "روز پزشک (بزرگداشت ابوعلی سینا)", isHoliday = false),
        HolidayEntry(6, 4, "جشن شهریورگان و روز کارمند", isHoliday = false),
        HolidayEntry(6, 5, "روز داروسازی (بزرگداشت زکریای رازی)", isHoliday = false),
        HolidayEntry(6, 13, "روز بزرگداشت ابوریحان بیرونی", isHoliday = false),
        HolidayEntry(6, 21, "روز سینما", isHoliday = false),
        HolidayEntry(6, 27, "روز شعر و ادب فارسی (بزرگداشت استاد شهریار)", isHoliday = false),
        HolidayEntry(7, 7, "روز آتش‌نشانی و ایمنی / بزرگداشت شمس تبریزی", isHoliday = false),
        HolidayEntry(7, 8, "روز بزرگداشت مولوی", isHoliday = false),
        HolidayEntry(7, 10, "جشن مهرگان", isHoliday = false),
        HolidayEntry(7, 16, "روز ملی کودک", isHoliday = false),
        HolidayEntry(7, 20, "روز بزرگداشت حافظ شیرازی", isHoliday = false),
        HolidayEntry(7, 26, "روز تربیت بدنی و ورزش", isHoliday = false),
        HolidayEntry(8, 7, "روز بزرگداشت کوروش بزرگ", isHoliday = false),
        HolidayEntry(8, 10, "جشن آبانگان", isHoliday = false),
        HolidayEntry(8, 13, "روز دانش‌آموز", isHoliday = false),
        HolidayEntry(8, 24, "روز کتاب و کتاب‌خوانی", isHoliday = false),
        HolidayEntry(9, 7, "روز نیروی دریایی ارتش", isHoliday = false),
        HolidayEntry(9, 9, "جشن آذرگان", isHoliday = false),
        HolidayEntry(9, 16, "روز دانشجو", isHoliday = false),
        HolidayEntry(9, 25, "روز پژوهش و فناوری", isHoliday = false),
        HolidayEntry(9, 30, "شب یلدا (جشن چله)", isHoliday = false),
        HolidayEntry(10, 1, "جشن خرم‌روز (نخستین جشن دی‌گان)", isHoliday = false),
        HolidayEntry(10, 5, "روز ایمنی در برابر زلزله", isHoliday = false),
        HolidayEntry(10, 20, "سالروز قتل امیرکبیر", isHoliday = false),
        HolidayEntry(11, 2, "جشن بهمنگان", isHoliday = false),
        HolidayEntry(11, 10, "جشن سده (آتش‌افروزی زمستان)", isHoliday = false),
        HolidayEntry(11, 12, "بازگشت امام خمینی (ره) به ایران", isHoliday = false),
        HolidayEntry(11, 29, "جشن سپندارمذگان (روز مهر و عشق ایرانی)", isHoliday = false),
        HolidayEntry(12, 5, "روز بزرگداشت خواجه نصیرالدین طوسی و روز مهندس", isHoliday = false),
        HolidayEntry(12, 15, "روز درختکاری", isHoliday = false),
        HolidayEntry(12, 25, "روز بزرگداشت پروین اعتصامی و پایان سرایش شاهنامه", isHoliday = false)
    )

    // ۳. تعطیلات رسمی متغیر قمری — از فایل نسخه‌دار lunar_holidays.json بارگذاری می‌شود
    // (source در JSON: مرکز تقویم مؤسسه ژئوفیزیک دانشگاه تهران)
    private val LUNAR_HOLIDAYS_MAP: Map<Int, List<HolidayEntry>> by lazy { LunarHolidaysLoader.load() }

    internal fun parseLunarHolidaysJson(text: String): Map<Int, List<HolidayEntry>> =
        LunarHolidaysLoader.parse(text)

    /**
     * بررسی پشتیبانی داده‌های رسمی تعطیلات قمری برای سال جلالی مورد نظر
     */
    fun hasOfficialLunarHolidays(year: Int): Boolean = year in LUNAR_HOLIDAYS_MAP

    /**
     * استخراج تمامی مناسبت‌ها و تعطیلات برای یک ماه خاص جلالی
     */
    fun getOccasionsForMonth(year: Int, month: Int): List<Occasion> {
        val list = mutableListOf<Occasion>()

        // ۱. تعطیلات رسمی ثابت خورشیدی (تعطیل رسمی = true)
        FIXED_SOLAR_HOLIDAYS.filter { it.month == month }.forEach { entry ->
            list.add(
                Occasion(
                    id = "solar_holiday_${month}_${entry.day}",
                    jalaliDate = formatJalaliDate(year, month, entry.day),
                    titleFa = entry.title,
                    isHoliday = true
                )
            )
        }

        // ۲. مناسبت‌های ملی، باستانی و فرهنگی خورشیدی (غیرتعطیل = false)
        SOLAR_CULTURAL_OCCASIONS.filter { it.month == month }.forEach { entry ->
            list.add(
                Occasion(
                    id = "solar_cultural_${month}_${entry.day}_${entry.title.hashCode()}",
                    jalaliDate = formatJalaliDate(year, month, entry.day),
                    titleFa = entry.title,
                    isHoliday = false
                )
            )
        }

        // ۳. بررسی روز ۳۰ اسفند در سال‌های کبیسه (تعطیل رسمی سال کبیسه) و چهارشنبه‌سوری (آخرین سه‌شنبهٔ سال)
        if (month == 12) {
            val esfandDays = JalaliDate.daysInMonth(year, 12)
            val lastDay = JalaliDate(year, 12, esfandDays)
            val chaharshanbeSuriDay = esfandDays - ((lastDay.dayOfWeek - 4 + 7) % 7)
            list.add(
                Occasion(
                    id = "solar_cultural_12_chaharshanbesuri",
                    jalaliDate = formatJalaliDate(year, 12, chaharshanbeSuriDay),
                    titleFa = "چهارشنبه‌سوری (آخرین سه‌شنبهٔ سال)",
                    isHoliday = false
                )
            )
            if (JalaliDate.isLeap(year)) {
                list.add(
                    Occasion(
                        id = "solar_12_30_leap",
                        jalaliDate = formatJalaliDate(year, 12, 30),
                        titleFa = "آخرین روز سال (تعطیل رسمی سال کبیسه)",
                        isHoliday = true
                    )
                )
            }
        }

        // ۴. تعطیلات رسمی متغیر قمری سال مورد نظر
        LUNAR_HOLIDAYS_MAP[year]?.filter { it.month == month }?.forEach { entry ->
            list.add(
                Occasion(
                    id = "lunar_${year}_${month}_${entry.day}_${entry.title.hashCode()}",
                    jalaliDate = formatJalaliDate(year, month, entry.day),
                    titleFa = entry.title,
                    isHoliday = entry.isHoliday
                )
            )
        }

        return list.sortedWith(
            compareBy<Occasion> { occ ->
                occ.jalaliDate.split("-").lastOrNull()?.toIntOrNull() ?: 0
            }.thenByDescending { it.isHoliday }
        )
    }

    private fun formatJalaliDate(year: Int, month: Int, day: Int): String {
        val mStr = month.toString().padStart(2, '0')
        val dStr = day.toString().padStart(2, '0')
        return "$year-$mStr-$dStr"
    }
}
