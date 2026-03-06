package com.smartsales.prism.domain.scheduler

/**
 * 级联提醒视觉层级
 *
 * 根据 offset 决定提醒的样式和行为：
 * - EARLY: 提前预警（标准横幅，无全屏，尊重 DND）
 * - DEADLINE: 到期闹钟（全屏 Activity + 持续振动 + 绕过 DND）
 */
enum class CascadeTier {
    EARLY,    // >0m — ⏰ 标准横幅，尊重 DND
    DEADLINE; // 0m  — 🚨 全屏闹钟，绕过 DND

    companion object {
        fun from(offsetMinutes: Int): CascadeTier = when (offsetMinutes) {
            0    -> DEADLINE
            else -> EARLY
        }
    }
}
