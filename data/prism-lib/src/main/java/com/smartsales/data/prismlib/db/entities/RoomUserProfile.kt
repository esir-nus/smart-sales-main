package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.UserProfile

@Entity(tableName = "user_profile")
data class RoomUserProfile(
    @PrimaryKey
    val userId: String,
    val displayName: String,
    val role: String,
    val preferences: Map<String, String>,
    val lastActiveAt: Long
) {
    fun toDomain(): UserProfile = UserProfile(
        userId = userId,
        displayName = displayName,
        role = role,
        preferences = preferences,
        lastActiveAt = lastActiveAt
    )

    companion object {
        fun fromDomain(domain: UserProfile): RoomUserProfile = RoomUserProfile(
            userId = domain.userId,
            displayName = domain.displayName,
            role = domain.role,
            preferences = domain.preferences,
            lastActiveAt = domain.lastActiveAt
        )
    }
}
