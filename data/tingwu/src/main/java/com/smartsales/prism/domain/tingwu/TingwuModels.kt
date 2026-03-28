package com.smartsales.prism.domain.tingwu

import kotlinx.serialization.Serializable
import java.io.File

/**
 * 提交给 Tingwu Pipeline 的参数请求。
 */
data class TingwuRequest(
    val audioAssetName: String,
    val language: String = "zh-CN",
    val ossObjectKey: String? = null,
    val fileUrl: String? = null,
    val diarizationEnabled: Boolean = true,
    val sessionId: String? = null,
    val customPromptEnabled: Boolean = false,
    val customPromptName: String? = null,
    val customPromptText: String? = null,
    val durationMs: Long? = null,
    val audioFilePath: File? = null
)

/**
 * 转写流程的密封状态。
 */
sealed class TingwuJobState {
    data object Idle : TingwuJobState()
    
    data class InProgress(
        val jobId: String,
        val progressPercent: Int,
        val statusLabel: String? = null,
        val artifacts: TingwuJobArtifacts? = null
    ) : TingwuJobState()

    data class Completed(
        val jobId: String,
        val transcriptMarkdown: String,
        val artifacts: TingwuJobArtifacts? = null,
        val statusLabel: String? = null
    ) : TingwuJobState()

    data class Failed(
        val jobId: String,
        val reason: String,
        val errorCode: String? = null
    ) : TingwuJobState()
}

/**
 * 转写完成后的丰富智能结构数据，用于直接序列化至 SSD 并供下游分析层引用。
 */
@Serializable
data class TingwuJobArtifacts(
    val outputMp3Path: String? = null,
    val outputMp4Path: String? = null,
    val outputThumbnailPath: String? = null,
    val outputSpectrumPath: String? = null,
    val resultLinks: List<TingwuResultLink> = emptyList(),
    val transcriptionUrl: String? = null,
    val autoChaptersUrl: String? = null,
    val customPromptUrl: String? = null,
    val extraResultUrls: Map<String, String> = emptyMap(),
    val meetingAssistanceRaw: String? = null,
    val keywords: List<String> = emptyList(),
    val transcriptMarkdown: String? = null,
    val chapters: List<TingwuChapter>? = null,
    val smartSummary: TingwuSmartSummary? = null,
    val diarizedSegments: List<DiarizedSegment>? = null,
    val recordingOriginDiarizedSegments: List<DiarizedSegment>? = null,
    val speakerLabels: Map<String, String> = emptyMap(),
)

@Serializable
data class TingwuSpeakerSummary(
    val name: String? = null,
    val summary: String
)

@Serializable
data class TingwuQuestionAnswer(
    val question: String,
    val answer: String
)

@Serializable
data class TingwuResultLink(
    val label: String,
    val url: String
)

@Serializable
data class DiarizedSegment(
    val speakerId: String?,
    val speakerIndex: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

@Serializable
data class TingwuArtifactBundle(
    val speakerLabels: Map<String, String>,
    val diarizedSegments: List<DiarizedSegment>,
    val chapters: List<TingwuChapter>?,
    val smartSummary: TingwuSmartSummary?,
    val meetingAssistanceRaw: String? = null,
    val resultLinks: Map<String, String>,
    val outputSpectrumPath: String? = null // URL to the generated audio wave image
)

@Serializable
data class TingwuChapter(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val summary: String? = null
)

@Serializable
data class TingwuSmartSummary(
    val summary: String? = null,
    val keyPoints: List<String> = emptyList(),
    val speakerSummaries: List<TingwuSpeakerSummary> = emptyList(),
    val questionAnswers: List<TingwuQuestionAnswer> = emptyList()
)
