package com.smartsales.prism.ui

import com.smartsales.prism.domain.analyst.TaskBoardItem
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.ScheduledTask
import kotlinx.coroutines.Job

internal class AgentUiBridge(
    val getCurrentSessionId: () -> String?,
    val setCurrentSessionId: (String?) -> Unit,
    val getSessionBootstrapJob: () -> Job?,
    val setSessionBootstrapJob: (Job?) -> Unit,
    val getUiState: () -> UiState,
    val setUiState: (UiState) -> Unit,
    val getInputText: () -> String,
    val setInputText: (String) -> Unit,
    val getIsSending: () -> Boolean,
    val setIsSending: (Boolean) -> Unit,
    val setErrorMessage: (String?) -> Unit,
    val setToastMessage: (String?) -> Unit,
    val getHistory: () -> List<ChatMessage>,
    val setHistory: (List<ChatMessage>) -> Unit,
    val getTaskBoardItems: () -> List<TaskBoardItem>,
    val setTaskBoardItems: (List<TaskBoardItem>) -> Unit,
    val getSessionTitle: () -> String,
    val setSessionTitle: (String) -> Unit,
    val setHeroUpcoming: (List<ScheduledTask>) -> Unit,
    val setHeroAccomplished: (List<ScheduledTask>) -> Unit
)
