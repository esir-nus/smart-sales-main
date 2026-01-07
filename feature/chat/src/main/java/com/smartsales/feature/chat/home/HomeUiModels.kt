// File: feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeUiModels.kt
// Module: :feature:chat
// Summary: UI models for Home screen - data classes for chat messages, devices, audio
// Author: created on 2026-01-06

package com.smartsales.feature.chat.home

import com.smartsales.domain.config.QuickSkillId
import java.util.UUID

/** UI 模型：代表 Home 页里的一条聊天气泡。 */
data class ChatMessageUi(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatMessageRole,
    val content: String,
    val rawContent: String? = null,
    val sanitizedContent: String? = null,
    val timestampMillis: Long,
    val isStreaming: Boolean = false,
    val hasError: Boolean = false,
    val isSmartAnalysis: Boolean = false
)

/** 区分用户与助手消息（Home 层级）。 */
enum class ChatMessageRole { USER, ASSISTANT }

/** UI 模型：输入框下方的快捷技能。 */
data class QuickSkillUi(
    val id: QuickSkillId,
    val label: String,
    val description: String? = null,
    val isRecommended: Boolean = false
)

/** UI 模型：设备横幅的轻量快照。 */
data class DeviceSnapshotUi(
    val deviceName: String? = null,
    val statusText: String,
    val connectionState: DeviceConnectionStateUi,
    val wifiName: String? = null,
    val serviceAddress: String? = null,
    val errorSummary: String? = null
)

/** 设备连接状态（用于 Home 横幅）。 */
enum class DeviceConnectionStateUi { DISCONNECTED, CONNECTING, WAITING_FOR_NETWORK, CONNECTED, ERROR }

/** UI 模型：音频/转写状态摘要。 */
data class AudioSummaryUi(
    val headline: String,
    val detail: String? = null,
    val syncedCount: Int = 0,
    val pendingUploadCount: Int = 0,
    val pendingTranscriptionCount: Int = 0,
    val lastSyncedAtMillis: Long? = null
)
