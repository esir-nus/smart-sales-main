package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAudioRepository : AudioRepository {
    private val _audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    var artifacts: TingwuJobArtifacts? = null

    override fun getAudioFiles(): Flow<List<AudioFile>> = _audioFiles

    override suspend fun syncFromDevice() {}

    override suspend fun addLocalAudio(uriString: String) {}

    override suspend fun startTranscription(audioId: String) {}

    override fun deleteAudio(audioId: String) {
        _audioFiles.value = _audioFiles.value.filter { it.id != audioId }
    }

    override fun toggleStar(audioId: String) {
        _audioFiles.value = _audioFiles.value.map { 
            if (it.id == audioId) it.copy(isStarred = !it.isStarred) else it 
        }
    }

    override fun getAudio(audioId: String): AudioFile? {
        return _audioFiles.value.find { it.id == audioId }
    }

    override fun getBoundSessionId(audioId: String): String? {
        return getAudio(audioId)?.boundSessionId
    }

    override fun bindSession(audioId: String, sessionId: String) {
        _audioFiles.value = _audioFiles.value.map {
            if (it.id == audioId) it.copy(boundSessionId = sessionId) else it
        }
    }

    override suspend fun getArtifacts(audioId: String): TingwuJobArtifacts? {
        return artifacts
    }
}
