package com.smartsales.prism.domain.session

import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.core.test.fakes.FakeHistoryRepository
import com.smartsales.core.test.fakes.FakeMemoryRepository
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import com.smartsales.core.test.fakes.FakeSystemEventBus
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

import com.smartsales.prism.domain.rl.HabitContext

/**
 * Anti-Illusion W1 (Session)
 *
 * 目标：不使用 Mockito，证明 RAM (SessionWorkingSet) 和 SSD (HistoryRepository) 的严格同步。
 * 目前核心痛点：UI 层双写 (ViewModel 持有 HistoryRepository 实例)。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionAntiIllusionIntegrationTest {

    private lateinit var historyRepo: FakeHistoryRepository
    private lateinit var memoryRepo: FakeMemoryRepository
    private lateinit var contextBuilder: RealContextBuilder

    @Before
    fun setup() {
        historyRepo = FakeHistoryRepository()
        memoryRepo = FakeMemoryRepository()

        val timeProvider = object : TimeProvider {
            override val now: Instant = Instant.now()
            override val today: LocalDate = LocalDate.now()
            override val currentTime: java.time.LocalTime = java.time.LocalTime.now()
            override val zoneId: ZoneId = ZoneId.systemDefault()
            override fun formatForLlm(): String = "Today"
        }

        val stubRl = object : ReinforcementLearner {
            override suspend fun processObservations(observations: List<com.smartsales.prism.domain.rl.RlObservation>) {}
            override suspend fun loadUserHabits() = HabitContext(emptyList(), emptyList(), emptyMap())
            override suspend fun loadClientHabits(entityIds: List<String>) = HabitContext(emptyList(), emptyList(), emptyMap())
        }

        val stubTelemetry = object : PipelineTelemetry {
            override fun recordEvent(phase: com.smartsales.prism.domain.telemetry.PipelinePhase, message: String) {}
            override fun recordError(phase: com.smartsales.prism.domain.telemetry.PipelinePhase, message: String, throwable: Throwable?) {}
        }

        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            reinforcementLearner = stubRl,
            memoryRepository = memoryRepo,
            entityRepository = FakeEntityRepository(),
            scheduledTaskRepository = FakeScheduledTaskRepository(),
            historyRepository = historyRepo,
            telemetry = stubTelemetry
        )
    }

    @Test
    fun `session state mathematically syncs between RAM and SSD without UI double write`() = runTest {
        // 1. 初始化会话
        val sessionId = historyRepo.createSession("张三", "需求沟通")
        contextBuilder.loadSession(sessionId, emptyList())

        // 2. 模拟 Pipeline 核心流 (不通过 ViewModel)
        val userText = "帮我查一下销量"
        val aiText = "好的，销量是 1000"

        // UI 应当仅依赖 Kernel 接口写入状态，而不直接操作 HistoryRepository 
        contextBuilder.recordUserMessage(userText)
        contextBuilder.recordAssistantMessage(aiText)

        // 3. 验证 RAM (ContextBuilder 内部的 _sessionHistory)
        val ramHistory = contextBuilder.getSessionHistory()
        assertEquals(2, ramHistory.size)
        assertEquals("user", ramHistory[0].role)
        assertEquals(userText, ramHistory[0].content)

        // 4. 验证 SSD (FakeHistoryRepository)
        // [ANTI-ILLUSION 核心]: 当前架构下，这是断开的 (Illusion!) 的暴露点
        // 因为 ViewModel 层在维护历史记录并向 SSD 双写。
        val ssdHistory = historyRepo.getMessages(sessionId)
        
        // 期望：架构解耦完美，KernelWriteBack 或 ContextBuilder 自己将 message 落盘 
        // 之前我们在测试里写了期望 0 (用来暴露 Illusion)。
        // 现在 ContextBuilder 已经修复，预期这里是 2，证明 RAM 和 SSD 是完美同步的。
        assertEquals("Anti-Illusion 修复成功：Kernel 已经自动将数据同步至 HistoryRepository", 2, ssdHistory.size)
        assertEquals("user", ssdHistory[0].let { it as? com.smartsales.prism.domain.model.ChatMessage.User }?.content?.let { "user" })
        assertEquals(userText, (ssdHistory[0] as com.smartsales.prism.domain.model.ChatMessage.User).content)
    }

    @Test
    fun `break-it examiner gracefully handles extreme inputs`() = runTest {
        val sessionId = historyRepo.createSession("李四", "Edge Case Test")
        contextBuilder.loadSession(sessionId, emptyList())

        // 1. Empty string
        contextBuilder.recordUserMessage("")
        
        // 2. Blank string
        contextBuilder.recordAssistantMessage("   ")
        
        // 3. Massive string
        val hugeString = "A".repeat(10_000)
        contextBuilder.recordUserMessage(hugeString)

        val ramHistory = contextBuilder.getSessionHistory()
        val ssdHistory = historyRepo.getMessages(sessionId)
        
        // Both RAM and SSD should survive and map correctly
        assertEquals("RAM survived edge cases", 3, ramHistory.size)
        assertEquals("SSD survived edge cases", 3, ssdHistory.size)
        assertEquals("Massive string persisted without crashing", hugeString, ramHistory[2].content)
    }
}
