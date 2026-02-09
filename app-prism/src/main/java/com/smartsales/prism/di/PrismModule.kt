package com.smartsales.prism.di

import com.smartsales.prism.data.fakes.FakeAudioRepository


import com.smartsales.prism.data.real.RealContextBuilder
import com.smartsales.prism.data.fakes.FakeExecutor
import com.smartsales.prism.data.fakes.FakeHistoryRepository
import com.smartsales.prism.data.fakes.FakePublisher
import com.smartsales.prism.data.persistence.RoomMemoryRepository
import com.smartsales.prism.data.persistence.RoomEntityRepository
import com.smartsales.prism.data.persistence.RoomUserHabitRepository
import com.smartsales.prism.data.fakes.FakeUserProfileRepository 
import com.smartsales.prism.data.real.DashscopeExecutor
import com.smartsales.prism.data.real.PrismOrchestrator
import com.smartsales.prism.domain.audio.AudioRepository

import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.Publisher
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.domain.repository.UserProfileRepository

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
    abstract fun bindMemoryRepository(impl: RoomMemoryRepository): MemoryRepository
    
    @Binds @Singleton
    abstract fun bindEntityRepository(impl: RoomEntityRepository): EntityRepository
    
    @Binds @Singleton
    abstract fun bindClientProfileHub(fake: com.smartsales.prism.data.fakes.FakeClientProfileHub): com.smartsales.prism.domain.crm.ClientProfileHub
    

    // === Audio ===
    
    @Binds @Singleton
    abstract fun bindAudioRepository(fake: FakeAudioRepository): AudioRepository

    // === User Profile ===

    @Binds @Singleton
    abstract fun bindUserProfileRepository(fake: FakeUserProfileRepository): UserProfileRepository

    // === Analyst Tools ===

    @Binds @Singleton
    abstract fun bindToolRegistry(fake: com.smartsales.prism.data.fakes.FakeToolRegistry): com.smartsales.prism.domain.analyst.ToolRegistry

    // === User Habit ===

    @Binds @Singleton
    abstract fun bindUserHabitRepository(impl: RoomUserHabitRepository): com.smartsales.prism.domain.habit.UserHabitRepository

    // === RL Module ===

    @Binds @Singleton
    abstract fun bindReinforcementLearner(fake: com.smartsales.prism.data.fakes.FakeReinforcementLearner): com.smartsales.prism.domain.rl.ReinforcementLearner

    // === Time ===

    @Binds @Singleton
    abstract fun bindTimeProvider(impl: com.smartsales.prism.data.time.SystemTimeProvider): com.smartsales.prism.domain.time.TimeProvider

    // === Pairing ===

    @Binds @Singleton
    abstract fun bindPairingService(impl: com.smartsales.prism.data.pairing.RealPairingService): com.smartsales.prism.domain.pairing.PairingService

    // === Badge Audio Pipeline ===

    @Binds @Singleton
    abstract fun bindBadgeAudioPipeline(impl: com.smartsales.prism.data.audio.RealBadgeAudioPipeline): com.smartsales.prism.domain.audio.BadgeAudioPipeline

    // === Coach Pipeline ===

    @Binds @Singleton
    abstract fun bindCoachPipeline(impl: com.smartsales.prism.data.real.coach.RealCoachPipeline): com.smartsales.prism.domain.coach.CoachPipeline
}
