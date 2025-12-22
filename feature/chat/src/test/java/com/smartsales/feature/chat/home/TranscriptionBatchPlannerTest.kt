// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/TranscriptionBatchPlannerTest.kt
// 模块：:feature:chat
// 说明：验证伪流式批次切分规则的确定性
// 作者：创建于 2025-12-22
package com.smartsales.feature.chat.home

import com.smartsales.feature.media.audiofiles.TranscriptionBatchPlanner
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionBatchPlannerTest {
    @Test
    fun `plan splits lines deterministically`() {
        val markdown = "## 逐字稿\n- A\n- B\n- C"
        val plan = TranscriptionBatchPlanner.plan(markdown, batchSize = 2)
        assertEquals(2, plan.totalBatches)
        assertEquals(2, plan.batches[0].lineCount)
        assertEquals("## 逐字稿\n- A", plan.batches[0].markdownChunk)
        assertEquals(2, plan.batches[1].lineCount)
        assertEquals("- B\n- C", plan.batches[1].markdownChunk)

        val second = TranscriptionBatchPlanner.plan(markdown, batchSize = 2)
        assertEquals(plan.batches, second.batches)
    }
}
