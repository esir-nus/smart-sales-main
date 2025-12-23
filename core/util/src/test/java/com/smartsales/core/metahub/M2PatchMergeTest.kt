package com.smartsales.core.metahub

// 文件：core/util/src/test/java/com/smartsales/core/metahub/M2PatchMergeTest.kt
// 模块：:core:util
// 说明：验证 M2 补丁合并的确定性与覆盖规则
// 作者：创建于 2025-12-23

import org.junit.Assert.assertEquals
import org.junit.Test

class M2PatchMergeTest {

    @Test
    fun applyM2PatchHistory_respectsOrderAndDeterminism() {
        val prov = Provenance(source = "test", updatedAt = 100L)
        val first = M2PatchRecord(
            patchId = "p1",
            createdAt = 10L,
            prov = prov,
            payload = ConversationDerivedStateDelta(
                rawSignals = RawSignals(
                    dealRiskLevel = DerivedEnumWithProv(value = "LOW", prov = prov)
                )
            )
        )
        val second = M2PatchRecord(
            patchId = "p2",
            createdAt = 20L,
            prov = prov,
            payload = ConversationDerivedStateDelta(
                rawSignals = RawSignals(
                    dealRiskLevel = DerivedEnumWithProv(value = "HIGH", prov = prov)
                )
            )
        )

        val effective = applyM2PatchHistory(listOf(first, second))

        assertEquals("HIGH", effective.rawSignals.dealRiskLevel?.value)
        assertEquals(20L, effective.updatedAt)
        assertEquals(2, effective.version)
    }

    @Test
    fun applyM2PatchHistory_lastNonNullWinsAndKeepsOtherFields() {
        val prov = Provenance(source = "test", updatedAt = 100L)
        val first = M2PatchRecord(
            patchId = "p1",
            createdAt = 10L,
            prov = prov,
            payload = ConversationDerivedStateDelta(
                uiSignals = UiSignals(stage = "DISCOVERY"),
                smartAnalysisRefs = listOf(ArtifactRef(artifactId = "a1"))
            )
        )
        val second = M2PatchRecord(
            patchId = "p2",
            createdAt = 30L,
            prov = prov,
            payload = ConversationDerivedStateDelta(
                uiSignals = UiSignals(stage = "NEGOTIATION")
            )
        )

        val effective = applyM2PatchHistory(listOf(first, second))

        assertEquals("NEGOTIATION", effective.uiSignals.stage)
        assertEquals(listOf(ArtifactRef(artifactId = "a1")), effective.smartAnalysisRefs)
        assertEquals(30L, effective.updatedAt)
        assertEquals(2, effective.version)
    }

    @Test
    fun applyM2PatchHistory_emptyHistoryIsDeterministic() {
        val effective = applyM2PatchHistory(emptyList())

        assertEquals(0L, effective.updatedAt)
        assertEquals(0, effective.version)
        assertEquals(ConversationDerivedState(), effective)
    }
}
