package com.smartsales.prism.di

import com.smartsales.prism.data.audio.RealAudioRepository


import com.smartsales.prism.data.real.RealContextBuilder
import com.smartsales.prism.data.fakes.FakeExecutor
import com.smartsales.prism.data.persistence.RoomHistoryRepository
import com.smartsales.prism.data.fakes.FakePublisher
import com.smartsales.prism.data.persistence.RoomMemoryRepository
import com.smartsales.prism.data.persistence.RoomEntityRepository
import com.smartsales.prism.data.persistence.RoomUserHabitRepository
import com.smartsales.prism.data.fakes.FakeUserProfileRepository 
import com.smartsales.prism.data.real.DashscopeExecutor
import com.smartsales.prism.data.real.session.LlmSessionTitleGenerator
import com.smartsales.prism.domain.session.SessionTitleGenerator
import com.smartsales.prism.data.real.PrismOrchestrator
import com.smartsales.prism.domain.audio.AudioRepository

import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.Publisher
import com.smartsales.prism.data.real.telemetry.RealPipelineTelemetry
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.domain.repository.UserProfileRepository

import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.data.aicore.AiCoreConfig

import dagger.Binds
import dagger.Provides
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
    abstract fun bindKernelWriteBack(impl: RealContextBuilder): com.smartsales.prism.domain.pipeline.KernelWriteBack
    
    @Binds @Singleton
    abstract fun bindPipelineTelemetry(impl: RealPipelineTelemetry): PipelineTelemetry
    
    @Binds @Singleton
    abstract fun bindExecutor(impl: DashscopeExecutor): Executor
    
    @Binds @Singleton
    abstract fun bindPublisher(fake: FakePublisher): Publisher
    
    // === Repositories ===
    
    @Binds @Singleton
    abstract fun bindHistoryRepository(impl: RoomHistoryRepository): HistoryRepository
    
    @Binds @Singleton
    abstract fun bindMemoryRepository(impl: RoomMemoryRepository): MemoryRepository
    
    @Binds @Singleton
    abstract fun bindEntityRepository(impl: RoomEntityRepository): EntityRepository
    
    @Binds @Singleton
    abstract fun bindClientProfileHub(fake: com.smartsales.prism.data.fakes.FakeClientProfileHub): com.smartsales.prism.domain.crm.ClientProfileHub
    

    // === Audio ===
    
    @Binds @Singleton
    abstract fun bindAudioRepository(real: RealAudioRepository): AudioRepository

    // === User Profile ===

    @Binds @Singleton
    abstract fun bindUserProfileRepository(fake: FakeUserProfileRepository): UserProfileRepository

    // === Analyst Tools ===

    @Binds @Singleton
    abstract fun bindToolRegistry(fake: com.smartsales.prism.data.fakes.FakeToolRegistry): com.smartsales.prism.domain.analyst.ToolRegistry

    @Binds @Singleton
    abstract fun bindLightningRouter(impl: com.smartsales.prism.data.real.RealLightningRouter): com.smartsales.prism.domain.analyst.LightningRouter

    @Binds @Singleton
    abstract fun bindEntityResolverService(impl: com.smartsales.prism.data.real.RealEntityResolverService): com.smartsales.prism.domain.analyst.EntityResolverService

    @Binds @Singleton
    abstract fun bindAnalystPipeline(impl: com.smartsales.prism.data.real.RealAnalystPipeline): com.smartsales.prism.domain.analyst.AnalystPipeline

    @Binds @Singleton
    abstract fun bindArchitectService(impl: com.smartsales.prism.data.real.RealArchitectService): com.smartsales.prism.domain.analyst.ArchitectService

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

    // === Mascot System I ===
    
    @Binds @Singleton
    abstract fun bindMascotService(impl: com.smartsales.prism.data.real.RealMascotService): com.smartsales.prism.domain.mascot.MascotService

    @Binds @Singleton
    abstract fun bindSystemEventBus(impl: com.smartsales.prism.data.real.RealSystemEventBus): com.smartsales.prism.domain.system.SystemEventBus



    // === Entity Writer ===

    @Binds @Singleton
    abstract fun bindEntityWriter(impl: com.smartsales.prism.data.real.RealEntityWriter): com.smartsales.prism.domain.memory.EntityWriter

    @Binds @Singleton
    abstract fun bindSessionTitleGenerator(impl: LlmSessionTitleGenerator): SessionTitleGenerator

    // === Input Parser (Turbo Router) ===

    @Binds @Singleton
    abstract fun bindInputParserService(impl: com.smartsales.prism.data.parser.RealInputParserService): com.smartsales.prism.domain.parser.InputParserService

    @Binds @Singleton
    abstract fun bindEntityDisambiguationService(impl: com.smartsales.prism.data.disambiguation.RealEntityDisambiguationService): com.smartsales.prism.domain.disambiguation.EntityDisambiguationService

    companion object {
        @Provides
        @Singleton
        fun provideAiCoreConfig(): AiCoreConfig {
            return AiCoreConfig()
        }

        @Provides
        @Singleton
        fun provideMetaHub(): MetaHub {
            return InMemoryMetaHub()
        }

        @Provides
        @Singleton
        fun provideTingwuPipeline(
            optionalConfig: java.util.Optional<AiCoreConfig>,
            fake: com.smartsales.prism.data.fakes.FakeTingwuPipeline,
            real: com.smartsales.prism.data.tingwu.RealTingwuPipeline
        ): com.smartsales.prism.domain.tingwu.TingwuPipeline {
            val preferFake = optionalConfig.orElse(AiCoreConfig()).preferFakeTingwu
            return if (preferFake) fake else real
        }
    }
}
