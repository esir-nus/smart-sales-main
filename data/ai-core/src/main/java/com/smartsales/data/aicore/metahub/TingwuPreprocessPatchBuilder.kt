package com.smartsales.data.aicore.metahub

import com.smartsales.core.metahub.BatchPlanItem
import com.smartsales.core.metahub.ConversationDerivedStateDelta
import com.smartsales.core.metahub.IndexRange
import com.smartsales.core.metahub.M2PatchRecord
import com.smartsales.core.metahub.PreprocessSnapshot
import com.smartsales.core.metahub.Provenance
import kotlin.math.min

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/TingwuPreprocessPatchBuilder.kt
// 模块：:data:ai-core
// 说明：Tingwu 预处理快照转为 M2 补丁（内部派生）
// 作者：创建于 2025-12-24

internal object TingwuPreprocessPatchBuilder {
    private const val DEFAULT_BATCH_SIZE = 20

    /**
     * 说明：基于转写 Markdown 构建 M2 预处理补丁，规则固定且可复现。
     */
    fun build(
        sessionId: String,
        jobId: String,
        transcriptMarkdown: String,
        createdAt: Long,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): M2PatchRecord {
        val normalizedLines = transcriptMarkdown
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val first20 = normalizedLines.take(MAX_PREVIEW_LINES)
        val batchPlan = buildFixedLineBatchPlan(
            totalLines = normalizedLines.size,
            batchSize = batchSize
        )
        val prov = Provenance(
            source = "tingwu.preprocess",
            updatedAt = createdAt
        )
        return M2PatchRecord(
            patchId = "m2_preprocess_${sessionId}_${jobId}_$createdAt",
            createdAt = createdAt,
            prov = prov,
            payload = ConversationDerivedStateDelta(
                preprocess = PreprocessSnapshot(
                    first20Rendered = first20,
                    suspiciousBoundaries = emptyList(),
                    batchPlan = batchPlan,
                    prov = prov
                )
            )
        )
    }

    /**
     * 说明：固定行数分批，确保批次范围与顺序稳定可复现。
     */
    private fun buildFixedLineBatchPlan(
        totalLines: Int,
        batchSize: Int
    ): List<BatchPlanItem> {
        if (totalLines <= 0 || batchSize <= 0) return emptyList()
        val totalBatches = (totalLines + batchSize - 1) / batchSize
        return (0 until totalBatches).map { index ->
            val start = index * batchSize
            val endInclusive = min(totalLines - 1, start + batchSize - 1)
            val range = IndexRange(start = start, endInclusive = endInclusive)
            BatchPlanItem(
                batchId = "b${index + 1}",
                editableRange = range,
                examineRange = range,
                tailLookaheadEnabled = false
            )
        }
    }

    private const val MAX_PREVIEW_LINES = 20
}
