package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.analyst.AnalystResponse
import com.smartsales.prism.domain.analyst.AnalystState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAnalystPipelineTest {

    private lateinit var pipeline: FakeAnalystPipeline

    @Before
    fun setup() {
        pipeline = FakeAnalystPipeline()
    }

    @Test
    fun `L2 Scenario 1 - Normal Chat - returns Chat response and stays IDLE`() = runTest {
        // Given
        assertEquals(AnalystState.IDLE, pipeline.state.value)

        // When
        val response = pipeline.handleInput("Hello, who are you?")

        // Then
        assertTrue(response is AnalystResponse.Chat)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 2 - Plan Trigger - returns Plan response and transitions to PROPOSAL`() = runTest {
        // Given
        assertEquals(AnalystState.IDLE, pipeline.state.value)

        // When (Trigger plan generation)
        val response = pipeline.handleInput("analyze this company")

        // Then
        assertTrue(response is AnalystResponse.Plan)
        assertEquals(AnalystState.PROPOSAL, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 3 - Execution Confirmation - returns Analysis and resets to IDLE`() = runTest {
        // Given (in PROPOSAL state)
        pipeline.handleInput("analyze this company")
        assertEquals(AnalystState.PROPOSAL, pipeline.state.value)

        // When (Confirm execution)
        val response = pipeline.handleInput("ok, proceed")

        // Then
        assertTrue(response is AnalystResponse.Analysis)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }
    
    @Test
    fun `L2 Scenario 4 - Execution Cancellation - returns Chat and resets to IDLE`() = runTest {
        // Given (in PROPOSAL state)
        pipeline.handleInput("analyze this company")
        assertEquals(AnalystState.PROPOSAL, pipeline.state.value)

        // When (Cancel execution)
        val response = pipeline.handleInput("cancel")

        // Then
        assertTrue(response is AnalystResponse.Chat)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }
}
