package com.smartsales.feature.media

import android.content.ContentResolver
import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.feature.media.processing.DefaultGifFrameExtractor
import com.smartsales.feature.media.processing.GifFrameExtractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// 文件路径: feature/media/src/main/java/com/smartsales/feature/media/MediaModule.kt
// 文件作用: 提供媒體同步接口的默认依赖
// 文件作者: Codex
// 最近修改: 2026-01-09
@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {
    @Binds
    @Singleton
    fun bindMediaSyncCoordinator(impl: FakeMediaSyncCoordinator): MediaSyncCoordinator

    @Binds
    @Singleton
    fun bindGifTransferCoordinator(impl: DefaultGifTransferCoordinator): GifTransferCoordinator

    @Binds
    @Singleton
    fun bindWavDownloadCoordinator(impl: DefaultWavDownloadCoordinator): WavDownloadCoordinator
}

@Module
@InstallIn(SingletonComponent::class)
object MediaProvidesModule {
    @Provides
    @Singleton
    fun provideGifFrameExtractor(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider
    ): GifFrameExtractor = DefaultGifFrameExtractor(context.contentResolver, dispatchers)
}
