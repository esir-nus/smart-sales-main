package com.smartsales.feature.chat.history

/**
 * Fake implementation for testing. Tracks method calls and stores data in memory.
 */
class FakeChatHistoryRepository : ChatHistoryRepository {
    private val sessions = mutableMapOf<String, List<ChatMessageEntity>>()
    val loadCalls = mutableListOf<String>()
    val saveCalls = mutableListOf<Pair<String, List<ChatMessageEntity>>>()
    val deleteCalls = mutableListOf<String>()
    
    override suspend fun loadLatestSession(sessionId: String): List<ChatMessageEntity> {
        loadCalls.add(sessionId)
        return sessions[sessionId] ?: emptyList()
    }
    
    override suspend fun saveMessages(sessionId: String, messages: List<ChatMessageEntity>) {
        saveCalls.add(sessionId to messages)
        sessions[sessionId] = messages
    }
    
    override suspend fun deleteSession(sessionId: String) {
        deleteCalls.add(sessionId)
        sessions.remove(sessionId)
    }
    
    fun reset() {
        sessions.clear()
        loadCalls.clear()
        saveCalls.clear()
        deleteCalls.clear()
    }
}
