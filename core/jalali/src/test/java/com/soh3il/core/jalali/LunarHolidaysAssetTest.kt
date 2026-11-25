package com.soh3il.core.jalali

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** بارگذاری و اعتبارسنجی تعطیلات قمری از فایل نسخه‌دار lunar_holidays.json */
class LunarHolidaysAssetTest {

    @Test
    fun jsonAssetParsesAndCovers1403To1407() {
        val years = listOf(1403, 1404, 1405, 1406, 1407).filter { IranianHolidays.hasOfficialLunarHolidays(it) }
        assertEquals(listOf(1403, 1404, 1405, 1406, 1407), years)
    }

    @Test
    fun jsonAssetHasCompleteAndAccurateYearEntries() {
        val lunar1403 = (1..12).flatMap { IranianHolidays.getOccasionsForMonth(1403, it) }
            .filter { it.id.startsWith("lunar_1403_") }
        assertEquals(17, lunar1403.size)
        assertTrue(lunar1403.any { it.jalaliDate == "1403-01-13" && it.titleFa.contains("علی") })
        assertTrue(lunar1403.any { it.jalaliDate == "1403-01-22" && it.titleFa.contains("فطر") })
        assertTrue(lunar1403.any { it.jalaliDate == "1403-04-26" && it.titleFa.contains("عاشورا") })

        val lunar1405 = (1..12).flatMap { IranianHolidays.getOccasionsForMonth(1405, it) }
            .filter { it.id.startsWith("lunar_1405_") }
        assertEquals(19, lunar1405.size)
        assertTrue(lunar1405.any { it.jalaliDate == "1405-01-01" && it.titleFa.contains("فطر") })
        assertTrue(lunar1405.any { it.jalaliDate == "1405-12-19" && it.titleFa.contains("فطر") })

        val lunar1406 = (1..12).flatMap { IranianHolidays.getOccasionsForMonth(1406, it) }
            .filter { it.id.startsWith("lunar_1406_") }
        assertEquals(17, lunar1406.size)
        assertTrue(lunar1406.any { it.jalaliDate == "1406-02-27" && it.titleFa.contains("قربان") })
        assertTrue(lunar1406.any { it.jalaliDate == "1406-03-04" && it.titleFa.contains("غدیر") })
        assertTrue(lunar1406.any { it.jalaliDate == "1406-03-24" && it.titleFa.contains("تاسوعا") })
        assertTrue(lunar1406.any { it.jalaliDate == "1406-08-11" && it.titleFa.contains("فاطمه") })
        assertTrue(lunar1406.any { it.jalaliDate == "1406-12-08" && it.titleFa.contains("فطر") })

        val lunar1407 = (1..12).flatMap { IranianHolidays.getOccasionsForMonth(1407, it) }
            .filter { it.id.startsWith("lunar_1407_") }
        assertEquals(18, lunar1407.size)
        assertTrue(lunar1407.any { it.jalaliDate == "1407-01-03" && it.titleFa.contains("صادق") })
        assertTrue(lunar1407.any { it.jalaliDate == "1407-02-16" && it.titleFa.contains("قربان") })
        assertTrue(lunar1407.any { it.jalaliDate == "1407-03-15" && it.titleFa.contains("عاشورا") })
        assertTrue(lunar1407.any { it.jalaliDate == "1407-11-27" && it.titleFa.contains("فطر") })
    }

    @Test
    fun parserHandlesEscapedQuotesAndSpecialBrackets() {
        val json = """
            {"years": {"1408": [
              {"m": 3, "d": 5, "t": "عید \"سعید\" [آزمایشی] {ویژه}", "h": true}
            ]}}
        """.trimIndent()
        val parsed = IranianHolidays.parseLunarHolidaysJson(json)
        assertEquals(1, parsed[1408]?.size)
        assertEquals(3, parsed[1408]?.get(0)?.month)
        assertEquals("عید \"سعید\" [آزمایشی] {ویژه}", parsed[1408]?.get(0)?.title)
    }

    @Test
    fun parserIgnoresMalformedEntries() {
        val json = """
            {"years": {
              "1409": [
                {"m": 13, "d": 5, "t": "ماه نامعتبر", "h": true},
                {"m": 2, "d": 40, "t": "روز نامعتبر", "h": true},
                {"m": 2, "d": 10, "t": "", "h": true},
                {"m": 2, "d": 10, "t": "معتبر", "h": false}
              ]
            }}
        """.trimIndent()
        val parsed = IranianHolidays.parseLunarHolidaysJson(json)
        assertEquals(1, parsed[1409]?.size)
        assertEquals("معتبر", parsed[1409]?.get(0)?.title)
    }
}
