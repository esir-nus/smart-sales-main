package com.smartsales.data.prismlib.di

import com.smartsales.data.prismlib.pipeline.PrismOrchestrator
import com.smartsales.domain.prism.core.Orchestrator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Pipeline 核心组件的 Hilt 绑定模块
 * 
 * TODO(Phase 3): 取消注释 @InstallIn 或删除此文件（与 PrismPipelineModule 重复）
 * 当前被禁用以避免与 FakeCoreModule 冲突
 */
@Module
// @InstallIn(SingletonComponent::class) // Disabled for Skeleton phase
abstract class PipelineModule {

    @Binds
    @Singleton
    abstract fun bindOrchestrator(impl: PrismOrchestrator): Orchestrator
}
