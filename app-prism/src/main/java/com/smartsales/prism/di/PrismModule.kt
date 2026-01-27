package com.smartsales.prism.di

import com.smartsales.prism.domain.core.FakeHistoryRepository
import com.smartsales.prism.domain.core.FakeOrchestrator
import com.smartsales.prism.domain.core.HistoryRepository
import com.smartsales.prism.domain.core.Orchestrator
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
    
    @Binds
    @Singleton
    abstract fun bindOrchestrator(fake: FakeOrchestrator): Orchestrator
    
    @Binds
    @Singleton
    abstract fun bindHistoryRepository(fake: FakeHistoryRepository): HistoryRepository
}
