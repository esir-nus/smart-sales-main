package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.crm.ActivityType
import com.smartsales.prism.domain.crm.writeback.KernelWriteBack
import com.smartsales.prism.domain.memory.EntityRef
import com.smartsales.prism.domain.memory.EntityEntry

class FakeKernelWriteBack : KernelWriteBack {
    val updatedEntities = mutableMapOf<String, EntityRef>()
    val removedEntities = mutableSetOf<String>()
    val activities = mutableListOf<String>()

    override suspend fun updateEntityInSession(entityId: String, ref: EntityRef, entry: EntityEntry?) {
        updatedEntities[entityId] = ref
    }

    override suspend fun removeEntityFromSession(entityId: String) {
        removedEntities.add(entityId)
    }

    override suspend fun recordActivity(entityId: String, type: ActivityType, summary: String) {
        activities.add("$entityId-$type-$summary")
    }
}
