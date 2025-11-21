package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/AudioTranscriptionModule.kt
// 模块：:app
// 说明：提供 AudioTranscriptionCoordinator 的 Hilt 绑定
// 作者：创建于 2025-11-21

import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface AudioTranscriptionModule {
    @Binds
    fun bindAudioTranscriptionCoordinator(
        impl: DefaultAudioTranscriptionCoordinator,
    ): AudioTranscriptionCoordinator
}
