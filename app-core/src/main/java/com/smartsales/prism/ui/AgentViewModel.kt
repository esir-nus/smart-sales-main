package com.smartsales.prism.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.pipeline.AgentActivityController
import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.core.pipeline.MascotService
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.system.SystemEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * AgentViewModel (Layer 4/5 Presentation)
 *
 * 仅保留公开状态、协作器装配与入口分发，旧全量运行时逻辑已下沉到稳定支持文件。
 */
@HiltViewModel
class AgentViewModel @Inject constructor(
    private val intentOrchestrator: IntentOrchestrator,
    private val historyRepository: HistoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val activityController: AgentActivityController,
    private val mascotService: MascotService,
    private val eventBus: SystemEventBus,
    private val audioRepository: AudioRepository,
    private val contextBuilder: com.smartsales.core.context.ContextBuilder,
    private val toolRegistry: com.smartsales.core.pipeline.ToolRegistry
) : ViewModel(), IAgentViewModel {

    override val agentActivity = activityController.activity

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    override val uiState: StateFlow<UiState> = _uiState.onEach { state ->
        com.smartsales.core.telemetry.PipelineValve.tag(
            checkpoint = com.smartsales.core.telemetry.PipelineValve.Checkpoint.UI_STATE_EMITTED,
            payloadSize = state.javaClass.simpleName.hashCode(),
            summary = "AgentViewModel emitted ${state.javaClass.simpleName}",
            rawDataDump = state.toString()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Idle)

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

    private val _taskBoardItems = MutableStateFlow<List<com.smartsales.prism.domain.analyst.TaskBoardItem>>(emptyList())
    override val taskBoardItems = _taskBoardItems.asStateFlow()

    private val _sessionTitle = MutableStateFlow(DEFAULT_AGENT_SESSION_TITLE)
    override val sessionTitle = _sessionTitle.asStateFlow()

    private val _heroUpcoming = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroUpcoming = _heroUpcoming.asStateFlow()

    private val _heroAccomplished = MutableStateFlow<List<ScheduledTask>>(emptyList())
    override val heroAccomplished = _heroAccomplished.asStateFlow()

    override val mascotState = mascotService.state

    private var currentSessionId: String? = null
    private var sessionBootstrapJob: Job? = null

    override val currentDisplayName: String
        get() = userProfileRepository.profile.value.displayName

    override val heroGreeting = userProfileRepository.profile
        .map { profile ->
            val hour = java.time.LocalTime.now().hour
            val timeGreeting = when {
                hour < 5 -> "夜深了"
                hour < 12 -> "早上好"
                hour < 18 -> "下午好"
                else -> "晚上好"
            }
            "$timeGreeting, ${profile.displayName}"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val bridge = AgentUiBridge(
        getCurrentSessionId = { currentSessionId },
        setCurrentSessionId = { currentSessionId = it },
        getSessionBootstrapJob = { sessionBootstrapJob },
        setSessionBootstrapJob = { sessionBootstrapJob = it },
        getUiState = { _uiState.value },
        setUiState = { _uiState.value = it },
        getInputText = { _inputText.value },
        setInputText = { _inputText.value = it },
        getIsSending = { _isSending.value },
        setIsSending = { _isSending.value = it },
        setErrorMessage = { _errorMessage.value = it },
        setToastMessage = { _toastMessage.value = it },
        getHistory = { _history.value },
        setHistory = { _history.value = it },
        getTaskBoardItems = { _taskBoardItems.value },
        setTaskBoardItems = { _taskBoardItems.value = it },
        getSessionTitle = { _sessionTitle.value },
        setSessionTitle = { _sessionTitle.value = it },
        setHeroUpcoming = { _heroUpcoming.value = it },
        setHeroAccomplished = { _heroAccomplished.value = it }
    )

    private val sessionCoordinator = AgentSessionCoordinator(
        historyRepository = historyRepository,
        contextBuilder = contextBuilder,
        audioRepository = audioRepository,
        activityController = activityController,
        bridge = bridge
    )
    private val toolCoordinator = AgentToolCoordinator(
        toolRegistry = toolRegistry,
        contextBuilder = contextBuilder,
        sessionCoordinator = sessionCoordinator,
        bridge = bridge
    )
    private val pipelineCoordinator = AgentPipelineCoordinator(
        intentOrchestrator = intentOrchestrator,
        toolRegistry = toolRegistry,
        sessionCoordinator = sessionCoordinator,
        toolCoordinator = toolCoordinator,
        bridge = bridge
    )
    private val runtimeSupport = AgentRuntimeSupport(
        scheduledTaskRepository = scheduledTaskRepository,
        eventBus = eventBus,
        bridge = bridge
    )
    private val debugSupport = AgentDebugSupport(bridge)

    init {
        startNewSession()
        refreshHeroDashboard()
        runtimeSupport.launchIdleWatcher(
            scope = viewModelScope,
            uiState = uiState,
            inputText = inputText
        )
    }

    fun refreshHeroDashboard() {
        runtimeSupport.refreshHeroDashboard(viewModelScope)
    }

    fun startNewSession() {
        sessionCoordinator.startNewSession(viewModelScope)
    }

    fun switchSession(sessionId: String, triggerAutoRename: Boolean = false) {
        sessionCoordinator.switchSession(viewModelScope, sessionId, triggerAutoRename)
    }

    override fun clearToast() {
        _toastMessage.value = null
    }

    override fun updateInput(text: String) {
        _inputText.value = text
    }

    override fun clearError() {
        _errorMessage.value = null
    }

    fun cycleDebugState() {
    }

    override fun amendAnalystPlan() {
        _uiState.value = UiState.Idle
        _toastMessage.value = "请在底部输入框输入您要修改的计划内容"
    }

    override fun interactWithMascot(interaction: MascotInteraction) {
        viewModelScope.launch { mascotService.interact(interaction) }
    }

    override fun updateSessionTitle(newTitle: String) {
        viewModelScope.launch {
            sessionCoordinator.updateSessionTitle(newTitle)
        }
    }

    override fun selectTaskBoardItem(itemId: String) {
        toolCoordinator.selectTaskBoardItem(viewModelScope, itemId)
    }

    override fun confirmAnalystPlan() {
        pipelineCoordinator.confirmAnalystPlan(viewModelScope)
    }

    override fun send() {
        pipelineCoordinator.send(viewModelScope)
    }

    override fun debugRunScenario(scenario: String) {
        debugSupport.debugRunScenario(scenario)
    }
}
