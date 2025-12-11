package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/GeneralOutputGuards.kt
// 模块：:feature:chat
// 说明：GENERAL 输出轻量守护：去除过度重复句子并限制长度，避免用户看到刷屏回复
// 作者：创建于 2025-12-10

import java.util.Locale

/** GENERAL 最终输出守护：句子去重（同句最多 2 次）并限制总长度。 */
internal fun applyGeneralOutputGuards(
    text: String,
    maxLength: Int = 240,
    maxRepeatPerSentence: Int = 2
): String {
    if (text.isBlank()) return text
    val sentences = splitSentences(text)
    val seenCount = mutableMapOf<String, Int>()
    val builder = StringBuilder()
    for (sentence in sentences) {
        val normalized = normalizeSentenceForGuard(sentence)
        if (normalized.isEmpty()) continue
        val count = seenCount.getOrDefault(normalized, 0)
        if (count >= maxRepeatPerSentence) continue
        val nextLength = builder.length + sentence.length
        if (nextLength > maxLength) break
        builder.append(sentence)
        seenCount[normalized] = count + 1
    }
    return builder.toString().ifBlank { text.trim() }
}

private fun splitSentences(text: String): List<String> {
    val result = mutableListOf<String>()
    val buffer = StringBuilder()
    text.forEach { ch ->
        buffer.append(ch)
        if (ch == '。' || ch == '！' || ch == '？' || ch == '!' || ch == '?') {
            result.add(buffer.toString())
            buffer.clear()
        }
    }
    if (buffer.isNotEmpty()) {
        result.add(buffer.toString())
    }
    return result
}

internal fun normalizeSentenceForGuard(sentence: String): String {
    return sentence.lowercase(Locale.getDefault())
        .replace(Regex("\\s+"), "")
        // 去掉英文和常见中文标点，让"预算有限，需给到折扣"和"预算有限需给到折扣"归一
        .replace(Regex("[\\p{Punct}，。！？；：、]"), "")
        .trim()
}
