package com.smartsales.core.metahub

// 文件：core/util/src/test/java/com/smartsales/core/metahub/TranscriptMetadataMergeTest.kt
// 模块：:core:util
// 说明：验证 TranscriptMetadata 合并的非空覆盖与说话人合并规则
// 作者：创建于 2025-12-06

import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptMetadataMergeTest {

    @Test
    fun mergeWith_preservesExistingSpeakerAndMergesNewOnes() {
        val base = TranscriptMetadata(
            transcriptId = "t1",
            sessionId = "s1",
            speakerMap = mapOf(
                "1" to SpeakerMeta(displayName = "客户", role = SpeakerRole.CUSTOMER, confidence = 0.9f)
            ),
            createdAt = 5L,
            extra = mapOf("note" to "old")
        )
        val delta = TranscriptMetadata(
            transcriptId = "t1",
            sessionId = "s1",
            speakerMap = mapOf(
                "1" to SpeakerMeta(displayName = "客户A", role = SpeakerRole.CUSTOMER, confidence = 0.7f),
                "2" to SpeakerMeta(displayName = "销售", role = SpeakerRole.SALES, confidence = 1.1f)
            ),
            createdAt = 10L,
            extra = mapOf("source" to "llm")
        )

        val merged = base.mergeWith(delta)

        assertEquals("客户A", merged.speakerMap["1"]?.displayName)
        // 新增说话人应保留
        assertEquals("销售", merged.speakerMap["2"]?.displayName)
        // 置信度应被夹紧到 [0,1]
        assertEquals(1f, merged.speakerMap["2"]?.confidence)
        // createdAt 取更大值
        assertEquals(10L, merged.createdAt)
        assertEquals(mapOf("note" to "old", "source" to "llm"), merged.extra)
    }
}
