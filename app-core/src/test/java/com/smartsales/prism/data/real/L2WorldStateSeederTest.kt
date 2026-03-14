package com.smartsales.prism.data.real

import com.smartsales.core.context.ContextDepth
import com.smartsales.core.context.RealContextBuilder
import com.smartsales.core.test.fakes.*
import com.smartsales.prism.data.fakes.FakePipelineTelemetry
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import com.smartsales.prism.data.rl.RealReinforcementLearner
import com.smartsales.data.crm.writer.RealEntityWriter
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.scheduler.ScheduledTask
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class L2WorldStateSeederTest {

    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var fakeEntityRepo: FakeEntityRepository
    private lateinit var fakeMemoryRepo: FakeMemoryRepository
    private lateinit var fakeHabitRepo: FakeUserHabitRepository
    private lateinit var fakeTaskRepo: ScheduledTaskRepository
    private lateinit var entityWriter: RealEntityWriter

    @Before
    fun setup() {
        fakeEntityRepo = FakeEntityRepository()
        fakeMemoryRepo = FakeMemoryRepository()
        fakeHabitRepo = FakeUserHabitRepository()
        
        val rl = RealReinforcementLearner(fakeHabitRepo)
        val timeProvider = FakeTimeProvider()
        val historyRepo = FakeHistoryRepository()

        val fakeTaskRepo = object : ScheduledTaskRepository {
            override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> = emptyFlow()
            override suspend fun insertTask(task: ScheduledTask): String = "fake-task"
            override suspend fun getTask(id: String): ScheduledTask? = null
            override suspend fun updateTask(task: ScheduledTask) {}
            override suspend fun deleteItem(id: String) {}
            override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()
            override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null
            override fun observeByEntityId(entityId: String): kotlinx.coroutines.flow.Flow<List<ScheduledTask>> = kotlinx.coroutines.flow.flowOf(emptyList())
        }
        this.fakeTaskRepo = fakeTaskRepo

        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            reinforcementLearner = rl,
            memoryRepository = fakeMemoryRepo,
            entityRepository = fakeEntityRepo,
            scheduledTaskRepository = fakeTaskRepo,
            historyRepository = historyRepo,
            telemetry = FakePipelineTelemetry()
        )

        entityWriter = RealEntityWriter(
            entityRepository = fakeEntityRepo,
            timeProvider = timeProvider,
            kernelWriteBack = contextBuilder,
            appScope = TestScope(UnconfinedTestDispatcher())
        )
    }

    @Test
    fun `Scenario 1 - Fragmented Aliases`() = runTest {
        seedWorldState(entityWriter, fakeMemoryRepo, fakeHabitRepo, fakeEntityRepo, fakeTaskRepo) {
            val baseId = entityClue("字节")
            // Simulate the LLM knowing that "字节跳动" is actually the same company
            entityClue("字节跳动", resolvedId = baseId)
            // And "ByteDance"
            val finalId = entityClue("ByteDance", resolvedId = baseId)
            
            // Now the exact string alias should map to the correct entity
            val resolved1 = fakeEntityRepo.findByAlias("字节跳动").firstOrNull()
            assertNotNull(resolved1)
            assertEquals("All aliases should resolve to same entity", finalId, resolved1!!.entityId)
            
            val allEntities = fakeEntityRepo.getAll(100)
            assertEquals("Should only be exactly 1 entity in the DB after deduplication", 1, allEntities.size)
        }
    }

    @Test
    fun `Scenario 2 - Overlapping Noise`() = runTest {
        seedWorldState(entityWriter, fakeMemoryRepo, fakeHabitRepo, fakeEntityRepo, fakeTaskRepo) {
            // Zhang Wei at ByteDance
            val id1 = entityClue("张伟 (字节)", "字节跳动", "CEO")
            entityClue("张伟", resolvedId = id1)
            
            // Zhang Wei at Tencent
            val id2 = entityClue("张伟 (腾讯)", "腾讯", "CTO")
            entityClue("张伟", resolvedId = id2)
            
            val allEntities = fakeEntityRepo.getAll(100)
            // Expecting 2 total entities (2 distinct people)
            assertEquals("Should be 2 distinct core entities created", 2, allEntities.size)
            
            val aliasMatches = fakeEntityRepo.findByAlias("张伟")
            assertEquals("Searching alias '张伟' should return both entities", 2, aliasMatches.size)
        }
    }

    @Test
    fun `Scenario 3 - Contextual Assembly`() = runTest {
        seedWorldState(entityWriter, fakeMemoryRepo, fakeHabitRepo, fakeEntityRepo, fakeTaskRepo) {
            val cxoId = entityClue("马化腾", "腾讯")
            
            memory("Discussed the new cloud strategy", cxoId)
            memory("He is out of office next week", cxoId)
            
            habit("communication_style", "direct", cxoId)
            habit("contact_method", "wechat", cxoId)
            
            // Rebuild context like the pipeline would
            val enhancedContext = contextBuilder.build(
                userText = "what do we know about Pony?",
                mode = com.smartsales.prism.domain.model.Mode.ANALYST,
                resolvedEntityIds = listOf(cxoId),
                depth = ContextDepth.FULL
            )
            
            // The context should contain elements from memory and rl
            val contextString = enhancedContext.toString()
            println("--- Assembled Context ---\n$contextString\n------------------------")
            
            assertTrue("Context should contain the company", contextString.contains("腾讯"))
            
            val allHabits = fakeHabitRepo.getByEntity(cxoId)
            assertEquals("Habits should be linked to entity", 2, allHabits.size)
            assertTrue("Should contain communication_style", allHabits.any { it.habitKey == "communication_style" && it.habitValue == "direct" })
            
            val allMemories = fakeMemoryRepo.getByEntityId(cxoId, limit = 10)
            assertEquals("Memory should be successfully injected and linked to entity", 2, allMemories.size)
            assertTrue("Memory content is present", allMemories.any { it.content.contains("Discussed the new cloud strategy") })
        }
    }

    @Test
    fun `Scenario 4 - Chaos Seed Injection`() = runTest {
        seedWorldState(entityWriter, fakeMemoryRepo, fakeHabitRepo, fakeEntityRepo, fakeTaskRepo) {
            injectChaosSeed()
        }
        
        val allMemories = fakeMemoryRepo.getAll(100)
        assertEquals("Should be 5 memories from chaos seed", 5, allMemories.size)
        
        val mem005 = allMemories.find { it.entryId == "mem_005" }
        assertNotNull("mem_005 should be successfully injected", mem005)
        assertEquals("VERBAL_WIN", mem005?.outcomeStatus)
        assertEquals("技术拉通并解决异议，法务走账中", mem005?.displayContent)
        
        val allEntities = fakeEntityRepo.getAll(10)
        assertEquals("Should be 2 entities from chaos seed", 2, allEntities.size)
        
        val ent101 = allEntities.find { it.entityId == "ent_101" }
        assertNotNull("ent_101 should be successfully injected", ent101)
        assertEquals("CONTRACT_NEGOTIATION", ent101?.dealStage)
        assertEquals("字节跳动", ent101?.displayName)
    }
}
