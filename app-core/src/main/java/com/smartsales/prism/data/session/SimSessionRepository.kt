package com.smartsales.prism.data.session

import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.SchedulerFollowUpContext
import com.smartsales.prism.domain.model.SchedulerFollowUpTaskSummary
import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.model.SessionKind
import com.smartsales.prism.domain.model.UiState
import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val SIM_SESSION_METADATA_FILENAME = "sim_session_metadata.json"

/**
 * SIM 会话持久化仓库。
 * 使用独立命名空间保存会话元数据与消息，避免复用智能版会话表。
 */
class SimSessionRepository(
    private val filesDir: File
) {

    private val metadataFile = File(filesDir, SIM_SESSION_METADATA_FILENAME)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "kind"
    }

    @Synchronized
    fun loadSessions(): List<SimPersistedSession> {
        val metadata = runCatching { readMetadata() }.getOrElse { emptyList() }
        return metadata.map { entry ->
            SimPersistedSession(
                preview = entry.toDomain(),
                messages = runCatching { readMessages(entry.sessionId) }.getOrElse { emptyList() }
            )
        }
    }

    @Synchronized
    fun saveSession(
        preview: SessionPreview,
        messages: List<ChatMessage>
    ) {
        val metadata = readMetadata().toMutableList()
        val updatedEntry = preview.toStored()
        val existingIndex = metadata.indexOfFirst { it.sessionId == preview.id }
        if (existingIndex >= 0) {
            metadata[existingIndex] = updatedEntry
        } else {
            metadata += updatedEntry
        }
        writeMetadata(metadata)
        writeMessages(preview.id, messages)
    }

    @Synchronized
    fun deleteSession(sessionId: String) {
        val metadata = readMetadata().filterNot { it.sessionId == sessionId }
        writeMetadata(metadata)
        messageFile(sessionId).delete()
    }

    private fun readMetadata(): List<SimStoredSessionMetadata> {
        if (!metadataFile.exists()) return emptyList()
        val raw = metadataFile.readText()
        if (raw.isBlank()) return emptyList()
        return json.decodeFromString(raw)
    }

    private fun writeMetadata(entries: List<SimStoredSessionMetadata>) {
        metadataFile.writeText(json.encodeToString(entries))
    }

    private fun readMessages(sessionId: String): List<ChatMessage> {
        val file = messageFile(sessionId)
        if (!file.exists()) return emptyList()
        val raw = file.readText()
        if (raw.isBlank()) return emptyList()
        val durableMessages: List<SimStoredMessage> = json.decodeFromString(raw)
        return durableMessages.mapNotNull { it.toDomain() }
    }

    private fun writeMessages(sessionId: String, messages: List<ChatMessage>) {
        val durableMessages = messages.mapNotNull { it.toStored() }
        messageFile(sessionId).writeText(json.encodeToString(durableMessages))
    }

    private fun messageFile(sessionId: String): File {
        return File(filesDir, "sim_session_${sessionId}_messages.json")
    }
}

data class SimPersistedSession(
    val preview: SessionPreview,
    val messages: List<ChatMessage>
)

@Serializable
private data class SimStoredSessionMetadata(
    val sessionId: String,
    val clientName: String,
    val summary: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val linkedAudioId: String? = null,
    val sessionKind: String = SessionKind.GENERAL.name,
    val schedulerFollowUpContext: SimStoredSchedulerFollowUpContext? = null
)

