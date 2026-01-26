package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.UserHabit

@Entity(tableName = "user_habits")
data class RoomUserHabit(
    @PrimaryKey
    val habitKey: String,
    val habitValue: String,
    val confidence: Float,
    val source: String,
    val observedAt: Long,
    val entityId: String?
) {
    fun toDomain(): UserHabit = UserHabit(
        habitKey = habitKey,
        habitValue = habitValue,
        confidence = confidence,
        source = source,
        observedAt = observedAt,
        entityId = entityId
    )

    companion object {
        fun fromDomain(domain: UserHabit): RoomUserHabit = RoomUserHabit(
            habitKey = domain.habitKey,
            habitValue = domain.habitValue,
            confidence = domain.confidence,
            source = domain.source,
            observedAt = domain.observedAt,
            entityId = domain.entityId
        )
    }
}
