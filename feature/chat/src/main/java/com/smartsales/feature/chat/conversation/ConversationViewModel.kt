package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.core.ChatHistoryItem
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.stream.ChatStreamCoordinator
import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ConversationViewModel: Thin wrapper around ConversationReducer.
 * 
 * Platform Boundary layer connecting pure Reducer to Android streaming.
 * 
 * P3.1.B2: Added streaming side effects with caller-passed scope.
 * 
 * Note: NOT an @HiltViewModel (injected into HomeScreenViewModel).
 */
class ConversationViewModel @Inject constructor(
    private val homeOrchestrator: HomeOrchestrator
) {
    
    /**
     * Migration bridge: callback to sync messages to HomeUiState.
     * TODO(P3.8): Remove when HomeScreenViewModel is deleted.
     */
    var onMessagesChanged: ((List<ChatMessageUi>) -> Unit)? = null
    
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()
    
    // Constructed inline like HomeScreenViewModel pattern
    private val chatStreamCoordinator = ChatStreamCoordinator { req -> 
        homeOrchestrator.streamChat(req) 
    }
    
    /**
     * Dispatch intent to Reducer with optional scope for side effects.
     * 
     * P3.1.B2: SendMessage triggers streaming via ChatStreamCoordinator.
     * 
     * @param scope CoroutineScope from caller (HomeScreenViewModel.viewModelScope)
     * @param context SendContext for building requests (sessionId, persona, etc.)
     */
    fun dispatch(
        intent: ConversationIntent, 
        scope: CoroutineScope? = null,
        context: SendContext? = null
    ) {
        _state.value = ConversationReducer.reduce(_state.value, intent)
        
        // Migration bridge: sync messages to HomeUiState
        onMessagesChanged?.invoke(_state.value.messages)
        
        // P3.1.B2: Side effect handling
        when (intent) {
            is ConversationIntent.SendMessage -> handleSendEffect(scope, context)
            else -> Unit
        }
    }
    
    private fun handleSendEffect(scope: CoroutineScope?, context: SendContext?) {
        if (!_state.value.isSending) return
        requireNotNull(scope) { "Scope required for SendMessage" }
        requireNotNull(context) { "SendContext required for SendMessage" }
        
        val state = _state.value
        val userMessage = state.messages.lastOrNull { it.role == ChatMessageRole.USER }
            ?: return
        
        // Create assistant placeholder
        val assistantId = generateMessageId()
        val assistantPlaceholder = ChatMessageUi(
            id = assistantId,
            role = ChatMessageRole.ASSISTANT,
            content = "",
            timestampMillis = System.currentTimeMillis(),
            isStreaming = true
        )
        
        // Add placeholder to messages
        dispatch(ConversationIntent.MessageReceived(assistantPlaceholder))
        
        // Build request (basic: no skill routing, no audio context)
        val request = buildBasicChatRequest(state, context)
        
        // Start streaming
        chatStreamCoordinator.start(
            scope = scope,
            request = request,
            onDelta = { token ->
                dispatch(ConversationIntent.StreamDelta(assistantId, token))
            },
            onCompleted = { fullText ->
                dispatch(ConversationIntent.StreamCompleted(assistantId, fullText))
            },
            onError = { throwable ->
                dispatch(ConversationIntent.StreamError(throwable.message ?: "Unknown error"))
            }
        )
    }
    
    private fun buildBasicChatRequest(
        state: ConversationState,
        context: SendContext
    ): ChatRequest {
        val history = state.messages.map { ui ->
            ChatHistoryItem(
                role = if (ui.role == ChatMessageRole.USER) ChatRole.USER else ChatRole.ASSISTANT,
                content = ui.content
            )
        }
        
        val userMessage = state.messages.lastOrNull { it.role == ChatMessageRole.USER }
            ?: throw IllegalStateException("No user message to send")
        
        return ChatRequest(
            sessionId = context.sessionId,
            userMessage = userMessage.content,
            quickSkillId = null,  // P3.1.B2: Basic send only
            audioContextSummary = null,
            history = history,
            isFirstAssistantReply = context.isFirstAssistant,
            persona = context.salesPersona
        )
    }
    
    private var messageIdCounter = 0
    private fun generateMessageId(): String = "msg_${System.currentTimeMillis()}_${messageIdCounter++}"
}
