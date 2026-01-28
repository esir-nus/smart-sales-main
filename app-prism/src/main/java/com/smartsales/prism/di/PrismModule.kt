package com.smartsales.prism.di

import com.smartsales.prism.data.fakes.FakeContextBuilder
import com.smartsales.prism.data.fakes.FakeExecutor
import com.smartsales.prism.data.fakes.FakeHistoryRepository
import com.smartsales.prism.data.fakes.FakeMemoryRepository
import com.smartsales.prism.data.fakes.FakeMemoryWriter
import com.smartsales.prism.data.fakes.FakeOrchestrator
import com.smartsales.prism.data.fakes.FakePublisher
import com.smartsales.prism.data.fakes.FakeRelevancyRepository
import com.smartsales.prism.domain.memory.MemoryRepository
import com.smartsales.prism.domain.memory.MemoryWriter
import com.smartsales.prism.domain.memory.RelevancyRepository
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.Orchestrator
import com.smartsales.prism.domain.pipeline.Publisher
import com.smartsales.prism.domain.repository.HistoryRepository
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
    abstract fun bindOrchestrator(fake: FakeOrchestrator): Orchestrator
    
    @Binds @Singleton
    abstract fun bindContextBuilder(fake: FakeContextBuilder): ContextBuilder
    
    @Binds @Singleton
    abstract fun bindExecutor(fake: FakeExecutor): Executor
    
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
}
