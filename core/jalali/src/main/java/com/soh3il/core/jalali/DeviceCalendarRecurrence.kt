package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ShortMonthPolicy

/** Android's calendar provider supports Gregorian RRULEs, without our Jalali extension. */
fun Reminder.toDeviceCalendarRrule(): String? {
    require(recurrence.type != RecurrenceType.MONTHLY_JALALI && recurrence.type != RecurrenceType.YEARLY_JALALI) {
        "تقویم گوشی از تکرار ماهانه یا سالانه خورشیدی پشتیبانی نمی‌کند. برای حفظ این تکرار از فایل ICS استفاده کنید."
    }
    if (recurrence.type == RecurrenceType.MONTHLY_GREGORIAN || recurrence.type == RecurrenceType.YEARLY_GREGORIAN) {
        val base = DateConverter.fromJalali(jalaliYear, jalaliMonth, jalaliDay).gregorian
        val anchor = recurrence.dayOfMonth ?: base.dayOfMonth
        require(anchor == base.dayOfMonth) { "روز مبنای تکرار با تاریخ شروع یکسان نیست" }
        val mayClamp = if (recurrence.type == RecurrenceType.MONTHLY_GREGORIAN) anchor > 28 else base.monthValue == 2 && anchor == 29
        require(!mayClamp || recurrence.shortMonthPolicy == ShortMonthPolicy.SKIP_MONTH) {
            "تقویم گوشی از انتقال تکرار به آخرین روز ماه کوتاه پشتیبانی نمی‌کند. برای حفظ این تکرار از فایل ICS استفاده کنید."
        }
    }
    val rule = IcsRecurrenceCodec.standardRrule(this)?.removePrefix("RRULE:") ?: return null
    return rule
}
