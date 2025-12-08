package com.smartsales.feature.usercenter.di

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/di/UserCenterModule.kt
// 模块：:feature:usercenter
// 说明：绑定用户中心依赖的 Hilt 模块
// 作者：创建于 2025-11-21

import com.smartsales.feature.usercenter.data.PersistentUserProfileRepository
import com.smartsales.feature.usercenter.data.UserProfileRepository
import com.smartsales.feature.usercenter.data.OnboardingStateRepository
import com.smartsales.feature.usercenter.data.PersistentOnboardingStateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface UserCenterModule {
    @Binds
    @Singleton
    fun bindUserProfileRepository(impl: PersistentUserProfileRepository): UserProfileRepository

    @Binds
    @Singleton
    fun bindOnboardingStateRepository(impl: PersistentOnboardingStateRepository): OnboardingStateRepository
}
