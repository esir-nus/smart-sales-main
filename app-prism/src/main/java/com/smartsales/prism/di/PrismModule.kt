package com.smartsales.prism.di

import com.smartsales.prism.domain.core.FakeOrchestrator
import com.smartsales.prism.domain.core.Orchestrator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Prism DI Module
 * 
 * Provides Orchestrator binding for the Prism monolith.
 * Phase 2: Using FakeOrchestrator for skeleton development.
 * TODO: Switch back to RealOrchestrator in Phase 3.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PrismModule {
    
    @Binds
    @Singleton
    abstract fun bindOrchestrator(fake: FakeOrchestrator): Orchestrator
}
