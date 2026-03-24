package com.smartsales.prism.domain.scheduler

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 显式日期+钟点解析器。
 *
 * 复用调度域已经接受的“明天/后天 + 早上/下午 + X点”语义，
 * 避免在改期场景重新发明一套独立的时间语法。
 */
object ExactTimeCueResolver {

    private enum class RelativeDayFamily {
        REAL_TODAY,
        REAL_PLUS_1,
        REAL_PLUS_2,
        PAGE_PLUS_1
    }

    fun normalizeRelativeDayStartTime(
        transcript: String?,
        startTimeIso: String,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): String? {
        val lawfulDate = computeLawfulAnchorDate(transcript, nowIso, timezone, displayedDateIso) ?: return startTimeIso
        val zoned = try {
            OffsetDateTime.parse(startTimeIso)
        } catch (_: Exception) {
            return startTimeIso
        }
        return zoned.withYear(lawfulDate.year)
            .withMonth(lawfulDate.monthValue)
            .withDayOfMonth(lawfulDate.dayOfMonth)
            .toString()
    }

    fun normalizeRelativeDayAnchorDate(
        transcript: String?,
        anchorDateIso: String,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): String? {
        val lawfulDate = computeLawfulAnchorDate(transcript, nowIso, timezone, displayedDateIso) ?: return anchorDateIso
        return lawfulDate.toString()
    }

