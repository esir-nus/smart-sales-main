package com.smartsales.prism.domain.repository

import com.smartsales.prism.domain.memory.UserProfile
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing User Profile
 */
interface UserProfileRepository {
    val profile: StateFlow<UserProfile>

    suspend fun getProfile(): UserProfile
    suspend fun updateProfile(profile: UserProfile)
}
