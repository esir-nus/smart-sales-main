package com.smartsales.feature.usercenter.data

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/data/UserProfileRepository.kt
// 模块：:feature:usercenter
// 说明：用户信息的存储仓库，当前使用内存 stub
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import com.smartsales.feature.usercenter.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface UserProfileRepository {
    suspend fun load(): Result<UserProfile>
    suspend fun save(profile: UserProfile): Result<Unit>
}

@Singleton
class InMemoryUserProfileRepository @Inject constructor() : UserProfileRepository {
    private val mutex = Mutex()
    private var profile = UserProfile(
        userName = "SmartSales 用户",
        email = "user@example.com",
        tokensRemaining = 120,
        featureFlags = linkedMapOf(
            "启用聊天" to true,
            "展示转写摘要" to true,
            "实验性功能" to false
        )
    )

    override suspend fun load(): Result<UserProfile> = mutex.withLock {
        Result.Success(profile)
    }

    override suspend fun save(profile: UserProfile): Result<Unit> = mutex.withLock {
        // 未来可接入本地存储，这里只是更新内存。
        delay(20)
        this.profile = profile
        Result.Success(Unit)
    }
}
