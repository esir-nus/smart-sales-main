package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.habit.UserHabitRepository
import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.rl.ObservationSource
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * WorldStateSeeder DSL — Anti-Illusion Infrastructure
 *
 * Provides a fluent DSL for seeding complex, organic B2B scenarios into the standard
 * Fake repositories. The seeder forces mutations through the actual [EntityWriter] interface
 * so that tests accurately reflect the deduplication and alias-merging logic of production,
 * rather than constructing perfect, hallucinated states.
 */
class WorldStateSeeder(
    private val entityWriter: EntityWriter,
    private val memoryRepository: MemoryRepository,
    private val userHabitRepository: UserHabitRepository
) {
    /**
     * Simulates an extraction hit from the LLM, passing it through the [EntityWriter] resolution cascade.
     * Optionally accepts a [resolvedId] to simulate the LLM successfully disambiguating an alias.
     * Returns the canonical `entityId` generated or resolved by the system natively.
     */
    suspend fun entityClue(
        name: String,
        company: String? = null,
        title: String? = null,
        resolvedId: String? = null,
        type: com.smartsales.prism.domain.memory.EntityType = com.smartsales.prism.domain.memory.EntityType.PERSON
    ): String {
        val result = entityWriter.upsertFromClue(
            clue = name,
            resolvedId = resolvedId,
            type = type,
            source = "world-state-seeder"
        )
        val id = result.entityId
        
        // If we purposefully map a new alias to an existing entity, we register it
        if (resolvedId != null && name != result.displayName) {
            entityWriter.registerAlias(id, name)
        }
        
        // Update profile attributes if provided
        val profileUpdates = mutableMapOf<String, String?>()
        if (company != null) profileUpdates["accountId"] = company
        if (title != null) profileUpdates["jobTitle"] = title
        
        if (profileUpdates.isNotEmpty()) {
            entityWriter.updateProfile(id, profileUpdates)
        }
        
        return id
    }

    /**
     * Directly constructs and injects a MemoryEntry into the MemoryRepository.
     */
    suspend fun memory(
        content: String,
        entityId: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val entry = MemoryEntry(
            entryId = UUID.randomUUID().toString(),
            sessionId = "historic-session",
            entryType = MemoryEntryType.USER_MESSAGE,
            content = content,
            isArchived = false,
            createdAt = timestamp,
            updatedAt = timestamp,
            structuredJson = """["$entityId"]"""
        )
        memoryRepository.save(entry)
    }

    /**
     * Directly inserts an observation into the UserHabitRepository.
     */
    suspend fun habit(
        key: String,
        value: String,
        entityId: String? = null
    ) {
        userHabitRepository.observe(
            key = key,
            value = value,
            entityId = entityId,
            source = ObservationSource.INFERRED
        )
    }
}

/**
 * Entry point for the DSL.
 * Example:
 * ```
 * seedWorldState(entityWriter, memoryRepo, habitRepo) {
 *    val id = entityClue("张总", "字节跳动")
 *    memory("He prefers email", id)
 *    habit("preferred_contact", "email", id)
 * }
 * ```
 */
suspend fun seedWorldState(
    entityWriter: EntityWriter,
    memoryRepository: MemoryRepository,
    userHabitRepository: UserHabitRepository,
    block: suspend WorldStateSeeder.() -> Unit
) {
    val seeder = WorldStateSeeder(entityWriter, memoryRepository, userHabitRepository)
    seeder.block()
}
