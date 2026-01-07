package com.smartsales.domain.transcription

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/transcription/V1TingwuMacroWindowFilterTest.kt
// Module: :feature:chat
// Summary: Unit tests for V1TingwuMacroWindowFilter.
// Author: created on 2025-12-30

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1TingwuMacroWindowFilterTest {

    @Test
    fun `anchor adds captureStartMs offset`() {
        val segments = listOf(RelativeSegment(startMs = 0, endMs = 500, text = "a"))
        val anchored = V1TingwuMacroWindowFilter.anchorToAbs(1000, segments)

        assertEquals(listOf(AbsSegment(absStartMs = 1000, absEndMs = 1500, text = "a")), anchored)
    }

    @Test
    fun `half open boundaries exclude exact edges`() {
        val absSegments = listOf(
            AbsSegment(absStartMs = 900, absEndMs = 1000, text = "left"),
            AbsSegment(absStartMs = 2000, absEndMs = 2100, text = "right")
        )

        val filtered = V1TingwuMacroWindowFilter.filterToMacroWindow(
            absStartMs = 1000,
            absEndMs = 2000,
            absSegments = absSegments
        )

        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `partial overlap is included`() {
        val absSegments = listOf(
            AbsSegment(absStartMs = 1500, absEndMs = 2500, text = "overlap")
        )

        val filtered = V1TingwuMacroWindowFilter.filterToMacroWindow(
            absStartMs = 1000,
            absEndMs = 2000,
            absSegments = absSegments
        )

        assertEquals(absSegments, filtered)
    }

    @Test
    fun `invalid segment end before start is dropped`() {
        val segments = listOf(RelativeSegment(startMs = 1000, endMs = 500, text = "bad"))

        val anchored = V1TingwuMacroWindowFilter.anchorToAbs(0, segments)

        assertTrue(anchored.isEmpty())
    }

    @Test
    fun `invalid macro window returns empty`() {
        val absSegments = listOf(AbsSegment(absStartMs = 0, absEndMs = 10, text = "x"))

        val filtered = V1TingwuMacroWindowFilter.filterToMacroWindow(
            absStartMs = 1000,
            absEndMs = 1000,
            absSegments = absSegments
        )

        assertTrue(filtered.isEmpty())
    }
}
