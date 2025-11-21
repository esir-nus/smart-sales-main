package com.smartsales.aitest.audio

// 文件：app/src/main/java/com/smartsales/aitest/audio/MediaPlayerAudioPlaybackController.kt
// 模块：:app
// 说明：使用 Android MediaPlayer 实现 AudioPlaybackController 接口
// 作者：创建于 2025-11-21

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.media.audiofiles.AudioPlaybackController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import java.io.File

@Singleton
class MediaPlayerAudioPlaybackController @Inject constructor(
    private val dispatchers: DispatcherProvider
) : AudioPlaybackController {

    private var mediaPlayer: MediaPlayer? = null

    override suspend fun play(file: File): Result<Unit> = withContext(dispatchers.main) {
        runCatching {
            val player = mediaPlayer ?: MediaPlayer().also { mediaPlayer = it }
            player.reset()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            player.setDataSource(file.absolutePath)
            player.prepare()
            player.start()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) }
        )
    }

    override suspend fun pause(): Result<Unit> = withContext(dispatchers.main) {
        val player = mediaPlayer ?: return@withContext Result.Success(Unit)
        runCatching {
            if (player.isPlaying) {
                player.pause()
            }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) }
        )
    }

    override suspend fun stop(): Result<Unit> = withContext(dispatchers.main) {
        val player = mediaPlayer ?: return@withContext Result.Success(Unit)
        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(it) }
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AudioPlaybackModule {
    @Binds
    fun bindAudioPlaybackController(impl: MediaPlayerAudioPlaybackController): AudioPlaybackController
}
