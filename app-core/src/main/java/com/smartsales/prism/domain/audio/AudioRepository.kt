package com.smartsales.prism.domain.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

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
     * 上传本地音频 (测试用)
     */
    suspend fun addLocalAudio(uriString: String)
    
    /**
     * 开始转写
     */
    suspend fun startTranscription(audioId: String)
    
    /**
     * 删除音频
     */
    suspend fun deleteAudio(audioId: String): AudioDeleteResult
    
    /**
     * 切换收藏状态
     */
    fun toggleStar(audioId: String)
    
    /**
     * 获取音频文件
     */
    fun getAudio(audioId: String): AudioFile?
    
    /**
     * 获取绑定的会话ID
     */
    fun getBoundSessionId(audioId: String): String?
    
    /**
     * 绑定会话
     */
    fun bindSession(audioId: String, sessionId: String)
    
    /**
     * 获取完整转写智能结果（用于展开态）
     */
    suspend fun getArtifacts(audioId: String): com.smartsales.prism.domain.tingwu.TingwuJobArtifacts?
}

sealed interface AudioDeleteResult {
    data object NotFound : AudioDeleteResult
    data class LocalOnly(val filename: String) : AudioDeleteResult
    data class Badge(
        val filename: String,
        val remoteDeleteSucceeded: Boolean
    ) : AudioDeleteResult
}

/**
 * 音频文件数据模型
 */
@Serializable
data class AudioFile(
    val id: String,
    val filename: String,
    val timeDisplay: String,
    val source: AudioSource,
    val status: TranscriptionStatus,
    val isStarred: Boolean = false,
    val isTestImport: Boolean = false,
    val summary: String? = null,
    val progress: Float = 0f,
    val boundSessionId: String? = null, // Links to Analysis chat session
    val activeJobId: String? = null,
    val lastErrorMessage: String? = null
)

@Serializable
enum class AudioSource {
    SMARTBADGE, PHONE
}

@Serializable
enum class TranscriptionStatus {
    PENDING, TRANSCRIBING, TRANSCRIBED
}
