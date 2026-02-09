package com.smartsales.prism.ui.drawers.scheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.SchedulerActionResult
import com.smartsales.prism.domain.scheduler.InspirationRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val orchestrator: Orchestrator,
    private val scheduleBoard: ScheduleBoard,
    private val inspirationRepository: InspirationRepository
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
    
    // 改期目标日期 (显示琥珀色发光效果)
    private val _rescheduledDates = MutableStateFlow<Set<Int>>(emptySet())
    val rescheduledDates: StateFlow<Set<Int>> = _rescheduledDates.asStateFlow()

    // 冲突警告
    private val _conflictWarning = MutableStateFlow<String?>(null)
    val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    // 灵感箱展开状态
    private val _isInspirationsExpanded = MutableStateFlow(false)
    val isInspirationsExpanded: StateFlow<Boolean> = _isInspirationsExpanded.asStateFlow()
    
    // 冲突卡片展开状态
    private val _expandedConflictIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedConflictIds: StateFlow<Set<String>> = _expandedConflictIds.asStateFlow()

    fun toggleConflictExpansion(id: String) {
        val current = _expandedConflictIds.value
        if (current.contains(id)) {
            _expandedConflictIds.value = current - id
        } else {
            _expandedConflictIds.value = current + id
        }
    }

    // Timeline Items — 响应 dayOffset 和刷新触发器变化
    // 合并：日期特定任务 + 全局灵感
    val timelineItems: StateFlow<List<TimelineItemModel>> = combine(
        combine(_activeDayOffset, _refreshTrigger.asSharedFlow()) { offset, _ -> offset }
            .flatMapLatest { offset -> taskRepository.getTimelineItems(offset) },
        inspirationRepository.getAll()
    ) { tasks, inspirations ->
        // 灵感在前，任务在后
        inspirations + tasks
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Aliased Actions ---
    fun onItemClick(id: String) {
        android.util.Log.d("SchedulerVM", "Item clicked: $id")
    }
    
    fun onDeleteItem(id: String) = deleteItem(id)
    
    /**
     * 改期操作 — 调用 Orchestrator 处理 (LLM 解析在那里发生)
     */
    fun onReschedule(id: String, text: String) {
        android.util.Log.d("SchedulerVM", "🔄 Reschedule: id=$id, input='$text'")
        viewModelScope.launch {
            val result = orchestrator.processSchedulerAction(id, text)
            if (result is SchedulerActionResult.Success && result.newDayOffset != null) {
                _rescheduledDates.value += result.newDayOffset
                android.util.Log.d("SchedulerVM", "Rescheduled to day offset: ${result.newDayOffset}")
            }
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
        _unacknowledgedDates.value -= dayOffset
        _rescheduledDates.value -= dayOffset  // 同时清除改期高亮
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
            _pipelineStatus.value = "🗑️ 已删除"
            triggerRefresh()
        }
    }

    // --- Inspiration Actions ---
    
    fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }
    
    fun deleteInspiration(id: String) {
        viewModelScope.launch {
            inspirationRepository.delete(id)
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
                            _conflictWarning.value = "⚠️ 与「${conflict.overlaps.first().title}」时间重叠"
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
     * 处理冲突解决 — 执行用户选择的动作
     */
    fun handleConflictResolution(action: com.smartsales.prism.domain.scheduler.ConflictAction) {
        viewModelScope.launch {
            when (action.action) {
                com.smartsales.prism.domain.scheduler.ActionType.KEEP_A -> {
                    taskRepository.deleteItem(action.taskToRemove!!)
                    _conflictWarning.value = null
                    android.util.Log.d("SchedulerVM", "Resolved: KEEP_A, deleted ${action.taskToRemove}")
                }
                com.smartsales.prism.domain.scheduler.ActionType.KEEP_B -> {
                    taskRepository.deleteItem(action.taskToRemove!!)
                    _conflictWarning.value = null
                    android.util.Log.d("SchedulerVM", "Resolved: KEEP_B, deleted ${action.taskToRemove}")
                }
                com.smartsales.prism.domain.scheduler.ActionType.RESCHEDULE -> {
                    if (action.taskToReschedule != null && action.rescheduleText != null) {
                        onReschedule(action.taskToReschedule, action.rescheduleText)
                        _conflictWarning.value = null
                    } else {
                        _conflictWarning.value = "无法识别改期指令"
                    }
                }
                com.smartsales.prism.domain.scheduler.ActionType.COEXIST -> {
                    _conflictWarning.value = null
                    android.util.Log.d("SchedulerVM", "Resolved: COEXIST — keeping both")
                }
                com.smartsales.prism.domain.scheduler.ActionType.NONE -> {
                    // 解析失败 — 保留现有警告，不覆盖
                }
            }
            scheduleBoard.refresh()
            triggerRefresh()
        }
    }

    /**
     * 🧪 DEV ONLY: 快速运行测试场景
     * 
     * Available scenarios:
     * - CLEAN: 清除明日任务
     * - 1PM/3PM: 基础任务创建
     * - MEETING_SMART: Wave 3 测试 — 会议应触发 smart (-1h, -15m, -5m)
     * - CALL_SINGLE: Wave 3 测试 — 电话应触发 single (-15m)
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
                    clearConflictWarning()
                    _pipelineStatus.value = "🧹 已清除明日任务"
                    // 确保切换到明天，这样用户能看到清空效果
                    onDateSelected(1)
                }
                "1PM" -> simulateTranscript("明天下午1点做实验A")
                "3PM" -> simulateTranscript("明天下午3点做实验B")
                
                // Wave 3: Smart Reminder Inference L2 Tests
                "MEETING_SMART" -> {
                    // 「会议」应该触发 smart cascade (-1h, -15m, -5m)
                    simulateTranscript("明天下午2点部门会议")
                    android.util.Log.d("SchedulerVM", "🧪 L2: MEETING_SMART — 预期 reminder=smart, alarmCascade=[-1h,-15m,-5m]")
                }
                "CALL_SINGLE" -> {
                    // 「电话」应该触发 single (-15m)
                    simulateTranscript("明天下午4点给张总打电话")
                    android.util.Log.d("SchedulerVM", "🧪 L2: CALL_SINGLE — 预期 reminder=single, alarmCascade=[-15m]")
                }
            }
        }
    }
}
