package com.smartsales.prism.di

import com.smartsales.prism.data.audio.RealAudioRepository


import com.smartsales.core.context.RealContextBuilder
import com.smartsales.prism.data.fakes.FakePublisher



import com.smartsales.prism.data.fakes.FakeUserProfileRepository 
import com.smartsales.core.llm.DashscopeExecutor
import com.smartsales.prism.domain.session.SessionTitleGenerator
import com.smartsales.prism.domain.audio.AudioRepository

import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.core.context.ContextBuilder
import com.smartsales.core.llm.Executor
import com.smartsales.prism.domain.pipeline.Publisher
import com.smartsales.prism.data.real.telemetry.RealPipelineTelemetry
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.core.pipeline.HabitListener
import com.smartsales.core.pipeline.RealHabitListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
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
    
    @Binds @Singleton
    abstract fun bindContextBuilder(impl: RealContextBuilder): ContextBuilder

    @Binds
    @Singleton
    abstract fun bindKernelWriteBack(impl: RealContextBuilder): com.smartsales.prism.domain.crm.writeback.KernelWriteBack
    
    @Binds @Singleton
    abstract fun bindPipelineTelemetry(impl: RealPipelineTelemetry): PipelineTelemetry
    
    @Binds @Singleton
    abstract fun bindExecutor(impl: DashscopeExecutor): Executor
    
    @Binds @Singleton
    abstract fun bindPublisher(fake: FakePublisher): Publisher
    
    // === Repositories ===
    

    
    
    
    @Binds @Singleton
    abstract fun bindClientProfileHub(real: com.smartsales.data.crm.hub.RealClientProfileHub): com.smartsales.prism.domain.crm.ClientProfileHub
    

    // === Audio ===
    
    @Binds @Singleton
    abstract fun bindAudioRepository(real: RealAudioRepository): AudioRepository

    // === User Profile ===

    @Binds @Singleton
    abstract fun bindUserProfileRepository(fake: FakeUserProfileRepository): UserProfileRepository

    // === Analyst Tools ===

    @Binds @Singleton
    abstract fun bindToolRegistry(impl: com.smartsales.core.pipeline.RealToolRegistry): com.smartsales.core.pipeline.ToolRegistry

    @Binds
    @dagger.multibindings.IntoSet
    abstract fun bindExportCsvPlugin(plugin: com.smartsales.prism.data.real.plugins.ExportCsvPlugin): com.smartsales.core.pipeline.PrismPlugin

    @Binds
    @dagger.multibindings.IntoSet
    abstract fun bindTestingPdfPlugin(plugin: com.smartsales.prism.data.real.plugins.TestingPdfPlugin): com.smartsales.core.pipeline.PrismPlugin

    @Binds
    @dagger.multibindings.IntoSet
    abstract fun bindTestingEmailPlugin(plugin: com.smartsales.prism.data.real.plugins.TestingEmailPlugin): com.smartsales.core.pipeline.PrismPlugin

    @Binds @Singleton
    abstract fun bindLightningRouter(impl: com.smartsales.core.pipeline.RealLightningRouter): com.smartsales.core.pipeline.LightningRouter

    @Binds @Singleton
    abstract fun bindEntityResolverService(impl: com.smartsales.core.pipeline.RealEntityResolverService): com.smartsales.core.pipeline.EntityResolverService



    @Binds @Singleton
    abstract fun bindReinforcementLearner(real: com.smartsales.prism.data.rl.RealReinforcementLearner): com.smartsales.prism.domain.rl.ReinforcementLearner

    @Binds @Singleton
    abstract fun bindHabitListener(real: RealHabitListener): HabitListener

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
    abstract fun bindMascotService(impl: com.smartsales.core.pipeline.RealMascotService): com.smartsales.core.pipeline.MascotService

    @Binds @Singleton
    abstract fun bindSystemEventBus(impl: com.smartsales.prism.data.real.RealSystemEventBus): com.smartsales.prism.domain.system.SystemEventBus



    // === Entity Writer ===

    @Binds
    @Singleton
    abstract fun bindEntityWriter(impl: com.smartsales.data.crm.writer.RealEntityWriter): com.smartsales.prism.domain.memory.EntityWriter

    @Binds @Singleton
    abstract fun bindSessionTitleGenerator(impl: com.smartsales.prism.data.real.session.SemanticSessionTitleGenerator): SessionTitleGenerator

    // === Input Parser (Turbo Router) ===

    @Binds @Singleton
    abstract fun bindInputParserService(impl: com.smartsales.core.pipeline.RealInputParserService): com.smartsales.core.pipeline.InputParserService

    @Binds @Singleton
    abstract fun bindEntityDisambiguationService(impl: com.smartsales.core.pipeline.RealEntityDisambiguationService): com.smartsales.core.pipeline.EntityDisambiguationService

    @Binds @Singleton
    abstract fun bindUnifiedPipeline(impl: com.smartsales.core.pipeline.RealUnifiedPipeline): com.smartsales.core.pipeline.UnifiedPipeline

    companion object {
        @Provides
        @Singleton
        @Named("AppScope")
        fun provideAppCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }

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
