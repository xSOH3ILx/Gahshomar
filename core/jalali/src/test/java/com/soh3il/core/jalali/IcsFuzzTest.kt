package com.soh3il.core.jalali

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.random.Random

/**
 * فازینگ ورودی ICS: ورودی‌های خراب، ناتمام، بزرگ‌شده و تصادفی
 * نباید کرش یا سقف منابع (OOM/بی‌نهایت) ایجاد کنند.
 */
class IcsFuzzTest {

    private fun parse(text: String, strict: Boolean = false) =
        IcsParser.importFromIcsStream(ByteArrayInputStream(text.toByteArray(Charsets.UTF_8)), strict = strict)

    @Test
    fun emptyAndBlankInputs() {
        assertEquals(0, parse("").size)
        assertEquals(0, parse("\r\n".repeat(100)).size)
        assertEquals(0, parse("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n").size)
    }

    @Test
    fun truncatedEventIsIgnoredLazilyAndRejectedInStrict() {
        val truncated = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:ناتمام"
        assertEquals(0, parse(truncated).size)
        assertThrows(IllegalArgumentException::class.java) { parse(truncated, strict = true) }
    }

    @Test
    fun malformedDtStartIsSkippedWithoutCrash() {
        val bad = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:x\r\nDTSTART:NOTADATE\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
        assertEquals(0, parse(bad).size)
        assertThrows(IllegalArgumentException::class.java) { parse(bad, strict = true) }
    }

    @Test
    fun oversizedStreamThrowsInsteadOfOOM() {
        val big = "A".repeat(IcsParser.MAX_STREAM_BYTES.toInt() + 1)
        assertThrows(IllegalArgumentException::class.java) {
            IcsParser.importFromIcsStream(ByteArrayInputStream(big.toByteArray(Charsets.UTF_8)))
        }
    }

    @Test
    fun hugeSingleLineWithinCapParsesAndOverCapIsRejected() {
        val line = "SUMMARY:" + "X".repeat(2 * 1024 * 1024)
        val validIcs = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n$line\r\nDTSTART:20250101T090000\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
        val result = parse(validIcs)
        assertEquals(1, result.size)

        val overCapLine = "SUMMARY:" + "X".repeat(IcsParser.MAX_STREAM_BYTES.toInt() + 64)
        assertThrows(IllegalArgumentException::class.java) {
            parse("BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\n$overCapLine\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n")
        }
    }

    @Test
    fun eventLimitRespectedNonStrict() {
        val event = "BEGIN:VEVENT\r\nSUMMARY:t\r\nDTSTART:20250101T090000\r\nEND:VEVENT\r\n"
        val ics = "BEGIN:VCALENDAR\r\n" + event.repeat(50) + "END:VCALENDAR\r\n"
        assertEquals(7, IcsParser.importFromIcsStream(
            ByteArrayInputStream(ics.toByteArray(Charsets.UTF_8)), maxEvents = 7
        ).size)
    }

    @Test
    fun eventLimitStrictThrows() {
        val event = "BEGIN:VEVENT\r\nSUMMARY:t\r\nDTSTART:20250101T090000\r\nEND:VEVENT\r\n"
        val ics = "BEGIN:VCALENDAR\r\n" + event.repeat(5) + "END:VCALENDAR\r\n"
        assertThrows(IllegalArgumentException::class.java) {
            IcsParser.importFromIcsStream(ByteArrayInputStream(ics.toByteArray(Charsets.UTF_8)), maxEvents = 2, strict = true)
        }
    }

    @Test
    fun controlCharactersAreSanitized() {
        val ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:a\u0000b\u0007c\r\n" +
            "DTSTART:20250101T090000\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
        val parsed = parse(ics)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].title.none { it.code < 0x20 })
    }

    @Test
    fun randomGarbageNeverThrowsNonStrict() {
        val rnd = Random(42)
        repeat(200) {
            val bytes = ByteArray(rnd.nextInt(0, 4096))
            rnd.nextBytes(bytes)
            val rawResult = IcsParser.importFromIcsStream(ByteArrayInputStream(bytes))
            assertTrue(rawResult.size >= 0)
            val text = bytes.toString(Charsets.ISO_8859_1)
            val utf8Result = IcsParser.importFromIcsStream(
                ByteArrayInputStream(text.toByteArray(Charsets.UTF_8))
            )
            assertTrue(utf8Result.size >= 0)
        }
    }

    @Test
    fun nestedBeginEventIsRejectedInStrict() {
        val ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nBEGIN:VEVENT\r\nSUMMARY:x\r\nDTSTART:20250101T090000\r\nEND:VEVENT\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
        assertThrows(IllegalArgumentException::class.java) { parse(ics, strict = true) }
    }
}
