package com.smartsales.prism.ui.fakes

import com.smartsales.prism.ui.IAgentViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.core.pipeline.AgentActivity
import com.smartsales.core.pipeline.MascotState
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.prism.domain.analyst.TaskBoardItem

/**
 * Fake ViewModel for Parallel UI (Skin) Development.
 * Driven entirely by static flows and manual debug triggers to evaluate UI states
 * totally decoupled from the Project Mono (Layer 3) pipeline migration.
 */
class FakeAgentViewModel : IAgentViewModel {
    private val _agentActivity = MutableStateFlow<AgentActivity?>(null)
    override val agentActivity = _agentActivity.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    override val uiState = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    override val inputText = _inputText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    override val isSending = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    override val errorMessage = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    override val toastMessage = _toastMessage.asStateFlow()

    private val _history = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val history = _history.asStateFlow()

    private val _taskBoardItems = MutableStateFlow<List<TaskBoardItem>>(emptyList())
    override val taskBoardItems = _taskBoardItems.asStateFlow()

    private val _sessionTitle = MutableStateFlow("UI Preview Session")
    override val sessionTitle = _sessionTitle.asStateFlow()

    private val _heroUpcoming = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroUpcoming = _heroUpcoming.asStateFlow()

    private val _heroAccomplished = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroAccomplished = _heroAccomplished.asStateFlow()

    private val _mascotState = MutableStateFlow<MascotState>(MascotState.Hidden)
    override val mascotState = _mascotState.asStateFlow()

    override val currentDisplayName: String = "UI Designer"
    
    private val _heroGreeting = MutableStateFlow("☀️ 早上好, UI Designer")
    override val heroGreeting = _heroGreeting.asStateFlow()

    override fun clearToast() { _toastMessage.value = null }
    override fun updateInput(text: String) { _inputText.value = text }
    override fun clearError() { _errorMessage.value = null }
    override fun confirmAnalystPlan() {
        _isSending.value = true
        _uiState.value = UiState.Thinking("Simulating plan execution...")
    }
    override fun send() {
        if (_inputText.value.isNotBlank()) {
            _history.value = _history.value + ChatMessage.User(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                content = _inputText.value
            )
            _inputText.value = ""
            _uiState.value = UiState.Thinking("Simulating response...")
        }
    }
    override fun amendAnalystPlan() { _uiState.value = UiState.Idle }
    override fun interactWithMascot(interaction: MascotInteraction) {}
    override fun updateSessionTitle(newTitle: String) { _sessionTitle.value = newTitle }
    override fun selectTaskBoardItem(itemId: String) {}

    // Expose a public method to manually force UI states for @Preview 
    // or debug testing without the real pipeline.
    override fun debugRunScenario(scenario: String) {
        when (scenario) {
            "IDLE" -> _uiState.value = UiState.Idle
            "LOADING" -> _uiState.value = UiState.Loading
            "THINKING" -> _uiState.value = UiState.Thinking("正在分析数据...")
            "STREAMING" -> _uiState.value = UiState.Streaming("好的，我正在为您处理这")
            "RESPONSE" -> _uiState.value = UiState.Response("处理完成。")
            "SCHEDULER_TASK" -> _uiState.value = UiState.SchedulerTaskCreated(
                taskId = "task_123",
                title = "与张总过技术方案",
                dayOffset = 1,
                scheduledAtMillis = System.currentTimeMillis() + 86400000,
                durationMinutes = 60,
                isReschedule = false
            )
            "SCHEDULER_MULTI_TASK" -> _uiState.value = UiState.SchedulerMultiTaskCreated(
                tasks = listOf(
                    UiState.SchedulerTaskCreated("t1", "任务1", 0, System.currentTimeMillis(), 30),
                    UiState.SchedulerTaskCreated("t2", "任务2", 1, System.currentTimeMillis() + 86400000, 60)
                ),
                hasConflict = false
            )
            "TOAST" -> _uiState.value = UiState.Toast("背景任务已更新")
            "HW_HINT" -> _uiState.value = UiState.BadgeDelegationHint
            "ERROR" -> _uiState.value = UiState.Error("网络连接超时，请重试")
            "WAITING_CLARIFICATION" -> _uiState.value = UiState.AwaitingClarification(
                question = "开会需要多久？",
                clarificationType = com.smartsales.prism.domain.model.ClarificationType.MISSING_DURATION
            )
            "MARKDOWN_STRATEGY" -> _uiState.value = UiState.MarkdownStrategyState(
                title = "跟进策略建议",
                markdownContent = "### 核心建议\n1. **确认预算**：确保对方预算>50k。\n2. **推进试用**：建议提供两周免费试用。"
            )
            "EXECUTING_TOOL" -> _uiState.value = UiState.ExecutingTool("系统搜索工具")
            else -> _uiState.value = UiState.Idle
        }
    }
}
