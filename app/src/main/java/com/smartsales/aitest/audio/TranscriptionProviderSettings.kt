// 文件：app/src/main/java/com/smartsales/aitest/audio/TranscriptionProviderSettings.kt
// 模块：:app
// 说明：保存当前选择的转写提供方（内存态，供调试壳快速切换）
// 作者：创建于 2025-12-15
package com.smartsales.aitest.audio

/**
 * MVP 阶段：仅保存在内存，不写入 DataStore。
 * 默认使用 XFyun，满足本里程碑“展示讯飞结果”的目标。
 */
object TranscriptionProviderSettings {
    @Volatile
    var current: TranscriptionProvider = TranscriptionProvider.XFYUN
}

