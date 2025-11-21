package com.smartsales.feature.media.audiofiles

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioPlaybackController.kt
// 模块：:feature:media
// 说明：抽象音频播放控制器，便于在 ViewModel 与 App 层之间解耦
// 作者：创建于 2025-11-21

import com.smartsales.core.util.Result
import java.io.File

/**
 * 音频播放控制器：ViewModel 仅依赖接口，由 App 层提供具体实现（MediaPlayer 等）。
 */
interface AudioPlaybackController {
    suspend fun play(file: File): Result<Unit>
    suspend fun pause(): Result<Unit>
    suspend fun stop(): Result<Unit> = pause()
}
