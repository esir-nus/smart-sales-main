package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeUserProfileRepository @Inject constructor() : UserProfileRepository {

    private val _profile = MutableStateFlow(
        UserProfile(
            id = 0,
            displayName = "Frank",
            role = "Sales Manager",
            industry = "Technology",
            experienceLevel = "Expert",
            preferredLanguage = "zh-CN",
            updatedAt = System.currentTimeMillis(),
            communicationPlatform = "WeChat",
            experienceYears = "10 years"
        )
    )

    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    override suspend fun getProfile(): UserProfile {
        return _profile.value
    }

    override suspend fun updateProfile(profile: UserProfile) {
        _profile.value = profile.copy(updatedAt = System.currentTimeMillis())
    }
}
