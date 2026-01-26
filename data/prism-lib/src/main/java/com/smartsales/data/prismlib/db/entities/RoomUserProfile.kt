package com.smartsales.data.prismlib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsales.domain.prism.core.entities.ExperienceLevel
import com.smartsales.domain.prism.core.entities.UserProfile

@Entity(tableName = "user_profile")
data class RoomUserProfile(
    @PrimaryKey
    val id: Int,
    val displayName: String,
    val preferredLanguage: String,
    val experienceLevel: ExperienceLevel,
    val industry: String?,
    val role: String?,
    val updatedAt: Long
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        displayName = displayName,
        preferredLanguage = preferredLanguage,
        experienceLevel = experienceLevel,
        industry = industry,
        role = role,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(domain: UserProfile): RoomUserProfile = RoomUserProfile(
            id = domain.id,
            displayName = domain.displayName,
            preferredLanguage = domain.preferredLanguage,
            experienceLevel = domain.experienceLevel,
            industry = domain.industry,
            role = domain.role,
            updatedAt = domain.updatedAt
        )
    }
}
