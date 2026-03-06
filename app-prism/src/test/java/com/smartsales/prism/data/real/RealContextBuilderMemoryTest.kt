package com.smartsales.prism.data.real

import com.smartsales.prism.data.fakes.FakeEntityRepository
import com.smartsales.prism.data.fakes.FakeMemoryRepository
import com.smartsales.prism.data.rl.RealReinforcementLearner
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.data.fakes.FakeUserHabitRepository
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.model.Mode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.smartsales.prism.data.fakes.FakePipelineTelemetry

/**
 * Tests for RealContextBuilder's Entity Knowledge Context (Wave 3).
 * Verifies the "First Turn Context" strategy with entity data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealContextBuilderMemoryTest {

    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var entityRepository: FakeEntityRepository
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var habitRepository: FakeUserHabitRepository
    private lateinit var reinforcementLearner: RealReinforcementLearner
    private lateinit var scheduledTaskRepository: TestScheduledTaskRepository
    private lateinit var memoryRepository: FakeMemoryRepository

    @Before
    fun setup() {
        timeProvider = FakeTimeProvider()
        habitRepository = FakeUserHabitRepository()
        reinforcementLearner = RealReinforcementLearner(habitRepository)
        entityRepository = FakeEntityRepository()
        memoryRepository = FakeMemoryRepository()

        scheduledTaskRepository = TestScheduledTaskRepository()
        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            reinforcementLearner = reinforcementLearner,
            memoryRepository = memoryRepository,
            entityRepository = entityRepository,
            scheduledTaskRepository = scheduledTaskRepository,
            telemetry = FakePipelineTelemetry()
        )
    }

    @Test
    fun `first turn loads entity knowledge`() = runTest {
        // Arrange: 添加测试实体
        entityRepository.save(EntityEntry(
            entityId = "p-001",
            entityType = EntityType.PERSON,
            displayName = "孙扬浩",
            aliasesJson = """["孙工"]""",
            jobTitle = "工程师",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ))

        // Act: 首轮消息
        val context = contextBuilder.build("聊聊孙扬浩", Mode.ANALYST, listOf("p-001"))

        // Assert: entityKnowledge 应该被填充
        assertNotNull("entityKnowledge should be populated on first turn", context.entityKnowledge)
        assertTrue("Should contain entity name", context.entityKnowledge!!.contains("孙扬浩"))
        // Aliases were removed from entityKnowledge to avoid leaking offensive names to LLM
    }

    @Test
    fun `first turn with no entities returns null entityKnowledge`() = runTest {
        // Act: 首轮消息，无实体
        val context2 = contextBuilder.build("where is it", Mode.ANALYST)

        // Assert: entityKnowledge 应为 null
        assertNull("entityKnowledge should be null with no entities", context2.entityKnowledge)
    }

    @Test
    fun `subsequent turns use cached entity knowledge`() = runTest {
        // Arrange: 添加实体
        entityRepository.save(EntityEntry(
            entityId = "p-002",
            entityType = EntityType.PERSON,
            displayName = "张总",
            aliasesJson = "[]",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ))
        
        // 首轮加载
        val firstContext = contextBuilder.build("你好", Mode.ANALYST, listOf("p-002"))
        assertNotNull(firstContext.entityKnowledge)
        
        // 模拟 LLM/EntityWriter 将实体加入到上下文中 (Active Set)
        contextBuilder.updateEntityInSession("p-002", com.smartsales.prism.domain.pipeline.EntityRef("p-002", "张总", EntityType.PERSON.name))
        
        // Arrange: 建立对话历史（触发 subsequent turn 条件）
        contextBuilder.recordUserMessage("你好")
        contextBuilder.recordAssistantMessage("你好！")

        // Act: 第二轮（不应重新加载）
        val newSessionContext = contextBuilder.build("Session 2 start", Mode.ANALYST)

        // Assert: entityKnowledge 应该仍然存在（从 RAM 读取，非重新加载）
        assertNotNull("entityKnowledge should persist from session cache", newSessionContext.entityKnowledge)
        assertEquals("Should be same cached value", firstContext.entityKnowledge, newSessionContext.entityKnowledge)
    }
    
    @Test
    fun `resetSession clears entity knowledge and re-enables loading`() = runTest {
        // Arrange: 添加实体并首轮加载
        entityRepository.save(EntityEntry(
            entityId = "a-001",
            entityType = EntityType.ACCOUNT,
            displayName = "摩升泰",
            aliasesJson = "[]",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ))
        contextBuilder.build("test", Mode.ANALYST, listOf("a-001"))
        
        // Reset 会话
        contextBuilder.resetSession()
        
        // Act: 新会话首轮
        val context = contextBuilder.build("test memory", Mode.ANALYST, listOf("a-001"))
        
        // Assert: 应重新加载
        assertNotNull("entityKnowledge should reload after session reset", context.entityKnowledge)
        assertTrue("Should contain account", context.entityKnowledge!!.contains("摩升泰"))
    }

    @Test
    fun `entity knowledge groups by type`() = runTest {
        // Arrange: 添加不同类型实体
        entityRepository.save(EntityEntry(
            entityId = "p-100",
            entityType = EntityType.PERSON,
            displayName = "李明",
            aliasesJson = "[]",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ))
        entityRepository.save(EntityEntry(
            entityId = "a-100",
            entityType = EntityType.ACCOUNT,
            displayName = "华为",
            aliasesJson = "[]",
            lastUpdatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        ))

        // Act
        val context = contextBuilder.build("test", Mode.ANALYST, listOf("p-100", "a-100"))

        // Assert: JSON 应包含分组
        assertNotNull(context.entityKnowledge)
        assertTrue("Should have people group", context.entityKnowledge!!.contains("people"))
        assertTrue("Should have accounts group", context.entityKnowledge!!.contains("accounts"))
        assertTrue("Should contain person name", context.entityKnowledge!!.contains("李明"))
        assertTrue("Should contain account name", context.entityKnowledge!!.contains("华为"))
    }
}
