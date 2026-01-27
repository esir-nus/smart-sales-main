package com.smartsales.prism.domain.core

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 假历史仓库 — 骨架开发阶段
 * 返回硬编码示例会话
 */
@Singleton
class FakeHistoryRepository @Inject constructor() {

    fun getSessions(): List<SessionPreview> {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        return listOf(
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
            // Video/Audio review (Recent 30 Days)
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
                timestamp = now - 45 * oneDay, // > 30 days
                isPinned = false
            ),
             SessionPreview(
                id = "archive-2",
                clientName = "孙经理",
                summary = "产品演示汇报",
                timestamp = now - 46 * oneDay,
                isPinned = false
            )
        )
    }
    
    /**
     * 按日期分组会话
     */
    fun getGroupedSessions(): Map<String, List<SessionPreview>> {
        val sessions = getSessions()
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        val pinned = sessions.filter { it.isPinned }
        val unpinned = sessions.filter { !it.isPinned }
        
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
            
            // Group archived by YYYY-MM
            if (archived.isNotEmpty()) {
                // In a real app, use DateTimeFormatter. Here we simulate for skeleton.
                // Assuming all test data is roughly consistent or we just dump them into a generic archive for now
                // Or split them properly if we want to be fancy.
                // For skeleton, let's just use one bucket or simple mock logic per item if needed.
                // Spec says "🗂️ 2025-12". Let's just hardcode a group for the fake data.
                put("🗂️ 2025-12", archived)
            }
        }
    }
}
