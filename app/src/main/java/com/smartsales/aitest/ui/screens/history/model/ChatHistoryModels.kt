package com.smartsales.aitest.ui.screens.history.model

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/history/model/ChatHistoryModels.kt
// 模块：:app
// 说明：聊天历史的 UI 模型与分组工具（本地模拟）
// 作者：创建于 2025-12-02

import java.util.Calendar
import java.util.Locale

data class ChatSessionUi(
    val id: String,
    val title: String,
    val preview: String,
    val messageCount: Int,
    val createdAt: Long,
    val lastMessageAt: Long,
    val isPinned: Boolean = false
)

data class ChatHistoryGroupUi(
    val timeLabel: String,
    val sessions: List<ChatSessionUi>
)

fun groupSessionsByTime(sessions: List<ChatSessionUi>, now: Long = System.currentTimeMillis()): List<ChatHistoryGroupUi> {
    if (sessions.isEmpty()) return emptyList()
    val pinned = sessions.filter { it.isPinned }.sortedByDescending { it.lastMessageAt }
    val others = sessions.filterNot { it.isPinned }

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now
    setToStartOfDay(calendar)
    val startToday = calendar.timeInMillis
    calendar.add(Calendar.DATE, -1)
    val startYesterday = calendar.timeInMillis
    calendar.add(Calendar.DATE, -6)
    val startWeek = calendar.timeInMillis

    val todayList = mutableListOf<ChatSessionUi>()
    val yesterdayList = mutableListOf<ChatSessionUi>()
    val weekList = mutableListOf<ChatSessionUi>()
    val earlierList = mutableListOf<ChatSessionUi>()

    others.forEach { session ->
        val ts = session.lastMessageAt
        when {
            ts >= startToday -> todayList.add(session)
            ts >= startYesterday -> yesterdayList.add(session)
            ts >= startWeek -> weekList.add(session)
            else -> earlierList.add(session)
        }
    }

    fun List<ChatSessionUi>.sorted() = sortedByDescending { it.lastMessageAt }
    val groups = mutableListOf<ChatHistoryGroupUi>()
    if (pinned.isNotEmpty()) groups.add(ChatHistoryGroupUi("置顶", pinned))
    if (todayList.isNotEmpty()) groups.add(ChatHistoryGroupUi("今天", todayList.sorted()))
    if (yesterdayList.isNotEmpty()) groups.add(ChatHistoryGroupUi("昨天", yesterdayList.sorted()))
    if (weekList.isNotEmpty()) groups.add(ChatHistoryGroupUi("本周", weekList.sorted()))
    if (earlierList.isNotEmpty()) groups.add(ChatHistoryGroupUi("更早", earlierList.sorted()))
    return groups
}

private fun setToStartOfDay(calendar: Calendar) {
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
}
