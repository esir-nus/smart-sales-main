package com.smartsales.data.prismlib.pipeline

import com.smartsales.domain.prism.core.*
import com.smartsales.domain.prism.core.entities.UserHabit
import com.smartsales.domain.prism.core.entities.UserProfile
import com.smartsales.domain.prism.core.repositories.MemoryEntryRepository
import com.smartsales.domain.prism.core.repositories.RelevancyRepository
import com.smartsales.domain.prism.core.repositories.UserHabitRepository
import com.smartsales.domain.prism.core.repositories.UserProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实上下文构建器 — 从各仓库组装 EnhancedContext
 * @see Prism-V1.md §2.2 #1
 */
@Singleton
class RealContextBuilder @Inject constructor(
    private val memoryEntryRepository: MemoryEntryRepository,
    private val relevancyRepository: RelevancyRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userHabitRepository: UserHabitRepository,
    private val sessionCache: SessionCache
) : ContextBuilder {

    override suspend fun buildContext(userText: String, mode: Mode): EnhancedContext {
        // 1. 获取用户档案
        val profile = userProfileRepository.get()
        val profileSnapshot = profile?.toSnapshot()

        // 2. 获取用户习惯 (高置信度优先)
        val habits = userHabitRepository.getAll()
            .filter { it.confidence >= 0.5f }
            .sortedByDescending { it.confidence }
            .take(10)
            .map { it.toSnapshot() }

        // 3. 获取最近记忆 (Hot Zone)
        val recentMemories = memoryEntryRepository.getHotZone()
            .take(5)
            .map { entry ->
                MemoryHit(
                    entryId = entry.id,
                    snippet = entry.payloadJson?.take(200) ?: entry.displayContent.take(200),
                    score = 1.0f // Hot Zone 条目都是高相关
                )
            }

        // 4. 提取实体上下文 (从用户文本中识别的实体)
        val entityContext = extractEntities(userText)

        // 5. 获取 Session Cache 快照
        val cacheSnapshot = sessionCache.getSnapshot()

        return EnhancedContext(
            userText = userText,
            audioTranscripts = emptyList(), // Chunk D 补充
            imageAnalysis = emptyList(),     // Chunk D 补充
            memoryHits = recentMemories,
            entityContext = entityContext,
            userProfile = profileSnapshot,
            userHabits = habits,
            sessionCacheSnapshot = cacheSnapshot,
            mode = mode
        )
    }

    /**
     * 从用户文本中提取实体引用
     * Phase 2: 简化实现 — 从 Relevancy 全量匹配
     * Phase 3: 使用 LLM 实体识别
     */
    private suspend fun extractEntities(userText: String): Map<String, EntityRef> {
        val allEntities = relevancyRepository.getAll()
        val result = mutableMapOf<String, EntityRef>()

        for (entity in allEntities) {
            // 简单子字符串匹配
            if (userText.contains(entity.displayName, ignoreCase = true)) {
                result[entity.entityId] = EntityRef(
                    entityId = entity.entityId,
                    displayName = entity.displayName
                )
            }
            // 也检查别名
            for (alias in entity.aliases) {
                if (userText.contains(alias.alias, ignoreCase = true)) {
                    result[entity.entityId] = EntityRef(
                        entityId = entity.entityId,
                        displayName = entity.displayName
                    )
                    break
                }
            }
        }
        return result
    }

    private fun UserProfile.toSnapshot() = UserProfileSnapshot(
        displayName = displayName,
        preferredLanguage = preferredLanguage,
        experienceLevel = experienceLevel.name,
        industry = industry,
        role = role
    )

    private fun UserHabit.toSnapshot() = UserHabitSnapshot(
        habitKey = habitKey,
        habitValue = habitValue,
        confidence = confidence
    )
}
