// File: data/ai-core/src/test/java/com/smartsales/data/aicore/disector/FakeDisectorTest.kt
// Module: :data:ai-core
// Summary: Tests for FakeDisector behavior
// Author: created on 2026-01-14

package com.smartsales.data.aicore.disector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for FakeDisector.
 * 
 * Verifies that the Fake behaves correctly for testing scenarios.
 */
class FakeDisectorTest {
    
    private lateinit var fake: FakeDisector
    
    @Before
    fun setUp() {
        fake = FakeDisector()
    }
    
    @Test
    fun `default returns single batch plan`() {
        val plan = fake.createPlan(
            totalMs = 300_000L,
            audioAssetId = "test_audio",
            recordingSessionId = "test_session"
        )
        
        assertEquals(1, plan.batches.size)
        assertEquals(1, plan.batches[0].batchIndex)
        assertEquals(0L, plan.batches[0].absStartMs)
        assertEquals(300_000L, plan.batches[0].absEndMs)
    }
    
    @Test
    fun `stubPlan overrides default`() {
        val customPlan = DisectorPlan(
            disectorPlanId = "custom_plan",
            audioAssetId = "custom_asset",
            recordingSessionId = "custom_session",
            totalMs = 1_800_000L,
            batches = listOf(
                DisectorBatch(1, "b1", 0, 600_000, 0, 600_000),
                DisectorBatch(2, "b2", 600_000, 1_200_000, 590_000, 1_200_000),
                DisectorBatch(3, "b3", 1_200_000, 1_800_000, 1_190_000, 1_800_000)
            )
        )
        fake.stubPlan = customPlan
        
        val plan = fake.createPlan(999_999L, "ignored", "ignored")
        
        assertEquals(customPlan, plan)
        assertEquals(3, plan.batches.size)
    }
    
    @Test
    fun `tracks createPlan calls`() {
        fake.createPlan(100_000L, "asset1", "session1")
        fake.createPlan(200_000L, "asset2", "session2")
        
        assertEquals(2, fake.createPlanCalls.size)
        assertEquals("asset1", fake.createPlanCalls[0].audioAssetId)
        assertEquals(200_000L, fake.createPlanCalls[1].totalMs)
    }
    
    @Test
    fun `reset clears state`() {
        fake.stubPlan = DisectorPlan("p", "a", "s", 0, emptyList())
        fake.createPlan(100_000L, "x", "y")
        
        fake.reset()
        
        assertNull(fake.stubPlan)
        assertTrue(fake.createPlanCalls.isEmpty())
    }
}
