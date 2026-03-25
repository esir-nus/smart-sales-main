package com.smartsales.prism.ui.sim

import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.session.SimPersistedSession
import com.smartsales.prism.data.session.SimSessionRepository
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.UiState
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

internal const val SIM_HISTORY_GROUP_PINNED = "置顶"
internal const val SIM_HISTORY_GROUP_TODAY = "今天"
internal const val SIM_HISTORY_GROUP_LAST_30_DAYS = "最近30天"
private val SimHistoryMonthFormatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.getDefault())

internal data class SimSessionRecord(
    val preview: SessionPreview,
    val messages: List<ChatMessage>
)

internal data class SimAgentUiBridge(
    val getCurrentSessionId: () -> String?,
    val getCurrentSchedulerFollowUpContext: () -> SchedulerFollowUpContext?,
    val getSelectedSchedulerFollowUpTaskId: () -> String?,
    val getUiState: () -> UiState,
    val setCurrentSessionId: (String?) -> Unit,
    val setUiState: (UiState) -> Unit,
    val setInputText: (String) -> Unit,
    val setIsSending: (Boolean) -> Unit,
    val setErrorMessage: (String?) -> Unit,
    val setToastMessage: (String?) -> Unit,
    val setHistory: (List<ChatMessage>) -> Unit,
    val setSessionTitle: (String) -> Unit,
    val setGroupedSessions: (Map<String, List<SessionPreview>>) -> Unit,
    val setCurrentLinkedAudioId: (String?) -> Unit,
    val setCurrentSchedulerFollowUpContext: (SchedulerFollowUpContext?) -> Unit,
    val setSelectedSchedulerFollowUpTaskId: (String?) -> Unit,
    val removeArtifactTranscriptReveal: (Set<String>) -> Unit
)