    fun buildExactStartTimeFromExplicitCue(
        transcript: String?,
        anchorDateIso: String,
        timeHint: String?,
        timezone: String?
    ): String? {
        val cueSource = timeHint?.takeIf { it.isNotBlank() } ?: transcript ?: return null
        val parsedTime = parseExplicitClockCue(cueSource) ?: return null
        val zoneId = runCatching { ZoneId.of(timezone ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
        return LocalDate.parse(anchorDateIso)
            .atTime(parsedTime)
            .atZone(zoneId)
            .toOffsetDateTime()
            .toString()
    }

    fun parseClockTime(raw: String): LocalTime? {
        return runCatching { LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
    }

    fun resolveExactDayClockStartTime(
        transcript: String?,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): String? {
        val lawfulDate = computeLawfulAnchorDate(transcript, nowIso, timezone, displayedDateIso) ?: return null
        return buildExactStartTimeFromExplicitCue(
            transcript = transcript,
            anchorDateIso = lawfulDate.toString(),
            timeHint = null,
            timezone = timezone
        )
    }

    private fun detectRelativeDayFamily(transcript: String?): RelativeDayFamily? {
        val normalized = transcript?.lowercase()?.trim() ?: return null
        return when {
            normalized.contains("下一天") || normalized.contains("后一天") || normalized.contains("nextday") || normalized.contains("next day") -> {
                RelativeDayFamily.PAGE_PLUS_1
            }
            normalized.contains("后天") || normalized.contains("day after tomorrow") -> {
                RelativeDayFamily.REAL_PLUS_2
            }
            normalized.contains("明天") || normalized.contains("tomorrow") -> {
                RelativeDayFamily.REAL_PLUS_1
            }
            normalized.contains("今天") || normalized.contains("today") || normalized.contains("今晚") -> {
                RelativeDayFamily.REAL_TODAY
            }
            else -> null
        }
    }

    private fun computeLawfulAnchorDate(
        transcript: String?,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): LocalDate? {
        val family = detectRelativeDayFamily(transcript) ?: return null
        return when (family) {
            RelativeDayFamily.REAL_TODAY -> resolveNowDate(nowIso, timezone)
            RelativeDayFamily.REAL_PLUS_1 -> resolveNowDate(nowIso, timezone)?.plusDays(1)
            RelativeDayFamily.REAL_PLUS_2 -> resolveNowDate(nowIso, timezone)?.plusDays(2)
            RelativeDayFamily.PAGE_PLUS_1 -> displayedDateIso?.let(LocalDate::parse)?.plusDays(1)
        }
    }

    private fun resolveNowDate(nowIso: String?, timezone: String?): LocalDate? {
        val rawNowIso = nowIso ?: return null
        val zoneId = runCatching { ZoneId.of(timezone ?: "UTC") }.getOrDefault(ZoneId.of("UTC"))
        return runCatching {
            Instant.parse(rawNowIso).atZone(zoneId).toLocalDate()
        }.getOrElse {
            runCatching {
                OffsetDateTime.parse(rawNowIso).atZoneSameInstant(zoneId).toLocalDate()
            }.getOrElse {
                runCatching {
                    LocalDateTime.parse(rawNowIso, DateTimeFormatter.ISO_DATE_TIME).atZone(zoneId).toLocalDate()
                }.getOrNull()
            }
        }
    }

    private fun parseExplicitClockCue(raw: String): LocalTime? {
        val normalized = raw.lowercase().trim()
        parseAmPmClock(normalized)?.let { return it }
        parseEmbedded24HourClock(normalized)?.let { return it }
        parseChineseClock(normalized)?.let { return it }
        return null
    }

    private fun parseEmbedded24HourClock(normalized: String): LocalTime? {
        val match = Regex("""\b(\d{1,2}):(\d{2})\b""").find(normalized) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }

    private fun parseAmPmClock(normalized: String): LocalTime? {
        val match = Regex("""\b(?:at\s*)?(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)\b""")
            .find(normalized)
            ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        val marker = match.groupValues[3]
        val normalizedHour = when {
            marker.startsWith("p") && hour in 1..11 -> hour + 12
            marker.startsWith("a") && hour == 12 -> 0
            else -> hour
        }
        return runCatching { LocalTime.of(normalizedHour, minute) }.getOrNull()
    }

    private fun parseChineseClock(normalized: String): LocalTime? {
        val match = Regex("""(凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?([零一二两三四五六七八九十百\d]{1,3})点(半)?""")
            .find(normalized)
            ?: return null
        val prefix = match.groupValues[1]
        val rawHour = parseChineseNumber(match.groupValues[2]) ?: return null
        val minute = if (match.groupValues[3] == "半") 30 else 0
        val normalizedHour = when (prefix) {
            "凌晨", "半夜", "午夜" -> if (rawHour == 12) 0 else rawHour
            "早上", "上午" -> if (rawHour == 12) 0 else rawHour
            "中午" -> when {
                rawHour in 1..10 -> rawHour + 12
                rawHour == 12 -> 12
                else -> rawHour
            }
            "下午", "晚上", "今晚" -> when {
                rawHour in 1..11 -> rawHour + 12
                rawHour == 12 -> 12
                else -> rawHour
            }
            else -> when {
                rawHour == 1 -> 13
                rawHour in 2..6 -> rawHour + 12
                else -> rawHour
            }
        }
        return runCatching { LocalTime.of(normalizedHour, minute) }.getOrNull()
    }

    private fun parseChineseNumber(raw: String): Int? {
        raw.toIntOrNull()?.let { return it }
        val normalized = raw.replace("两", "二")
        val digits = mapOf(
            '零' to 0,
            '一' to 1,
            '二' to 2,
            '三' to 3,
            '四' to 4,
            '五' to 5,
            '六' to 6,
            '七' to 7,
            '八' to 8,
            '九' to 9
        )
        if (normalized == "十") return 10
        if (normalized.contains("十")) {
            val parts = normalized.split("十")
            val tens = when (val prefix = parts.firstOrNull().orEmpty()) {
                "" -> 1
                else -> digits[prefix.singleOrNull()] ?: return null
            }
            val ones = when (val suffix = parts.getOrNull(1).orEmpty()) {
                "" -> 0
                else -> digits[suffix.singleOrNull()] ?: return null
            }
            return tens * 10 + ones
        }
        if (normalized.length == 1) return digits[normalized.single()]
        return null
    }
}
