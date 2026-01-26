package com.smartsales.aitest.di

import com.smartsales.domain.prism.core.*
import com.smartsales.domain.prism.core.fakes.*
import com.smartsales.domain.prism.core.linters.*
import com.smartsales.domain.prism.core.tools.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Prism Core 的 Fake 实现模块 — 用于 Skeleton 阶段的 UI 开发
 * Phase 3 会替换为 RealCoreModule
 */
@Module
@InstallIn(SingletonComponent::class)
object FakeCoreModule {
    
    // ============ Core Pipeline ============
    
    @Provides
    @Singleton
    fun provideOrchestrator(): Orchestrator = FakeOrchestrator()
    
    @Provides
    @Singleton
    fun provideContextBuilder(): ContextBuilder = FakeContextBuilder()
    
    @Provides
    @Singleton
    fun provideExecutor(): Executor = FakeExecutor()
    
    @Provides
    @Singleton
    fun providePlanner(): Planner = FakePlanner()
    
    @Provides
    @Singleton
    fun provideModePublisher(): ModePublisher = FakeModePublisher()
    
    @Provides
    @Singleton
    fun provideMemoryWriter(): MemoryWriter = FakeMemoryWriter()
    
    @Provides
    @Singleton
    fun provideSessionCache(): SessionCache = FakeSessionCache()
    
    @Provides
    @Singleton
    fun provideMemoryCenterNotifier(): MemoryCenterNotifier = FakeMemoryCenterNotifier()
    
    // ============ Input Tools ============
    
    @Provides
    @Singleton
    fun provideTingwuRunner(): TingwuRunner = FakeTingwuRunner()
    
    @Provides
    @Singleton
    fun provideVisionAnalyzer(): VisionAnalyzer = FakeVisionAnalyzer()
    
    @Provides
    @Singleton
    fun provideUrlFetcher(): UrlFetcher = FakeUrlFetcher()
    
    @Provides
    @Singleton
    fun provideBleConnector(): BleConnector = FakeBleConnector()
    
    // ============ Linters ============
    
    @Provides
    fun provideEntityLinter(): EntityLinter = EntityLinter()
    
    @Provides
    fun providePlanLinter(): PlanLinter = PlanLinter()
    
    @Provides
    fun provideSchedulerLinter(): SchedulerLinter = SchedulerLinter()
    
    @Provides
    fun provideRelevancyLinter(): RelevancyLinter = RelevancyLinter()
}

