package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.UserHabit

@Entity(tableName = "user_habits")
data class RoomUserHabit(
    @PrimaryKey
    val habitKey: String,
    val habitValue: String,
    val entityId: String?,
    val isExplicit: Boolean,
    val confidence: Float,
    val observationCount: Int,
    val rejectionCount: Int,
    val lastObservedAt: Long,
    val createdAt: Long
) {
    fun toDomain(): UserHabit = UserHabit(
        habitKey = habitKey,
        habitValue = habitValue,
        entityId = entityId,
        isExplicit = isExplicit,
        confidence = confidence,
        observationCount = observationCount,
        rejectionCount = rejectionCount,
        lastObservedAt = lastObservedAt,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: UserHabit): RoomUserHabit = RoomUserHabit(
            habitKey = domain.habitKey,
            habitValue = domain.habitValue,
            entityId = domain.entityId,
            isExplicit = domain.isExplicit,
            confidence = domain.confidence,
            observationCount = domain.observationCount,
            rejectionCount = domain.rejectionCount,
            lastObservedAt = domain.lastObservedAt,
            createdAt = domain.createdAt
        )
    }
}
