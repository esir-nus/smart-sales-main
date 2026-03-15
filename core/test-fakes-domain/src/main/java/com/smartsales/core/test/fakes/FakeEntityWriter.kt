package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.EntityWriter
import com.smartsales.prism.domain.memory.UpsertResult
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.ProfileUpdateResult

class FakeEntityWriter : EntityWriter {
    
    var nextUpsertResult: UpsertResult = UpsertResult("fake-entity-id", true, "Fake Entity")

    override suspend fun upsertFromClue(
        clue: String,
        resolvedId: String?,
        type: EntityType,
        source: String
    ): UpsertResult {
        return nextUpsertResult
    }

    override suspend fun updateAttribute(entityId: String, key: String, value: String) {
        // No-op
    }

    override suspend fun registerAlias(entityId: String, alias: String) {
        // No-op
    }

    override suspend fun updateProfile(
        entityId: String,
        updates: Map<String, String?>
    ): ProfileUpdateResult {
        return ProfileUpdateResult(entityId, emptyList())
    }

    override suspend fun delete(entityId: String) {
        // No-op
    }
}
