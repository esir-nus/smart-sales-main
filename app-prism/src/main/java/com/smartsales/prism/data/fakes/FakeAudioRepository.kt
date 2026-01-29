package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 假音频仓库 — 模拟设备同步和转写延迟
 */
@Singleton
class FakeAudioRepository @Inject constructor() : AudioRepository {
    
    private val _audioFiles = MutableStateFlow(generateSampleData())
    
    override fun getAudioFiles(): Flow<List<AudioFile>> = _audioFiles.asStateFlow()
    
    override suspend fun syncFromDevice() {
        // 模拟 BLE 同步延迟
        delay(1500)
        // 在实际实现中会从设备拉取新文件
    }
    
    override suspend fun startTranscription(audioId: String) {
        // 模拟转写过程
        updateFileStatus(audioId, TranscriptionStatus.TRANSCRIBING)
        
        // 模拟进度更新
        for (progress in listOf(0.25f, 0.5f, 0.75f, 1.0f)) {
            delay(500)
            updateFileProgress(audioId, progress)
        }
        
        // 完成转写
        updateFileStatus(audioId, TranscriptionStatus.TRANSCRIBED)
        updateFileSummary(audioId, "AI生成的会议摘要...")
    }
    
    override fun deleteAudio(audioId: String) {
        _audioFiles.update { files ->
            files.filter { it.id != audioId }
        }
    }
    
    override fun toggleStar(audioId: String) {
        _audioFiles.update { files ->
            files.map { file ->
                if (file.id == audioId) file.copy(isStarred = !file.isStarred) else file
            }
        }
    }
    
    private fun updateFileStatus(audioId: String, status: TranscriptionStatus) {
        _audioFiles.update { files ->
            files.map { file ->
                if (file.id == audioId) file.copy(status = status) else file
            }
        }
    }
    
    private fun updateFileProgress(audioId: String, progress: Float) {
        _audioFiles.update { files ->
            files.map { file ->
                if (file.id == audioId) file.copy(progress = progress) else file
            }
        }
    }
    
    private fun updateFileSummary(audioId: String, summary: String) {
        _audioFiles.update { files ->
            files.map { file ->
                if (file.id == audioId) file.copy(summary = summary) else file
            }
        }
    }
    
    override fun getAudio(audioId: String): AudioFile? {
        return _audioFiles.value.find { it.id == audioId }
    }
    
    override fun bindSession(audioId: String, sessionId: String) {
        _audioFiles.update { files ->
            files.map { file ->
                if (file.id == audioId) file.copy(boundSessionId = sessionId) else file
            }
        }
    }
    
    override fun getBoundSessionId(audioId: String): String? {
        return _audioFiles.value.find { it.id == audioId }?.boundSessionId
    }
    
    private fun generateSampleData(): List<AudioFile> = listOf(
        // Item 1: Transcribed & Starred
        AudioFile(
            id = "1",
            filename = "Q4_年度预算会议.wav",
            timeDisplay = "1 day",
            source = AudioSource.SMARTBADGE,
            status = TranscriptionStatus.TRANSCRIBED,
            isStarred = true, // Starred
            summary = "财务部关于Q4预算的最终审核意见，重点讨论了SaaS订阅模式的成本结构..."
        ),
        // Item 2: Transcribing
        AudioFile(
            id = "2",
            filename = "meeting_notes.wav",
            timeDisplay = "10:15",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.TRANSCRIBING,
            isStarred = false,
            progress = 0.45f
        ),
        // Item 3: Transcribed (Unstarred)
        AudioFile(
            id = "3",
            filename = "产品研发周会.wav",
            timeDisplay = "2 days",
            source = AudioSource.SMARTBADGE,
            status = TranscriptionStatus.TRANSCRIBED,
            isStarred = false, // Unstarred
            summary = "讨论了Prism架构的内存模型优化方案，确定了三层存储结构的实施细节..."
        ),
        // Item 4: Pending (SmartBadge - Un-transcribed 1)
        AudioFile(
            id = "4",
            filename = "客户拜访_张总_20260124.wav",
            timeDisplay = "14:30",
            source = AudioSource.SMARTBADGE,
            status = TranscriptionStatus.PENDING,
            isStarred = false
        ),
        // Item 5: Pending (Phone - Un-transcribed 2)
        AudioFile(
            id = "5",
            filename = "临时想法记录.wav",
            timeDisplay = "09:20",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.PENDING,
            isStarred = false
        )
    )
}
