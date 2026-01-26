package com.smartsales.domain.prism.core.entities

/**
 * 用户档案 — User Center 设置
 * @see Prism-V1.md §5.8
 */
data class UserProfile(
    val id: Int = 0,
    val displayName: String,
    val preferredLanguage: String = "zh-CN",
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val industry: String? = null,
    val role: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    EXPERT
}
