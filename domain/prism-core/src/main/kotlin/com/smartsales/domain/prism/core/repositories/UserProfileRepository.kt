package com.smartsales.domain.prism.core.repositories

import com.smartsales.domain.prism.core.entities.UserProfile

/**
 * 用户档案仓库
 * @see Prism-V1.md §5.8
 */
interface UserProfileRepository {
    suspend fun get(): UserProfile?
    suspend fun save(profile: UserProfile)
}
