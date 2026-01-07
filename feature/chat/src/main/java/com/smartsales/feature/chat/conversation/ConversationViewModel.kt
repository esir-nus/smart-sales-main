package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.core.ChatHistoryItem
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.domain.stream.StreamingCoordinator
import com.smartsales.domain.stream.CompletionDecision
import com.smartsales.feature.chat.core.publisher.ChatPublisher
import com.smartsales.feature.chat.core.publisher.ChatPublisherImpl
import com.smartsales.feature.chat.core.publisher.GeneralChatV1Finalizer
import com.smartsales.feature.chat.core.v1.V1GeneralCompletionEvaluator
import com.smartsales.feature.chat.core.v1.V1GeneralRetryPolicy
import com.smartsales.feature.chat.core.v1.V1GeneralRetryEffects
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
 * Note: NOT an @HiltViewModel (injected into HomeViewModel).
 */
class ConversationViewModel @Inject constructor(
    private val homeOrchestrator: HomeOrchestrator
) {
    
    /**
     * Migration bridge: callback to sync messages to HomeUiState.
     * TODO(P3.9): Remove when HomeViewModel is deleted.
     */
    var onMessagesChanged: ((List<ChatMessageUi>) -> Unit)? = null
    
    /**
     * P3.8: Migration bridge for SmartAnalysis state.
     * TODO(P3.9): Remove when HomeViewModel is deleted.
     */
    var onSmartAnalysisModeChanged: ((Boolean, String?) -> Unit)? = null
    
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()
    
    // Constructed inline like HomeViewModel pattern
    private val chatStreamCoordinator = StreamingCoordinator { req -> 
        homeOrchestrator.streamChat(req) 
    }
    
    // V1 infrastructure for retry support (pure Kotlin, no DI needed)
    private val chatPublisher: ChatPublisher = ChatPublisherImpl()
    private val v1Finalizer = GeneralChatV1Finalizer(chatPublisher)
    
    /**
     * Dispatch intent to Reducer with optional scope for side effects.
     * 
     * P3.1.B2: SendMessage triggers streaming via StreamingCoordinator.
     * 
     * @param scope CoroutineScope from caller (HomeViewModel.viewModelScope)
     * @param context SendContext for building requests (sessionId, persona, etc.)
     */
    fun dispatch(
        intent: ConversationIntent, 
        scope: CoroutineScope? = null,
        context: SendContext? = null,
        analysisContext: SmartAnalysisContext? = null
    ) {
        _state.value = ConversationReducer.reduce(_state.value, intent)
        
        // Migration bridge: sync messages to HomeUiState
        onMessagesChanged?.invoke(_state.value.messages)
        
        // P3.8: Migration bridge for SmartAnalysis state
        onSmartAnalysisModeChanged?.invoke(
            _state.value.isSmartAnalysisMode,
            _state.value.smartAnalysisGoal
        )
        
        // Side effect handling
        when (intent) {
            is ConversationIntent.SendMessage -> handleSendEffect(scope, context)
            is ConversationIntent.SendSmartAnalysis -> handleSmartAnalysisEffect(scope, analysisContext)
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
    
    // P3.8.1: SmartAnalysis side effect handler (complete implementation)
    private fun handleSmartAnalysisEffect(scope: CoroutineScope?, analysisContext: SmartAnalysisContext?) {
        if (!_state.value.isSending) return
        requireNotNull(scope) { "Scope required for SendSmartAnalysis" }
        requireNotNull(analysisContext) { "SmartAnalysisContext required for SendSmartAnalysis" }
        
        val goal = _state.value.smartAnalysisGoal ?: ""
        val messages = analysisContext.messages
        
        // Input classification
        val bucket = com.smartsales.domain.chat.InputClassifier.classifyUserInput(
            goal.ifBlank { _state.value.inputText }, 
            messages
        )
        val target = com.smartsales.domain.chat.InputClassifier.findSmartAnalysisPrimaryContent(goal, messages)
        
        // Guard: not enough content for analysis
        if (target == null) {
            // Dispatch error and reset state
            dispatch(ConversationIntent.StreamError("内容太少，无法智能分析，请先粘贴对话或纪要再试。"))
            return
        }
        
        // Build analysis parameters
        val isLowInfoGoal = goal.isNotBlank() && com.smartsales.domain.chat.InputClassifier.isLowInfoAnalysisInput(goal)
        val analysisGoal = if (goal.isNotBlank() && !isLowInfoGoal) goal else "通用分析"
        val hasCustomGoal = goal.isNotBlank() && !isLowInfoGoal
        val preface = if (goal.isBlank() || isLowInfoGoal || bucket == com.smartsales.domain.chat.InputBucket.NOISE) {
            "提示：你没有额外说明分析目标，我会基于最近的一段对话或内容做通用智能分析。"
        } else null
        
        // Build user message
        val contextContent = com.smartsales.domain.chat.InputClassifier.findContextForAnalysis(target.content, messages)
        val userMessage = com.smartsales.domain.chat.ChatMessageBuilder.buildSmartAnalysisUserMessage(
            mainContent = target.content,
            context = contextContent,
            goal = analysisGoal
        )
        
        // Create user message UI
        val userDisplayText = if (hasCustomGoal) {
            "智能分析：${goal.take(60)}"
        } else {
            "智能分析（通用）"
        }
        val userMessageUi = ChatMessageUi(
            id = generateMessageId(),
            role = ChatMessageRole.USER,
            content = userDisplayText,
            timestampMillis = System.currentTimeMillis()
        )
        
        // Create assistant placeholder
        val assistantId = generateMessageId()
        val assistantPlaceholder = ChatMessageUi(
            id = assistantId,
            role = ChatMessageRole.ASSISTANT,
            content = "正在智能分析当前会话内容…",
            timestampMillis = System.currentTimeMillis(),
            isStreaming = true
        )
        
        // Add messages to state
        dispatch(ConversationIntent.MessageReceived(userMessageUi))
        dispatch(ConversationIntent.MessageReceived(assistantPlaceholder))
        
        // Build chat request with SmartAnalysis skill
        val request = ChatRequest(
            sessionId = analysisContext.sessionId,
            userMessage = userMessage,  // Built SmartAnalysis message
            quickSkillId = com.smartsales.domain.config.QuickSkillId.SMART_ANALYSIS.name,
            audioContextSummary = null,
            history = _state.value.messages.map { ui ->
                ChatHistoryItem(
                    role = if (ui.role == ChatMessageRole.USER) ChatRole.USER else ChatRole.ASSISTANT,
                    content = ui.content
                )
            },
            isFirstAssistantReply = false,
            persona = analysisContext.salesPersona
        )
        
        // Start streaming with preface transform
        chatStreamCoordinator.start(
            scope = scope,
            request = request,
            onDelta = { token ->
                dispatch(ConversationIntent.StreamDelta(assistantId, token))
            },
            onCompleted = { fullText ->
                // Apply preface transform
                val transformed = buildString {
                    preface?.let {
                        append(it.trim()).append("\n\n")
                    }
                    append(fullText.trim())
                }.trim()
                dispatch(ConversationIntent.StreamCompleted(assistantId, transformed))
            },
            onError = { throwable ->
                dispatch(ConversationIntent.StreamError(throwable.message ?: "SmartAnalysis失败"))
            }
        )
    }
    
    private var messageIdCounter = 0
    private fun generateMessageId(): String = "msg_${System.currentTimeMillis()}_${messageIdCounter++}"
    
    /**
     * P3.9.3: Start streaming with callbacks pattern.
     * 
     * Delegates to StreamingCoordinator and calls back to HSVM for side effects.
     * Supports V1 retry configuration for general chat.
     */
    fun startStreaming(
        context: StreamingContext,
        callbacks: StreamingCallbacks,
        scope: CoroutineScope,
        v1RetryConfig: V1RetryConfig? = null
    ) {
        val request = context.request
        val assistantId = context.assistantId
        
        // V1 retry setup (moved from HSVM)
        val v1RetryActive = v1RetryConfig != null && v1RetryConfig.maxRetries > 0
        var v1TerminalFailureReason: String? = null
        
        val completionEvaluator: (suspend (String, Int) -> CompletionDecision)? = if (v1RetryActive) {
            val evaluator = V1GeneralCompletionEvaluator(v1Finalizer)
            val lambda: suspend (String, Int) -> CompletionDecision = { rawFullText, attempt ->
                val eval = evaluator.evaluate(
                    rawFullText = rawFullText,
                    attempt = attempt,
                    maxRetries = v1RetryConfig!!.maxRetries,
                    enableReasonAwareRetry = v1RetryConfig.enableReasonAware
                )
                if (eval.decision == CompletionDecision.Terminal) {
                    v1TerminalFailureReason = eval.finalizeResult.failureReason
                }
                eval.decision
            }
            lambda
        } else null
        
        val requestProvider: ((Int) -> ChatRequest)? = if (v1RetryActive) {
            val repairInstruction = V1GeneralRetryPolicy.buildRepairInstruction()
            val lambda: (Int) -> ChatRequest = { attempt ->
                if (attempt == 0) request else request.copy(
                    userMessage = request.userMessage + "\n\n" + repairInstruction
                )
            }
            lambda
        } else null
        
        val onRetryStart: suspend (Int) -> Unit = if (v1RetryActive) {
            { attempt ->
                // Notify callback for UI reset
                callbacks.onRetryStart(attempt)
            }
        } else {
            { _ -> }
        }
        
        val onTerminal: suspend (String, Int) -> Unit = if (v1RetryActive) {
            { fullText, attempt ->
                callbacks.onTerminal(fullText, attempt, v1TerminalFailureReason)
            }
        } else {
            { _, _ -> }
        }
        
        // Delegate to chatStreamCoordinator with V1 retry config
        chatStreamCoordinator.start(
            scope = scope,
            request = request,
            onDelta = { token ->
                callbacks.onDelta(assistantId, token)
            },
            onCompleted = { fullText ->
                callbacks.onCompleted(assistantId, fullText)
            },
            onError = { throwable ->
                callbacks.onError(assistantId, throwable)
            },
            maxRetries = v1RetryConfig?.maxRetries ?: 0,
            completionEvaluator = completionEvaluator,
            requestProvider = requestProvider,
            onRetryStart = onRetryStart,
            onTerminal = onTerminal
        )
    }

}
