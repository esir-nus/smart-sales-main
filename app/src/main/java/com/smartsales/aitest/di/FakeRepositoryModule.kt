package com.smartsales.aitest.di

import com.smartsales.prism.domain.memory.*
import com.smartsales.prism.data.fakes.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 的 Fake 实现模块 — 用于 Skeleton 阶段
 * Phase 3 会替换为 RealRepositoryModule
 */
@Module
@InstallIn(SingletonComponent::class)
object FakeRepositoryModule {
    
    @Provides
    @Singleton
    fun provideRelevancyRepository(): RelevancyRepository = FakeRelevancyRepository()
    
    @Provides
    @Singleton
    fun provideMemoryEntryRepository(): MemoryEntryRepository = FakeMemoryEntryRepository()
    
    // @Provides
    // @Singleton
    // fun provideUserProfileRepository(): UserProfileRepository = FakeUserProfileRepository()
    
    // @Provides
    // @Singleton
    // fun provideUserHabitRepository(): UserHabitRepository = FakeUserHabitRepository()
    
    // @Provides
    // @Singleton
    // fun provideSessionsRepository(): SessionsRepository = FakeSessionsRepository()
    
    // @Provides
    // @Singleton
    // fun provideScheduledTaskRepository(): ScheduledTaskRepository = FakeScheduledTaskRepository()
    
    // @Provides
    // @Singleton
    // fun provideInspirationRepository(): InspirationRepository = FakeInspirationRepository()
    
    // @Provides
    // @Singleton
    // fun provideArtifactRepository(): ArtifactRepository = FakeArtifactRepository()
}
