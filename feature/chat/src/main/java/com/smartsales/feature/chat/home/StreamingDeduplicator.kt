package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/StreamingDeduplicator.kt
// 模块：:feature:chat
// 说明：流式增量去重器，兼容“增量 token”与“累计快照”两种返回，避免助手回复重复
// 作者：创建于 2025-12-10

/**
 * 将上游流式 chunk 合并为当前快照：
 * - 如果上游 chunk 是“累计快照”，取最长公共前缀后追加后缀。
 * - 如果是“增量 token”，直接追加。
 * - 遇到回退/重置时，按 newContent 覆盖。
 */
internal class StreamingDeduplicator {
    private var lastSnapshot: String = ""

    /** 合并新 chunk，返回最新完整文本。 */
    fun mergeSnapshot(current: String, incoming: String): String {
        val base = if (lastSnapshot.isNotEmpty()) lastSnapshot else current
        if (incoming.isEmpty()) return base

        val (delta, isReset) = extractDelta(incoming, base)
        val nextSnapshot = when {
            // 上游输出是累计快照：直接采用快照
            incoming.startsWith(base) -> incoming
            // 上游回退：保持当前内容
            base.startsWith(incoming) -> base
            // 明确重置：直接采用新文本
            isReset -> incoming
            // 常规增量或无法匹配：按基线追加 delta
            delta.isNotEmpty() -> base + delta
            else -> base
        }
        lastSnapshot = nextSnapshot
        return nextSnapshot
    }

    /**
     * 提取新增内容：
     * - 优先使用"新文本前缀 == 旧文本"判断；
     * - 否则按最长公共前缀切分，过短则视为重置直接返回新文本。
     */
    internal fun extractDelta(newContent: String, lastContent: String): Delta {
        if (lastContent.isEmpty()) return Delta(newContent, isReset = false)

        // 1) Very small chunks after we already have content:视为增量 token，直接追加
        if (newContent.length <= MAX_TOKEN_CHUNK) {
            return Delta(newContent, isReset = false)
        }

        // 2) 累计快照：新文本以前缀包含旧文本
        if (newContent.startsWith(lastContent)) {
            return Delta(newContent.substring(lastContent.length), isReset = false)
        }

        // 3) 较长公共前缀：认为是"修订版"，只追加后缀
        val lcp = longestCommonPrefix(newContent, lastContent)
        if (lcp >= MIN_PREFIX_LENGTH) {
            return Delta(newContent.substring(lcp), isReset = false)
        }

        // 4) 否则视为重置
        return Delta(newContent, isReset = true)
    }

    private fun longestCommonPrefix(a: String, b: String): Int {
        val max = minOf(a.length, b.length)
        var i = 0
        while (i < max && a[i] == b[i]) {
            i++
        }
        return i
    }

    companion object {
        private const val MIN_PREFIX_LENGTH = 4
        // "很短"的 chunk，当上游已输出内容时视为纯增量 token
        private const val MAX_TOKEN_CHUNK = 3
    }

    internal data class Delta(
        val text: String,
        val isReset: Boolean
    )
}
