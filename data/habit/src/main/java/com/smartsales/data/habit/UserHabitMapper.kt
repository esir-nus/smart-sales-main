package com.smartsales.data.habit

import com.smartsales.prism.data.persistence.UserHabitEntity
import com.smartsales.prism.domain.habit.UserHabit

/**
 * 域模型映射 for Habit
 */
fun UserHabit.toEntity(): UserHabitEntity = UserHabitEntity(
    habitKey = habitKey,
    habitValue = habitValue,
    entityIdOrEmpty = entityId ?: "",
    inferredCount = inferredCount,
    explicitPositive = explicitPositive,
    explicitNegative = explicitNegative,
    lastObservedAt = lastObservedAt,
    createdAt = createdAt
)

fun UserHabitEntity.toDomain(): UserHabit = UserHabit(
    habitKey = habitKey,
    habitValue = habitValue,
    entityId = entityIdOrEmpty.takeIf { it.isNotEmpty() },
    inferredCount = inferredCount,
    explicitPositive = explicitPositive,
    explicitNegative = explicitNegative,
    lastObservedAt = lastObservedAt,
    createdAt = createdAt
)
