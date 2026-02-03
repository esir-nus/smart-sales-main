package com.smartsales.prism.ui.drawers.scheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val orchestrator: Orchestrator,
    private val scheduleBoard: ScheduleBoard
) : ViewModel() {

    // --- State ---
    
    // Day Selection (offset from today: 0=today, 1=tomorrow, -1=yesterday)
    private val _activeDayOffset = MutableStateFlow(0)
    val activeDayOffset: StateFlow<Int> = _activeDayOffset.asStateFlow()
    
    // 刷新触发器 — 用于手动刷新 timeline
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    
    // Multi-Select Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    // Selection Set
    private val _selectedInspirationIds = MutableStateFlow(setOf<String>())
    val selectedInspirationIds: StateFlow<Set<String>> = _selectedInspirationIds.asStateFlow()
    
    // Pipeline 状态反馈
    private val _pipelineStatus = MutableStateFlow<String?>(null)
    val pipelineStatus: StateFlow<String?> = _pipelineStatus.asStateFlow()

    // 未确认的日期 (显示呼吸发光效果的日期)
    private val _unacknowledgedDates = MutableStateFlow<Set<Int>>(emptySet())
    val unacknowledgedDates: StateFlow<Set<Int>> = _unacknowledgedDates.asStateFlow()

    // 冲突警告
    private val _conflictWarning = MutableStateFlow<String?>(null)
    val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    // Timeline Items — 响应 dayOffset 和刷新触发器变化
    val timelineItems: StateFlow<List<TimelineItemModel>> = _activeDayOffset
        .flatMapLatest { offset ->
            _refreshTrigger.asSharedFlow()
            taskRepository.getTimelineItems(offset)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Aliased Actions ---
    fun onItemClick(id: String) {
        android.util.Log.d("SchedulerVM", "Item clicked: $id")
    }
    
    fun onDeleteItem(id: String) = deleteItem(id)
    
    /**
     * 改期操作 — 调用 Orchestrator 处理 (LLM 解析在那里发生)
     */
    fun onReschedule(id: String, text: String) {
        viewModelScope.launch {
            orchestrator.processSchedulerAction(id, text)
            triggerRefresh()
        }
    }
    
    fun onToggleSelection(id: String) = toggleItemSelection(id)
    fun onEnterSelectionMode() = toggleSelectionMode(true)
    fun onExitSelectionMode() = toggleSelectionMode(false)

    // --- Actions ---

    fun onDateSelected(dayOffset: Int) {
        _activeDayOffset.value = dayOffset
        // 确认日期 (停止发光)
        acknowledgeDate(dayOffset)
        android.util.Log.d("SchedulerVM", "Day offset changed to: $dayOffset")
    }

    /**
     * 确认日期 — 从未确认集合中移除 (停止发光)
     */
    fun acknowledgeDate(dayOffset: Int) {
        val current = _unacknowledgedDates.value.toMutableSet()
        current.remove(dayOffset)
        _unacknowledgedDates.value = current
    }

    /**
     * 添加未确认日期 — 显示发光效果
     */
    private fun addUnacknowledgedDate(dayOffset: Int) {
        val current = _unacknowledgedDates.value.toMutableSet()
        current.add(dayOffset)
        _unacknowledgedDates.value = current
    }

    fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedInspirationIds.value = emptySet()
        }
    }

    fun toggleItemSelection(id: String) {
        val current = _selectedInspirationIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedInspirationIds.value = current
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            taskRepository.deleteItem(id)
            triggerRefresh()
        }
    }

    /**
     * 🧪 DEV ONLY: 模拟转录消息，绕过硬件直接测试 Pipeline
     */
    fun simulateTranscript(fakeMessage: String) {
        viewModelScope.launch {
            android.util.Log.d("SchedulerVM", "🧪 Simulating transcript: $fakeMessage")
            _pipelineStatus.value = "处理中..."
            
            // 使用 createScheduledTask 直接路由到 Scheduler Pipeline
            val result = orchestrator.createScheduledTask(fakeMessage)
            
            android.util.Log.d("SchedulerVM", "🧪 Pipeline result: $result")
            
            when (result) {
                is UiState.SchedulerTaskCreated -> {
                    _pipelineStatus.value = "✅ 已创建: ${result.title}"
                    addUnacknowledgedDate(result.dayOffset)
                    _activeDayOffset.value = result.dayOffset
                    
                    // 冲突检测 (创建后，信息性提示)
                    scheduleBoard.refresh()
                    when (val conflict = scheduleBoard.checkConflict(
                        result.scheduledAtMillis,
                        result.durationMinutes,
                        excludeId = result.taskId  // 排除刚创建的任务，避免自冲突
                    )) {
                        is ConflictResult.Conflict -> {
                            _conflictWarning.value = "⚠️ 与「${conflict.overlaps.first().title}」时间冲突"
                        }
                        is ConflictResult.Clear -> {
                            _conflictWarning.value = null
                        }
                    }
                }
                is UiState.Response -> {
                    _pipelineStatus.value = "✅ ${result.content}"
                    // Fallback: 无法确定日期时默认明天
                    addUnacknowledgedDate(1)
                    _activeDayOffset.value = 1
                }
                is UiState.Error -> {
                    _pipelineStatus.value = "❌ ${result.message}"
                }
                else -> {
                    _pipelineStatus.value = "⚠️ 未知响应类型"
                }
            }
            
            triggerRefresh()
        }
    }
    
    /**
     * 手动触发 Timeline 刷新
     */
    fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }
    
    /**
     * 清除 Pipeline 状态
     */
    fun clearPipelineStatus() {
        _pipelineStatus.value = null
    }

    /**
     * 清除冲突警告
     */
    fun clearConflictWarning() {
        _conflictWarning.value = null
    }

    /**
     * 🧪 DEV ONLY: 快速运行测试场景
     */
    fun debugRunScenario(scenario: String) {
        if (!com.smartsales.prism.BuildConfig.DEBUG) return
        viewModelScope.launch {
            when (scenario) {
                "CLEAN" -> {
                    // 删除所有明天的任务 (activeDayOffset = 1)
                    val items = taskRepository.getTimelineItems(1).first()
                    items.filterIsInstance<TimelineItemModel.Task>().forEach { 
                        taskRepository.deleteItem(it.id) 
                    }
                    triggerRefresh()
                    clearConflictWarning()
                    _pipelineStatus.value = "🧹 已清除明日任务"
                    // 确保切换到明天，这样用户能看到清空效果
                    onDateSelected(1)
                }
                "1PM" -> simulateTranscript("明天下午1点做实验A")
                "3PM" -> simulateTranscript("明天下午3点做实验B")
            }
        }
    }
}
