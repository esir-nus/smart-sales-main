package com.smartsales.feature.chat.history

import android.util.Log
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.LogTags
import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.home.ChatMessageRole
import javax.inject.Inject
import kotlinx.coroutines.withContext

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryRepository.kt
// 模块：:feature:chat
// 说明：定义并实现聊天记录的持久化仓库
// 作者：创建于 2025-11-22
interface ChatHistoryRepository {
    suspend fun loadLatestSession(sessionId: String = DEFAULT_SESSION_ID): List<ChatMessageEntity>
    suspend fun saveMessages(
        sessionId: String = DEFAULT_SESSION_ID,
        messages: List<ChatMessageEntity>
    )
    suspend fun deleteSession(sessionId: String)

    companion object {
        const val DEFAULT_SESSION_ID = "default"
    }
}

class RoomChatHistoryRepository @Inject constructor(
    private val dao: ChatHistoryDao,
    private val dispatchers: DispatcherProvider
) : ChatHistoryRepository {

    override suspend fun loadLatestSession(sessionId: String): List<ChatMessageEntity> {
        return withContext(dispatchers.io) {
            runCatching { dao.loadMessages(sessionId) }
                .onFailure { Log.w(TAG, "读取聊天记录失败", it) }
                .getOrElse { emptyList() }
        }
    }

    override suspend fun saveMessages(sessionId: String, messages: List<ChatMessageEntity>) {
        withContext(dispatchers.io) {
            runCatching {
                dao.deleteBySession(sessionId)
                if (messages.isNotEmpty()) {
                    dao.insertMessages(messages)
                }
            }.onFailure { Log.w(TAG, "保存聊天记录失败", it) }
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        withContext(dispatchers.io) {
            runCatching { dao.deleteBySession(sessionId) }
                .onFailure { Log.w(TAG, "删除聊天记录失败", it) }
        }
    }

    companion object {
        private const val TAG = "${LogTags.CHAT}/History"
    }
}

/** 临时辅助：将 UI 模型转换为实体，方便 ViewModel 调用。 */
fun ChatMessageUi.toEntity(sessionId: String): ChatMessageEntity = ChatMessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    content = content,
    timestampMillis = timestampMillis
)

fun ChatMessageEntity.toUiModel(): ChatMessageUi = ChatMessageUi(
    id = id,
    role = when (role) {
        ChatMessageRole.USER.name -> ChatMessageRole.USER
        else -> ChatMessageRole.ASSISTANT
    },
    content = content,
    timestampMillis = timestampMillis,
    isStreaming = false,
    hasError = false
)
