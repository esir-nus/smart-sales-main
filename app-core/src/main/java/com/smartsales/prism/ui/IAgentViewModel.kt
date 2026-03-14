package com.smartsales.prism.ui

import kotlinx.coroutines.flow.StateFlow
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.core.pipeline.MascotState
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.prism.domain.analyst.TaskBoardItem

/**
 * The "Skin" Contract for Agent Intelligence UI.
 * This interface defines the pure StateFlows and Action triggers for the Presentation Layer.
 * UI Developers can build against this contract using Fake ViewModels without waiting for backend changes.
 */
interface IAgentViewModel {
    val agentActivity: StateFlow<AgentActivity?>
    val uiState: StateFlow<UiState>
    val inputText: StateFlow<String>
    val isSending: StateFlow<Boolean>
    val errorMessage: StateFlow<String?>
    val toastMessage: StateFlow<String?>
    val history: StateFlow<List<ChatMessage>>
    val taskBoardItems: StateFlow<List<TaskBoardItem>>
    val sessionTitle: StateFlow<String>
    val heroUpcoming: StateFlow<List<ScheduledTask>>
    val heroAccomplished: StateFlow<List<ScheduledTask>>
    val mascotState: StateFlow<MascotState>
    val currentDisplayName: String
    val heroGreeting: StateFlow<String>

    fun clearToast()
    fun updateInput(text: String)
    fun clearError()
    fun confirmAnalystPlan()
    fun send()
    fun amendAnalystPlan()
    fun interactWithMascot(interaction: MascotInteraction)
    fun updateSessionTitle(newTitle: String)
    fun selectTaskBoardItem(itemId: String)
    fun debugRunScenario(scenario: String)
}
