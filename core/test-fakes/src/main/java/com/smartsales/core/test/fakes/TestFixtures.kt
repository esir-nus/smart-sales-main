package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType

/**
 * Single Source of Truth for E2E Test Fixtures.
 * Fakes must NOT load these natively; Tests must inject them explicitly.
 */
object TestFixtures {
    
    val dayMs = 86400000L

    fun getHuaweiVisitEntry(baseTimeMs: Long): MemoryEntry {
        val feb4 = baseTimeMs - (4 * dayMs)
        return MemoryEntry(
            entryId = "seed-huawei-visit",
            sessionId = "session-feb04",
            content = "今天去华为坂田基地拜访，见了IT部门CTO李总。他们目前用的是竞品的CRM系统，合同明年3月到期。" +
                    "李总对我们的AI辅助功能很感兴趣，特别是智能会议摘要和客户画像分析。" +
                    "他提了两个顾虑：1) 数据安全合规（华为对数据主权要求很高），2) 和现有ERP系统的集成。" +
                    "约了下周三再做一次技术方案演示，需要带上解决方案架构师老陈。",
            entryType = MemoryEntryType.TASK_RECORD,
            createdAt = feb4,
            updatedAt = feb4,
            structuredJson = """{"relatedEntityIds":["huawei-001","li-cto-001"]}"""
        )
    }

    fun getAmeerMeetingEntry(baseTimeMs: Long): MemoryEntry {
        val feb9 = baseTimeMs + (1 * dayMs)
        return MemoryEntry(
            entryId = "seed-ameer-meeting",
            sessionId = "session-feb08",
            content = "2月9日下午2点，和 Professor Ameer 的年度回顾会议。" +
                    "需要准备：1) 过去一年的合作成果总结（3个联合项目），2) 下一年的合作提案，3) NUS实验室参观安排。" +
                    "Ameer 教授比较关注AI在东南亚市场的应用场景，准备几个本地化案例。" +
                    "地点：NUS Computing 学院会议室 COM2-02-12。",
            entryType = MemoryEntryType.SCHEDULE_ITEM,
            createdAt = baseTimeMs,
            updatedAt = baseTimeMs,
            scheduledAt = feb9,
            structuredJson = """{"relatedEntityIds":["ameer-prof-001"]}"""
        )
    }

    fun getBossWangBirthdayEntry(baseTimeMs: Long): MemoryEntry {
        val jan28 = baseTimeMs - (11 * dayMs)
        return MemoryEntry(
            entryId = "seed-boss-wang-birthday",
            sessionId = "session-jan28",
            content = "今天是王总生日，送了一套景德镇手工青花瓷茶壶给他。王总很开心，" +
                    "聊到他最近在收藏紫砂壶，下次可以找个宜兴的壶送。" +
                    "他顺便提了一下Q2的预算申请已经批了，让我下周把正式报价单发过去。" +
                    "王总的助理小张说老板下个月要去日本出差两周（3月10-24日），报价要赶在那之前定下来。",
            entryType = MemoryEntryType.TASK_RECORD,
            createdAt = jan28,
            updatedAt = jan28,
            structuredJson = """{"relatedEntityIds":["wang-boss-001"]}"""
        )
    }
}
