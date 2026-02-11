package com.smartsales.prism.domain.scheduler

import java.time.Instant
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

    // 匹配顺序: 先匹配复合（N个小时），再匹配简单（N小时）
    private val PATTERNS: List<Pair<Regex, (MatchResult) -> Long>> = listOf(
        // "半小时后" / "半小时以后"
        Regex("半小时(?:以)?后") to { _: MatchResult -> 30L },
        // "一刻钟后" / "一刻钟以后"
        Regex("一刻钟(?:以)?后") to { _: MatchResult -> 15L },
        // "N个小时后" / "N小时后" / "N个小时以后"
        Regex("(\\d+)\\s*个?小时(?:以)?后") to { m: MatchResult -> m.groupValues[1].toLong() * 60 },
        // "N分钟后" / "N分钟以后"
        Regex("(\\d+)\\s*分钟(?:以)?后") to { m: MatchResult -> m.groupValues[1].toLong() },
    )

    /**
     * 解析用户输入中的相对时间
     *
     * @param userText 用户原始输入
     * @param nowMillis 当前时刻 (epoch millis)
     * @param zoneId 本地时区
     * @return "YYYY-MM-DD HH:mm" 格式的绝对时间，或 null（无匹配）
     */
    fun resolve(userText: String, nowMillis: Long, zoneId: ZoneId): String? {
        for ((pattern, extractor) in PATTERNS) {
            val match = pattern.find(userText)
            if (match != null) {
                val offsetMinutes = extractor(match)
                val resolved = Instant.ofEpochMilli(nowMillis)
                    .plusSeconds(offsetMinutes * 60)
                return FORMATTER.format(resolved.atZone(zoneId))
            }
        }
        return null
    }

    /**
     * 生成 LLM 提示注入文本
     *
     * @return 提示文本，或 null
     */
    fun buildHint(userText: String, nowMillis: Long, zoneId: ZoneId): String? {
        val resolved = resolve(userText, nowMillis, zoneId) ?: return null
        // 提取用户原文中匹配的相对时间表达
        val matchedExpr = PATTERNS.firstNotNullOfOrNull { (p, _) -> p.find(userText)?.value }
        return "## 相对时间已解析\n\"$matchedExpr\" = $resolved，请直接使用此时间作为 startTime"
    }
}
