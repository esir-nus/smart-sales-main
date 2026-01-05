package com.smartsales.feature.chat.home.voiceprint

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/voiceprint/VoiceprintLabUiState.kt
// 模块：:feature:chat
// 说明：声纹开发者工具 UI 状态（已弃用，保留备用）
// 作者：创建于 2026-01-05

/**
 * 说明：声纹开发者工具（Voiceprint Lab）的 UI 状态（仅内存态）。
 *
 * **注意：此功能已弃用，仅保留备用。**
 *
 * 重要安全要求：
 * - 严禁在 ViewModel 内保存音频 base64（敏感数据）；base64 只由 UI 临时持有并在请求后清空。
 */
data class VoiceprintLabUiState(
    val registerInProgress: Boolean = false,
    val deleteInProgress: Boolean = false,
    val lastFeatureId: String? = null,
    val lastMessage: String? = null,
    val lastApiHost: String? = null,
    val lastApiPath: String? = null,
    val lastHttpCode: Int? = null,
    val lastBusinessCode: String? = null,
    val lastBusinessDesc: String? = null,
    // 仅用于提示当前设置概况；不包含 featureIds 明文。
    val enabledSetting: Boolean? = null,
    val configuredFeatureIdCount: Int? = null,
)
