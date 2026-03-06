package com.smartsales.prism.di

import com.smartsales.prism.data.asr.FunAsrService
import com.smartsales.prism.domain.asr.AsrService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ASR 模块 — 提供 ASR 服务绑定
 * 
 * Wave 1: FakeAsrService (已完成)
 * Wave 2: FunAsrService (当前)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AsrModule {
    
    @Binds
    @Singleton
    abstract fun bindAsrService(impl: FunAsrService): AsrService
}
