// File: feature/chat/src/main/java/com/smartsales/domain/chat/ChatCoordinatorImpl.kt
// Module: :feature:chat
// Summary: ChatCoordinator implementation - orchestrates chat streaming
// Author: created on 2026-01-07

package com.smartsales.domain.chat

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.domain.error.ChatError
import com.smartsales.domain.error.toChatError
import com.smartsales.domain.stream.StreamingCoordinator
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatHistoryItem
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ChatCoordinatorImpl: Domain-layer orchestrator for chat and streaming.
 *
 * Responsibilities:
 * - Build chat requests from domain parameters
 * - Orchestrate streaming via StreamingCoordinator
 * - Extract and persist metadata (first reply only)
 * - Emit events for UI consumption
 *
 * Design:
 * - Zero Android imports (portable)
 * - Event-based architecture (Flow<ChatEvent>)
 * - Stateless except for active stream tracking
 */
@Singleton
class ChatCoordinatorImpl @Inject constructor(
    private val metaHub: MetaHub,
    private val homeOrchestrator: HomeOrchestrator,
) : ChatCoordinator {

    // Event emission
    private val _chatEvents = MutableSharedFlow<ChatEvent>(replay = 0, extraBufferCapacity = 64)
    override val chatEvents: Flow<ChatEvent> = _chatEvents.asSharedFlow()

    // Active stream tracking
    private var activeScope: CoroutineScope? = null
    
    // Streaming coordinator created inline like ConversationViewModel
    private val streamingCoordinator = StreamingCoordinator { req ->
        homeOrchestrator.streamChat(req)
    }

    override fun sendMessage(params: SendMessageParams) {
        val scope = activeScope ?: return

        // Build ChatRequest from domain params
        val request = buildChatRequest(params)
        
        // Generate assistant message ID
        val assistantId = "msg-${System.currentTimeMillis()}"

        scope.launch {
            try {
                // Emit StreamStarted
                _chatEvents.emit(ChatEvent.StreamStarted(assistantId))

                // Start streaming
                streamingCoordinator.start(
                    scope = scope,
                    request = request,
                    onDelta = { token ->
                        _chatEvents.emit(ChatEvent.StreamDelta(assistantId, token))
                    },
                    onCompleted = { rawFullText ->
                        handleStreamCompleted(
                            params = params,
                            assistantId = assistantId,
                            rawFullText = rawFullText,
                            isSmartAnalysis = false
                        )
                    },
                    onError = { throwable ->
                        val error = ChatError.NetworkError(throwable, throwable.message)
                        _chatEvents.emit(ChatEvent.StreamError(assistantId, error))
                    }
                    // TODO(Phase 2): Add V1 retry logic with maxRetries + completionEvaluator
                )
            } catch (e: Exception) {
                // Catch any exception (network failure, etc.) and emit error instead of crash
                val error = e.toChatError()
                _chatEvents.emit(ChatEvent.StreamError(assistantId, error))
            }
        }
    }

    override fun sendSmartAnalysis(params: SmartAnalysisParams) {
        val scope = activeScope ?: return
        
        scope.launch {
            // Build simple message for SmartAnalysis
            val allContent = params.chatHistory.joinToString("\n") { it.content }
            if (allContent.isBlank()) {
                val error = ChatError.ValidationError("内容太少，无法智能分析，请先粘贴对话或纪要再试。")
                _chatEvents.emit(ChatEvent.StreamError("error_${System.currentTimeMillis()}", error))
                return@launch
            }
            
            // Build user message
            val userMessage = ChatMessageBuilder.buildSmartAnalysisUserMessage(
                mainContent = allContent.take(2000),
                context = null,
                goal = params.goal.ifBlank { "通用分析" }
            )
            
            val assistantId = "smart_${System.currentTimeMillis()}"
            _chatEvents.emit(ChatEvent.StreamStarted(assistantId))
            
            // Build request
            val request = com.smartsales.feature.chat.core.ChatRequest(
                sessionId = params.sessionId,
                userMessage = userMessage,
                quickSkillId = "SMART_ANALYSIS",
                audioContextSummary = null,
                history = params.chatHistory.map {
                    com.smartsales.feature.chat.core.ChatHistoryItem(
                        role = if (it.role == MessageRole.USER) com.smartsales.feature.chat.core.ChatRole.USER else com.smartsales.feature.chat.core.ChatRole.ASSISTANT,
                        content = it.content
                    )
                },
                isFirstAssistantReply = false,
                persona = params.persona
            )
            
            // Stream
            streamingCoordinator.start(
                scope = scope,
                request = request,
                onDelta = { token ->
                    _chatEvents.emit(ChatEvent.StreamDelta(assistantId, token))
                },
                onCompleted = { fullText ->
                    // Simple display text extraction
                    val displayText = ChatPublisher.extractDisplayText(fullText)
                    _chatEvents.emit(ChatEvent.StreamCompleted(
                        result = ChatCompletionResult(
                            assistantId = assistantId,
                            rawFullText = fullText,
                            displayText = displayText,
                            metadata = null,
                            titleCandidate = null,
                            isSmartAnalysis = true
                        )
                    ))
                },
                onError = { throwable ->
                    val error = ChatError.NetworkError(throwable, throwable.message)
                    _chatEvents.emit(ChatEvent.StreamError(assistantId, error))
                }
            )
        }
    }


    override fun resetStream() {
        // Reset active stream state (for session switch or error recovery)
        activeScope = null
    }

    /**
     * Internal: Set the coroutine scope for streaming.
     * Called by HomeViewModel during initialization.
     */
    fun setScope(scope: CoroutineScope) {
        activeScope = scope
    }

    // ====== Private Helpers ======

    private fun buildChatRequest(params: SendMessageParams): ChatRequest {
        val history = params.chatHistory.map { msg ->
            ChatHistoryItem(
                role = when (msg.role) {
                    MessageRole.USER -> ChatRole.USER
                    MessageRole.ASSISTANT -> ChatRole.ASSISTANT
                },
                content = msg.content
            )
        }

        return ChatRequest(
            sessionId = params.sessionId,
            userMessage = params.userMessage,
            quickSkillId = params.skillId,
            audioContextSummary = params.audioContext,
            history = history,
            isFirstAssistantReply = params.isFirstAssistantReply,
            persona = params.persona
        )
    }

    private suspend fun handleStreamCompleted(
        params: SendMessageParams,
        assistantId: String,
        rawFullText: String,
        isSmartAnalysis: Boolean
    ) {
        // Extract display text using ChatPublisher
        val displayText = ChatPublisher.extractDisplayText(rawFullText)

        // Extract metadata for first GENERAL reply only
        val metadata = if (params.isFirstAssistantReply && params.skillId == null && !isSmartAnalysis) {
            extractAndPersistMetadata(params.sessionId, rawFullText)
        } else {
            null
        }

        // Extract title candidate from <Rename> tag
        val titleCandidate = parseRenameCandidate(rawFullText, isSmartAnalysis)

        // Build completion result
        val result = ChatCompletionResult(
            assistantId = assistantId,
            rawFullText = rawFullText,
            displayText = displayText,
            metadata = metadata,
            titleCandidate = titleCandidate,
            isSmartAnalysis = isSmartAnalysis
        )

        // Emit StreamCompleted
        _chatEvents.emit(ChatEvent.StreamCompleted(result))
    }

    private suspend fun extractAndPersistMetadata(
        sessionId: String,
        rawFullText: String
    ): SessionMetadata? {
        // Extract channels (Visible2User + Metadata)
        val channels = ChatPublisher.extractChannels(rawFullText)
        val metadataJson = channels.metadataJson

        // Try parsing metadata from <Metadata> tag first, then fallback to trailing JSON
        val candidates = buildList {
            metadataJson?.takeIf { it.isNotBlank() }?.let { add(it) }
            MetadataParser.findLastJsonBlock(rawFullText)?.text?.let { tail ->
                if (tail != metadataJson) add(tail)
            }
        }

        val parsed = candidates.firstNotNullOfOrNull { 
            MetadataParser.parseGeneralChatMetadata(it, sessionId) 
        }

        val metadata = parsed?.takeIf { it.hasMeaningfulGeneralFields() } ?: return null

        // Add analysis source and timestamp
        val patch = metadata.copy(
            latestMajorAnalysisSource = AnalysisSource.GENERAL_FIRST_REPLY,
            latestMajorAnalysisAt = System.currentTimeMillis()
        )

        // Merge with existing metadata and persist
        val existing = runCatching { metaHub.getSession(sessionId) }.getOrNull()
        val merged = existing?.mergeWith(patch) ?: patch

        runCatching { metaHub.upsertSession(merged) }
            .onFailure { 
                // Log error but don't fail the entire flow
                // TODO: Add proper logging
            }

        return merged
    }

    private fun parseRenameCandidate(raw: String, isSmartAnalysis: Boolean): TitleCandidate? {
        val blockRegex = Regex("<\\s*Rename\\s*>([\\s\\S]*?)</\\s*Rename\\s*>", RegexOption.IGNORE_CASE)
        val block = blockRegex.find(raw)?.groupValues?.getOrNull(1)?.trim() ?: return null

        val nameRegex = Regex("<\\s*Name\\s*>([\\s\\S]*?)</\\s*Name\\s*>", RegexOption.IGNORE_CASE)
        val titleRegex = Regex("<\\s*Title6\\s*>([\\s\\S]*?)</\\s*Title6\\s*>", RegexOption.IGNORE_CASE)

        val name = nameRegex.find(block)?.groupValues?.getOrNull(1)?.trim()
        val title6 = titleRegex.find(block)?.groupValues?.getOrNull(1)?.trim()

        // Reject placeholder ellipsis (LLM sometimes outputs "..." when uncertain)
        fun String?.isValidTitlePart() = !this.isNullOrBlank() && this != "..." && this != "…"
        val cleanName = name?.takeIf { it.isValidTitlePart() }
        val cleanTitle = title6?.takeIf { it.isValidTitlePart() }

        if (cleanName == null && cleanTitle == null) return null

        return TitleCandidate(
            name = cleanName,
            title6 = cleanTitle,
            source = if (isSmartAnalysis) TitleSource.SMART_ANALYSIS else TitleSource.GENERAL,
            createdAt = System.currentTimeMillis()
        )
    }
}

// Extension function for metadata validation
private fun SessionMetadata.hasMeaningfulGeneralFields(): Boolean {
    return !mainPerson.isNullOrBlank() ||
           !shortSummary.isNullOrBlank() ||
           !summaryTitle6Chars.isNullOrBlank() ||
           !location.isNullOrBlank()
}
