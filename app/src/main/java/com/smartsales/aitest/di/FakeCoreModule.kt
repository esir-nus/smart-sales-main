package com.smartsales.aitest.di

// 文件：app/src/main/java/com/smartsales/aitest/di/FakeCoreModule.kt
// 模块：:app
// 说明：Prism Core 的 Fake 实现模块 — 暂时禁用
//       问题: Prism interfaces 在 :app-prism (application module) 中，无法被 :app 依赖
//       解决: 需要将 Prism domain layer 移至 library module 或使用 shared 模块
// 作者：暂时禁用于 2026-01-30

// import com.smartsales.prism.domain.pipeline.*
// import com.smartsales.prism.domain.memory.*
// import com.smartsales.prism.data.fakes.*
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Prism Core 的 Fake 实现模块 — 暂时禁用
 * 
 * 问题: Orchestrator, ContextBuilder, Executor 等接口定义在 :app-prism,
 *       而 :app-prism 是 application module, 不能被 :app 依赖
 * 
 * TODO: 将 prism.domain 包移至独立 library module (如 :core:prism-domain)
 */
@Module
@InstallIn(SingletonComponent::class)
object FakeCoreModule {
    
    // ============ Core Pipeline (暂时禁用) ============
    
    // @Provides
    // @Singleton
    // fun provideOrchestrator(): Orchestrator = FakeOrchestrator()
    
    // @Provides
    // @Singleton
    // fun provideContextBuilder(): ContextBuilder = FakeContextBuilder()
    
    // @Provides
    // @Singleton
    // fun provideExecutor(): Executor = FakeExecutor()
    
    // @Provides
    // @Singleton
    // fun provideMemoryWriter(): MemoryWriter = FakeMemoryWriter()
}
