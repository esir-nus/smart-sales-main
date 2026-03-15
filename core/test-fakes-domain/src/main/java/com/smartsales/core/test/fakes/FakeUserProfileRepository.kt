package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUserProfileRepository : UserProfileRepository {
    
    private val _profile = MutableStateFlow(
        UserProfile(
            id = 0,
            displayName = "Default User",
            role = "sales_rep",
            industry = "technology",
            experienceLevel = "expert",
            updatedAt = System.currentTimeMillis()
        )
    )
    override val profile: StateFlow<UserProfile> = _profile

    override suspend fun getProfile(): UserProfile {
        return _profile.value
    }

    override suspend fun updateProfile(profile: UserProfile) {
        _profile.value = profile
    }
}
