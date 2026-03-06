package com.smartsales.prism.ui.components.agent

import com.smartsales.prism.domain.model.UiState

/**
 * Extension functions to map the domain [UiState] to our decoupled 
 * Agent Intelligence UI Primitives.
 * 
 * This ensures the PrismChatScreen remains clean and the Primitives
 * remain completely agnostic of the domain models.
 */

fun UiState.toActiveTaskHorizonState(): ActiveTaskHorizonState {
    return when (this) {
        is UiState.ExecutingTool -> ActiveTaskHorizonState(
            isVisible = true,
            overarchingGoal = "Executing Workflow", 
            currentActivity = "Running $toolName",
            activeToolStream = "[Tool: $toolName]",
            canCancel = true
        )
        is UiState.Thinking -> ActiveTaskHorizonState(
            isVisible = true,
            overarchingGoal = "Processing Request...",
            currentActivity = hint ?: "Thinking...",
            activeToolStream = null,
            canCancel = true
        )
        is UiState.Loading -> ActiveTaskHorizonState(
            isVisible = true,
            overarchingGoal = "Initializing...",
            currentActivity = "Waking up agent pipeline...",
            activeToolStream = null,
            canCancel = true
        )
        else -> ActiveTaskHorizonState(
            isVisible = false, 
            overarchingGoal = "", 
            currentActivity = "", 
            activeToolStream = null, 
            canCancel = false
        )
    }
}

fun UiState.toThinkingCanvasState(isCollapsed: Boolean = false): ThinkingCanvasState {
    return when (this) {
        is UiState.Streaming -> ThinkingCanvasState(
            isVisible = true,
            streamingContent = partialContent,
            isCollapsed = isCollapsed
        )
        else -> ThinkingCanvasState(
            isVisible = false, 
            streamingContent = "", 
            isCollapsed = true
        )
    }
}

fun UiState.toLiveArtifactBuilderState(conflictNode: ConflictData? = null): LiveArtifactBuilderState {
    return when (this) {
        is UiState.MarkdownStrategyState -> LiveArtifactBuilderState(
            isVisible = true,
            title = title,
            markdownContent = markdownContent,
            conflictNode = conflictNode
        )
        else -> LiveArtifactBuilderState(
            isVisible = false, 
            title = "", 
            markdownContent = "", 
            conflictNode = null
        )
    }
}
