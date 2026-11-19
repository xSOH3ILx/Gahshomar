package com.soh3il.core.jalali

import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object IcsParser {

    const val MAX_EVENTS = 5_000
    const val MAX_STREAM_BYTES = 10 * 1024 * 1024L // 10MB limit

    /**
     * صدور رویدادها در قالب فایل استاندارد جهانی iCalendar (RFC 5545)
     */
    fun exportToIcsString(reminders: List<Reminder>): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//SOH3IL//Persian Calendar//FA\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        appendTehranTimeZone(sb)

        val nowStr = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())

        reminders.forEach { rem ->
            val gDate = DateConverter.fromJalali(rem.jalaliYear, rem.jalaliMonth, rem.jalaliDay).gregorian

            val dtStart = String.format(
                Locale.ROOT,
                "%04d%02d%02dT%02d%02d00",
                gDate.year, gDate.monthValue, gDate.dayOfMonth,
                rem.hour, rem.minute
            )

            sb.append("BEGIN:VEVENT\r\n")
            sb.append(foldLine("UID:soh3il-${rem.id}-${rem.jalaliYear}${rem.jalaliMonth}${rem.jalaliDay}@soh3il.app"))
            sb.append("\r\n")
            sb.append("DTSTAMP:$nowStr\r\n")
            sb.append(foldLine("DTSTART;TZID=Asia/Tehran:$dtStart"))
            sb.append("\r\n")
            sb.append(foldLine("SUMMARY:${escapeIcs(rem.title)}"))
            sb.append("\r\n")
            if (rem.description.isNotBlank()) {
                sb.append(foldLine("DESCRIPTION:${escapeIcs(rem.description)}"))
                sb.append("\r\n")
            }

            val rrule = IcsRecurrenceCodec.standardRrule(rem)
            if (rrule != null) {
                sb.append(foldLine(rrule))
                sb.append("\r\n")
            }
            sb.append(foldLine("X-SOH3IL-RECURRENCE:${IcsRecurrenceCodec.xRecurrence(rem)}"))
            sb.append("\r\n")
            sb.append("X-SOH3IL-ENABLED:${rem.isEnabled}\r\n")
            sb.append("X-SOH3IL-COLOR:${rem.colorHex}\r\n")

            rem.preReminders.forEach { pre ->
                val minutes = pre.toMinutes()
                sb.append("BEGIN:VALARM\r\n")
                sb.append("ACTION:DISPLAY\r\n")
                sb.append(foldLine("DESCRIPTION:${escapeIcs(rem.title)}"))
                sb.append("\r\n")
                if (minutes % 1440 == 0L) {
                    sb.append("TRIGGER:-P${minutes / 1440}D\r\n")
                } else if (minutes % 60 == 0L) {
                    sb.append("TRIGGER:-PT${minutes / 60}H\r\n")
                } else {
                    sb.append("TRIGGER:-PT${minutes}M\r\n")
                }
                sb.append("END:VALARM\r\n")
            }

            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    // Preserve historical Tehran DST transitions for external iCalendar consumers.
    private fun appendTehranTimeZone(sb: StringBuilder) {
        val rules = ZoneId.of("Asia/Tehran").rules
        require(rules.transitionRules.isEmpty()) { "قواعد آیندهٔ منطقهٔ زمانی برای خروجی پشتیبانی نمی‌شود" }
        val transitions = rules.transitions
        val initial = transitions.firstOrNull()?.offsetBefore ?: rules.getOffset(Instant.EPOCH)
        val format = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT)
        sb.append("BEGIN:VTIMEZONE\r\nTZID:Asia/Tehran\r\n")
        sb.append("BEGIN:STANDARD\r\nDTSTART:00010101T000000\r\n")
        sb.append("TZOFFSETFROM:${icsOffset(initial)}\r\nTZOFFSETTO:${icsOffset(initial)}\r\nEND:STANDARD\r\n")
        transitions.forEach { transition ->
            val kind = if (rules.isDaylightSavings(transition.instant.plusSeconds(1))) "DAYLIGHT" else "STANDARD"
            sb.append("BEGIN:$kind\r\n")
            sb.append("DTSTART:${format.format(transition.dateTimeBefore)}\r\n")
            sb.append("TZOFFSETFROM:${icsOffset(transition.offsetBefore)}\r\n")
            sb.append("TZOFFSETTO:${icsOffset(transition.offsetAfter)}\r\nEND:$kind\r\n")
        }
        sb.append("END:VTIMEZONE\r\n")
    }

    private fun icsOffset(offset: ZoneOffset): String {
        val seconds = kotlin.math.abs(offset.totalSeconds)
        val sign = if (offset.totalSeconds < 0) "-" else "+"
        val hhmm = String.format(Locale.ROOT, "%s%02d%02d", sign, seconds / 3600, seconds % 3600 / 60)
        return if (seconds % 60 == 0) hhmm else hhmm + String.format(Locale.ROOT, "%02d", seconds % 60)
    }

    /**
     * خواندن ایمن رویدادها از فایل ICS ورودی مطابق با RFC 5545 (با پشتیبانی از Line Unfolding،
     * مهار سرریز حافظه با سقف ۱۰ مگابایت و حداکثر ۵,۰۰۰ رویداد، و پاک‌سازی کاراکترهای کنترلی).
     */
    fun importFromIcsStream(inputStream: InputStream, maxEvents: Int = MAX_EVENTS, strict: Boolean = false): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        val eventLimit = maxEvents.coerceIn(0, MAX_EVENTS)
        if (eventLimit == 0) return emptyList()

        val reader = IcsBoundedInputStream(inputStream, MAX_STREAM_BYTES).bufferedReader(Charsets.UTF_8)
        val unfoldedLines = mutableListOf<String>()
        var currentBuilder: StringBuilder? = null

        for (rawLine in reader.lineSequence()) {
            if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
                currentBuilder?.append(rawLine.substring(1))
            } else {
                currentBuilder?.let { unfoldedLines.add(it.toString()) }
                currentBuilder = StringBuilder(rawLine)
            }
        }
        currentBuilder?.let { unfoldedLines.add(it.toString()) }
        if (strict) {
            require(unfoldedLines.any { it.trim().equals("BEGIN:VCALENDAR", true) } &&
                unfoldedLines.any { it.trim().equals("END:VCALENDAR", true) }) { "ساختار فایل تقویم معتبر نیست" }
        }

        var inEvent = false
        var inAlarm = false
        var currentSummary = ""
        var currentDescription = ""
        var currentDtStart = ""
        var currentDtStartZoneId: ZoneId? = null
        var currentDtStartIsUtc = false
        var currentRrule = ""
        var currentXRecurrence = ""
        var currentEnabled = true
        var currentColor = "#2563EB"
        val currentPreReminders = mutableListOf<PreReminder>()

        for (line in unfoldedLines) {
            val trimmed = line.trim()
            if (strict && trimmed.equals("METHOD:CANCEL", ignoreCase = true)) {
                throw IllegalArgumentException("فایل لغو رویداد پشتیبانی نمی‌شود")
            } else if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                if (strict && inEvent) throw IllegalArgumentException("ساختار رویدادها معتبر نیست")
                inEvent = true
                inAlarm = false
                currentSummary = ""
                currentDescription = ""
                currentDtStart = ""
                currentDtStartZoneId = null
                currentDtStartIsUtc = false
                currentRrule = ""
                currentXRecurrence = ""
                currentEnabled = true
                currentColor = "#2563EB"
                currentPreReminders.clear()
            } else if (trimmed.equals("BEGIN:VALARM", ignoreCase = true)) {
                inAlarm = true
            } else if (trimmed.equals("END:VALARM", ignoreCase = true)) {
                inAlarm = false
            } else if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && currentSummary.isNotBlank() && currentDtStart.length >= 8) {
                    val sanitizedSummary = sanitizeText(currentSummary)
                    val sanitizedDescription = sanitizeText(currentDescription)
                    val parsed = parseEventToReminder(
                        summary = sanitizedSummary,
                        description = sanitizedDescription,
                        dtStart = currentDtStart,
                        dtStartZoneId = currentDtStartZoneId,
                        dtStartIsUtc = currentDtStartIsUtc,
                        rrule = currentRrule,
                        xRecurrence = currentXRecurrence,
                        preReminders = currentPreReminders.toList(),
                        isEnabled = currentEnabled, colorHex = currentColor, strict = strict
                    )
                    if (parsed != null) {
                        if (strict && reminders.size >= eventLimit) throw IllegalArgumentException("تعداد رویدادهای فایل بیش از سقف مجاز است")
                        reminders.add(parsed)
                        if (!strict && reminders.size >= eventLimit) {
                            break
                        }
                    }
                }
                if (strict && (currentSummary.isBlank() || currentDtStart.length < 8)) {
                    throw IllegalArgumentException("عنوان یا تاریخ یکی از رویدادها معتبر نیست")
                }
                inEvent = false
                inAlarm = false
            } else if (inEvent) {
                if (inAlarm) {
                    if (trimmed.startsWith("TRIGGER:", ignoreCase = true)) {
                        val triggerStr = trimmed.substring(8).trim()
                        currentPreReminders.add(IcsAlarmDuration.parse(triggerStr))
                    }
                } else {
                    when {
                        trimmed.startsWith("SUMMARY:", ignoreCase = true) ->
                            currentSummary = unescapeIcs(trimmed.substring(8))
                        trimmed.startsWith("DESCRIPTION:", ignoreCase = true) ->
                            currentDescription = unescapeIcs(trimmed.substring(12))
                        trimmed.startsWith("DTSTART:", ignoreCase = true) -> {
                            currentDtStart = trimmed.substring(8)
                            currentDtStartIsUtc = currentDtStart.endsWith("Z", ignoreCase = true)
                        }
                        trimmed.startsWith("DTSTART;", ignoreCase = true) -> {
                            val property = trimmed.substringBefore(":")
                            currentDtStart = trimmed.substringAfter(":")
                            currentDtStartIsUtc = currentDtStart.endsWith("Z", ignoreCase = true)
                            val tzid = property.split(";")
                                .firstOrNull { it.startsWith("TZID=", ignoreCase = true) }
                                ?.substringAfter("=")
                            currentDtStartZoneId = tzid?.let { runCatching { ZoneId.of(it) }.getOrNull() }
                            if (strict && tzid != null && currentDtStartZoneId == null) throw IllegalArgumentException("منطقهٔ زمانی رویداد شناخته نشد")
                        }
                        trimmed.startsWith("RRULE:", ignoreCase = true) ->
                            currentRrule = trimmed.substring(6)
                        trimmed.startsWith("X-SOH3IL-RECURRENCE:", ignoreCase = true) ->
                            currentXRecurrence = trimmed.substringAfter(":")
                        trimmed.startsWith("X-SOH3IL-ENABLED:", ignoreCase = true) ->
                            currentEnabled = trimmed.substringAfter(":").equals("true", ignoreCase = true)
                        trimmed.startsWith("X-SOH3IL-COLOR:", ignoreCase = true) -> {
                            val value = trimmed.substringAfter(":")
                            if (value.matches(Regex("#[0-9A-Fa-f]{6}"))) currentColor = value
                        }
                        strict && (trimmed.startsWith("EXDATE", true) || trimmed.startsWith("RDATE", true) || trimmed.startsWith("RECURRENCE-ID", true) || trimmed.startsWith("EXRULE", true) || trimmed.equals("STATUS:CANCELLED", true)) ->
                            throw IllegalArgumentException("استثنای تکرار ICS پشتیبانی نمی‌شود")
                    }
                }
            }
        }

        if (strict && (inEvent || inAlarm)) throw IllegalArgumentException("رویداد ناتمام در فایل تقویم وجود دارد")
        return reminders
    }

    private fun parseEventToReminder(
        summary: String,
        description: String,
        dtStart: String,
        rrule: String,
        dtStartZoneId: ZoneId? = null,
        dtStartIsUtc: Boolean = false,
        xRecurrence: String = "",
        preReminders: List<PreReminder> = emptyList(),
        isEnabled: Boolean = true,
        colorHex: String = "#2563EB",
        strict: Boolean = false
    ): Reminder? {
        return runCatching {
            val normalizedDtStart = dtStart.removeSuffix("Z").removeSuffix("z")
            val year = normalizedDtStart.substring(0, 4).toInt()
            val month = normalizedDtStart.substring(4, 6).toInt()
            val day = normalizedDtStart.substring(6, 8).toInt()

            var hour = 9
            var minute = 0
            val hasTime = normalizedDtStart.contains("T") && normalizedDtStart.length >= 13
            if (hasTime) {
                val timePart = normalizedDtStart.substringAfter("T")
                hour = timePart.substring(0, 2).toIntOrNull() ?: 9
                minute = timePart.substring(2, 4).toIntOrNull() ?: 0
            }

            val sourceDateTime = LocalDateTime.of(
                LocalDate.of(year, month, day),
                LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            )
            val sourceZone = when {
                dtStartIsUtc -> ZoneOffset.UTC
                else -> dtStartZoneId ?: JalaliDate.TEHRAN_ZONE
            }
            val tehranDateTime = if (hasTime) {
                ZonedDateTime.of(sourceDateTime, sourceZone)
                    .withZoneSameInstant(JalaliDate.TEHRAN_ZONE)
                    .toLocalDateTime()
            } else {
                sourceDateTime
            }
            val jResult = DateConverter.fromGregorian(
                tehranDateTime.year,
                tehranDateTime.monthValue,
                tehranDateTime.dayOfMonth
            )
            val recurrence = if (xRecurrence.isNotBlank()) {
                IcsRecurrenceCodec.parseXRecurrence(xRecurrence, strict)
            } else {
                IcsRecurrenceCodec.parseStandardRrule(rrule, strict)
            }
            if (strict && recurrence.type != RecurrenceType.NONE && (dtStartIsUtc || (dtStartZoneId != null && dtStartZoneId != JalaliDate.TEHRAN_ZONE))) {
                throw IllegalArgumentException("تکرار با منطقهٔ زمانی غیر از تهران پشتیبانی نمی‌شود")
            }
            if (strict && recurrence.type in setOf(RecurrenceType.MONTHLY_JALALI, RecurrenceType.YEARLY_JALALI) && recurrence.dayOfMonth != null) {
                require(recurrence.dayOfMonth == jResult.jalali.day) { "روز تکرار با تاریخ شروع یکسان نیست" }
            }
            if (recurrence.type == RecurrenceType.MONTHLY_GREGORIAN || recurrence.type == RecurrenceType.YEARLY_GREGORIAN) {
                require(recurrence.dayOfMonth == null || recurrence.dayOfMonth == tehranDateTime.dayOfMonth) {
                    "روز مبنای تکرار میلادی با تاریخ شروع یکسان نیست"
                }
            }

            Reminder(
                title = summary,
                description = description,
                jalaliYear = jResult.jalali.year,
                jalaliMonth = jResult.jalali.month,
                jalaliDay = jResult.jalali.day,
                hour = tehranDateTime.hour,
                minute = tehranDateTime.minute,
                recurrence = recurrence,
                preReminders = preReminders,
                isEnabled = isEnabled,
                colorHex = colorHex
            )
        }.getOrElse { error ->
            if (strict) throw IllegalArgumentException("یکی از رویدادهای فایل معتبر یا قابل پشتیبانی نیست: ${error.message}", error)
            null
        }
    }

    fun foldLine(line: String): String {
        if (line.toByteArray(Charsets.UTF_8).size <= 75) return line
        val out = StringBuilder()
        val current = StringBuilder()
        var byteCount = 0
        var first = true
        for (codePoint in line.codePoints().toArray()) {
            val ch = String(Character.toChars(codePoint))
            val charBytes = ch.toByteArray(Charsets.UTF_8).size
            val max = if (first) 75 else 74
            if (byteCount + charBytes > max && current.isNotEmpty()) {
                if (!first) out.append("\r\n ")
                out.append(current)
                current.clear()
                byteCount = 0
                first = false
            }
            current.append(ch)
            byteCount += charBytes
        }
        if (current.isNotEmpty()) {
            if (!first) out.append("\r\n ")
            out.append(current)
        }
        return out.toString()
    }

    fun escapeIcs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n", "\\n")
    }

    fun unescapeIcs(text: String): String {
        return buildString {
            var index = 0
            while (index < text.length) {
                val char = text[index++]
                if (char == '\\' && index < text.length) {
                    when (val escaped = text[index++]) {
                        'n', 'N' -> append('\n')
                        '\\', ';', ',' -> append(escaped)
                        else -> { append('\\'); append(escaped) }
                    }
                } else append(char)
            }
        }
    }

    fun sanitizeText(text: String): String {
        return text.filter { ch ->
            ch == '\n' || ch == '\t' || (!ch.isISOControl() && ch != '\uFEFF')
        }
    }
}
