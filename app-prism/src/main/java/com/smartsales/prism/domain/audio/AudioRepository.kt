package com.smartsales.prism.domain.audio

import kotlinx.coroutines.flow.Flow

/**
 * 音频仓库 — 获取和管理录音文件
 * @see prism-ui-ux-contract.md §1.8
 */
interface AudioRepository {
    /**
     * 获取所有音频文件流 (响应式)
     */
    fun getAudioFiles(): Flow<List<AudioFile>>
    
    /**
     * 触发同步
     */
    suspend fun syncFromDevice()
    
    /**
     * 开始转写
     */
    suspend fun startTranscription(audioId: String)
    
    /**
     * 删除音频
     */
    fun deleteAudio(audioId: String)
    
    /**
     * 切换收藏状态
     */
    fun toggleStar(audioId: String)
}

/**
 * 音频文件数据模型
 */
data class AudioFile(
    val id: String,
    val filename: String,
    val timeDisplay: String,
    val source: AudioSource,
    val status: TranscriptionStatus,
    val isStarred: Boolean = false,
    val summary: String? = null,
    val progress: Float = 0f
)

enum class AudioSource {
    SMARTBADGE, PHONE
}

enum class TranscriptionStatus {
    PENDING, TRANSCRIBING, TRANSCRIBED
}
