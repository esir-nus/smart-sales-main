// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/params/TranscriptionLaneSelector.kt
// 模块：:data:ai-core
// 说明：转写提供方选择与禁用原因判定
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.params

import java.util.Locale

data class TranscriptionLaneDecision(
    val requestedProvider: String,
    val selectedProvider: String,
    val disabledReason: String?,
    val xfyunEnabledSetting: Boolean,
) {
    val usedFallback: Boolean = selectedProvider != requestedProvider
}

object TranscriptionLaneSelector {
    fun resolve(snapshot: AiParaSettingsSnapshot): TranscriptionLaneDecision {
        val transcription = snapshot.transcription
        val requested = transcription.provider.trim().uppercase(Locale.US)
        val normalizedRequested = if (requested.isBlank()) "" else requested
        val xfyunEnabled = transcription.xfyunEnabled

        return when (normalizedRequested) {
            TRANSCRIPTION_PROVIDER_TINGWU -> TranscriptionLaneDecision(
                requestedProvider = TRANSCRIPTION_PROVIDER_TINGWU,
                selectedProvider = TRANSCRIPTION_PROVIDER_TINGWU,
                disabledReason = null,
                xfyunEnabledSetting = xfyunEnabled,
            )
            TRANSCRIPTION_PROVIDER_XFYUN -> {
                if (xfyunEnabled) {
                    TranscriptionLaneDecision(
                        requestedProvider = TRANSCRIPTION_PROVIDER_XFYUN,
                        selectedProvider = TRANSCRIPTION_PROVIDER_XFYUN,
                        disabledReason = null,
                        xfyunEnabledSetting = true,
                    )
                } else {
                    // 重要：讯飞未显式开启时必须 fail-soft 回退 Tingwu，避免“试试看”触发异常与配额消耗。
                    TranscriptionLaneDecision(
                        requestedProvider = TRANSCRIPTION_PROVIDER_XFYUN,
                        selectedProvider = TRANSCRIPTION_PROVIDER_TINGWU,
                        disabledReason = "XFYUN_DISABLED_BY_SETTING",
                        xfyunEnabledSetting = false,
                    )
                }
            }
            else -> {
                // 重要：未知 provider 统一回退 Tingwu，确保默认链路稳定可用。
                val reason = if (normalizedRequested.isBlank()) {
                    "PROVIDER_EMPTY"
                } else {
                    "PROVIDER_UNKNOWN:$normalizedRequested"
                }
                TranscriptionLaneDecision(
                    requestedProvider = normalizedRequested.ifBlank { "UNKNOWN" },
                    selectedProvider = TRANSCRIPTION_PROVIDER_TINGWU,
                    disabledReason = reason,
                    xfyunEnabledSetting = xfyunEnabled,
                )
            }
        }
    }
}
