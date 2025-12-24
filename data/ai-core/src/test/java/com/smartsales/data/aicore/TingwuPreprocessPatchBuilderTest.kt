package com.smartsales.data.aicore

import com.smartsales.core.metahub.BatchPlanItem
import com.smartsales.core.metahub.IndexRange
import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.metahub.SuspiciousBoundary
import com.smartsales.data.aicore.metahub.TingwuPreprocessPatchBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Test

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/TingwuPreprocessPatchBuilderTest.kt
// 模块：:data:ai-core
// 说明：验证 Tingwu 预处理补丁写入 MetaHub 的确定性输出
// 作者：创建于 2025-12-24

class TingwuPreprocessPatchBuilderTest {
    @Test
    fun `append patch yields preprocess snapshot`() = runTest {
        val metaHub = InMemoryMetaHub()
        val transcriptMarkdown = (1..25).joinToString("\n") { "line-$it" }
        // 说明：固定时间+固定输入，确保批次/预览结果稳定。
        val patch = TingwuPreprocessPatchBuilder.build(
            sessionId = "s-1",
            jobId = "job-1",
            transcriptMarkdown = transcriptMarkdown,
            createdAt = 1_000L
        )

        metaHub.appendM2Patch(sessionId = "s-1", patch = patch)

        val preprocess = assertNotNull(metaHub.getEffectiveM2("s-1")?.preprocess)
        assertEquals(20, preprocess.first20Rendered.size)
        assertEquals("line-1", preprocess.first20Rendered.first())
        assertEquals("tingwu.preprocess", preprocess.prov?.source)
        assertEquals(2, preprocess.batchPlan.size)
        assertEquals(0, preprocess.batchPlan[0].editableRange.start)
        assertEquals(19, preprocess.batchPlan[0].editableRange.endInclusive)
        assertEquals(20, preprocess.batchPlan[1].editableRange.start)
        assertEquals(24, preprocess.batchPlan[1].editableRange.endInclusive)
    }

    @Test
    fun `trace snapshot overrides fallback and stays deterministic`() = runTest {
        val metaHub = InMemoryMetaHub()
        val transcriptMarkdown = (1..25).joinToString("\n") { "line-$it" }
        // 说明：故意打乱顺序，验证 trace 覆盖与排序确定性。
        val traceBatchPlan = listOf(
            BatchPlanItem(
                batchId = "b2",
                editableRange = IndexRange(start = 200, endInclusive = 219),
                examineRange = IndexRange(start = 200, endInclusive = 219)
            ),
            BatchPlanItem(
                batchId = "b1",
                editableRange = IndexRange(start = 100, endInclusive = 119),
                examineRange = IndexRange(start = 100, endInclusive = 119)
            )
        )
        val traceSuspicious = listOf(
            SuspiciousBoundary(index = 5, reason = "gap"),
            SuspiciousBoundary(index = 1, reason = "gap")
        )
        val patch = TingwuPreprocessPatchBuilder.build(
            sessionId = "s-1",
            jobId = "job-1",
            transcriptMarkdown = transcriptMarkdown,
            createdAt = 1_000L,
            traceBatchPlan = traceBatchPlan,
            traceSuspiciousBoundaries = traceSuspicious
        )

        metaHub.appendM2Patch(sessionId = "s-1", patch = patch)

        val preprocess = assertNotNull(metaHub.getEffectiveM2("s-1")?.preprocess)
        assertEquals(listOf("b1", "b2"), preprocess.batchPlan.map { it.batchId })
        assertEquals(listOf(100, 200), preprocess.batchPlan.map { it.editableRange.start })
        assertEquals(listOf(1, 5), preprocess.suspiciousBoundaries.map { it.index })
    }
}
