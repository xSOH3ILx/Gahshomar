package com.soh3il.core.jalali

/**
 * پارسر سبک و بدون وابستگی خارجی برای فایل lunar_holidays.json.
 * برای پشتیبانی از رشته‌های شامل آکولاد، براکت و کاراکترهای Escape شده به جای Regex استفاده می‌شود.
 */
internal object LunarHolidaysLoader {

    private const val RESOURCE_PATH = "/com/soh3il/core/jalali/lunar_holidays.json"

    fun load(): Map<Int, List<IranianHolidays.HolidayEntry>> = runCatching {
        val stream = LunarHolidaysLoader::class.java.getResourceAsStream(RESOURCE_PATH)
            ?: error("$RESOURCE_PATH not found")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        parse(text)
    }.getOrElse { error ->
        java.util.logging.Logger.getLogger("IranianHolidays").warning(
            "failed to load lunar holidays data: ${error.message}"
        )
        emptyMap()
    }

    fun parse(text: String): Map<Int, List<IranianHolidays.HolidayEntry>> {
        val root = JsonCursor(text).parseValue() as? Map<*, *> ?: return emptyMap()
        val yearsObj = root["years"] as? Map<*, *> ?: return emptyMap()
        val result = mutableMapOf<Int, List<IranianHolidays.HolidayEntry>>()
        for ((yearKey, rawEntries) in yearsObj) {
            val year = (yearKey as? String)?.toIntOrNull() ?: continue
            val list = rawEntries as? List<*> ?: continue
            val entries = mutableListOf<IranianHolidays.HolidayEntry>()
            for (item in list) {
                val obj = item as? Map<*, *> ?: continue
                val month = (obj["m"] as? Number)?.toInt() ?: continue
                val day = (obj["d"] as? Number)?.toInt() ?: continue
                val title = (obj["t"] as? String)?.trim().orEmpty()
                val isHoliday = obj["h"] as? Boolean ?: false
                if (month in 1..12 && day in 1..31 && title.isNotEmpty()) {
                    entries.add(IranianHolidays.HolidayEntry(month, day, title, isHoliday))
                }
            }
            result[year] = entries
        }
        return result
    }

    private class JsonCursor(private val src: String) {
        private var pos = 0

        fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= src.length) error("Unexpected end of JSON")
            return when (src[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected token '${src[pos]}' at index $pos")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (peek('}')) {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (peek('}')) {
                    pos++
                    return map
                }
                expect(',')
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek(']')) {
                pos++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                if (peek(']')) {
                    pos++
                    return list
                }
                expect(',')
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) error("Unterminated escape")
                        when (val esc = src[pos++]) {
                            '"', '\\', '/' -> sb.append(esc)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > src.length) error("Invalid unicode escape")
                                sb.append(src.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }
                            else -> error("Unknown escape '\\$esc'")
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            error("Unterminated string")
        }

        private fun parseBoolean(): Boolean = if (src.startsWith("true", pos)) {
            pos += 4; true
        } else if (src.startsWith("false", pos)) {
            pos += 5; false
        } else {
            error("Invalid boolean at $pos")
        }

        private fun parseNull(): Any? {
            if (!src.startsWith("null", pos)) error("Invalid null at $pos")
            pos += 4
            return null
        }

        private fun parseNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            while (pos < src.length && src[pos] in '0'..'9') pos++
            val isFloat = pos < src.length && (src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E')
            if (isFloat) {
                while (pos < src.length && src[pos] in "0123456789.eE+-") pos++
                return src.substring(start, pos).toDouble()
            }
            return src.substring(start, pos).toLong()
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(expected: Char): Boolean = pos < src.length && src[pos] == expected

        private fun expect(expected: Char) {
            if (pos >= src.length || src[pos] != expected) {
                error("Expected '$expected' at index $pos")
            }
            pos++
        }
    }
}
