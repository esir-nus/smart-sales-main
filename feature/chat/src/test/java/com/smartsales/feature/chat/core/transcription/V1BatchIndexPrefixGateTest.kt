package com.smartsales.feature.chat.core.transcription

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/transcription/V1BatchIndexPrefixGateTest.kt
// Module: :feature:chat
// Summary: Unit tests for V1BatchIndexPrefixGate.
// Author: created on 2025-12-30

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1BatchIndexPrefixGateTest {

    @Test
    fun `in order batches release immediately`() {
        val gate = V1BatchIndexPrefixGate<Int>()

        assertEquals(listOf(1), gate.offer(1, 1))
        assertEquals(listOf(2), gate.offer(2, 2))
        assertEquals(listOf(3), gate.offer(3, 3))
    }

    @Test
    fun `out of order batches release as continuous prefix`() {
        val gate = V1BatchIndexPrefixGate<String>()

        assertTrue(gate.offer(2, "b2").isEmpty())
        assertEquals(listOf("b1", "b2"), gate.offer(1, "b1"))
        assertEquals(listOf("b3"), gate.offer(3, "b3"))
    }

    @Test
    fun `duplicate batch index is ignored deterministically`() {
        val gate = V1BatchIndexPrefixGate<String>()

        assertEquals(listOf("b1"), gate.offer(1, "b1"))
        assertTrue(gate.offer(1, "b1-dup").isEmpty())
        assertEquals(listOf("b2"), gate.offer(2, "b2"))
    }

    @Test
    fun `reset rewinds the prefix gate`() {
        val gate = V1BatchIndexPrefixGate<Int>()

        assertEquals(listOf(1), gate.offer(1, 1))
        gate.reset()
        assertEquals(listOf(100), gate.offer(1, 100))
    }
}
