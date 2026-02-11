package com.smartsales.prism.domain.scheduler

/**
 * 级联提醒视觉层级
 *
 * 根据 offset 决定提醒的样式和行为：
 * - EARLY: 提前预警（标准横幅，无全屏）
 * - FINAL: 最后提醒（升级横幅，带贪睡）
 * - DEADLINE: 到期闹钟（全屏 Activity + 持续振动）
 */
enum class CascadeTier {
    EARLY,    // ≥5m — ⏰ 标准横幅
    FINAL,    // 1m  — ⚠️ 升级横幅
    DEADLINE; // 0m  — 🚨 全屏闹钟

    companion object {
        fun from(offsetMinutes: Int): CascadeTier = when (offsetMinutes) {
            0    -> DEADLINE
            1    -> FINAL
            else -> EARLY
        }
    }
}
