package com.smartsales.data.prismlib.di

import com.smartsales.data.prismlib.pipeline.*
import com.smartsales.domain.prism.core.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Prism Pipeline Hilt 模块 — 绑定核心管道接口
 * 
 * TODO(Phase 3): 取消注释 @InstallIn 以启用真实实现
 * 当前被禁用以避免与 FakeCoreModule 冲突
 */
@Module
// @InstallIn(SingletonComponent::class) // Disabled for Skeleton phase
abstract class PrismPipelineModule {

    @Binds
    @Singleton
    abstract fun bindContextBuilder(impl: RealContextBuilder): ContextBuilder

    @Binds
    @Singleton
    abstract fun bindExecutor(impl: DashscopeExecutor): Executor

    @Binds
    @Singleton
    abstract fun bindOrchestrator(impl: PrismOrchestrator): Orchestrator

    @Binds
    @Singleton
    abstract fun bindMemoryWriter(impl: RoomMemoryWriter): MemoryWriter

    @Binds
    @Singleton
    abstract fun bindSessionCache(impl: InMemorySessionCache): SessionCache
}
