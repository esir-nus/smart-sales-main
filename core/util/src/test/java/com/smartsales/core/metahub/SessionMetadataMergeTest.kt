package com.smartsales.core.metahub

// 文件：core/util/src/test/java/com/smartsales/core/metahub/SessionMetadataMergeTest.kt
// 模块：:core:util
// 说明：校验 SessionMetadata 合并规则的幂等与非空覆盖
// 作者：创建于 2025-12-06

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionMetadataMergeTest {

    @Test
    fun mergeWith_preservesExistingWhenNewIsNull() {
        val old = SessionMetadata(
            sessionId = "s1",
            mainPerson = "客户A",
            shortSummary = "旧摘要",
            summaryTitle6Chars = "旧标题",
            location = "北京",
            stage = SessionStage.DISCOVERY,
            riskLevel = RiskLevel.LOW,
            tags = setOf("老标签"),
            lastUpdatedAt = 10L,
            crmRows = listOf(CrmRow(client = "c1", owner = "o1"))
        )
        val delta = SessionMetadata(
            sessionId = "s1",
            lastUpdatedAt = 20L
        )

        val merged = old.mergeWith(delta)

        assertEquals("客户A", merged.mainPerson)
        assertEquals("旧摘要", merged.shortSummary)
        assertEquals("旧标题", merged.summaryTitle6Chars)
        assertEquals("北京", merged.location)
        assertEquals(SessionStage.DISCOVERY, merged.stage)
        assertEquals(RiskLevel.LOW, merged.riskLevel)
        assertEquals(setOf("老标签"), merged.tags)
        assertEquals(listOf(CrmRow(client = "c1", owner = "o1")), merged.crmRows)
        assertEquals(20L, merged.lastUpdatedAt)
    }

    @Test
    fun mergeWith_overridesNonNullAndMergesCollections() {
        val old = SessionMetadata(
            sessionId = "s2",
            mainPerson = "客户A",
            shortSummary = "旧摘要",
            tags = setOf("老标签", ""),
            crmRows = listOf(CrmRow(client = "c1", owner = "o1")),
            lastUpdatedAt = 5L
        )
        val delta = SessionMetadata(
            sessionId = "s2",
            mainPerson = "客户B",
            shortSummary = null,
            summaryTitle6Chars = "新标题",
            location = "上海",
            tags = setOf("新标签", " "),
            crmRows = listOf(CrmRow(client = "c1", owner = "o1"), CrmRow(client = "c2", owner = "o2")),
            lastUpdatedAt = 8L
        )

        val merged = old.mergeWith(delta)

        assertEquals("客户B", merged.mainPerson)
        // null 不应覆盖旧值
        assertEquals("旧摘要", merged.shortSummary)
        assertEquals("新标题", merged.summaryTitle6Chars)
        assertEquals("上海", merged.location)
        assertEquals(setOf("老标签", "新标签"), merged.tags)
        assertEquals(
            listOf(
                CrmRow(client = "c1", owner = "o1"),
                CrmRow(client = "c2", owner = "o2")
            ),
            merged.crmRows
        )
        assertEquals(8L, merged.lastUpdatedAt)
    }
}
