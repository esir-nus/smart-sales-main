package com.smartsales.feature.chat.home

// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/home/StreamingDeduplicatorTest.kt
// 模块：:feature:chat
// 说明：验证流式增量去重器在累计快照/增量 token/重置场景下的合并效果
// 作者：创建于 2025-12-10

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingDeduplicatorTest {

    @Test
    fun `growing snapshots append only new suffix`() {
        val dedup = StreamingDeduplicator()
        var current = dedup.mergeSnapshot("", "<visible2user>罗总关注价格</visible2user>")
        assertEquals("罗总关注价格", current)

        current = dedup.mergeSnapshot(current, "<visible2user>罗总关注价格，预算有限</visible2user>")
        assertEquals("罗总关注价格，预算有限", current)

        current = dedup.mergeSnapshot(current, "<visible2user>罗总关注价格，预算有限，需要明确交付</visible2user>")
        assertEquals("罗总关注价格，预算有限，需要明确交付", current)
    }

    @Test
    fun `resets replace previous snapshot when overlap is tiny`() {
        val dedup = StreamingDeduplicator()
        var current = dedup.mergeSnapshot("", "<visible2user>hello world</visible2user>")
        assertEquals("hello world", current)

        current = dedup.mergeSnapshot(current, "<visible2user>新一轮回答开始了</visible2user>")
        assertEquals("新一轮回答开始了", current)
    }

    @Test
    fun `pure delta tokens still append sequentially`() {
        val dedup = StreamingDeduplicator()
        var current = dedup.mergeSnapshot("", "<visible2user>客")
        current = dedup.mergeSnapshot(current, "<visible2user>客户")
        current = dedup.mergeSnapshot(current, "<visible2user>客户关心")

        assertEquals("客户关心", current)
    }
}
