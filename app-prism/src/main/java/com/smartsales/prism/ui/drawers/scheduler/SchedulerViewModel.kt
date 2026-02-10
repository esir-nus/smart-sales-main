package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.EntityType
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
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val taskRepository: ScheduledTaskRepository,
    private val orchestrator: Orchestrator,
    private val scheduleBoard: ScheduleBoard,
    private val inspirationRepository: InspirationRepository,
    private val memoryRepository: MemoryRepository,
    private val entityWriter: EntityWriter,
    private val badgeAudioPipeline: BadgeAudioPipeline
) : ViewModel() {

    init {
        // 监听 Badge Audio Pipeline 事件，合并到 SchedulerViewModel 后处理
        viewModelScope.launch {
            badgeAudioPipeline.events.collect { event ->
                handlePipelineEvent(event)
            }
        }
    }

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

    // 冲突视觉指示器 — 标记所有冲突卡片的 ID
    private val _conflictedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val conflictedTaskIds: StateFlow<Set<String>> = _conflictedTaskIds.asStateFlow()

    // 引发冲突的卡片 ID (呼吸发光)
    private val _causingTaskId = MutableStateFlow<String?>(null)
    val causingTaskId: StateFlow<String?> = _causingTaskId.asStateFlow()

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
     * 改期操作 — 检测冲突组，走对应管线
     * 
     * 如果任务在冲突组中: 删除所有冲突任务 → 用 LLM 重建用户想要的任务
     * 否则: 常规单任务改期（Wave 8 管线）
     */
    fun onReschedule(id: String, text: String) {
        val conflictGroup = _conflictedTaskIds.value
        if (id in conflictGroup && conflictGroup.size > 1) {
            android.util.Log.d("SchedulerVM", "🔀 Conflict group reschedule: ${conflictGroup.size} tasks, input='$text'")
            resolveConflictGroup(conflictGroup, text)
        } else {
            android.util.Log.d("SchedulerVM", "🔄 Reschedule: id=$id, input='$text'")
            viewModelScope.launch {
                // Wave 8: Unified Pipeline — create new, delete old
                val result = orchestrator.createScheduledTask(text, replaceItemId = id)
                handleCreateResult(result, isReschedule = true)
                triggerRefresh()
            }
        }
    }
    
    /**
     * 冲突组解决 — 删除全部旧任务，用 LLM 重建
     * 
     * 不区分 keep/reschedule/coexist/cancel — 统一走 delete-and-recreate:
     * 1. 收集所有冲突任务的上下文
     * 2. 删除所有旧任务
     * 3. LLM 根据用户指令创建需要保留的任务（可能 0/1/N 个）
     */
    private fun resolveConflictGroup(conflictedIds: Set<String>, userText: String) {
        viewModelScope.launch {
            // 1. 收集冲突任务上下文
            val allItems = scheduleBoard.upcomingItems.value
            val conflictedTasks = conflictedIds.mapNotNull { id ->
                allItems.find { it.entryId == id }
            }
            
            val taskContext = conflictedTasks.joinToString("\n") { task ->
                val time = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(task.scheduledAt))
                "- ${task.title} @ $time (${task.durationMinutes}分钟)"
            }
            
            val enrichedInput = """
当前有以下冲突的任务:
$taskContext

用户指令: $userText

请根据用户指令，返回用户希望保留的任务（可能修改时间）。
如果用户要取消某个任务，就不要返回那个任务。
如果用户要改期某个任务，返回修改后的新任务。
如果用户要保留所有任务，返回所有任务。
            """.trimIndent()
            
            android.util.Log.d("SchedulerVM", "Conflict group context:\n$taskContext")
            
            // 2. 删除所有旧的冲突任务
            conflictedIds.forEach { id ->
                taskRepository.deleteItem(id)
                android.util.Log.d("SchedulerVM", "🗑️ Deleted conflicted task: $id")
            }
            
            // 3. 清除冲突状态
            _conflictWarning.value = null
            _conflictedTaskIds.value = emptySet()
            _causingTaskId.value = null
            
            // 4. 调用现有管线重建任务（MultiTask 或 SingleTask）
            val result = orchestrator.createScheduledTask(enrichedInput, replaceItemId = null)
            android.util.Log.d("SchedulerVM", "Conflict resolution result: $result")
            handleCreateResult(result, isReschedule = true)
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
            
            val result = orchestrator.createScheduledTask(fakeMessage)
            android.util.Log.d("SchedulerVM", "🧪 Pipeline result: $result")
            
            handleCreateResult(result)
            
            triggerRefresh()
        }
    }

    // =====================
    // 共享后处理 — simulate 和 badge pipeline 共用
    // =====================

    /**
     * 任务创建后处理 — 日期高亮 + 冲突检测 + 刷新
     * 由 simulateTranscript() 和 handlePipelineEvent() 共用
     */
    private suspend fun onTaskCreated(
        taskId: String, title: String, dayOffset: Int,
        scheduledAtMillis: Long, durationMinutes: Int
    ) {
        _pipelineStatus.value = "✅ 已创建: $title"
        addUnacknowledgedDate(dayOffset)
        _activeDayOffset.value = dayOffset
        
        // 0 时长 = fire-off 提醒，跳过冲突检测
        if (durationMinutes <= 0) return
        
        // 冲突检测 (仅对有时长的任务)
        scheduleBoard.refresh()
        when (val conflict = scheduleBoard.checkConflict(
            scheduledAtMillis, durationMinutes,
            excludeId = taskId
        )) {
            is ConflictResult.Conflict -> {
                _conflictWarning.value = "⚠️ 与「${conflict.overlaps.first().title}」时间重叠"
                _conflictedTaskIds.value = conflict.overlaps.map { it.entryId }.toSet() + taskId
                _causingTaskId.value = taskId
            }
            is ConflictResult.Clear -> {
                _conflictWarning.value = null
                _conflictedTaskIds.value = emptySet()
                _causingTaskId.value = null
            }
        }
    }

    /**
     * 多任务创建后处理 — 聚合所有冲突结果
     * 修复 Last Writer Wins bug: 不再逐个覆盖，而是收集所有冲突后统一更新
     */
    private suspend fun onMultiTaskCreated(tasks: List<UiState.SchedulerTaskCreated>) {
        if (tasks.isEmpty()) return
        
        // 高亮所有任务的日期
        tasks.forEach { task -> addUnacknowledgedDate(task.dayOffset) }
        // 切换到最后一个任务的日期
        _activeDayOffset.value = tasks.last().dayOffset
        
        // 聚合冲突检测
        scheduleBoard.refresh()
        val allConflictedIds = mutableSetOf<String>()
        var causingTask: UiState.SchedulerTaskCreated? = null
        
        // 只对有时长的任务做冲突检测 (0 时长 = fire-off, 跳过)
        tasks.filter { it.durationMinutes > 0 }.forEach { task ->
            when (val conflict = scheduleBoard.checkConflict(
                task.scheduledAtMillis, task.durationMinutes,
                excludeId = task.taskId
            )) {
                is ConflictResult.Conflict -> {
                    allConflictedIds.addAll(conflict.overlaps.map { it.entryId })
                    allConflictedIds.add(task.taskId)
                    if (causingTask == null) causingTask = task  // 第一个冲突任务
                }
                is ConflictResult.Clear -> { /* 无冲突 */ }
            }
        }
        
        // 统一更新冲突状态 (不会覆盖)
        if (allConflictedIds.isNotEmpty()) {
            _conflictWarning.value = "⚠️ ${tasks.size} 个任务中发现时间冲突"
            _conflictedTaskIds.value = allConflictedIds
            _causingTaskId.value = causingTask?.taskId
        } else {
            _conflictWarning.value = null
            _conflictedTaskIds.value = emptySet()
            _causingTaskId.value = null
        }
    }

    /**
     * 统一结果分发 — 所有 createScheduledTask 调用点共用
     * 确保 single/multi task 都正确走 conflict detection + date highlight
     */
    private suspend fun handleCreateResult(result: UiState, isReschedule: Boolean = false) {
        when (result) {
            is UiState.SchedulerTaskCreated -> {
                onTaskCreated(
                    result.taskId, result.title, result.dayOffset,
                    result.scheduledAtMillis, result.durationMinutes
                )
                if (isReschedule) _rescheduledDates.value += result.dayOffset
            }
            is UiState.SchedulerMultiTaskCreated -> {
                onMultiTaskCreated(result.tasks)
                _pipelineStatus.value = "✅ 已创建 ${result.tasks.size} 个任务"
                if (isReschedule) {
                    result.tasks.forEach { _rescheduledDates.value += it.dayOffset }
                }
            }
            is UiState.Error -> {
                _pipelineStatus.value = "❌ ${result.message}"
            }
            else -> {
                _pipelineStatus.value = "⚠️ 未知响应类型"
            }
        }
    }

    /**
     * Badge Audio Pipeline 事件处理 — 进度 + 完成后处理
     */
    private suspend fun handlePipelineEvent(event: PipelineEvent) {
        when (event) {
            is PipelineEvent.Downloading -> _pipelineStatus.value = "📥 下载中..."
            is PipelineEvent.Transcribing -> _pipelineStatus.value = "🎙️ 转写中..."
            is PipelineEvent.Processing -> _pipelineStatus.value = "⚙️ 处理中..."
            is PipelineEvent.Complete -> {
                when (val result = event.result) {
                    is SchedulerResult.TaskCreated -> {
                        onTaskCreated(
                            result.taskId, result.title, result.dayOffset,
                            result.scheduledAtMillis, result.durationMinutes
                        )
                    }
                    is SchedulerResult.InspirationSaved -> {
                        _pipelineStatus.value = "💡 灵感已保存"
                    }
                    is SchedulerResult.MultiTaskCreated -> {
                        if (result.tasks.isEmpty()) {
                            _pipelineStatus.value = "✅ 多任务已创建"
                        } else {
                            onMultiTaskCreated(result.tasks.map { task ->
                                UiState.SchedulerTaskCreated(
                                    task.taskId, task.title, task.dayOffset,
                                    task.scheduledAtMillis, task.durationMinutes
                                )
                            })
                            _pipelineStatus.value = "✅ 已创建 ${result.tasks.size} 个任务"
                        }
                    }
                    is SchedulerResult.AwaitingClarification -> {
                        _pipelineStatus.value = "❓ ${result.question}"
                    }
                    is SchedulerResult.Ignored -> {
                        // 非调度意图，不更新状态
                    }
                }
                triggerRefresh()
            }
            is PipelineEvent.Error -> {
                _pipelineStatus.value = "❌ ${event.message}"
            }
            else -> { /* RecordingStarted 等 — 不需要处理 */ }
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
        _conflictedTaskIds.value = emptySet()
        _causingTaskId.value = null
    }

    /**
     * 处理冲突解决 — 顺序执行多个动作（支持复合指令如"取消A，改期B"）
     */
    fun handleConflictResolution(resolution: com.smartsales.prism.domain.scheduler.ConflictResolution) {
        viewModelScope.launch {
            android.util.Log.d("SchedulerVM", "Conflict resolution: ${resolution.actions.size} actions")
            
            for (action in resolution.actions) {
                when (action.action) {
                    com.smartsales.prism.domain.scheduler.ActionType.KEEP_A -> {
                        taskRepository.deleteItem(action.taskToRemove!!)
                        android.util.Log.d("SchedulerVM", "Resolved: KEEP_A, deleted ${action.taskToRemove}")
                    }
                    com.smartsales.prism.domain.scheduler.ActionType.KEEP_B -> {
                        taskRepository.deleteItem(action.taskToRemove!!)
                        android.util.Log.d("SchedulerVM", "Resolved: KEEP_B, deleted ${action.taskToRemove}")
                    }
                    com.smartsales.prism.domain.scheduler.ActionType.RESCHEDULE -> {
                        if (action.taskToReschedule != null && action.rescheduleText != null) {
                            onReschedule(action.taskToReschedule, action.rescheduleText)
                            android.util.Log.d("SchedulerVM", "Resolved: RESCHEDULE ${action.taskToReschedule}")
                        } else {
                            android.util.Log.w("SchedulerVM", "RESCHEDULE missing fields, skipping")
                        }
                    }
                    com.smartsales.prism.domain.scheduler.ActionType.COEXIST -> {
                        android.util.Log.d("SchedulerVM", "Resolved: COEXIST — keeping both")
                    }
                    com.smartsales.prism.domain.scheduler.ActionType.NONE -> {
                        android.util.Log.w("SchedulerVM", "Resolved: NONE — skipping")
                    }
                }
            }
            
            // 所有动作执行完毕后，统一清除冲突状态
            _conflictWarning.value = null
            _conflictedTaskIds.value = emptySet()
            _causingTaskId.value = null
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
                "SEED_MEMORY" -> {
                    debugSeedMemories()
                    _pipelineStatus.value = "💾 已注入记忆 seed data"
                }
            }
        }
    }

    private suspend fun debugSeedMemories() {
        val now = System.currentTimeMillis()
        
        // 1. Taizhou U / Ameer / Sun (Feb 8 debugging)
        val m1Content = "台州大学的ameer教授和他弟弟2月8日来摩升泰公司调试最新的桌面机械臂代码，蔡瑞江，孙扬浩对接，协助相关调试工作。 有以下主要以下待办事项：1. yolo模型需要优化训练，将书籍封面剔除在外，只识别打开的书籍。  2. 新的音响麦克风套件表现稳定。3.阿里多模态目前迭代迅速，不太稳定，会时常出现网络问题导致任务失败。 4.阿里作业模式的相关服务不够完善，例如模型单次调用智能识别文字，或者图形。有图形就会优先识别图形而忽略文字。蔡瑞江提议先将作业用户群体限制在小学4年级或以下，加强亲子互动，家庭关系构建可能更加实用。 5.阿里的多模态调试页面还不够完善，等待更新。"
        val e1a = entityWriter.upsertFromClue("Ameer教授", null, EntityType.PERSON, "debug_seed")
        val e1b = entityWriter.upsertFromClue("孙扬浩", null, EntityType.PERSON, "debug_seed")
        val e1c = entityWriter.upsertFromClue("蔡瑞江", null, EntityType.PERSON, "debug_seed")
        
        memoryRepository.save(MemoryEntry(
            entryId = "mem-seed-1",
            sessionId = "debug-session",
            content = m1Content,
            entryType = MemoryEntryType.USER_MESSAGE,
            createdAt = now,
            updatedAt = now,
            structuredJson = """{"relatedEntityIds": ["${e1a.entityId}", "${e1b.entityId}", "${e1c.entityId}"]}"""
        ))

        // 2. Smart Assistant / Sun (Product Pitch)
        val m2Content = "承时利和公司孙扬浩孙工当前销售智能助理产品已处于打磨阶段，功能主要定位是全方位智能助手，亮点包涵：1.长期记忆系统，能智能调用记忆库，知识库，文件库；2.成长型智能体，会随着使用加深对于用户和客户的理解；3智能行程管理，只能安排用户行程，智能提醒，解决事件冲突；4.智能专家，随着和用户的沟通，不断推荐最有用的业务工具，例如年报，日报，pdf报告，思维导图，数据分析等"
        val e2a = entityWriter.upsertFromClue("智能助理", null, EntityType.PRODUCT, "debug_seed")
        // Reuse Sun Yanghao (e1b)
        
        memoryRepository.save(MemoryEntry(
            entryId = "mem-seed-2",
            sessionId = "debug-session",
            content = m2Content,
            entryType = MemoryEntryType.USER_MESSAGE,
            createdAt = now,
            updatedAt = now,
            structuredJson = """{"relatedEntityIds": ["${e1b.entityId}", "${e2a.entityId}"]}"""
        ))

        // 3. Jiangxi U / Ata (Model photos)
        val m3Content = "江西大学Ata教授上周4再次询问模型训练用照片，等待安排；一月28号30号也曾反复询问"
        val e3a = entityWriter.upsertFromClue("Ata教授", null, EntityType.PERSON, "debug_seed")
        
        memoryRepository.save(MemoryEntry(
            entryId = "mem-seed-3",
            sessionId = "debug-session",
            content = m3Content,
            entryType = MemoryEntryType.USER_MESSAGE,
            createdAt = now,
            updatedAt = now,
            structuredJson = """{"relatedEntityIds": ["${e3a.entityId}"]}"""
        ))
        
        android.util.Log.d("SchedulerVM", "✅ Seeded 3 memories and associated entities")
    }
}
