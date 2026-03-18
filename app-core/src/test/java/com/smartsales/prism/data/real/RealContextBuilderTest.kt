package com.smartsales.prism.data.real

import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.pipeline.RealUnifiedPipeline

import com.smartsales.core.test.fakes.FakeEntityRepository
import com.smartsales.prism.data.rl.RealReinforcementLearner
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.core.test.fakes.FakeUserHabitRepository
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.core.test.fakes.FakeMemoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * RealContextBuilder 测试 — Wave 3 习惯注入验证
 * 
 * 测试目标:
 * 1. build() 调用 loadUserHabits() — 全局习惯
 * 2. EntityWriter write-through 更新 RAM Section 1
 */
class RealContextBuilderTest {
    
    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var habitRepository: FakeUserHabitRepository
    private lateinit var reinforcementLearner: RealReinforcementLearner
    private lateinit var memoryRepository: FakeMemoryRepository
    private lateinit var scheduledTaskRepository: TestScheduledTaskRepository
    private lateinit var mockMemoryRepo: FakeMemoryRepository
    private lateinit var mockEntityRepo: FakeEntityRepository
    private lateinit var mockHistoryRepo: com.smartsales.core.test.fakes.FakeHistoryRepository
    
    @Before
    fun setup() {
        timeProvider = FakeTimeProvider()
        habitRepository = FakeUserHabitRepository()
        habitRepository.clear()  // Reset seed data for test isolation
        reinforcementLearner = RealReinforcementLearner(habitRepository)
        memoryRepository = FakeMemoryRepository()
        scheduledTaskRepository = TestScheduledTaskRepository()
        // Initialize mock repos for the new constructor call
        mockMemoryRepo = FakeMemoryRepository()
        mockEntityRepo = FakeEntityRepository()
        mockHistoryRepo = com.smartsales.core.test.fakes.FakeHistoryRepository()
        
        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            reinforcementLearner = reinforcementLearner,
            memoryRepository = mockMemoryRepo,
            entityRepository = mockEntityRepo,
            scheduledTaskRepository = scheduledTaskRepository,
            historyRepository = mockHistoryRepo,
            telemetry = com.smartsales.prism.data.fakes.FakePipelineTelemetry()
        )
    }
    
    @Test
    fun `build() injects global habits`() = runTest {
        // Arrange: 添加全局习惯
        habitRepository.observe(
            key = "preferred_meeting_time",
            value = "morning",
            entityId = null,
            source = ObservationSource.USER_POSITIVE
        )
        
        // Act
        val context = contextBuilder.build("hello", Mode.ANALYST)
        
        // Assert
        assertNotNull(context.habitContext)
        assertEquals(1, context.habitContext!!.userHabits.size)
        assertEquals("preferred_meeting_time", context.habitContext!!.userHabits[0].habitKey)
        assertEquals(0, context.habitContext!!.clientHabits.size) // null entityIds
    }
    
    @Test
    fun `build() includes all EnhancedContext fields`() = runTest {
        // Act
        val context = contextBuilder.build("test input", Mode.ANALYST)
        
        // Assert: Verify all fields are set
        assertEquals("test input", context.userText)
        assertEquals(Mode.ANALYST, context.modeMetadata.currentMode)
        assertNotNull(context.currentDate)
        assertNotNull(context.habitContext) // Wave 3 field
    }
    
    @Test
    fun `habitContext is populated even with no habits`() = runTest {
        // Act: No habits seeded
        val context = contextBuilder.build("test input", Mode.ANALYST)
        
        // Assert: habitContext should exist but be empty
        assertNotNull(context.habitContext)
        assertEquals(0, context.habitContext!!.userHabits.size)
        assertEquals(0, context.habitContext!!.clientHabits.size)
    }
    
    @Test
    fun `multiple habit observations aggregate correctly`() = runTest {
        // Arrange: 添加多个全局习惯
        habitRepository.observe(
            key = "preferred_meeting_time",
            value = "morning",
            entityId = null,
            source = ObservationSource.USER_POSITIVE
        )
        habitRepository.observe(
            key = "communication_style",
            value = "direct",
            entityId = null,
            source = ObservationSource.INFERRED
        )
        
        // Act
        val context = contextBuilder.build("hello", Mode.ANALYST)
        
        // Assert
        assertNotNull(context.habitContext)
        assertEquals(2, context.habitContext!!.userHabits.size)
    }

    @Test
    fun `build() injects schedule context`() = runTest {
        // Arrange: Seed a task
        timeProvider.setDateTime(2026, 2, 2, 9, 0)
        val task = com.smartsales.prism.domain.scheduler.ScheduledTask(
            id = "t1",
            timeDisplay = "10:00",
            title = "Important Meeting",
            urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.L1_CRITICAL,
            startTime = timeProvider.now.plusSeconds(60 * 60),
            durationMinutes = 45,
            keyPerson = "Boss",
            location = "Room 101"
        )
        scheduledTaskRepository.items.emit(listOf(task))

        // Act
        val context = contextBuilder.build("hello", Mode.ANALYST)

        // Assert
        assertNotNull(context.scheduleContext)
        val schedule = context.scheduleContext!!
        assert(schedule.contains("Important Meeting"))
        assert(schedule.contains("关键人: Boss"))
        assert(schedule.contains("地点: Room 101"))
        assertNotNull(context.schedulerPatternContext)
        val pattern = context.schedulerPatternContext!!
        assertEquals(1, pattern.upcomingTaskCount)
        assertEquals("morning", pattern.preferredTimeWindow)
        assertEquals(45, pattern.preferredDurationMinutes)
        assertEquals("same_day", pattern.leadTimeStyle)
        assertEquals("critical_heavy", pattern.urgencyStyle)
    }

    @Test
    fun `build() with ContextDepth FULL loads all 3 sections`() = runTest {
        // Arrange
        habitRepository.observe("key", "val", null, ObservationSource.USER_POSITIVE)
        val entityId = "e123"
        val entry = com.smartsales.prism.domain.memory.EntityEntry(
            entityId = entityId,
            displayName = "Frank",
            aliasesJson = "[]",
            entityType = com.smartsales.prism.domain.memory.EntityType.PERSON,
            createdAt = 0L,
            lastUpdatedAt = 0L
        )
        mockEntityRepo.save(entry)
        
        // Act (First turn, SHOULD search memory)
        val context = contextBuilder.build(
            userText = "hello", 
            mode = Mode.ANALYST,
            resolvedEntityIds = listOf(entityId),
            depth = com.smartsales.core.context.ContextDepth.FULL
        )

        // Assert
        assertNotNull("FULL should load habit context", context.habitContext)
        assertEquals("Should have queried global habits", 1, habitRepository.getGlobalHabitsCount)
        assertNotNull("FULL should load entity knowledge", context.entityKnowledge)
        assertEquals("Should have queried entity by ID", 1, mockEntityRepo.getByIdCount)
    }

    @Test
    fun `build() with ContextDepth MINIMAL bypasses DB reads`() = runTest {
        // Arrange
        val entityId = "e123"
        val entry = com.smartsales.prism.domain.memory.EntityEntry(
            entityId = entityId,
            displayName = "Frank",
            aliasesJson = "[]",
            entityType = com.smartsales.prism.domain.memory.EntityType.PERSON,
            createdAt = 0L,
            lastUpdatedAt = 0L
        )
        mockEntityRepo.save(entry)

        // Act (First turn, but MINIMAL depth)
        val context = contextBuilder.build(
            userText = "hello",
            mode = Mode.ANALYST,
            resolvedEntityIds = listOf(entityId),
            depth = com.smartsales.core.context.ContextDepth.MINIMAL
        )

        // Assert Data Payload (Testing Illusion Preventer)
        assertNull("MINIMAL should NOT load habit context", context.habitContext)
        assertNull("MINIMAL should NOT load entity knowledge", context.entityKnowledge)
        assertNull("MINIMAL should NOT load schedule context", context.scheduleContext)
        assertNull("MINIMAL should NOT load scheduler pattern context", context.schedulerPatternContext)

        // Assert Physical Layer Bypasses (Mathematical Proof)
        assertEquals("MINIMAL should physically bypass UserHabitRepository LTM queries", 0, habitRepository.getGlobalHabitsCount)
        assertEquals("MINIMAL should physically bypass EntityRepository LTM queries", 0, mockEntityRepo.getByIdCount)
    }

    @Test
    fun `Session history pruning strictly bounds memory to 20 turns`() = runTest {
        // Act: Flood the kernel with 50 messages (25 turns)
        for (i in 1..25) {
            contextBuilder.recordUserMessage("user text $i")
            contextBuilder.recordAssistantMessage("bot response $i")
        }

        // Assert: Kernel must defensively prune the history
        val history = contextBuilder.getSessionHistory()
        assertEquals("History must be strictly bounded to 20 messages (10 turns)", 20, history.size)
        // 25 total turns. We keep the last 10 turns (turns 16 through 25).
        // First message in the kept history should be user text 16.
        assertEquals("user text 16", history.first().content)
        assertEquals("bot response 25", history.last().content)
    }

    @Test
    fun `build() ignores unbounded MemoryEntry table and guarantees O(1) entity token scale`() = runTest {
        // Arrange: Create a single Entity Entry
        val entityId = "e_poison"
        val entry = com.smartsales.prism.domain.memory.EntityEntry(
            entityId = entityId,
            displayName = "Poison Corp",
            aliasesJson = "[]",
            entityType = com.smartsales.prism.domain.memory.EntityType.ACCOUNT,
            createdAt = 0L,
            lastUpdatedAt = 0L
        )
        mockEntityRepo.save(entry)
        
        // Arrange: Inject 100 random noise memory entries into the LTM (The Poison)
        for(i in 0..99) {
            mockMemoryRepo.save(com.smartsales.prism.domain.memory.MemoryEntry(
                entryId = "m_$i",
                sessionId = "s_1",
                content = "Massive Noise $i",
                entryType = com.smartsales.prism.domain.memory.MemoryEntryType.TASK_RECORD,
                createdAt = 0L, 
                updatedAt = 0L,
                structuredJson = """{"relatedEntityIds":["$entityId"]}""",
                workflow = "test"
            ))
        }

        // Act: Force FULL depth context assembly
        val context = contextBuilder.build("status", Mode.ANALYST, listOf(entityId), com.smartsales.core.context.ContextDepth.FULL)

        // Assert: Kernel Context should NOT scale with the 100 LTM memories
        assertNotNull(context.entityKnowledge)
        val knowledge = context.entityKnowledge!!
        
        assertTrue("Context must contain the distilled entity graph", knowledge.contains("Poison Corp"))
        assertFalse(
            "Token bounded contextualization MUST drop unbounded raw memory notes", 
            knowledge.contains("Massive Noise")
        )
        
        // Exact generated JSON length check to prove O(1) scaling
        // {"accounts":[{"name":"Poison Corp"}]} => 37 chars
        assertEquals("EntityKnowledge length must be strictly bounded to the struct size, ignoring 100+ DB memories", 37, knowledge.length)
    }
}
