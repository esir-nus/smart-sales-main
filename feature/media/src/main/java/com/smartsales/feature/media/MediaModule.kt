package com.smartsales.feature.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 文件路径: feature/media/src/main/java/com/smartsales/feature/media/MediaModule.kt
// 文件作用: 提供媒體同步接口的默认依赖
// 文件作者: Codex
// 最近修改: 2025-02-14
@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {
    @Binds
    @Singleton
    fun bindMediaSyncCoordinator(impl: FakeMediaSyncCoordinator): MediaSyncCoordinator
}
