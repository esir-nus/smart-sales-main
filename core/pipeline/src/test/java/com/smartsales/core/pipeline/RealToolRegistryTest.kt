package com.smartsales.core.pipeline

import com.smartsales.core.context.ChatTurn
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealToolRegistryTest {

    @Test
    fun `executeTool with runtime gateway emits success and capability telemetry`() = runTest {
        val registry = RealToolRegistry(setOf(EchoPlugin()))
        val contextBuilder = FakeContextBuilder().apply {
            loadSession(
                "session-1",
                listOf(
                    ChatTurn("user", "Need context for echo"),
                    ChatTurn("assistant", "Acknowledged")
                )
            )
        }
        val gateway = RuntimePluginGateway(
            toolId = "ECHO_TEST",
            contextBuilder = contextBuilder,
            allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
        )
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()

        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints.add(checkpoint)
        }

        try {
            val states = registry.executeTool(
                toolId = "ECHO_TEST",
                request = PluginRequest(
                    rawInput = "Run echo test",
                    parameters = mapOf("message" to "Hello Gateway!")
                ),
                gateway = gateway
            ).toList()

            assertEquals(3, states.size)
            assertTrue(states.first() is UiState.ExecutingTool || states.first() is UiState.Thinking)
            assertTrue(states.last() is UiState.Response)
            val response = states.last() as UiState.Response
            assertTrue(response.content.contains("Hello Gateway!"))
            assertTrue(response.content.contains("Need context for echo"))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_DISPATCH_RECEIVED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_CAPABILITY_CALL))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `executeTool with missing runtime permission fails safely`() = runTest {
        val registry = RealToolRegistry(setOf(EchoPlugin()))
        val gateway = RuntimePluginGateway(
            toolId = "ECHO_TEST",
            contextBuilder = FakeContextBuilder(),
            allowedPermissions = emptySet()
        )
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()

        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints.add(checkpoint)
        }

        try {
            val states = registry.executeTool(
                toolId = "ECHO_TEST",
                request = PluginRequest(
                    rawInput = "Run echo test",
                    parameters = mapOf("message" to "Denied")
                ),
                gateway = gateway
            ).toList()

            assertEquals(1, states.size)
            assertTrue(states.single() is UiState.Error)
            val error = states.single() as UiState.Error
            assertTrue(error.message.contains("READ_SESSION_HISTORY"))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_DISPATCH_RECEIVED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `executeTool with unknown tool yields safe rejection telemetry`() = runTest {
        val registry = RealToolRegistry(emptySet())
        val gateway = RuntimePluginGateway(
            toolId = "UNKNOWN_TOOL",
            contextBuilder = FakeContextBuilder(),
            allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
        )
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()

        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints.add(checkpoint)
        }

        try {
            val states = registry.executeTool(
                toolId = "UNKNOWN_TOOL",
                request = PluginRequest(
                    rawInput = "Run unknown tool",
                    parameters = emptyMap()
                ),
                gateway = gateway
            ).toList()

            assertEquals(1, states.size)
            assertTrue(states.single() is UiState.Error)
            val error = states.single() as UiState.Error
            assertTrue(error.message.contains("Unknown tool ID"))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_DISPATCH_RECEIVED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `executeTool resolves legacy alias to semantic plugin id`() = runTest {
        val aliasPlugin = object : PrismPlugin {
            override val metadata = AnalystTool(
                id = PluginToolIds.ARTIFACT_GENERATE,
                label = "Artifact Generate",
                description = "Alias resolution test plugin",
                icon = "pdf"
            )
            override val requiredPermissions = emptySet<CoreModulePermission>()

            override fun execute(request: PluginRequest, gateway: PluginGateway): Flow<UiState> {
                return flowOf(UiState.Response("alias-ok:${request.parameters["accountName"]}"))
            }
        }
        val registry = RealToolRegistry(setOf(aliasPlugin))
        val gateway = RuntimePluginGateway(
            toolId = "GENERATE_PDF",
            contextBuilder = FakeContextBuilder(),
            allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
        )

        val states = registry.executeTool(
            toolId = "GENERATE_PDF",
            request = PluginRequest(
                rawInput = "帮我生成字节跳动的汇报",
                parameters = mapOf("accountName" to "字节跳动")
            ),
            gateway = gateway
        ).toList()

        val response = states.last() as UiState.Response
        assertEquals("alias-ok:字节跳动", response.content)
    }
}
