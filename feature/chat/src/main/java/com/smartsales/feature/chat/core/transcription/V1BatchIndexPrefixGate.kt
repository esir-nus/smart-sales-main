package com.smartsales.feature.chat.core.transcription

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/transcription/V1BatchIndexPrefixGate.kt
// Module: :feature:chat
// Summary: Deterministic prefix gate for batchIndex-ordered publishing.
// Author: created on 2025-12-30

/**
 * Deterministic prefix gate for batchIndex-ordered publishing.
 *
 * - batchIndex is 1-based.
 * - Out-of-order inputs are buffered.
 * - Only continuous prefix startIndex..k is released.
 */
class V1BatchIndexPrefixGate<T>(
    startIndex: Int = 1
) {
    private val buffer = mutableMapOf<Int, T>()
    private var nextIndex = startIndex.coerceAtLeast(1)

    /**
     * Offer a batch item.
     *
     * @return list of newly releasable items in ascending batchIndex order.
     */
    fun offer(batchIndex: Int, value: T): List<T> {
        if (batchIndex < 1) {
            return emptyList()
        }
        if (batchIndex < nextIndex) {
            // 忽略重复或过期批次，保持确定性
            return emptyList()
        }
        if (batchIndex > nextIndex) {
            buffer.putIfAbsent(batchIndex, value)
            return emptyList()
        }
        val released = mutableListOf<T>()
        var currentIndex = batchIndex
        released += value
        currentIndex++
        while (true) {
            val next = buffer.remove(currentIndex) ?: break
            released += next
            currentIndex++
        }
        nextIndex = currentIndex
        return released
    }

    fun reset(startIndex: Int = 1) {
        buffer.clear()
        nextIndex = startIndex.coerceAtLeast(1)
    }
}