@Serializable
private data class SimStoredSchedulerFollowUpContext(
    val sourceBadgeThreadId: String,
    val boundTaskIds: List<String>,
    val batchId: String? = null,
    val taskSummaries: List<SimStoredSchedulerFollowUpTaskSummary> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
private data class SimStoredSchedulerFollowUpTaskSummary(
    val taskId: String,
    val title: String,
    val dayOffset: Int,
    val scheduledAtMillis: Long,
    val durationMinutes: Int
)

@Serializable
private sealed interface SimStoredMessage {
    val id: String
    val timestamp: Long

    @Serializable
    @SerialName("user_text")
    data class UserText(
        override val id: String,
        override val timestamp: Long,
        val content: String
    ) : SimStoredMessage

    @Serializable
    @SerialName("ai_response")
    data class AiResponse(
        override val id: String,
        override val timestamp: Long,
        val content: String,
        val structuredJson: String? = null,
        val suggestAnalyst: Boolean = false
    ) : SimStoredMessage

    @Serializable
    @SerialName("ai_audio_artifacts")
    data class AiAudioArtifacts(
        override val id: String,
        override val timestamp: Long,
        val audioId: String,
        val title: String,
        val artifactsJson: String
    ) : SimStoredMessage

    @Serializable
    @SerialName("ai_error")
    data class AiError(
        override val id: String,
        override val timestamp: Long,
        val message: String,
        val retryable: Boolean = true
    ) : SimStoredMessage
}

private fun SessionPreview.toStored(): SimStoredSessionMetadata {
    return SimStoredSessionMetadata(
        sessionId = id,
        clientName = clientName,
        summary = summary,
        timestamp = timestamp,
        isPinned = isPinned,
        linkedAudioId = linkedAudioId,
        sessionKind = sessionKind.name,
        schedulerFollowUpContext = schedulerFollowUpContext?.toStored()
    )
}

private fun SimStoredSessionMetadata.toDomain(): SessionPreview {
    return SessionPreview(
        id = sessionId,
        clientName = clientName,
        summary = summary,
        timestamp = timestamp,
        isPinned = isPinned,
        linkedAudioId = linkedAudioId,
        sessionKind = SessionKind.entries.firstOrNull { it.name == sessionKind } ?: SessionKind.GENERAL,
        schedulerFollowUpContext = schedulerFollowUpContext?.toDomain()
    )
}

private fun SchedulerFollowUpContext.toStored(): SimStoredSchedulerFollowUpContext {
    return SimStoredSchedulerFollowUpContext(
        sourceBadgeThreadId = sourceBadgeThreadId,
        boundTaskIds = boundTaskIds,
        batchId = batchId,
        taskSummaries = taskSummaries.map { it.toStored() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun SchedulerFollowUpTaskSummary.toStored(): SimStoredSchedulerFollowUpTaskSummary {
    return SimStoredSchedulerFollowUpTaskSummary(
        taskId = taskId,
        title = title,
        dayOffset = dayOffset,
        scheduledAtMillis = scheduledAtMillis,
        durationMinutes = durationMinutes
    )
}

private fun SimStoredSchedulerFollowUpContext.toDomain(): SchedulerFollowUpContext {
    return SchedulerFollowUpContext(
        sourceBadgeThreadId = sourceBadgeThreadId,
        boundTaskIds = boundTaskIds,
        batchId = batchId,
        taskSummaries = taskSummaries.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun SimStoredSchedulerFollowUpTaskSummary.toDomain(): SchedulerFollowUpTaskSummary {
    return SchedulerFollowUpTaskSummary(
        taskId = taskId,
        title = title,
        dayOffset = dayOffset,
        scheduledAtMillis = scheduledAtMillis,
        durationMinutes = durationMinutes
    )
}

private fun ChatMessage.toStored(): SimStoredMessage? {
    return when (this) {
        is ChatMessage.User -> SimStoredMessage.UserText(
            id = id,
            timestamp = timestamp,
            content = content
        )

        is ChatMessage.Ai -> when (val state = uiState) {
            is UiState.Response -> SimStoredMessage.AiResponse(
                id = id,
                timestamp = timestamp,
                content = state.content,
                structuredJson = state.structuredJson,
                suggestAnalyst = state.suggestAnalyst
            )

            is UiState.AudioArtifacts -> SimStoredMessage.AiAudioArtifacts(
                id = id,
                timestamp = timestamp,
                audioId = state.audioId,
                title = state.title,
                artifactsJson = state.artifactsJson
            )

            is UiState.Error -> SimStoredMessage.AiError(
                id = id,
                timestamp = timestamp,
                message = state.message,
                retryable = state.retryable
            )

            else -> null
        }
    }
}

private fun SimStoredMessage.toDomain(): ChatMessage {
    return when (this) {
        is SimStoredMessage.UserText -> ChatMessage.User(
            id = id,
            timestamp = timestamp,
            content = content
        )

        is SimStoredMessage.AiResponse -> ChatMessage.Ai(
            id = id,
            timestamp = timestamp,
            uiState = UiState.Response(
                content = content,
                structuredJson = structuredJson,
                suggestAnalyst = suggestAnalyst
            )
        )

        is SimStoredMessage.AiAudioArtifacts -> ChatMessage.Ai(
            id = id,
            timestamp = timestamp,
            uiState = UiState.AudioArtifacts(
                audioId = audioId,
                title = title,
                artifactsJson = artifactsJson
            )
        )

        is SimStoredMessage.AiError -> ChatMessage.Ai(
            id = id,
            timestamp = timestamp,
            uiState = UiState.Error(
                message = message,
                retryable = retryable
            )
        )
    }
}
