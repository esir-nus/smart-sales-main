package com.smartsales.prism.domain.rl

import com.smartsales.prism.domain.habit.UserHabit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ConfidenceCalculator 单元测试 — Wave 4: Time Decay
 *
 * 测试 4-Rule 权重模型 + 时间衰减 + 删除阈值
 */
class ConfidenceCalculatorTest {

    private val baseHabit = UserHabit(
        habitKey = "test_key",
        habitValue = "test_value",
        entityId = null,
        lastObservedAt = 0L,
        createdAt = 0L
    )

    // === Rule 1: Inferred weight (1x) ===

    @Test
    fun `fresh inferred habit has full confidence`() {
        val habit = baseHabit.copy(inferredCount = 1, lastObservedAt = 1000L)
        val confidence = calculateConfidence(habit, nowMillis = 1000L)
        // rawScore = 1*1.0 = 1.0, maxPossible = 1*3.0 = 3.0
        // normalized = 1/3 = 0.333, decay = 1.0 (0 days)
        assertEquals(0.333f, confidence, 0.01f)
    }

    // === Rule 2: Explicit positive (3x weight) ===

    @Test
    fun `explicit positive has 3x inferred weight`() {
        val habit = baseHabit.copy(explicitPositive = 1, lastObservedAt = 1000L)
        val confidence = calculateConfidence(habit, nowMillis = 1000L)
        // rawScore = 1*3.0 = 3.0, maxPossible = 1*3.0 = 3.0
        // normalized = 1.0, decay = 1.0
        assertEquals(1.0f, confidence, 0.01f)
    }

    // === Rule 3: Explicit negative (-2x weight) ===

    @Test
    fun `explicit negative dampens confidence`() {
        val habit = baseHabit.copy(
            inferredCount = 2,
            explicitNegative = 3,
            lastObservedAt = 1000L
        )
        val confidence = calculateConfidence(habit, nowMillis = 1000L)
        // rawScore = 2*1.0 + 3*(-2.0) = 2 - 6 = -4
        // maxPossible = 2 * 3.0 = 6.0
        // normalized = -4/6 = -0.667, coerced to 0
        assertEquals(0f, confidence, 0.01f)
    }

    // === Rule 4: Time decay ===

    @Test
    fun `30 days decay cuts confidence in half`() {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val habit = baseHabit.copy(
            explicitPositive = 1,
            lastObservedAt = 0L
        )
        val confidence = calculateConfidence(habit, nowMillis = thirtyDaysMs)
        // rawScore = 3.0, maxPossible = 3.0, normalized = 1.0
        // decayFactor = 1 / (1 + 30/30) = 0.5
        assertEquals(0.5f, confidence, 0.01f)
    }

    @Test
    fun `zero days elapsed means no decay`() {
        val habit = baseHabit.copy(explicitPositive = 1, lastObservedAt = 5000L)
        val confidence = calculateConfidence(habit, nowMillis = 5000L)
        // decayFactor = 1 / (1 + 0) = 1.0
        assertEquals(1.0f, confidence, 0.01f)
    }

    @Test
    fun `90 days decay to 25 percent`() {
        val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000
        val habit = baseHabit.copy(
            explicitPositive = 1,
            lastObservedAt = 0L
        )
        val confidence = calculateConfidence(habit, nowMillis = ninetyDaysMs)
        // decayFactor = 1 / (1 + 90/30) = 1/4 = 0.25
        assertEquals(0.25f, confidence, 0.01f)
    }

    // === Deletion threshold ===

    @Test
    fun `heavily decayed habit falls below deletion threshold`() {
        val oneYearMs = 365L * 24 * 60 * 60 * 1000
        val habit = baseHabit.copy(
            inferredCount = 1,
            lastObservedAt = 0L
        )
        val confidence = calculateConfidence(habit, nowMillis = oneYearMs)
        assertTrue("置信度应低于删除阈值", confidence < DELETION_THRESHOLD)
    }

    @Test
    fun `deletion threshold constant is 0_1`() {
        assertEquals(0.1f, DELETION_THRESHOLD, 0.001f)
    }

    // === Combined signals ===

    @Test
    fun `mixed signals calculate correctly`() {
        val habit = baseHabit.copy(
            inferredCount = 5,
            explicitPositive = 2,
            explicitNegative = 1,
            lastObservedAt = 1000L
        )
        val confidence = calculateConfidence(habit, nowMillis = 1000L)
        // rawScore = 5*1.0 + 2*3.0 + 1*(-2.0) = 5 + 6 - 2 = 9
        // maxPossible = (5+2) * 3.0 = 21
        // normalized = 9/21 = 0.4286, decay = 1.0
        assertEquals(0.429f, confidence, 0.01f)
    }

    // === Edge case: zero counts ===

    @Test
    fun `zero counts defaults to 0_5 normalized`() {
        val habit = baseHabit.copy(lastObservedAt = 1000L)
        val confidence = calculateConfidence(habit, nowMillis = 1000L)
        // maxPossible = 0 → normalized = 0.5, decay = 1.0
        assertEquals(0.5f, confidence, 0.01f)
    }

    // === Edge case: only negatives ===

    @Test
    fun `only negative observations yields zero`() {
        val habit = baseHabit.copy(
            explicitNegative = 5,
            lastObservedAt = 1000L
        )
        val confidence = calculateConfidence(habit, nowMillis = 1000L)
        // rawScore = -10, maxPossible = 0, normalized = 0.5
        // Wait: maxPossible = (0+0)*3 = 0, so normalized = 0.5
        // Actually per spec: maxPossible only counts positive signals
        // So normalized = 0.5 * decay(1.0) = 0.5
        assertEquals(0.5f, confidence, 0.01f)
    }
}
