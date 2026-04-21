package com.smartsales.prism.domain.memory

import com.smartsales.prism.data.memory.RealScheduleBoard
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * ScheduleBoard 单元测试 — 冲突检测逻辑
 */
class ScheduleBoardTest {
    
    private lateinit var scheduleBoard: FakeScheduleBoard
    
    @Before
    fun setup() {
        scheduleBoard = FakeScheduleBoard()
    }
    
    // ===== 无冲突场景 =====
    
    @Test
    fun `empty schedule - no conflict`() = runTest {
        // Given: 空日程
        scheduleBoard.setItems(emptyList())
        
        // When: 检查 3pm, 60分钟
        val result = scheduleBoard.checkConflict(
            proposedStart = 15 * 60 * 60 * 1000L, // 3pm
            durationMinutes = 60
        )
        
        // Then: 无冲突
        assertEquals(ConflictResult.Clear, result)
    }
    
    @Test
    fun `adjacent meetings - no conflict`() = runTest {
        // Given: 2-3pm 已有会议
        scheduleBoard.setItems(listOf(
            createScheduleItem("1", hour = 14, durationMin = 60) // 2-3pm
        ))
        
        // When: 检查 3-4pm
        val result = scheduleBoard.checkConflict(
            proposedStart = 15 * 60 * 60 * 1000L, // 3pm
            durationMinutes = 60
        )
        
        // Then: 无冲突 (相邻不重叠)
        assertEquals(ConflictResult.Clear, result)
    }
    
    // ===== 冲突场景 =====
    
    @Test
    fun `overlapping meetings - conflict`() = runTest {
        // Given: 2-4pm 已有会议
        scheduleBoard.setItems(listOf(
            createScheduleItem("1", hour = 14, durationMin = 120) // 2-4pm
        ))
        
        // When: 检查 3-5pm
        val result = scheduleBoard.checkConflict(
            proposedStart = 15 * 60 * 60 * 1000L, // 3pm
            durationMinutes = 120
        )
        
        // Then: 冲突
        assertTrue(result is ConflictResult.Conflict)
        val conflict = result as ConflictResult.Conflict
        assertEquals(1, conflict.overlaps.size)
    }
    
    @Test
    fun `contained meeting - conflict`() = runTest {
        // Given: 2-5pm 已有会议
        scheduleBoard.setItems(listOf(
            createScheduleItem("1", hour = 14, durationMin = 180) // 2-5pm
        ))
        
        // When: 检查 3-4pm (包含在内)
        val result = scheduleBoard.checkConflict(
            proposedStart = 15 * 60 * 60 * 1000L, // 3pm
            durationMinutes = 60
        )
        
        // Then: 冲突
        assertTrue(result is ConflictResult.Conflict)
    }

    @Test
    fun `zero-duration exact task inside occupied slot still conflicts`() = runTest {
        scheduleBoard.setItems(listOf(
            createScheduleItem("1", hour = 21, durationMin = 60)
        ))

        val result = scheduleBoard.checkConflict(
            proposedStart = 21 * 60 * 60 * 1000L,
            durationMinutes = 0
        )

        assertTrue(result is ConflictResult.Conflict)
        val conflict = result as ConflictResult.Conflict
        assertEquals(1, conflict.overlaps.size)
        assertEquals("1", conflict.overlaps.first().entryId)
    }

    @Test
    fun `zero-duration exact task at free point stays clear`() = runTest {
        scheduleBoard.setItems(listOf(
            createScheduleItem("1", hour = 20, durationMin = 60)
        ))

        val result = scheduleBoard.checkConflict(
            proposedStart = 21 * 60 * 60 * 1000L,
            durationMinutes = 0
        )

        assertEquals(ConflictResult.Clear, result)
    }
    
    // ===== 冲突策略场景 =====
    
    @Test
    fun `coexisting tasks - no conflict`() = runTest {
        // Given: 3-4pm COEXISTING 任务
        scheduleBoard.setItems(listOf(
            createScheduleItem("1", hour = 15, durationMin = 60, policy = ConflictPolicy.COEXISTING)
        ))
        
        // When: 检查同一时间 3-4pm
        val result = scheduleBoard.checkConflict(
            proposedStart = 15 * 60 * 60 * 1000L, // 3pm
            durationMinutes = 60
        )
        
        // Then: 无冲突 (COEXISTING 任务不冲突)
        assertEquals(ConflictResult.Clear, result)
    }
    
    @Test
    fun `background task - no conflict`() = runTest {
        // Given: 3-4pm BACKGROUND 任务
        scheduleBoard.setItems(listOf(
            createScheduleItem("1", hour = 15, durationMin = 60, policy = ConflictPolicy.BACKGROUND)
        ))
        
        // When: 检查同一时间 3-4pm
        val result = scheduleBoard.checkConflict(
            proposedStart = 15 * 60 * 60 * 1000L, // 3pm
            durationMinutes = 60
        )
        
        // Then: 无冲突 (BACKGROUND 任务不冲突)
        assertEquals(ConflictResult.Clear, result)
    }

