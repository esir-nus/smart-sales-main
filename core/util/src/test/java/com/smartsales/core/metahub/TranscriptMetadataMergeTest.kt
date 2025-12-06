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
            transcriptId = "t-1",
            speakerMap = mapOf(
                "spk_a" to SpeakerMeta(
                    displayName = "顾客A",
                    role = SpeakerRole.CUSTOMER,
                    confidence = 0.9f
                ),
                "spk_b" to SpeakerMeta(
                    displayName = "销售B",
                    role = SpeakerRole.SALES,
                    confidence = 0.4f
                )
            ),
            createdAt = 10L
        )

        // 注意：这里 displayName 用 null，不要用 ""，
        // 否则 mergeSpeakers 会认为是新的空名字并覆盖掉旧的。
        val delta = TranscriptMetadata(
            transcriptId = "t-1",
            speakerMap = mapOf(
                // 覆盖已有 speaker：保留旧名字，更新角色和信心值（并夹紧）
                "spk_a" to SpeakerMeta(
                    displayName = null,         // => 保留 "顾客A"
                    role = SpeakerRole.OTHER,   // => 覆盖成 OTHER
                    confidence = 2.0f           // => 被 clamp 成 1.0
                ),
                // 新增 speaker：信心值为负数，预期 clamp 到 0.0
                "spk_c" to SpeakerMeta(
                    displayName = "教官",
                    role = SpeakerRole.OTHER,
                    confidence = -0.5f          // => 被 clamp 成 0.0
                )
            ),
            createdAt = 20L
        )

        val merged = base.mergeWith(delta)

        // 1) spk_a：保留旧名字 + 覆盖角色 + 信心值 clamp 到 1.0
        val a = merged.speakerMap["spk_a"]
        org.junit.Assert.assertNotNull(a)
        assertEquals("顾客A", a!!.displayName)
        assertEquals(SpeakerRole.OTHER, a.role)
        assertEquals(1.0f, a.confidence)

        // 2) spk_b：完全保留不变
        val b = merged.speakerMap["spk_b"]
        org.junit.Assert.assertNotNull(b)
        assertEquals("销售B", b!!.displayName)
        assertEquals(SpeakerRole.SALES, b.role)
        assertEquals(0.4f, b.confidence)

        // 3) spk_c：新增 speaker，信心值 clamp 到 0.0
        val c = merged.speakerMap["spk_c"]
        org.junit.Assert.assertNotNull(c)
        assertEquals("教官", c!!.displayName)
        assertEquals(SpeakerRole.OTHER, c.role)
        assertEquals(0.0f, c.confidence)

        // 4) 一共 3 个说话人
        assertEquals(3, merged.speakerMap.size)

        // 5) createdAt 使用较大的那个（20L）
        assertEquals(20L, merged.createdAt)
    }
}
