// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/enhancer/EnhancerIntegration.kt
// Module: :data:ai-core
// Summary: Lattice box for post-Tingwu transcript enhancement
// Author: created on 2026-01-15

package com.smartsales.data.aicore.tingwu.enhancer

import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.posttingwu.EnhancerInput
import com.smartsales.data.aicore.posttingwu.EnhancerUtterance
import com.smartsales.data.aicore.posttingwu.PostTingwuTranscriptEnhancer
import com.smartsales.data.aicore.posttingwu.applyEnhancerOutput
import com.smartsales.data.aicore.posttingwu.renderEnhancedMarkdown
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EnhancerIntegration: Lattice interface for post-transcription enhancement.
 *
 * Responsibility: Apply AI enhancement to diarized segments when enabled.
 */
interface EnhancerIntegration {
    /**
     * Run enhancement if enabled, otherwise return fallback.
     */
    suspend fun enhanceIfEnabled(
        jobId: String,
        transcription: TingwuTranscription?,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>,
        fallback: String
    ): String
}

/**
 * RealEnhancerIntegration: Production implementation.
 */
@Singleton
class RealEnhancerIntegration @Inject constructor(
    private val tingwuSettings: AiParaSettingsProvider,
    private val postTingwuTranscriptEnhancer: PostTingwuTranscriptEnhancer
) : EnhancerIntegration {

    override suspend fun enhanceIfEnabled(
        jobId: String,
        transcription: TingwuTranscription?,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>,
        fallback: String
    ): String {
        val settings = tingwuSettings.snapshot().tingwu.postTingwuEnhancer
        if (!settings.enabled) return fallback
        
        val utterances = buildEnhancerUtterances(diarizedSegments, transcription)
        if (utterances.isEmpty()) return fallback
        
        val output = runCatching {
            postTingwuTranscriptEnhancer.enhance(
                EnhancerInput(
                    jobId = jobId,
                    language = transcription?.language,
                    utterances = utterances
                )
            )
        }.onFailure {
            AiCoreLogger.w(TAG, "转写增强调用失败：${it.message}")
        }.getOrNull() ?: return fallback
        
        val lines = applyEnhancerOutput(
            utterances = utterances,
            output = output,
            baseSpeakerLabels = speakerLabels
        )
        if (lines.isEmpty()) return fallback
        return renderEnhancedMarkdown(lines)
    }

    private fun buildEnhancerUtterances(
        diarizedSegments: List<DiarizedSegment>,
        transcription: TingwuTranscription?
    ): List<EnhancerUtterance> {
        if (diarizedSegments.isNotEmpty()) {
            return diarizedSegments.sortedBy { it.startMs }.mapIndexed { index, segment ->
                EnhancerUtterance(
                    index = index,
                    startMs = segment.startMs,
                    endMs = segment.endMs,
                    speakerId = segment.speakerId,
                    text = segment.text
                )
            }
        }
        val segments = transcription?.segments.orEmpty()
        if (segments.isNotEmpty()) {
            return segments
                .sortedBy { it.start ?: 0.0 }
                .mapIndexed { index, segment ->
                    EnhancerUtterance(
                        index = index,
                        startMs = segment.start?.times(1000)?.toLong(),
                        endMs = segment.end?.times(1000)?.toLong(),
                        speakerId = segment.speaker,
                        text = segment.text?.trim().orEmpty()
                    )
                }
        }
        return emptyList()
    }

    companion object {
        private const val TAG = "EnhancerIntegration"
    }
}

/**
 * FakeEnhancerIntegration: Test double for EnhancerIntegration.
 */
class FakeEnhancerIntegration : EnhancerIntegration {
    var stubResult: String? = null
    val calls = mutableListOf<String>()

    override suspend fun enhanceIfEnabled(
        jobId: String,
        transcription: TingwuTranscription?,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>,
        fallback: String
    ): String {
        calls.add(jobId)
        return stubResult ?: fallback
    }

    fun reset() {
        stubResult = null
        calls.clear()
    }
}
