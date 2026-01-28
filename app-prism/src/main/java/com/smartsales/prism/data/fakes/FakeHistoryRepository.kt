package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.model.SessionPreview
import com.smartsales.prism.domain.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 假历史仓库 — 骨架开发阶段
 * 返回硬编码示例会话，支持置顶/重命名/删除操作
 */
@Singleton
class FakeHistoryRepository @Inject constructor() : HistoryRepository {
    
    // 使用可变列表支持修改操作
    private val sessions = mutableListOf<SessionPreview>().apply {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        addAll(listOf(
            // 置顶
            SessionPreview(
                id = "pinned-1",
                clientName = "张总",
                summary = "Q4预算审查",
                timestamp = now - 2 * oneDay,
                isPinned = true
            ),
            // 今天
            SessionPreview(
                id = "today-1",
                clientName = "王经理",
                summary = "A3项目跟进",
                timestamp = now - 2 * 60 * 60 * 1000L,
                isPinned = false
            ),
            // Recent 30 Days
            SessionPreview(
                id = "recent-1",
                clientName = "李财务",
                summary = "采购谈判中",
                timestamp = now - 5 * oneDay,
                isPinned = false
            ),
            SessionPreview(
                id = "recent-2",
                clientName = "陈主任",
                summary = "竞品价格分析",
                timestamp = now - 15 * oneDay,
                isPinned = false
            ),
            // Archived (YYYY-MM)
            SessionPreview(
                id = "archive-1",
                clientName = "赵总",
                summary = "合作意向确认",
                timestamp = now - 45 * oneDay,
                isPinned = false
            ),
            SessionPreview(
                id = "archive-2",
                clientName = "孙经理",
                summary = "产品演示汇报",
                timestamp = now - 46 * oneDay,
                isPinned = false
            )
        ))
    }

    override fun getSessions(): List<SessionPreview> = sessions.toList()
    
    override fun getSession(sessionId: String): SessionPreview? {
        return sessions.find { it.id == sessionId }
    }
    
    override fun togglePin(sessionId: String) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            val session = sessions[index]
            sessions[index] = session.copy(isPinned = !session.isPinned)
        }
    }
    
    override fun renameSession(sessionId: String, newClientName: String, newSummary: String) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            val session = sessions[index]
            sessions[index] = session.copy(clientName = newClientName, summary = newSummary)
        }
    }
    
    override fun deleteSession(sessionId: String) {
        sessions.removeIf { it.id == sessionId }
    }
    
    /**
     * 按日期分组会话
     */
    override fun getGroupedSessions(): Map<String, List<SessionPreview>> {
        val currentSessions = getSessions()
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        val pinned = currentSessions.filter { it.isPinned }
        val unpinned = currentSessions.filter { !it.isPinned }
        
        val today = unpinned.filter { now - it.timestamp < oneDay }
        val recent30Days = unpinned.filter { 
            val age = now - it.timestamp
            age >= oneDay && age < 30 * oneDay 
        }
        val archived = unpinned.filter { now - it.timestamp >= 30 * oneDay }
        
        return buildMap {
            if (pinned.isNotEmpty()) put("📌 置顶", pinned)
            if (today.isNotEmpty()) put("📅 今天", today)
            if (recent30Days.isNotEmpty()) put("🗓️ 最近30天", recent30Days)
            if (archived.isNotEmpty()) put("🗂️ 2025-12", archived)
        }
    }
}
