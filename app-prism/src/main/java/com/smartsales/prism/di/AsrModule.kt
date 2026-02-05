package com.smartsales.prism.di

import com.smartsales.prism.data.fakes.FakeAsrService
import com.smartsales.prism.domain.asr.AsrService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * ASR 模块 — 提供 ASR 服务绑定
 * 
 * Wave 1: 绑定 FakeAsrService
 * Wave 2: 切换到 FunAsrService
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AsrModule {
    
    @Binds
    @Singleton
    abstract fun bindAsrService(fake: FakeAsrService): AsrService
}
