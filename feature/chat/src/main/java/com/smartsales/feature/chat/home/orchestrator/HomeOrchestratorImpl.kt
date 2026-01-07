package com.smartsales.feature.chat.home.orchestrator

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
// 模块：:feature:chat
// 说明：Home 层 Orchestrator 实现，当前直通聊天服务并写入会话元数据
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.RiskLevel
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.SessionStage
import com.smartsales.domain.analysis.SmartAnalysisParser
import com.smartsales.domain.analysis.SmartAnalysisResult
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.QuickSkillId
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

private const val SMART_ANALYSIS_FAILURE_MESSAGE = "本次智能分析暂时不可用，请稍后重试。"

@Singleton
class HomeOrchestratorImpl @Inject constructor(
    private val aiChatService: AiChatService,
    private val metaHub: MetaHub
) : HomeOrchestrator {
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
        return flow {
            aiChatService.streamChat(request).collect { event ->
                if (event is ChatStreamEvent.Completed && shouldParseMetadata(request)) {
                    val result = buildSmartAnalysisResult(request, event.fullText)
                    result.metadata?.let { runCatching { metaHub.upsertSession(it) } }
                    emit(ChatStreamEvent.Completed(result.markdown))
                } else {
                    emit(event)
                }
            }
        }
    }

    private suspend fun buildSmartAnalysisResult(
        request: ChatRequest,
        assistantText: String
    ): SmartAnalysisResult {
        val source = resolveAnalysisSource(request)
        return SmartAnalysisParser.parseAndBuild(
            rawText = assistantText,
            sessionId = request.sessionId,
            metaHub = metaHub,
            source = source
        )
    }

    private fun shouldParseMetadata(request: ChatRequest): Boolean {
        val skillId = request.quickSkillId ?: return false
        return skillId == QuickSkillId.SMART_ANALYSIS.name
    }


    private fun resolveAnalysisSource(request: ChatRequest): AnalysisSource? {
        val mode = request.quickSkillId
        return when {
            mode == QuickSkillId.SMART_ANALYSIS.name -> AnalysisSource.SMART_ANALYSIS_USER
            mode == null && request.isFirstAssistantReply -> AnalysisSource.GENERAL_FIRST_REPLY
            else -> null
        }
    }
}

