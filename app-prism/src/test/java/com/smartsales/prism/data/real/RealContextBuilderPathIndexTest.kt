package com.smartsales.prism.data.real

import com.smartsales.prism.data.fakes.FakeEntityRepository
import com.smartsales.prism.data.fakes.FakeMemoryRepository
import com.smartsales.prism.data.fakes.FakeReinforcementLearner
import com.smartsales.prism.data.fakes.FakeTimeProvider
import com.smartsales.prism.data.fakes.FakeUserHabitRepository
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.scheduler.ParsedClues
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RealContextBuilder 路径索引测试 — Wave 2 Ship Criteria
 *
 * 核心验证：
 * 1. 第一次别名查询 → DB 命中 → 缓存到 pathIndex
 * 2. 第二次别名查询 → 缓存命中 → 不调用 findByAlias
 */
class RealContextBuilderPathIndexTest {

    private lateinit var contextBuilder: RealContextBuilder
    private lateinit var entityRepository: SpyEntityRepository
    private lateinit var timeProvider: FakeTimeProvider

    @Before
    fun setup() {
        timeProvider = FakeTimeProvider()
        entityRepository = SpyEntityRepository()
        val habitRepository = FakeUserHabitRepository()

        contextBuilder = RealContextBuilder(
            timeProvider = timeProvider,
            entityRepository = entityRepository,
            reinforcementLearner = FakeReinforcementLearner(habitRepository),
            memoryRepository = FakeMemoryRepository()
        )

        // 种子数据：张总 → p-001
        runTest {
            entityRepository.save(
                EntityEntry(
                    entityId = "p-001",
                    entityType = EntityType.PERSON,
                    displayName = "张总",
                    aliasesJson = "[\"张总\", \"张经理\"]",
                    lastUpdatedAt = 1000L,
                    createdAt = 1000L
                )
            )
        }
    }

    @Test
    fun `first clue lookup calls findByAlias and caches`() = runTest {
        val clues = ParsedClues(person = "张总")

        contextBuilder.buildWithClues("和张总开会", Mode.SCHEDULER, clues)

        // findByAlias 应被调用一次
        assertEquals(1, entityRepository.findByAliasCallCount)
        // getById 不应被调用（第一次走 findByAlias 路径）
        assertEquals(0, entityRepository.getByIdCallCount)
    }

    @Test
    fun `second clue lookup uses cache, skips findByAlias`() = runTest {
        val clues = ParsedClues(person = "张总")

        // 第一次调用：findByAlias + 缓存
        contextBuilder.buildWithClues("和张总开会", Mode.SCHEDULER, clues)
        assertEquals(1, entityRepository.findByAliasCallCount)

        // 第二次调用：应走缓存路径（getById），不调用 findByAlias
        contextBuilder.buildWithClues("再约张总", Mode.SCHEDULER, clues)
        assertEquals(1, entityRepository.findByAliasCallCount)  // 不增长
        assertEquals(1, entityRepository.getByIdCallCount)       // 缓存命中走 getById
    }

    @Test
    fun `cached entity is included in context`() = runTest {
        val clues = ParsedClues(person = "张总")

        // 第一次：DB 查询
        val ctx1 = contextBuilder.buildWithClues("和张总开会", Mode.SCHEDULER, clues)
        assertTrue(ctx1.entityContext.containsKey("person_candidate_0"))
        assertEquals("p-001", ctx1.entityContext["person_candidate_0"]?.entityId)

        // 第二次：缓存命中，结果一致
        val ctx2 = contextBuilder.buildWithClues("再约张总", Mode.SCHEDULER, clues)
        assertTrue(ctx2.entityContext.containsKey("person_candidate_0"))
        assertEquals("p-001", ctx2.entityContext["person_candidate_0"]?.entityId)
    }

    @Test
    fun `stale cache falls back to findByAlias`() = runTest {
        val clues = ParsedClues(person = "张总")

        // 第一次：DB 查询 + 缓存
        contextBuilder.buildWithClues("和张总开会", Mode.SCHEDULER, clues)
        assertEquals(1, entityRepository.findByAliasCallCount)

        // 模拟实体被删除（getById 返回 null）
        entityRepository.markStale("p-001")

        // 第二次：缓存命中但 getById 返回 null → fallback 到 findByAlias
        contextBuilder.buildWithClues("再约张总", Mode.SCHEDULER, clues)
        assertEquals(2, entityRepository.findByAliasCallCount)  // fallback 触发
    }

    @Test
    fun `location cache integration test`() = runTest {
        // 种子数据：会议室A → loc-001
        entityRepository.save(
            EntityEntry(
                entityId = "loc-001",
                entityType = EntityType.LOCATION,
                displayName = "会议室A",
                aliasesJson = "[\"会议室A\"]",
                lastUpdatedAt = 1000L,
                createdAt = 1000L
            )
        )

        val clues = ParsedClues(location = "会议室A")

        // 第一次：search() 查询 + 缓存
        contextBuilder.buildWithClues("在会议室A开会", Mode.SCHEDULER, clues)
        assertEquals(1, entityRepository.searchCallCount)
        assertEquals(0, entityRepository.getByIdCallCount)

        // 第二次：缓存命中 → getById，不调用 search
        contextBuilder.buildWithClues("再去会议室A", Mode.SCHEDULER, clues)
        assertEquals(1, entityRepository.searchCallCount)  // 不增长
        assertEquals(1, entityRepository.getByIdCallCount)  // 缓存命中
    }

    /**
     * Spy 版 EntityRepository — 计数 findByAlias 和 getById 调用次数
     *
     * 用组合模式（非继承），因为 FakeEntityRepository 是 final 类。
     */
    class SpyEntityRepository : EntityRepository {
        private val delegate = FakeEntityRepository()
        private val stalledIds = mutableSetOf<String>()
        
        var findByAliasCallCount = 0
            private set
        var getByIdCallCount = 0
            private set
        var searchCallCount = 0
            private set

        /** 测试辅助：标记某个 ID 为 stale（getById 返回 null） */
        fun markStale(entityId: String) {
            stalledIds.add(entityId)
        }

        override suspend fun findByAlias(alias: String): List<EntityEntry> {
            findByAliasCallCount++
            return delegate.findByAlias(alias)
        }

        override suspend fun getById(entityId: String): EntityEntry? {
            getByIdCallCount++
            if (entityId in stalledIds) return null
            return delegate.getById(entityId)
        }

        override suspend fun getByType(entityType: EntityType): List<EntityEntry> =
            delegate.getByType(entityType)

        override suspend fun save(entry: EntityEntry) = delegate.save(entry)

        override suspend fun search(query: String, limit: Int): List<EntityEntry> {
            searchCallCount++
            return delegate.search(query, limit)
        }
        
        override suspend fun getByAccountId(accountId: String): List<EntityEntry> =
            delegate.getByAccountId(accountId)
    }
}
