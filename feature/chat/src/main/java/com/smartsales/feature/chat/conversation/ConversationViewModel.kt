package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ConversationViewModel: Thin wrapper around ConversationReducer.
 * 
 * This is the "Platform Boundary" - connects pure portable Reducer to Android lifecycle.
 * 
 * Design:
 * - Holds StateFlow<ConversationState> backed by pure Reducer
 * - Exposes dispatch(intent) for UI events
 * - Side effects (streaming) deferred to P3.1.B2
 * 
 * Phase: P3.1.B1 - InputChanged intent only (no streaming)
 * 
 * Note: This is NOT an @HiltViewModel because it's injected into HomeScreenViewModel.
 * Hilt prohibits injecting ViewModels into other ViewModels.
 */
class ConversationViewModel @Inject constructor(
    private val homeOrchestrator: HomeOrchestrator
) {
    
    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()
    
    /**
     * Dispatch user intent to Reducer.
     * Pure state transition via ConversationReducer.reduce().
     * 
     * P3.1.B1: Only InputChanged supported. SendMessage deferred to B2.
     */
    fun dispatch(intent: ConversationIntent) {
        _state.value = ConversationReducer.reduce(_state.value, intent)
        
        // P3.1.B1: No side effects yet
        // P3.1.B2 will add streaming logic here
    }
}
