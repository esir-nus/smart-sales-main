package com.smartsales.prism.domain.scheduler

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 相对时间解析器 — Kotlin 侧预处理
 *
 * 检测中文相对时间模式（如 "2分钟后"、"半小时后"），
 * 计算绝对时间，注入 LLM 提示。LLM 数学不可靠，Kotlin 确定性更强。
 *
 * 覆盖: 数字型相对时间 (N分钟后, N小时后, 半小时后, 一刻钟后)
 * 不覆盖: 模糊表达 (过会儿, 待会, 等下) — 这些由 LLM 处理
 */
object RelativeTimeResolver {

    private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val DURATION_UNIT_REGEX = Regex(
        pattern = """((?:半小时|一刻钟|[零一二两三四五六七八九十百\d]+\s*个?(?:小时|分钟|分)))(以后|之后|后)"""
    )

    data class RelativeTimeResolution(
        val matchedText: String,
        val offsetMinutes: Long,
        val resolvedInstant: Instant,
        val formattedLocalDateTime: String,
        val startTimeIso: String
    )

    data class RelativeDurationDeltaResolution(
        val matchedText: String,
        val offsetMinutes: Long
    )

    // 匹配顺序: 先匹配复合（N个小时），再匹配简单（N小时）
    private val PATTERNS: List<Pair<Regex, (MatchResult) -> Long?>> = listOf(
        // "半小时后" / "半小时以后" / "半小时之后"
        Regex("半小时(?:以后|之后|后)") to { _: MatchResult -> 30L },
        // "一刻钟后" / "一刻钟以后" / "一刻钟之后"
        Regex("一刻钟(?:以后|之后|后)") to { _: MatchResult -> 15L },
        // "N个小时后" / "N小时后" / "N个小时以后" / "N个小时之后"
        Regex("([零一二两三四五六七八九十百\\d]+)\\s*个?小时(?:以后|之后|后)") to { m: MatchResult ->
            parseChineseNumber(m.groupValues[1])?.times(60L)
        },
        // "N分钟后" / "N分钟以后" / "N分钟之后"
        Regex("([零一二两三四五六七八九十百\\d]+)\\s*分钟(?:以后|之后|后)") to { m: MatchResult ->
            parseChineseNumber(m.groupValues[1])?.toLong()
        },
        // "N分后" / "N分以后" / "N分之后"
        Regex("([零一二两三四五六七八九十百\\d]+)\\s*分(?:以后|之后|后)") to { m: MatchResult ->
            parseChineseNumber(m.groupValues[1])?.toLong()
        },
    )

    private val POSITIVE_DELTA_PATTERNS: List<Pair<Regex, (MatchResult) -> Long?>> = listOf(
        Regex("(往后推)\\s*(半小时|一刻钟|[零一二两三四五六七八九十百\\d]+\\s*个?(?:小时|分钟|分))") to { m: MatchResult ->
            parseDurationMinutes(m.groupValues[2])
        },
        Regex("(推迟到|延后到|延期到)\\s*(半小时|一刻钟|[零一二两三四五六七八九十百\\d]+\\s*个?(?:小时|分钟|分))") to { m: MatchResult ->
            parseDurationMinutes(m.groupValues[2])
        },
        Regex("(推迟|推后|延后|延期)\\s*(半小时|一刻钟|[零一二两三四五六七八九十百\\d]+\\s*个?(?:小时|分钟|分))") to { m: MatchResult ->
            parseDurationMinutes(m.groupValues[2])
        }
    )

    private val NEGATIVE_DELTA_PATTERNS: List<Pair<Regex, (MatchResult) -> Long?>> = listOf(
        Regex("(往前提)\\s*(半小时|一刻钟|[零一二两三四五六七八九十百\\d]+\\s*个?(?:小时|分钟|分))") to { m: MatchResult ->
            parseDurationMinutes(m.groupValues[2])?.times(-1L)
        },
        Regex("(提前到)\\s*(半小时|一刻钟|[零一二两三四五六七八九十百\\d]+\\s*个?(?:小时|分钟|分))") to { m: MatchResult ->
            parseDurationMinutes(m.groupValues[2])?.times(-1L)
        },
        Regex("(提前|提早)\\s*(半小时|一刻钟|[零一二两三四五六七八九十百\\d]+\\s*个?(?:小时|分钟|分))") to { m: MatchResult ->
            parseDurationMinutes(m.groupValues[2])?.times(-1L)
        }
    )

