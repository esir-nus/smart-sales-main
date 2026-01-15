package com.smartsales.feature.chat

import com.smartsales.data.aicore.ExportFormat
import com.smartsales.data.aicore.TingwuRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake implementation for testing. Tracks method calls and supports stubbing.
 */
class FakeChatController : ChatController {
    override val state: StateFlow<ChatState> = MutableStateFlow(ChatState())
    
    // Call tracking
    val updateDraftCalls = mutableListOf<String>()
    val toggleSkillCalls = mutableListOf<ChatSkill>()
    val sendCalls = mutableListOf<String>()
    val requestExportCalls = mutableListOf<ExportFormat>()
    val startTranscriptJobCalls = mutableListOf<TingwuRequest>()
    val importTranscriptCalls = mutableListOf<Pair<String, String?>>()
    
    override fun updateDraft(text: String) {
        updateDraftCalls.add(text)
    }
    
    override fun toggleSkill(skill: ChatSkill) {
        toggleSkillCalls.add(skill)
    }
    
    override fun clearClipboardMessage() {}
    
    override fun clearError() {}
    
    override fun copyMarkdown() {}
    
    override suspend fun send(prompt: String) {
        sendCalls.add(prompt)
    }
    
    override suspend fun requestExport(format: ExportFormat) {
        requestExportCalls.add(format)
    }
    
    override suspend fun startTranscriptJob(request: TingwuRequest) {
        startTranscriptJobCalls.add(request)
    }
    
    override suspend fun importTranscript(markdown: String, sourceName: String?) {
        importTranscriptCalls.add(markdown to sourceName)
    }
    
    fun reset() {
        updateDraftCalls.clear()
        toggleSkillCalls.clear()
        sendCalls.clear()
        requestExportCalls.clear()
        startTranscriptJobCalls.clear()
        importTranscriptCalls.clear()
    }
}
