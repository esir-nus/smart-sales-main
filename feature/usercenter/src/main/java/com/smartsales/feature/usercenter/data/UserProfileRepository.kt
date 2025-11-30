package com.smartsales.feature.usercenter.data

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/data/UserProfileRepository.kt
// 模块：:feature:usercenter
// 说明：用户信息的存储仓库，当前使用内存 stub
// 作者：创建于 2025-11-30

import com.smartsales.feature.usercenter.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface UserProfileRepository {
    suspend fun load(): UserProfile
    suspend fun save(profile: UserProfile)
    suspend fun clear()
}

@Singleton
class InMemoryUserProfileRepository @Inject constructor() : UserProfileRepository {
    private val mutex = Mutex()
    private var profile: UserProfile = defaultProfile()

    override suspend fun load(): UserProfile = mutex.withLock {
        profile
    }

    override suspend fun save(profile: UserProfile) = mutex.withLock {
        this.profile = profile
    }

    override suspend fun clear() = mutex.withLock {
        // 访客状态：置空用户名与邮箱，并标记 isGuest。
        profile = guestProfile()
    }

    private fun defaultProfile(): UserProfile = UserProfile(
        displayName = "SmartSales 用户",
        email = "user@example.com",
        isGuest = false
    )

    private fun guestProfile(): UserProfile = UserProfile(
        displayName = "",
        email = "",
        isGuest = true
    )
}