    fun normalizeExplicitRelativeTimeTranscript(userText: String): String {
        return DURATION_UNIT_REGEX.replace(userText) { match ->
            "${match.groupValues[1]}后"
        }
    }

    fun resolveSignedDeltaMinutes(userText: String): RelativeDurationDeltaResolution? {
        val normalizedText = normalizeExplicitRelativeTimeTranscript(userText)
        for ((pattern, extractor) in POSITIVE_DELTA_PATTERNS + NEGATIVE_DELTA_PATTERNS) {
            val match = pattern.find(normalizedText) ?: continue
            val offsetMinutes = extractor(match) ?: return null
            return RelativeDurationDeltaResolution(
                matchedText = match.value,
                offsetMinutes = offsetMinutes
            )
        }
        return null
    }

    fun resolveExact(
        userText: String,
        nowMillis: Long,
        zoneId: ZoneId
    ): RelativeTimeResolution? {
        val normalizedText = normalizeExplicitRelativeTimeTranscript(userText)
        for ((pattern, extractor) in PATTERNS) {
            val match = pattern.find(normalizedText)
            if (match != null) {
                val offsetMinutes = extractor(match) ?: return null
                val resolvedInstant = Instant.ofEpochMilli(nowMillis)
                    .plusSeconds(offsetMinutes * 60)
                return RelativeTimeResolution(
                    matchedText = match.value,
                    offsetMinutes = offsetMinutes,
                    resolvedInstant = resolvedInstant,
                    formattedLocalDateTime = FORMATTER.format(resolvedInstant.atZone(zoneId)),
                    startTimeIso = resolvedInstant.atZone(zoneId).toOffsetDateTime().toString()
                )
            }
        }
        return null
    }

    fun resolveExact(
        userText: String,
        nowIso: String,
        timezone: String
    ): RelativeTimeResolution? {
        val zoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.of("UTC"))
        val nowMillis = parseNowMillis(nowIso) ?: return null
        return resolveExact(userText, nowMillis, zoneId)
    }

    /**
     * 解析用户输入中的相对时间
     *
     * @param userText 用户原始输入
     * @param nowMillis 当前时刻 (epoch millis)
     * @param zoneId 本地时区
     * @return "YYYY-MM-DD HH:mm" 格式的绝对时间，或 null（无匹配）
     */
    fun resolve(userText: String, nowMillis: Long, zoneId: ZoneId): String? {
        return resolveExact(userText, nowMillis, zoneId)?.formattedLocalDateTime
    }

    /**
     * 生成 LLM 提示注入文本
     *
     * @return 提示文本，或 null
     */
    fun buildHint(userText: String, nowMillis: Long, zoneId: ZoneId): String? {
        val resolved = resolveExact(userText, nowMillis, zoneId) ?: return null
        return buildString {
            appendLine("## 相对时间已解析")
            appendLine("\"${resolved.matchedText}\" = ${resolved.formattedLocalDateTime}")
            append("请直接使用此 ISO-8601 时间作为 startTime: ${resolved.startTimeIso}")
        }
    }

    fun buildHint(userText: String, nowIso: String, timezone: String): String? {
        val resolved = resolveExact(userText, nowIso, timezone) ?: return null
        return buildString {
            appendLine("## 相对时间已解析")
            appendLine("\"${resolved.matchedText}\" = ${resolved.formattedLocalDateTime}")
            append("请直接使用此 ISO-8601 时间作为 startTime: ${resolved.startTimeIso}")
        }
    }

    private fun parseNowMillis(nowIso: String): Long? {
        return runCatching { Instant.parse(nowIso).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(nowIso).toInstant().toEpochMilli() }
            .getOrNull()
    }

    private fun parseDurationMinutes(raw: String): Long? {
        val normalized = raw.trim()
        if (normalized.startsWith("半小时")) return 30L
        if (normalized.startsWith("一刻钟")) return 15L

        Regex("([零一二两三四五六七八九十百\\d]+)\\s*个?小时").find(normalized)?.let { match ->
            return parseChineseNumber(match.groupValues[1])?.times(60L)
        }
        Regex("([零一二两三四五六七八九十百\\d]+)\\s*分钟").find(normalized)?.let { match ->
            return parseChineseNumber(match.groupValues[1])?.toLong()
        }
        Regex("([零一二两三四五六七八九十百\\d]+)\\s*分").find(normalized)?.let { match ->
            return parseChineseNumber(match.groupValues[1])?.toLong()
        }
        return null
    }

    private fun parseChineseNumber(raw: String): Int? {
        raw.toIntOrNull()?.let { return it }
        val normalized = raw.trim().replace("两", "二")
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