internal class SimAgentSessionCoordinator(
    private val sessionRepository: SimSessionRepository,
    private val audioRepository: SimAudioRepository,
    private val bridge: SimAgentUiBridge
) {

    private val sessions = linkedMapOf<String, SimSessionRecord>()
    private val audioBindings = linkedMapOf<String, String>()

    fun loadPersistedSessions() {
        val loadedSessions = normalizeDuplicateAudioLinks(sessionRepository.loadSessions())
        sessions.clear()
        audioBindings.clear()
        loadedSessions.forEach { session ->
            sessions[session.preview.id] = SimSessionRecord(
                preview = session.preview,
                messages = session.messages
            )
            session.preview.linkedAudioId?.let { audioId ->
                audioBindings[audioId] = session.preview.id
            }
        }
        refreshGroupedSessions()
    }

    fun reconcileAudioBindings() {
        val sessionIds = sessions.keys.toSet()
        audioRepository.getAudioFilesSnapshot().forEach { audio ->
            val boundSessionId = audio.boundSessionId ?: return@forEach
            if (boundSessionId !in sessionIds) {
                audioRepository.clearBoundSession(audio.id)
            }
        }

        sessions.values
            .sortedByDescending { it.preview.timestamp }
            .forEach { record ->
                val audioId = record.preview.linkedAudioId ?: return@forEach
                val audio = audioRepository.getAudio(audioId)
                if (audio == null) {
                    updateSession(record.preview.id) { current ->
                        current.copy(preview = current.preview.copy(linkedAudioId = null))
                    }
                    audioBindings.remove(audioId)
                    return@forEach
                }

                if (audio.boundSessionId != record.preview.id) {
                    audioRepository.bindSession(audioId, record.preview.id)
                }
                audioBindings[audioId] = record.preview.id
            }
    }

    fun startNewSession() {
        bridge.setCurrentSessionId(null)
        bridge.setHistory(emptyList())
        bridge.setSessionTitle("新对话")
        bridge.setInputText("")
        bridge.setUiState(UiState.Idle)
        bridge.setErrorMessage(null)
        bridge.setCurrentLinkedAudioId(null)
        bridge.setCurrentSchedulerFollowUpContext(null)
        bridge.setSelectedSchedulerFollowUpTaskId(null)
    }

    fun createGeneralSession(): String {
        val sessionId = UUID.randomUUID().toString()
        val preview = SessionPreview(
            id = sessionId,
            clientName = "新对话",
            summary = "新会话",
            timestamp = System.currentTimeMillis(),
            sessionKind = SessionKind.GENERAL
        )
        sessions[sessionId] = SimSessionRecord(preview = preview, messages = emptyList())
        persistSession(sessionId)
        bridge.setCurrentSessionId(sessionId)
        bridge.setSessionTitle(preview.clientName)
        bridge.setCurrentLinkedAudioId(preview.linkedAudioId)
        bridge.setCurrentSchedulerFollowUpContext(null)
        bridge.setSelectedSchedulerFollowUpTaskId(null)
        refreshGroupedSessions()
        return sessionId
    }

    fun createSession(
        preview: SessionPreview,
        messages: List<ChatMessage>,
        autoSelect: Boolean,
        bindLinkedAudio: Boolean = false
    ): String {
        sessions[preview.id] = SimSessionRecord(preview = preview, messages = messages)
        if (bindLinkedAudio) {
            preview.linkedAudioId?.let { audioId ->
                reassignAudioBinding(audioId = audioId, sessionId = preview.id)
            }
        }
        persistSession(preview.id)
        if (autoSelect) {
            switchSession(preview.id)
        } else {
            refreshGroupedSessions()
        }
        return preview.id
    }

    fun switchSession(sessionId: String) {
        val record = sessions[sessionId] ?: return
        bridge.setCurrentSessionId(sessionId)
        bridge.setHistory(record.messages)
        bridge.setSessionTitle(record.preview.clientName)
        bridge.setUiState(UiState.Idle)
        bridge.setInputText("")
        bridge.setCurrentLinkedAudioId(record.preview.linkedAudioId)
        bridge.setCurrentSchedulerFollowUpContext(record.preview.schedulerFollowUpContext)
        bridge.setSelectedSchedulerFollowUpTaskId(
            defaultSelectedFollowUpTaskId(record.preview.schedulerFollowUpContext)
        )
    }

    fun togglePin(sessionId: String) {
        updateSession(sessionId) { record ->
            record.copy(preview = record.preview.copy(isPinned = !record.preview.isPinned))
        }
    }

    fun renameSession(sessionId: String, clientName: String, summary: String) {
        updateSession(sessionId) { record ->
            record.copy(
                preview = record.preview.copy(
                    clientName = clientName.ifBlank { record.preview.clientName },
                    summary = summary.ifBlank { record.preview.summary }
                )
            )
        }
    }

    fun deleteSession(sessionId: String) {
        val removed = sessions.remove(sessionId) ?: return
        removed.preview.linkedAudioId?.let { audioId ->
            audioBindings.remove(audioId)
            audioRepository.clearBoundSession(audioId)
        }
        clearTranscriptRevealState(removed.messages)
        sessionRepository.deleteSession(sessionId)
        if (bridge.getCurrentSessionId() == sessionId) {
            bridge.setCurrentSessionId(null)
            bridge.setHistory(emptyList())
            bridge.setSessionTitle("SIM")
            bridge.setUiState(UiState.Idle)
            bridge.setCurrentLinkedAudioId(null)
            bridge.setCurrentSchedulerFollowUpContext(null)
            bridge.setSelectedSchedulerFollowUpTaskId(null)
        }
        refreshGroupedSessions()
    }

    fun handleDeletedAudio(audioId: String) {
        val affectedSessionIds = buildSet {
            audioBindings[audioId]?.let(::add)
            sessions.values
                .filter { it.preview.linkedAudioId == audioId }
                .mapTo(this) { it.preview.id }
        }
        if (affectedSessionIds.isEmpty()) return

        audioBindings.remove(audioId)
        audioRepository.clearBoundSession(audioId)

        affectedSessionIds.forEach { sessionId ->
            updateSession(sessionId) { record ->
                record.copy(
                    preview = record.preview.copy(
                        linkedAudioId = null,
                        sessionKind = when (record.preview.sessionKind) {
                            SessionKind.AUDIO_GROUNDED -> SessionKind.GENERAL
                            else -> record.preview.sessionKind
                        }
                    )
                )
            }
        }

        if (bridge.getCurrentSessionId() in affectedSessionIds) {
            bridge.setUiState(UiState.Idle)
        }
    }

    fun currentSessionId(): String? = bridge.getCurrentSessionId()

    fun currentSession(): SimSessionRecord? = currentSessionId()?.let { sessions[it] }

    fun getSession(sessionId: String): SimSessionRecord? = sessions[sessionId]

    fun existingSessionIdForAudio(audioId: String): String? =
        audioBindings[audioId]?.takeIf { sessions.containsKey(it) }

    fun attachAudioToSession(
        sessionId: String,
        audioId: String,
        title: String,
        summary: String?,
        retainExistingTitle: Boolean,
        introMessage: String
    ) {
        val currentRecord = sessions[sessionId] ?: return
        currentRecord.preview.linkedAudioId
            ?.takeIf { it != audioId }
            ?.let { previousAudioId ->
                audioBindings.remove(previousAudioId)
                audioRepository.clearBoundSession(previousAudioId)
            }

        reassignAudioBinding(audioId = audioId, sessionId = sessionId)
        updateSession(sessionId) { record ->
            val currentTitle = if (retainExistingTitle) {
                record.preview.clientName
            } else {
                title
            }
            val currentSummary = if (retainExistingTitle) {
                record.preview.summary
            } else {
                (summary ?: "音频讨论").take(6)
            }
            record.copy(
                preview = record.preview.copy(
                    clientName = currentTitle,
                    summary = currentSummary,
                    linkedAudioId = audioId,
                    sessionKind = SessionKind.AUDIO_GROUNDED,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        appendAiMessage(sessionId, UiState.Response(introMessage))
        switchSession(sessionId)
    }

    fun appendUserMessage(sessionId: String, content: String) {
        val record = sessions[sessionId] ?: return
        val timestamp = System.currentTimeMillis()
        val newMessage = ChatMessage.User(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            content = content
        )
        sessions[sessionId] = record.copy(
            preview = record.preview.copy(timestamp = timestamp),
            messages = record.messages + newMessage
        )
        bridge.setHistory(sessions.getValue(sessionId).messages)
        persistSession(sessionId)
        refreshGroupedSessions()
    }

    fun appendUserMessageForSend(sessionId: String, content: String) {
        appendUserMessage(sessionId, content)
        updateSession(sessionId) { record ->
            record.copy(
                preview = record.preview.copy(
                    summary = content.take(6).ifBlank { record.preview.summary }
                )
            )
        }
    }

    fun appendAiMessage(sessionId: String, uiState: UiState) {
        val record = sessions[sessionId] ?: return
        val timestamp = System.currentTimeMillis()
        val newMessage = ChatMessage.Ai(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            uiState = uiState
        )
        sessions[sessionId] = record.copy(
            preview = record.preview.copy(timestamp = timestamp),
            messages = record.messages + newMessage
        )
        persistSession(sessionId)
        if (bridge.getCurrentSessionId() == sessionId) {
            bridge.setHistory(sessions.getValue(sessionId).messages)
        }
        refreshGroupedSessions()
    }

    fun updateSession(
        sessionId: String,
        transform: (SimSessionRecord) -> SimSessionRecord
    ) {
        val current = sessions[sessionId] ?: return
        val updated = transform(current)
        sessions[sessionId] = updated
        persistSession(sessionId)
        if (bridge.getCurrentSessionId() == sessionId) {
            bridge.setHistory(updated.messages)
            bridge.setSessionTitle(updated.preview.clientName)
            bridge.setCurrentLinkedAudioId(updated.preview.linkedAudioId)
            bridge.setCurrentSchedulerFollowUpContext(updated.preview.schedulerFollowUpContext)
            if (updated.preview.schedulerFollowUpContext == null) {
                bridge.setSelectedSchedulerFollowUpTaskId(null)
            }
        }
        refreshGroupedSessions()
    }

    private fun refreshGroupedSessions() {
        bridge.setGroupedSessions(
            groupSimHistorySessions(
                previews = sessions.values.map { it.preview }
            )
        )
    }

    private fun normalizeDuplicateAudioLinks(
        loadedSessions: List<SimPersistedSession>
    ): List<SimPersistedSession> {
        val newestByAudioId = loadedSessions
            .filter { !it.preview.linkedAudioId.isNullOrBlank() }
            .groupBy { it.preview.linkedAudioId!! }
            .mapValues { (_, sessionsForAudio) ->
                sessionsForAudio.maxByOrNull { it.preview.timestamp }?.preview?.id
            }

        var changed = false
        val normalized = loadedSessions.map { session ->
            val linkedAudioId = session.preview.linkedAudioId
            val shouldKeepLink = linkedAudioId != null && newestByAudioId[linkedAudioId] == session.preview.id
            if (linkedAudioId != null && !shouldKeepLink) {
                changed = true
                session.copy(preview = session.preview.copy(linkedAudioId = null))
            } else {
                session
            }
        }

        if (changed) {
            normalized.forEach { session ->
                sessionRepository.saveSession(session.preview, session.messages)
            }
        }
        return normalized
    }

    private fun reassignAudioBinding(audioId: String, sessionId: String) {
        val previousBoundSessionId = audioBindings[audioId]
        if (previousBoundSessionId != null && previousBoundSessionId != sessionId) {
            updateSession(previousBoundSessionId) { record ->
                record.copy(
                    preview = record.preview.copy(
                        linkedAudioId = null,
                        sessionKind = SessionKind.GENERAL
                    )
                )
            }
        }
        audioBindings[audioId] = sessionId
        audioRepository.bindSession(audioId, sessionId)
    }

    private fun persistSession(sessionId: String) {
        val record = sessions[sessionId] ?: return
        sessionRepository.saveSession(
            preview = record.preview,
            messages = record.messages
        )
    }

    private fun clearTranscriptRevealState(messages: List<ChatMessage>) {
        val messageIds = messages.mapNotNull { message ->
            (message as? ChatMessage.Ai)
                ?.takeIf { it.uiState is UiState.AudioArtifacts }
                ?.id
        }.toSet()
        if (messageIds.isNotEmpty()) {
            bridge.removeArtifactTranscriptReveal(messageIds)
        }
    }

    private fun defaultSelectedFollowUpTaskId(context: SchedulerFollowUpContext?): String? {
        if (context == null) return null
        return context.boundTaskIds.singleOrNull()
    }
}

internal fun groupSimHistorySessions(
    previews: List<SessionPreview>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Map<String, List<SessionPreview>> {
    val sorted = previews.sortedByDescending { it.timestamp }
    val pinned = sorted.filter { it.isPinned }
    val regular = sorted.filterNot { it.isPinned }

    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val last30Cutoff = today.minusDays(30)

    val todaySessions = mutableListOf<SessionPreview>()
    val last30Sessions = mutableListOf<SessionPreview>()
    val monthlySessions = linkedMapOf<String, MutableList<SessionPreview>>()

    regular.forEach { preview ->
        val sessionDateTime = Instant.ofEpochMilli(preview.timestamp).atZone(zoneId)
        val sessionDate = sessionDateTime.toLocalDate()
        when {
            !sessionDate.isBefore(today) -> todaySessions += preview
            !sessionDate.isBefore(last30Cutoff) -> last30Sessions += preview
            else -> {
                val monthLabel = sessionDateTime.format(SimHistoryMonthFormatter)
                monthlySessions.getOrPut(monthLabel) { mutableListOf() } += preview
            }
        }
    }

    return linkedMapOf<String, List<SessionPreview>>().apply {
        if (pinned.isNotEmpty()) put(SIM_HISTORY_GROUP_PINNED, pinned)
        if (todaySessions.isNotEmpty()) put(SIM_HISTORY_GROUP_TODAY, todaySessions)
        if (last30Sessions.isNotEmpty()) put(SIM_HISTORY_GROUP_LAST_30_DAYS, last30Sessions)
        monthlySessions.forEach { (monthLabel, sessionsInMonth) ->
            put(monthLabel, sessionsInMonth.sortedByDescending { it.timestamp })
        }
    }
}
