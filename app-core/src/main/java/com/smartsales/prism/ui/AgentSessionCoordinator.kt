package com.smartsales.prism.ui

import android.util.Log
import com.smartsales.core.context.ChatTurn
import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.pipeline.AgentActivityController
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.repository.HistoryRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class AgentSessionCoordinator(
    private val historyRepository: HistoryRepository,
    private val contextBuilder: ContextBuilder,
    private val audioRepository: AudioRepository,
    private val activityController: AgentActivityController,
    private val bridge: AgentUiBridge
) {

    fun startNewSession(scope: CoroutineScope) {
        persistCurrentMessages()

        bridge.setHistory(emptyList())
        bridge.setSessionTitle(DEFAULT_AGENT_SESSION_TITLE)
        bridge.setInputText("")
        bridge.setUiState(UiState.Idle)
        bridge.setErrorMessage(null)
        bridge.setTaskBoardItems(emptyList())
        activityController.reset()
        contextBuilder.resetSession()
        bridge.setCurrentSessionId(null)
        bridge.getSessionBootstrapJob()?.cancel()

        bridge.setSessionBootstrapJob(
            scope.launch {
                val sessionId = historyRepository.createSession(
                    DEFAULT_AGENT_SESSION_TITLE,
                    DEFAULT_AGENT_SESSION_SUMMARY
                )
                contextBuilder.loadSession(sessionId, emptyList())
                bridge.setCurrentSessionId(sessionId)
            }
        )
    }

    fun switchSession(
        scope: CoroutineScope,
        sessionId: String,
        triggerAutoRename: Boolean = false
    ) {
        persistCurrentMessages()
        scope.launch {
            val messages = historyRepository.getMessages(sessionId)
            val session = historyRepository.getSession(sessionId)

            val chatTurns = messages.map { msg ->
                when (msg) {
                    is ChatMessage.User -> ChatTurn("user", msg.content)
                    is ChatMessage.Ai -> ChatTurn(
                        "assistant",
                        (msg.uiState as? UiState.Response)?.content ?: ""
                    )
                }
            }
            contextBuilder.loadSession(sessionId, chatTurns)

            session?.linkedAudioId?.let { loadLinkedAudioContext(it) }

            bridge.setCurrentSessionId(sessionId)
            bridge.setHistory(messages)
            bridge.setSessionTitle(session?.clientName ?: FALLBACK_SWITCHED_SESSION_TITLE)
            bridge.setUiState(UiState.Idle)
            bridge.setInputText("")
            activityController.reset()
        }
    }

    suspend fun ensureActiveSessionReady(): String {
        bridge.getSessionBootstrapJob()?.join()
        bridge.getCurrentSessionId()?.let { return it }

        val sessionId = historyRepository.createSession(
            DEFAULT_AGENT_SESSION_TITLE,
            DEFAULT_AGENT_SESSION_SUMMARY
        )
        contextBuilder.loadSession(sessionId, emptyList())
        bridge.setCurrentSessionId(sessionId)
        return sessionId
    }

    suspend fun appendUserTurn(content: String) {
        ensureActiveSessionReady()
        contextBuilder.recordUserMessage(content)
        bridge.setHistory(
            bridge.getHistory() + ChatMessage.User(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                content = content
            )
        )
    }

    suspend fun appendAssistantTurn(uiState: UiState) {
        ensureActiveSessionReady()
        sessionMemoryText(uiState)?.let { content ->
            contextBuilder.recordAssistantMessage(content)
        }
        bridge.setHistory(
            bridge.getHistory() + ChatMessage.Ai(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                uiState = uiState
            )
        )
    }

    suspend fun updateSessionTitle(newTitle: String) {
        val title = newTitle.trim()
        if (title.isBlank()) return

        bridge.setSessionTitle(title)

        val sessionId = bridge.getCurrentSessionId() ?: return
        val session = historyRepository.getSession(sessionId) ?: return
        historyRepository.renameSession(sessionId, title, session.summary)
    }

    private suspend fun loadLinkedAudioContext(audioId: String) {
        try {
            val artifacts = audioRepository.getArtifacts(audioId) ?: return
            val payload = buildString {
                artifacts.smartSummary?.summary?.takeIf { it.isNotBlank() }?.let {
                    append("**系统摘要总结**\n")
                    append(it)
                    append("\n\n")
                }
                val transcript = artifacts.transcriptMarkdown ?: ""
                if (transcript.isNotBlank()) {
                    append("**详细转写原文**\n")
                    append(transcript)
                }
            }
            contextBuilder.loadDocumentContext(payload)
        } catch (e: Exception) {
            Log.e(AGENT_VM_LOG_TAG, "Failed to load documentContext", e)
        }
    }

    private fun sessionMemoryText(uiState: UiState): String? {
        return when (uiState) {
            is UiState.Response -> uiState.content
            is UiState.AwaitingClarification -> buildString {
                append(uiState.question)
                if (uiState.candidates.isNotEmpty()) {
                    append("\n候选项: ")
                    append(uiState.candidates.joinToString(" / ") { it.displayName })
                }
            }
            UiState.BadgeDelegationHint -> "该请求需要通过胸牌端继续完成。"
            is UiState.Error -> uiState.message
            else -> null
        }
    }

    private fun persistCurrentMessages() {
        // Architecture update: Kernel (ContextBuilder) now auto-syncs chat turns to HistoryRepository
        // via KernelWriteBack when IntentOrchestrator generates responses.
        // We no longer double-write from the UI layer.
    }
}

internal const val DEFAULT_AGENT_SESSION_TITLE = "新对话"
internal const val DEFAULT_AGENT_SESSION_SUMMARY = "新会话"
internal const val FALLBACK_SWITCHED_SESSION_TITLE = "对话"
internal const val AGENT_VM_LOG_TAG = "AgentVM"
