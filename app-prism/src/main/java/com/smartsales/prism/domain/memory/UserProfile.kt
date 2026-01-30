package com.smartsales.prism.domain.memory

/**
 * User Profile Configuration
 * @see Prism-V1.md §5.8
 */
data class UserProfile(
    val id: Int = 0, // Singleton
    val displayName: String,
    val role: String,                     // "sales_rep", "manager", "executive"
    val industry: String,                 // "technology", "manufacturing", "finance"
    val experienceLevel: String,          // "beginner", "intermediate", "expert"
    val preferredLanguage: String = "zh-CN",
    val updatedAt: Long,

    // [EXTENSION] For manual entry & contact suggestions
    val communicationPlatform: String = "", // e.g., "WeChat", "DingTalk"
    val experienceYears: String = ""        // Free text: "10 years"
)
