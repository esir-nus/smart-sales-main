package com.smartsales.prism.domain.scheduler

/**
 * 紧急程度 — LLM 分类后由 Kotlin 决定级联提醒间隔
 *
 * 替代旧的 ReminderType + TaskTypeHint 双枚举方案。
 * 每个等级自带级联配置、冲突策略和时长默认值。
 */
enum class UrgencyLevel {
    /** 赶飞机、签约、面试 — 错过=不可逆损失 */
    L1_CRITICAL,

    /** 会议、电话、汇报 — 错过=影响他人 */
    L2_IMPORTANT,

    /** 回邮件、买东西、日常 — 错过=无大碍 */
    L3_NORMAL,

    /** 喝水、站起来走走 — 即时单次提醒（0m） */
    FIRE_OFF;

    companion object {
        /**
         * 根据紧急程度生成确定性级联提醒偏移量
         * LLM 分类 → Kotlin 决定偏移量（纯函数，无 LLM 参与）
         */
        fun buildCascade(level: UrgencyLevel): List<String> = when (level) {
            L1_CRITICAL  -> listOf("-1h", "-10m", "0m")
            L2_IMPORTANT -> listOf("-30m", "0m")
            L3_NORMAL    -> listOf("0m")
            FIRE_OFF     -> listOf("0m")
        }

        /**
         * 将级联偏移字符串解析为毫秒值
         * e.g. "-1h" → 3600000, "-30m" → 1800000
         */
        fun parseCascadeOffset(offset: String): Long {
            // "0m" = 到点提醒，偏移量为0
            if (offset == "0m") return 0
            val regex = Regex("-(\\d+)([hm])")
            val match = regex.matchEntire(offset) ?: return 0
            val value = match.groupValues[1].toLong()
            val unit = match.groupValues[2]
            return when (unit) {
                "h" -> value * 60 * 60 * 1000
                "m" -> value * 60 * 1000
                else -> 0
            }
        }
    }
}
