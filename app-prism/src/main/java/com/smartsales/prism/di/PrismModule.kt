package com.smartsales.prism.di

import com.smartsales.prism.data.fakes.FakeAudioRepository
import com.smartsales.prism.data.fakes.FakeConflictResolutionService
import com.smartsales.prism.data.fakes.FakeConnectivityService
import com.smartsales.prism.data.real.RealContextBuilder
import com.smartsales.prism.data.fakes.FakeExecutor
import com.smartsales.prism.data.fakes.FakeHistoryRepository
import com.smartsales.prism.data.fakes.FakeMemoryRepository
import com.smartsales.prism.data.fakes.FakeMemoryWriter
import com.smartsales.prism.data.fakes.FakeOnboardingService
import com.smartsales.prism.data.fakes.FakePublisher
import com.smartsales.prism.data.fakes.FakeRelevancyRepository
import com.smartsales.prism.data.fakes.FakeUserProfileRepository 
import com.smartsales.prism.data.real.DashscopeExecutor
import com.smartsales.prism.data.real.PrismOrchestrator
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.connectivity.ConnectivityService
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.memory.MemoryWriter
import com.smartsales.prism.domain.memory.RelevancyRepository
import com.smartsales.prism.domain.onboarding.OnboardingService
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.Publisher
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.scheduler.ConflictResolutionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Prism DI Module
 * 
 * Provides bindings for the Prism monolith.
 * Phase 2: Using Fakes for skeleton development.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PrismModule {
    
    // === Core Pipeline ===
    
    @Binds @Singleton
    abstract fun bindOrchestrator(impl: PrismOrchestrator): Orchestrator
    
    @Binds @Singleton
    abstract fun bindContextBuilder(impl: RealContextBuilder): ContextBuilder
    
    @Binds @Singleton
    abstract fun bindExecutor(impl: DashscopeExecutor): Executor
    
    @Binds @Singleton
    abstract fun bindPublisher(fake: FakePublisher): Publisher
    
    // === Repositories ===
    
    @Binds @Singleton
    abstract fun bindHistoryRepository(fake: FakeHistoryRepository): HistoryRepository
    
    @Binds @Singleton
    abstract fun bindMemoryRepository(fake: FakeMemoryRepository): MemoryRepository
    
    @Binds @Singleton
    abstract fun bindRelevancyRepository(fake: FakeRelevancyRepository): RelevancyRepository
    
    // === Memory System ===
    
    @Binds @Singleton
    abstract fun bindMemoryWriter(fake: FakeMemoryWriter): MemoryWriter
    
    @Binds @Singleton
    abstract fun bindEntityResolver(impl: com.smartsales.prism.data.memory.RealEntityResolver): com.smartsales.prism.domain.memory.EntityResolver
    
    // === Connectivity ===
    
    @Binds @Singleton
    abstract fun bindConnectivityService(fake: FakeConnectivityService): ConnectivityService
    
    // === Onboarding ===
    
    @Binds @Singleton
    abstract fun bindOnboardingService(fake: FakeOnboardingService): OnboardingService
    
    // === Scheduler ===
    
    @Binds @Singleton
    abstract fun bindConflictResolutionService(fake: FakeConflictResolutionService): ConflictResolutionService
    
    // === Audio ===
    
    @Binds @Singleton
    abstract fun bindAudioRepository(fake: FakeAudioRepository): AudioRepository

    // === User Profile ===

    @Binds @Singleton
    abstract fun bindUserProfileRepository(fake: FakeUserProfileRepository): UserProfileRepository

    // === Analyst Tools ===

    @Binds @Singleton
    abstract fun bindToolRegistry(fake: com.smartsales.prism.data.fakes.FakeToolRegistry): com.smartsales.prism.domain.analyst.ToolRegistry

    // === Time ===

    @Binds @Singleton
    abstract fun bindTimeProvider(impl: com.smartsales.prism.data.time.SystemTimeProvider): com.smartsales.prism.domain.time.TimeProvider
}
