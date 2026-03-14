package com.smartsales.prism.ui.drawers.scheduler

import android.content.Context

import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import com.smartsales.prism.domain.audio.BadgeAudioPipeline
import com.smartsales.prism.domain.audio.PipelineEvent
import com.smartsales.prism.domain.audio.SchedulerResult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.memory.ConflictResult
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.core.pipeline.*
import com.smartsales.core.pipeline.*
import com.smartsales.core.pipeline.*
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.scheduler.InspirationRepository
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerRefreshBus
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository

import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.scheduler.AlarmScheduler
import com.smartsales.prism.domain.scheduler.TipGenerator
import com.smartsales.prism.data.notification.OemCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    val schedulerLinter: SchedulerLinter,
    private val unifiedPipeline: UnifiedPipeline,
    @ApplicationContext private val appContext: Context,
    private val taskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val inspirationRepository: InspirationRepository,
    private val memoryRepository: MemoryRepository,

    private val badgeAudioPipeline: BadgeAudioPipeline,
    private val asrService: AsrService,
    private val tipGenerator: TipGenerator,  // Wave 9: Smart Tips
    private val alarmScheduler: AlarmScheduler  // Wave 12: Task Completion
) : ViewModel(), ISchedulerViewModel {

    init {
        // 监听 Badge Audio Pipeline 事件，合并到 SchedulerViewModel 后处理
        viewModelScope.launch {
            badgeAudioPipeline.events.collect { event ->
                handlePipelineEvent(event)
            }
        }
        // 自动完成已过期任务 — 打开 Scheduler 时扫一遍今天的任务
        viewModelScope.launch { autoCompleteExpiredTasks() }
        // 实时闹钟反馈 — DEADLINE 闹钟触发时刷新 UI
        viewModelScope.launch {
            SchedulerRefreshBus.events.collect {
                android.util.Log.d("SchedulerVM", "alarmFired: sweeping expired tasks")
                autoCompleteExpiredTasks()
                triggerRefresh()
            }
        }

    }

    /**
     * 自动完成已过期的任务
     *
     * 过期条件: endTime < now && !isDone
     * ⚠️ 使用 endTime（startTime + duration），不是闹钟触发时间
     *    级联闹钟 [-1h, -15m, 0m] 的早期触发不代表任务过期
     */
    private suspend fun autoCompleteExpiredTasks() {
        val now = Instant.now()
        val today = LocalDate.now()
        val allItems = taskRepository.queryByDateRange(today, today).first()
        val expiredTasks = allItems
            .filterIsInstance<TimelineItemModel.Task>()
            .filter { !it.isDone }
            .filter { task ->
                val endTime = task.endTime
                    ?: task.startTime.plusSeconds(task.durationMinutes * 60L)
                endTime.isBefore(now)
            }

        expiredTasks.forEach { task ->
            taskRepository.updateTask(task.copy(isDone = true))
            alarmScheduler.cancelReminder(task.id)
        }

        if (expiredTasks.isNotEmpty()) {
            android.util.Log.d("SchedulerVM", "autoComplete: ${expiredTasks.size} tasks expired")
        }
    }

    // --- State ---
    
    // Day Selection (offset from today: 0=today, 1=tomorrow, -1=yesterday)
    private val _activeDayOffset = MutableStateFlow(0)
    override val activeDayOffset: StateFlow<Int> = _activeDayOffset.asStateFlow()
    
    // 刷新触发器 — 用于手动刷新 timeline
    private val _refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    
    // Multi-Select Mode
    private val _isSelectionMode = MutableStateFlow(false)
    override val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()
    
    // Selection Set
    private val _selectedInspirationIds = MutableStateFlow(setOf<String>())
    override val selectedInspirationIds: StateFlow<Set<String>> = _selectedInspirationIds.asStateFlow()
    
    // Wave 9: Smart Tips — 提示缓存（taskId → List<String>）
    private val _tipsCache = mutableMapOf<String, List<String>>()
    
    // Wave 9: Tips 加载状态（taskId → Boolean）
    private val _tipsLoading = MutableStateFlow<Set<String>>(emptySet())
    override val tipsLoading: StateFlow<Set<String>> = _tipsLoading.asStateFlow()
    
    /**
     * Wave 9: 获取已缓存的提示
     */
    override fun getCachedTips(taskId: String): List<String> = _tipsCache[taskId] ?: emptyList()
    
    // Pipeline 状态反馈
    private val _pipelineStatus = MutableStateFlow<String?>(null)
    override val pipelineStatus: StateFlow<String?> = _pipelineStatus.asStateFlow()

    // 未确认的日期 (显示呼吸发光效果的日期)
    private val _unacknowledgedDates = MutableStateFlow<Set<Int>>(emptySet())
    override val unacknowledgedDates: StateFlow<Set<Int>> = _unacknowledgedDates.asStateFlow()
    
    // 改期目标日期 (显示琥珀色发光效果)
    private val _rescheduledDates = MutableStateFlow<Set<Int>>(emptySet())
    override val rescheduledDates: StateFlow<Set<Int>> = _rescheduledDates.asStateFlow()

    // 冲突警告
    private val _conflictWarning = MutableStateFlow<String?>(null)
    override val conflictWarning: StateFlow<String?> = _conflictWarning.asStateFlow()

    // 冲突视觉指示器 — 标记所有冲突卡片的 ID
    private val _conflictedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    override val conflictedTaskIds: StateFlow<Set<String>> = _conflictedTaskIds.asStateFlow()

    // 引发冲突的卡片 ID (呼吸发光)
    private val _causingTaskId = MutableStateFlow<String?>(null)
    override val causingTaskId: StateFlow<String?> = _causingTaskId.asStateFlow()

    // 灵感箱展开状态
    private val _isInspirationsExpanded = MutableStateFlow(false)
    override val isInspirationsExpanded: StateFlow<Boolean> = _isInspirationsExpanded.asStateFlow()
    
    // 冲突卡片展开状态
    private val _expandedConflictIds = MutableStateFlow<Set<String>>(emptySet())
    override val expandedConflictIds: StateFlow<Set<String>> = _expandedConflictIds.asStateFlow()

    // 精确闹钟权限请求 — 一次性事件
    private val _exactAlarmPermissionNeeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val exactAlarmPermissionNeeded = _exactAlarmPermissionNeeded.asSharedFlow()
    private val exactAlarmPrompted = AtomicBoolean(false)

    override fun toggleConflictExpansion(id: String) {
        val current = _expandedConflictIds.value
        if (current.contains(id)) {
            _expandedConflictIds.value = current - id
        } else {
            _expandedConflictIds.value = current + id
        }
    }

    // Timeline Items — 响应 dayOffset 和刷新触发器变化
    // 合并：日期特定任务 + 全局灵感
    override val timelineItems: StateFlow<List<TimelineItemModel>> = combine(
        combine(_activeDayOffset, _refreshTrigger.asSharedFlow()) { offset, _ -> offset }
            .flatMapLatest { offset -> taskRepository.getTimelineItems(offset) },
        inspirationRepository.getAll()
    ) { tasks, inspirations ->
        // 灵感在前，任务在后
        inspirations + tasks
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Aliased Actions ---
    override fun onItemClick(id: String) {
        android.util.Log.d("SchedulerVM", "Item clicked: $id")
    }
    
    override fun onDeleteItem(id: String) = deleteItem(id)
    
    /**
     * 改期操作 — 检测冲突组，走对应管线
     * 
     * 如果任务在冲突组中: 删除所有冲突任务 → 用 LLM 重建用户想要的任务
     * 否则: 常规单任务改期（Wave 8 管线）
     */
    override fun onReschedule(id: String, text: String) {
        val conflictGroup = _conflictedTaskIds.value
        if (id in conflictGroup && conflictGroup.size > 1) {
            android.util.Log.d("SchedulerVM", "🔀 Conflict group reschedule: ${conflictGroup.size} tasks, input='$text'")
            resolveConflictGroup(conflictGroup, text)
        } else {
            android.util.Log.d("SchedulerVM", "🔄 Reschedule: id=$id, input='$text'")
            processThroughPipeline(text, replaceItemId = id, isReschedule = true)
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
            android.util.Log.d("SchedulerVM", "Conflict resolution input: $enrichedInput")
            processThroughPipeline(enrichedInput, replaceItemId = null, isReschedule = true)
        }
    }
    
    override fun onToggleSelection(id: String) = toggleItemSelection(id)
    override fun onEnterSelectionMode() = toggleSelectionMode(true)
    override fun onExitSelectionMode() = toggleSelectionMode(false)

    // --- Actions ---

    override fun onDateSelected(dayOffset: Int) {
        _activeDayOffset.value = dayOffset
        // 确认日期 (停止发光)
        acknowledgeDate(dayOffset)
        // 切换日期时清理该日过期任务
        viewModelScope.launch { autoCompleteExpiredTasks() }
        android.util.Log.d("SchedulerVM", "Day offset changed to: $dayOffset")
    }
    
    /**
     * Wave 9: Smart Tips — 卡片展开时触发提示加载
     * 
     * @param taskId 任务 ID
     * @param keyPersonEntityId 关键人物实体 ID（如果没有则跳过）
     */
    override fun onCardExpanded(taskId: String, keyPersonEntityId: String?) {
        if (keyPersonEntityId == null) {
            android.util.Log.d("SchedulerVM", "🔕 No keyPersonEntityId for task=$taskId, skipping tips")
            return
        }
        if (_tipsCache.containsKey(taskId)) {
            android.util.Log.d("SchedulerVM", "✅ Tips already cached for task=$taskId")
            return
        }
        
        viewModelScope.launch {
            _tipsLoading.value += taskId
            android.util.Log.d("SchedulerVM", "💡 Tips loading for task=$taskId, entity=$keyPersonEntityId")
            
            val task = taskRepository.getTask(taskId)
            if (task == null) {
                android.util.Log.e("SchedulerVM", "❌ Task not found: $taskId")
                _tipsLoading.value -= taskId
                return@launch
            }
            
            val tips = withTimeoutOrNull(5000L) {
                tipGenerator.generate(task)
            } ?: emptyList()
            
            _tipsCache[taskId] = tips
            _tipsLoading.value -= taskId
            android.util.Log.d("SchedulerVM", "💡 Tips loaded: ${tips.size} tips for $taskId")
            triggerRefresh()
        }
    }

    /**
     * 确认日期 — 从未确认集合中移除 (停止发光)
     */
    override fun acknowledgeDate(dayOffset: Int) {
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

    override fun toggleSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) {
            _selectedInspirationIds.value = emptySet()
        }
    }

    override fun toggleItemSelection(id: String) {
        val current = _selectedInspirationIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedInspirationIds.value = current
    }

    override fun deleteItem(id: String) {
        viewModelScope.launch {
            val task = taskRepository.getTask(id)  // 删除前获取实体关联
            taskRepository.deleteItem(id)
            _pipelineStatus.value = "🗑️ 已删除"
            // Wave 9: 清理提示缓存
            _tipsCache.remove(id)
            _tipsLoading.value -= id
            // nextAction 缓存同步 — 删除可能移除了当前 nextAction
            task?.keyPersonEntityId?.let { recomputeNextAction(it) }
            triggerRefresh()
        }
    }

    /**
     * Wave 12: 切换任务完成状态
     *
     * isDone=true  → 取消所有闹钟
     * isDone=false → 如果任务在未来，重新注册闹钟
     */
    override fun toggleDone(taskId: String) {
        viewModelScope.launch {
            val task = taskRepository.getTask(taskId) ?: run {
                android.util.Log.w("SchedulerVM", "toggleDone: task not found: $taskId")
                return@launch
            }
            val newDone = !task.isDone
            taskRepository.updateTask(task.copy(isDone = newDone))

            if (newDone) {
                // 完成 → 取消闹钟
                alarmScheduler.cancelReminder(taskId)
                
                // Cross-Off Lifecycle (Phase 3): Migrate to Factual Memory and delete from feed
                val memoryEntry = com.smartsales.prism.domain.mapper.TaskMemoryMapper.toMemoryEntry(task)
                memoryRepository.save(memoryEntry)
                taskRepository.deleteItem(taskId) // Remove from actionable feed
                
                android.util.Log.d("SchedulerVM", "toggleDone: id=$taskId, isDone=true → migrated to memory & deleted")
            } else {
                // 恢复 → 只对未来任务重新注册闹钟
                if (task.hasAlarm && task.alarmCascade.isNotEmpty() &&
                    task.startTime.isAfter(java.time.Instant.now())) {
                    alarmScheduler.scheduleCascade(
                        taskId = taskId,
                        taskTitle = task.title,
                        eventTime = task.startTime,
                        cascade = task.alarmCascade
                    )
                    android.util.Log.d("SchedulerVM", "toggleDone: id=$taskId, isDone=false → alarms restored")
                } else {
                    android.util.Log.d("SchedulerVM", "toggleDone: id=$taskId, isDone=false → skip alarm (past or no cascade)")
                }
            }

            // nextAction 缓存同步 — 完成/恢复任务后重算
            task.keyPersonEntityId?.let { recomputeNextAction(it) }

            triggerRefresh()
        }
    }

    /**
     * nextAction 缓存重算 — 查找实体最紧急的活跃 L1/L2 任务
     * 结果直接写入 EntityEntry.nextAction 字段
     */
    private suspend fun recomputeNextAction(entityId: String) {
        val topTask = taskRepository.getTopUrgentActiveForEntity(entityId)
        val nextActionValue = if (topTask != null) topTask.title else null
        // entityWriter.updateProfile(entityId, mapOf("nextAction" to nextActionValue))
        android.util.Log.d("SchedulerVM", "nextAction: entity=$entityId → ${nextActionValue ?: "<cleared>"} (Write delegated to UnifiedPipeline - Layer 4 violation fixed)")
    }

    // --- Inspiration Actions ---
    
    override fun toggleInspirations() {
        _isInspirationsExpanded.value = !_isInspirationsExpanded.value
    }
    
    override fun deleteInspiration(id: String) {
        viewModelScope.launch {
            inspirationRepository.delete(id)
        }
    }

    /**
     * 🧪 DEV ONLY: 模拟转录消息，绕过硬件直接测试 Pipeline
     */
    override fun simulateTranscript(fakeMessage: String) {
        android.util.Log.d("SchedulerVM", "🧪 Simulating transcript: $fakeMessage")
        _pipelineStatus.value = "处理中..."
        processThroughPipeline(fakeMessage)
    }

    private fun processThroughPipeline(text: String, replaceItemId: String? = null, isReschedule: Boolean = false) {
        viewModelScope.launch {
            val pipelineInput = PipelineInput(
                rawText = text,
                isVoice = false,
                intent = QueryQuality.CRM_TASK,
                replaceItemId = replaceItemId
            )
            unifiedPipeline.processInput(pipelineInput).collect { pResult ->
                when(pResult) {
                    is PipelineResult.MutationProposal -> {
                        if (pResult.task != null) {
                            val uiResult = UiState.SchedulerTaskCreated(
                                taskId = pResult.task!!.id,
                                title = pResult.task!!.title,
                                dayOffset = java.time.temporal.ChronoUnit.DAYS.between(
                                    java.time.LocalDate.now(),
                                    java.time.Instant.ofEpochMilli(pResult.task!!.startTime.toEpochMilli())
                                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                                ).toInt(),
                                scheduledAtMillis = pResult.task!!.startTime.toEpochMilli(),
                                durationMinutes = pResult.task!!.durationMinutes,
                                isReschedule = isReschedule
                            )
                            handleCreateResult(uiResult, isReschedule = isReschedule)
                            triggerRefresh()
                        } else {
                            android.util.Log.d("SchedulerVM", "🎙️ MutationProposal contained no task, ignoring.")
                        }
                    }
                    is PipelineResult.ClarificationNeeded -> {
                        handleCreateResult(UiState.AwaitingClarification(pResult.question, com.smartsales.prism.domain.model.ClarificationType.MISSING_TIME), isReschedule = isReschedule)
                        triggerRefresh()
                    }
                    else -> {
                        android.util.Log.d("SchedulerVM", "🎙️ Ignoring non-task pipeline result: $pResult")
                    }
                }
            }
        }
    }

    /**
     * 🎙️ DEV ONLY: 手机端录音 → ASR 转写 → 调度管线
     * 
     * 与硬件 badge 完全相同的管线，跳过 BLE 下载步骤:
     * WAV 文件 → AsrService.transcribe() → Orchestrator.createScheduledTask()
     */
    override fun simulateFromMic(wavFile: java.io.File) {
        viewModelScope.launch {
            android.util.Log.d("SchedulerVM", "🎙️ Mic recording → ASR: ${wavFile.name} (${wavFile.length()} bytes)")
            _pipelineStatus.value = "🎙️ 转写中..."

            when (val asrResult = asrService.transcribe(wavFile)) {
                is AsrResult.Success -> {
                    android.util.Log.d("SchedulerVM", "🎙️ Transcribed: ${asrResult.text}")
                    _pipelineStatus.value = "处理中..."
                    val pipelineInput = PipelineInput(
                        rawText = asrResult.text,
                        isVoice = true,
                        intent = QueryQuality.CRM_TASK
                    )
                    unifiedPipeline.processInput(pipelineInput).collect { pResult ->
                        when(pResult) {
                            is PipelineResult.MutationProposal -> {
                                if (pResult.task != null) {
                                    val uiResult = UiState.SchedulerTaskCreated(
                                        taskId = pResult.task!!.id,
                                        title = pResult.task!!.title,
                                        dayOffset = java.time.temporal.ChronoUnit.DAYS.between(
                                            java.time.LocalDate.now(),
                                            java.time.Instant.ofEpochMilli(pResult.task!!.startTime.toEpochMilli())
                                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                                        ).toInt(),
                                        scheduledAtMillis = pResult.task!!.startTime.toEpochMilli(),
                                        durationMinutes = pResult.task!!.durationMinutes,
                                        isReschedule = false
                                    )
                                    handleCreateResult(uiResult)
                                    triggerRefresh()
                                } else {
                                    android.util.Log.d("SchedulerVM", "🎙️ MutationProposal contained no task, ignoring.")
                                }
                            }
                            else -> {
                                android.util.Log.d("SchedulerVM", "🎙️ Ignoring non-task pipeline result: $pResult")
                            }
                        }
                    }
                }
                is AsrResult.Error -> {
                    android.util.Log.w("SchedulerVM", "🎙️ ASR failed: ${asrResult.message}")
                    _pipelineStatus.value = "❌ 转写失败: ${asrResult.message}"
                }
            }

            // 清理临时文件
            wavFile.delete()
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
                if (isReschedule || result.isReschedule) _rescheduledDates.value += result.dayOffset
                checkExactAlarmPermission()
            }
            is UiState.SchedulerMultiTaskCreated -> {
                onMultiTaskCreated(result.tasks)
                _pipelineStatus.value = "✅ 已创建 ${result.tasks.size} 个任务"
                if (isReschedule) {
                    result.tasks.forEach { _rescheduledDates.value += it.dayOffset }
                }
                checkExactAlarmPermission()
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
     * 精确闹钟权限检查 — 首次创建有效任务后触发一次
     * 未授予时 AlarmManager 可延迟最多 1 小时
     */
    private fun checkExactAlarmPermission() {
        if (exactAlarmPrompted.compareAndSet(false, true)) {
            if (OemCompat.needsExactAlarmPermission(appContext)) {
                _exactAlarmPermissionNeeded.tryEmit(Unit)
                android.util.Log.w("SchedulerVM", "精确闹钟权限未授予，已发出提示事件")
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
    override fun triggerRefresh() {
        _refreshTrigger.tryEmit(Unit)
    }

    /**
     * Drawer 打开时调用 — 刷新 + 清理过期任务
     */
    override fun onDrawerOpened() {
        viewModelScope.launch { autoCompleteExpiredTasks() }
        triggerRefresh()
    }
    
    /**
     * 清除 Pipeline 状态
     */
    override fun clearPipelineStatus() {
        _pipelineStatus.value = null
    }

    /**
     * 清除冲突警告
     */
    override fun clearConflictWarning() {
        _conflictWarning.value = null
        _conflictedTaskIds.value = emptySet()
        _causingTaskId.value = null
    }

    /**
     * 处理冲突解决 — 顺序执行多个动作（支持复合指令如"取消A，改期B"）
     */
    override fun handleConflictResolution(resolution: com.smartsales.prism.domain.scheduler.ConflictResolution) {
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
                        val task = action.taskToReschedule
                        val text = action.rescheduleText
                        if (task != null && text != null) {
                            onReschedule(task, text)
                            android.util.Log.d("SchedulerVM", "Resolved: RESCHEDULE $task")
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

            }
        }
    }


}
