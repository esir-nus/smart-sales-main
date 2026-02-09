package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.activity.ActivityAction
import com.smartsales.prism.domain.activity.ActivityPhase
import com.smartsales.prism.domain.activity.AgentActivityController
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.pipeline.DeliverableType
import com.smartsales.prism.domain.pipeline.ExecutionPlan
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.RetrievalScope
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TimelineItemModel
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Orchestrator 用于骨架开发
 * 返回模拟数据，不会崩溃
 */
@Singleton
class FakeOrchestrator @Inject constructor(
    private val activityController: AgentActivityController,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val timeProvider: TimeProvider
) : Orchestrator {
    
    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()
    
    override suspend fun processInput(input: String): UiState {
        // 开始活动追踪
        activityController.startPhase(ActivityPhase.PLANNING, ActivityAction.THINKING)
        activityController.appendTrace("分析用户输入: \"$input\"")
        
        // 模拟网络延迟
        delay(400)
        activityController.appendTrace("检索相关上下文...")
        delay(400)
        activityController.appendTrace("生成回复策略...")

        val result = when {
            input.startsWith("/plan") -> {
                // Scenario: Analyst Plan Card
                UiState.PlanCard(
                    ExecutionPlan(
                        retrievalScope = RetrievalScope.HOT_AND_CEMENT,
                        deliverables = listOf(
                            DeliverableType.KEY_INSIGHT,
                            DeliverableType.CHART,
                            DeliverableType.CHAT_RESPONSE
                        )
                    )
                )
            }
            input.startsWith("/think") -> {
                // Scenario: Thinking State
                UiState.Thinking(
                    hint = "正在分析销售数据..."
                )
            }
            input.startsWith("/md") -> {
                // Scenario: Complex Markdown
                UiState.Response(
                    """
                    # 销售分析报告
                    
                    根据**最近30天**的数据，我们发现：
                    *   转化率提升了 `15%`
                    *   客户满意度达到 ⭐⭐⭐⭐⭐
                    
                    > 建议继续保持当前的沟通策略。
                    """.trimIndent()
                )
            }
            input.startsWith("/error") -> {
                // Scenario: Error State
                activityController.error("模拟的网络连接错误 (Error 500)")
                throw RuntimeException("模拟的网络连接错误 (Error 500)")
            }
            else -> {
                // Default: Echo based on mode
                when (_currentMode.value) {
                    Mode.COACH -> UiState.Response("🎯 [Coach] 收到: $input")
                    Mode.ANALYST -> UiState.Response("🔬 [Analyst] 收到: $input")
                    else -> UiState.Response("🤖 [System] 收到: $input")
                }
            }
        }
        
        // 完成活动追踪
        activityController.complete()
        return result
    }
    
    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
    }
    
    override suspend fun createScheduledTask(input: String, replaceItemId: String?): UiState {
        // Fake: 模拟任务创建
        delay(500)
        activityController.startPhase(ActivityPhase.EXECUTING, ActivityAction.THINKING)
        activityController.appendTrace("解析输入: \"$input\"")
        delay(300)
        
        // Wave 8: Atomicity simulation
        if (replaceItemId != null) {
            activityController.appendTrace("替换旧任务: $replaceItemId")
             val items = scheduledTaskRepository.getTimelineItems(0).first()
             val item = items.find { it.id == replaceItemId }
             if (item is TimelineItemModel.Task) {
                 activityController.appendTrace("上下文: ${item.title}")
             }
        }
        
        activityController.appendTrace("创建日历事件...")
        
        // 实际插入任务到 Repository
        val tomorrow = timeProvider.today.plusDays(1)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val startTime = tomorrow.atTime(3, 0)
        
        val newTask = TimelineItemModel.Task(
            id = "", // Repository 会生成
            timeDisplay = "03:00",
            title = "赶飞机",
            isDone = false,
            hasAlarm = true,
            isSmartAlarm = true,
            dateRange = "${startTime.format(formatter)} - ...",
            location = "T2航站楼",
            notes = null,
            keyPerson = null,
            highlights = "必须带好护照",
            alarmCascade = listOf("-1h", "-15m", "-5m"),
            startTime = startTime.atZone(timeProvider.zoneId).toInstant(),
            endTime = startTime.plusHours(1).atZone(timeProvider.zoneId).toInstant()
        )
        val taskId = scheduledTaskRepository.insertTask(newTask)
        
        // Wave 8: Delete old if replacing
        if (replaceItemId != null) {
            scheduledTaskRepository.deleteItem(replaceItemId)
        }
        
        delay(300)
        activityController.complete()
        return UiState.SchedulerTaskCreated(
            taskId = taskId,
            title = "赶飞机",
            dayOffset = 1,
            scheduledAtMillis = startTime.atZone(timeProvider.zoneId).toInstant().toEpochMilli(),
            durationMinutes = 60
        )
    }

    // Wave 8: Deleted processSchedulerAction fake


}
