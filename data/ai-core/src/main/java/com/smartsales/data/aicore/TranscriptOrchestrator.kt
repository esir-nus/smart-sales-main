package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
// 模块：:data:ai-core
// 说明：转写元数据协同器，封装说话人标签等结构化写入
// 作者：创建于 2025-12-06

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.metahub.TranscriptSource
import com.smartsales.core.util.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class TranscriptMetadataRequest(
    val transcriptId: String,
    val sessionId: String?,
    val diarizedSegments: List<DiarizedSegment>,
    val speakerLabels: Map<String, String>,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 负责根据转写片段生成转写元数据，并写入 MetaHub。
 */
interface TranscriptOrchestrator {
    suspend fun inferTranscriptMetadata(request: TranscriptMetadataRequest): TranscriptMetadata?
}

@Singleton
class RealTranscriptOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val dispatchers: DispatcherProvider
) : TranscriptOrchestrator {

    override suspend fun inferTranscriptMetadata(
        request: TranscriptMetadataRequest
    ): TranscriptMetadata? = withContext(dispatchers.default) {
        val speakerMap = request.speakerLabels.mapValues { (_, label) ->
            SpeakerMeta(
                displayName = label.takeIf { it.isNotBlank() },
                role = null,
                confidence = 0.8f
            )
        }
        val metadata = TranscriptMetadata(
            transcriptId = request.transcriptId,
            sessionId = request.sessionId,
            speakerMap = speakerMap,
            source = TranscriptSource.TINGWU,
            createdAt = request.createdAt,
            diarizedSegmentsCount = request.diarizedSegments.size
        )
        metaHub.upsertTranscript(metadata)
        metadata
    }
}
