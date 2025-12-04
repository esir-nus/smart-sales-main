package com.smartsales.aitest

// 文件：app/src/main/java/com/smartsales/aitest/MetaHubModule.kt
// 模块：:app
// 说明：提供元数据中心的Hilt绑定，使用内存实现
// 作者：创建于 2025-12-04

import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.metahub.MetaHub
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MetaHubModule {
    @Provides
    @Singleton
    fun provideMetaHub(): MetaHub = InMemoryMetaHub()
}