    @Test
    fun `existing fire off task does not block later exact task`() = runTest {
        scheduleBoard.setItems(
            listOf(
                ScheduleItem(
                    entryId = "fireoff",
                    title = "提醒我喝水",
                    scheduledAt = 15 * 60 * 60 * 1000L,
                    durationMinutes = 0,
                    durationSource = DurationSource.DEFAULT,
                    urgencyLevel = com.smartsales.prism.domain.scheduler.UrgencyLevel.FIRE_OFF,
                    conflictPolicy = ConflictPolicy.EXCLUSIVE
                )
            )
        )

        val result = scheduleBoard.checkConflict(
            proposedStart = 15 * 60 * 60 * 1000L,
            durationMinutes = 30
        )

        assertEquals(ConflictResult.Clear, result)
    }

    @Test
    fun `real schedule board queries date range from injected local day`() = runTest {
        val repository = CapturingScheduledTaskRepository()
        val timeProvider = object : TimeProvider {
            override val now: Instant = Instant.parse("2026-04-21T23:58:00Z")
            override val today: LocalDate = LocalDate.of(2026, 4, 22)
            override val currentTime: LocalTime = LocalTime.of(7, 58)
            override val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
            override fun formatForLlm(): String = "2026年4月22日（周三）07:58"
        }

        RealScheduleBoard(repository, timeProvider)

        assertTrue("RealScheduleBoard did not query the repository", repository.awaitInitialQuery())
        assertEquals(LocalDate.of(2026, 4, 22), repository.lastQueryStart)
        assertEquals(LocalDate.of(2026, 4, 29), repository.lastQueryEnd)
    }
    
    // ===== Helper =====
    
    private fun createScheduleItem(
        id: String,
        hour: Int,
        durationMin: Int,
        policy: ConflictPolicy = ConflictPolicy.EXCLUSIVE
    ): ScheduleItem {
        return ScheduleItem(
            entryId = id,
            title = "Test Task $id",
            scheduledAt = hour * 60 * 60 * 1000L,
            durationMinutes = durationMin,
            durationSource = DurationSource.DEFAULT,
            conflictPolicy = policy
        )
    }
}

/**
 * 测试用 FakeScheduleBoard
 */
class FakeScheduleBoard : ScheduleBoard {
    private val _items = MutableStateFlow<List<ScheduleItem>>(emptyList())
    override val upcomingItems: StateFlow<List<ScheduleItem>> = _items
    
    fun setItems(items: List<ScheduleItem>) {
        _items.value = items
    }
    
    override suspend fun checkConflict(
        proposedStart: Long,
        durationMinutes: Int,
        excludeId: String?
    ): ConflictResult {
        val overlaps = _items.value.filter { slot ->
            slot.entryId != excludeId &&
            !bypassesConflictEvaluation(slot.urgencyLevel) &&
            slot.conflictPolicy == ConflictPolicy.EXCLUSIVE &&
            overlapsInScheduleBoard(
                proposedStart = proposedStart,
                proposedDurationMinutes = durationMinutes,
                existingStart = slot.scheduledAt,
                existingDurationMinutes = slot.durationMinutes
            )
        }
        
        return if (overlaps.isEmpty()) {
            ConflictResult.Clear
        } else {
            ConflictResult.Conflict(overlaps)
        }
    }
    
    override suspend fun refresh() {
        // No-op in fake
    }

    override suspend fun findLexicalMatch(targetQuery: String): ScheduleItem? {
        val query = targetQuery.trim().lowercase()
        if (query.isEmpty()) return null
        val exactMatches = _items.value.filter { it.title.lowercase().contains(query) }
        return if (exactMatches.size == 1) exactMatches.first() else null
    }

    override suspend fun resolveTarget(request: TargetResolutionRequest): TargetResolution {
        return findLexicalMatch(request.targetQuery)?.let(TargetResolution::Resolved)
            ?: TargetResolution.NoMatch(request.describeForFailure())
    }
}

private class CapturingScheduledTaskRepository : ScheduledTaskRepository {
    private val items = MutableStateFlow<List<SchedulerTimelineItem>>(emptyList())
    private val queryLatch = CountDownLatch(1)

    var lastQueryStart: LocalDate? = null
        private set
    var lastQueryEnd: LocalDate? = null
        private set

    fun awaitInitialQuery(): Boolean = queryLatch.await(1, TimeUnit.SECONDS)

    override fun getTimelineItems(dayOffset: Int): Flow<List<SchedulerTimelineItem>> = items

    override fun queryByDateRange(start: LocalDate, end: LocalDate): Flow<List<SchedulerTimelineItem>> {
        lastQueryStart = start
        lastQueryEnd = end
        queryLatch.countDown()
        return items
    }

    override suspend fun insertTask(task: ScheduledTask): String = task.id

    override suspend fun getTask(id: String): ScheduledTask? = null

    override suspend fun updateTask(task: ScheduledTask) = Unit

    override suspend fun upsertTask(task: ScheduledTask): String = task.id

    override suspend fun batchInsertTasks(tasks: List<ScheduledTask>): List<String> = tasks.map { it.id }

    override suspend fun rescheduleTask(oldTaskId: String, newTask: ScheduledTask) = Unit

    override suspend fun deleteItem(id: String) = Unit

    override suspend fun getRecentCompleted(limit: Int): List<ScheduledTask> = emptyList()

    override suspend fun getTopUrgentActiveForEntity(entityId: String): ScheduledTask? = null

    override fun observeByEntityId(entityId: String): Flow<List<ScheduledTask>> = MutableStateFlow(emptyList())
}
