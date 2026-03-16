package com.smartsales.prism.domain.memory

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
        val proposedEnd = proposedStart + (durationMinutes * 60_000L)
        
        val overlaps = _items.value.filter { slot ->
            slot.entryId != excludeId &&
            slot.conflictPolicy == ConflictPolicy.EXCLUSIVE &&
            slot.scheduledAt < proposedEnd && proposedStart < slot.endAt
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
}
