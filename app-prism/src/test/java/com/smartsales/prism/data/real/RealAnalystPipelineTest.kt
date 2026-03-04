package com.smartsales.prism.data.real

import com.smartsales.prism.domain.analyst.AnalystResponse
import com.smartsales.prism.domain.analyst.AnalystState
import com.smartsales.prism.domain.analyst.ArchitectService
import com.smartsales.prism.domain.analyst.InvestigationResult
import com.smartsales.prism.domain.analyst.PlanResult
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.TokenUsage
import com.smartsales.prism.domain.analyst.LightningRouter
import com.smartsales.prism.domain.analyst.RouterResult
import com.smartsales.prism.domain.analyst.EntityResolverService
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.pipeline.KernelWriteBack
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RealAnalystPipelineTest {

    private lateinit var contextBuilder: ContextBuilder
    private lateinit var executor: Executor
    private lateinit var architectService: ArchitectService
    private lateinit var consultantService: LightningRouter
    private lateinit var entityRepository: EntityRepository
    private lateinit var entityResolverService: EntityResolverService
    private lateinit var kernelWriteBack: KernelWriteBack
    private lateinit var entityDisambiguationService: com.smartsales.prism.domain.disambiguation.EntityDisambiguationService
    private lateinit var pipeline: RealAnalystPipeline

    @Before
    fun setup() {
        contextBuilder = mock()
        executor = mock()
        architectService = mock()
        consultantService = mock()
        entityRepository = mock()
        entityResolverService = mock()
        kernelWriteBack = mock()
        entityDisambiguationService = mock()

        // Default to pass-through so standard pipeline tests work
        kotlinx.coroutines.runBlocking {
            whenever(entityDisambiguationService.process(any())).thenReturn(
                com.smartsales.prism.domain.disambiguation.DisambiguationResult.PassThrough
            )
        }
        
        val toolRegistry: com.smartsales.prism.domain.analyst.ToolRegistry = mock()
        kotlinx.coroutines.runBlocking {
            whenever(toolRegistry.getAllTools()).thenReturn(emptyList())
        }
        pipeline = RealAnalystPipeline(
            contextBuilder = contextBuilder,
            executor = executor,
            architectService = architectService,
            entityDisambiguationService = entityDisambiguationService,
            toolRegistry = toolRegistry
        )
    }
    @Ignore("Pending LightningRouter integration")
    @Test
    fun `L2 Scenario 1 - Consultant returns null - returns Chat and stays IDLE`() = runTest {
        whenever(contextBuilder.build(any(), any(), any(), any())).thenReturn(mock())
        whenever(consultantService.evaluateIntent(any())).thenReturn(null)

        val response = pipeline.handleInput("分析", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("我没完全明白，能再详细说说你想分析的内容吗？", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }
    @Ignore("Pending LightningRouter integration")
    @Test
    fun `L2 Scenario 2 - info_sufficient is false - returns Chat and stays IDLE`() = runTest {
        whenever(contextBuilder.build(any(), any(), any(), any())).thenReturn(mock())
        val result = RouterResult(queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.CRM_TASK, infoSufficient = false, response = "请问您想分析哪个客户？", missingEntities = emptyList())
        whenever(consultantService.evaluateIntent(any())).thenReturn(result)

        val response = pipeline.handleInput("分析", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("请问您想分析哪个客户？", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }
    @Ignore("Pending LightningRouter integration")
    @Test
    fun `L2 Scenario 2_1 - queryQuality is NOISE - returns Chat and stays IDLE`() = runTest {
        whenever(contextBuilder.build(any(), any(), any(), any())).thenReturn(mock())
        val result = RouterResult(queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.NOISE, infoSufficient = false, response = "好的", missingEntities = emptyList())
        whenever(consultantService.evaluateIntent(any())).thenReturn(result)

        val response = pipeline.handleInput("我知道了", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("好的", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }
    @Ignore("Pending LightningRouter integration")
    @Test
    fun `L2 Scenario 2_2 - queryQuality is VAGUE - returns Chat and stays IDLE`() = runTest {
        whenever(contextBuilder.build(any(), any(), any(), any())).thenReturn(mock())
        val result = RouterResult(queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.VAGUE, infoSufficient = false, response = "您是指什么？", missingEntities = emptyList())
        whenever(consultantService.evaluateIntent(any())).thenReturn(result)

        val response = pipeline.handleInput("那个事", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("您是指什么？", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }
    @Ignore("Pending LightningRouter integration")
    @Test
    fun `L2 Scenario 2_3 - queryQuality is SIMPLE_QA - returns Chat and stays IDLE (Fast Track Bypass)`() = runTest {
        whenever(contextBuilder.build(any(), any(), any(), any())).thenReturn(mock())
        // Even if infoSufficient is true or false, SIMPLE_QA should always short-circuit
        val result = RouterResult(queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.SIMPLE_QA, infoSufficient = true, response = "公司有50个人", missingEntities = emptyList())
        whenever(consultantService.evaluateIntent(any())).thenReturn(result)

        val response = pipeline.handleInput("公司有几个人？", emptyList())

        assertTrue("Expected Chat but got ${response::class.simpleName}", response is AnalystResponse.Chat)
        assertEquals("公司有50个人", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 3 - info_sufficient is true - returns Plan and transitions to PROPOSAL`() = runTest {
        whenever(contextBuilder.build(any(), any(), any(), any())).thenReturn(mock())
        val result = RouterResult(queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.DEEP_ANALYSIS, infoSufficient = true, response = "准备分析", missingEntities = emptyList())
        whenever(consultantService.evaluateIntent(any())).thenReturn(result)
        
        val dummyPlan = PlanResult.Strategy("Test Plan", "Test Summary", "")
        whenever(architectService.generatePlan(any(), any(), any(), any())).thenReturn(dummyPlan)

        val response = pipeline.handleInput("分析大客户数据", emptyList())

        assertTrue(response is AnalystResponse.Plan)
        assertEquals("Test Plan", (response as AnalystResponse.Plan).title)
        assertEquals("Test Summary", (response as AnalystResponse.Plan).summary)
        assertEquals(AnalystState.PROPOSAL, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 5 - In PROPOSAL state - returns Analysis and transitions to RESULT`() = runTest {
        // Setup to reach PROPOSAL state
        whenever(contextBuilder.build(any(), any(), any(), any())).thenReturn(mock())
        val result = RouterResult(queryQuality = com.smartsales.prism.domain.analyst.QueryQuality.DEEP_ANALYSIS, infoSufficient = true, response = "准备分析", missingEntities = emptyList())
        whenever(consultantService.evaluateIntent(any())).thenReturn(result)
        
        val dummyPlan = PlanResult.Strategy("Test Plan", "Test Summary", "")
        whenever(architectService.generatePlan(any(), any(), any(), any())).thenReturn(dummyPlan)
        
        pipeline.handleInput("分析大客户数据", emptyList())
        assertEquals(AnalystState.PROPOSAL, pipeline.state.value)

        val dummyInvestigation = InvestigationResult("Test Report", emptyList())
        whenever(architectService.investigate(any(), any(), any())).thenReturn(dummyInvestigation)

        // Now test handling input in PROPOSAL state
        val response = pipeline.handleInput("ok", emptyList())

        assertTrue("Expected Analysis but got ${response::class.simpleName}", response is AnalystResponse.Analysis)
        assertEquals("Test Report", (response as AnalystResponse.Analysis).content)
        assertEquals(AnalystState.RESULT, pipeline.state.value)
    }
}
