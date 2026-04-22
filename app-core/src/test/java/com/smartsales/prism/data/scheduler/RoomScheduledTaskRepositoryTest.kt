package com.smartsales.prism.data.scheduler

import com.smartsales.prism.data.persistence.ScheduledTaskDao
import com.smartsales.prism.data.persistence.ScheduledTaskEntity
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.memory.DurationSource
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.scheduler.fakes.FakeTimeProvider
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RoomScheduledTaskRepositoryTest {

    private val timeProvider = FakeTimeProvider()

    @After
    fun tearDown() {
        SchedulerTelemetryDispatcher.resetTestSink()
    }

    @Test
    fun `upsertTask performs exactly one DAO upsert call without pre-read`() = runTest {
        val dao = CountingScheduledTaskDao()
        val repository = RoomScheduledTaskRepository(dao, timeProvider)

        repository.upsertTask(testTask(id = "task-1"))

        assertEquals(1, dao.upsertCalls)
        assertEquals(0, dao.getByIdCalls)
        assertEquals(0, dao.insertCalls)
        assertEquals(0, dao.updateCalls)
    }

    @Test
    fun `insertTask returns before telemetry sink completes`() = runTest {
        val dao = CountingScheduledTaskDao()
        val repository = RoomScheduledTaskRepository(dao, timeProvider)
        val tagCompleted = CompletableDeferred<Unit>()

        SchedulerTelemetryDispatcher.installTestSink { _, _, _, _ ->
            Thread.sleep(250)
            tagCompleted.complete(Unit)
        }

        repository.insertTask(testTask(id = "task-telemetry"))

        assertFalse(tagCompleted.isCompleted)
        tagCompleted.await()
    }

    private fun testTask(id: String): ScheduledTask {
        val startTime = Instant.parse("2026-04-21T02:00:00Z")
        return ScheduledTask(
            id = id,
            timeDisplay = "10:00 - 11:00",
            title = "Scheduler test task",
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            isDone = false,
            hasAlarm = true,
            startTime = startTime,
            endTime = startTime.plusSeconds(3600),
            durationMinutes = 60,
            durationSource = DurationSource.USER_SET,
            conflictPolicy = ConflictPolicy.EXCLUSIVE,
            dateRange = "10:00 - 11:00",
            alarmCascade = listOf("-30m", "0m")
        )
    }

    private class CountingScheduledTaskDao : ScheduledTaskDao {
        private val entities = linkedMapOf<String, ScheduledTaskEntity>()

        var insertCalls = 0
        var updateCalls = 0
        var upsertCalls = 0
        var getByIdCalls = 0

        override fun getByDateRange(startMs: Long, endMs: Long): Flow<List<ScheduledTaskEntity>> = emptyFlow()

        override suspend fun insert(entity: ScheduledTaskEntity) {
            insertCalls += 1
            entities[entity.taskId] = entity
        }

        override suspend fun insertAll(entities: List<ScheduledTaskEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun upsert(entity: ScheduledTaskEntity) {
            upsertCalls += 1
            entities[entity.taskId] = entity
        }

        override suspend fun update(entity: ScheduledTaskEntity) {
            updateCalls += 1
            entities[entity.taskId] = entity
        }

        override suspend fun getById(id: String): ScheduledTaskEntity? {
            getByIdCalls += 1
            return entities[id]
        }

        override suspend fun getActiveTasks(): List<ScheduledTaskEntity> = entities.values.toList()

        override suspend fun deleteById(id: String) {
            entities.remove(id)
        }

        override suspend fun getFutureExactTasksForReminderRestore(nowMs: Long): List<ScheduledTaskEntity> = emptyList()

        override suspend fun getRecentCompleted(startMs: Long, limit: Int): List<ScheduledTaskEntity> = emptyList()

        override suspend fun getTopUrgentActiveTask(entityId: String): ScheduledTaskEntity? = null

        override fun observeByEntityId(entityId: String): Flow<List<ScheduledTaskEntity>> = emptyFlow()
    }
}
