package com.smartsales.prism.di

import com.smartsales.prism.domain.core.Orchestrator
import com.smartsales.prism.domain.core.RealOrchestrator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Prism DI Module
 * 
 * Provides Orchestrator binding for the Prism monolith.
 * Phase 3: Using RealOrchestrator with DashScope API.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PrismModule {
    
    @Binds
    @Singleton
    abstract fun bindOrchestrator(real: RealOrchestrator): Orchestrator
}

