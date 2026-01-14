// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/params/TranscriptionLaneSelector.kt
// 模块：:data:ai-core
// 说明：转写提供方选择（V7+ 仅 Tingwu）
// 作者：创建于 2025-12-22
package com.smartsales.data.aicore.params

import java.util.Locale

data class TranscriptionLaneDecision(
    val requestedProvider: String,
    val selectedProvider: String,
    val disabledReason: String?,
) {
    val usedFallback: Boolean = selectedProvider != requestedProvider
}

object TranscriptionLaneSelector {
    fun resolve(snapshot: AiParaSettingsSnapshot): TranscriptionLaneDecision {
        val transcription = snapshot.transcription
        val requested = transcription.provider.trim().uppercase(Locale.US)
        val normalizedRequested = if (requested.isBlank()) "" else requested

        return when (normalizedRequested) {
            TRANSCRIPTION_PROVIDER_TINGWU -> TranscriptionLaneDecision(
                requestedProvider = TRANSCRIPTION_PROVIDER_TINGWU,
                selectedProvider = TRANSCRIPTION_PROVIDER_TINGWU,
                disabledReason = null,
            )
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
                )
            }
        }
    }
}
