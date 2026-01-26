package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.entities.UserProfile
import com.smartsales.domain.prism.core.repositories.UserProfileRepository

class FakeUserProfileRepository : UserProfileRepository {
    private var profile: UserProfile? = UserProfile(
        displayName = "测试用户",
        preferredLanguage = "zh-CN"
    )
    
    override suspend fun get() = profile
    
    override suspend fun save(profile: UserProfile) {
        this.profile = profile
    }
}
