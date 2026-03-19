package com.smartsales.prism.data.real.plugins

import com.smartsales.core.context.ChatTurn
import com.smartsales.core.pipeline.CoreModulePermission
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.PluginToolIds
import com.smartsales.core.pipeline.RealToolRegistry
import com.smartsales.core.pipeline.RuntimePluginGateway
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtifactGeneratePluginTest {

    @Test
    fun `artifact plugin uses runtime history capability and yields report draft`() = runTest {
        val registry = RealToolRegistry(setOf(ArtifactGeneratePlugin()))
        val contextBuilder = FakeContextBuilder().apply {
            loadSession(
                "session-1",
                listOf(
                    ChatTurn("user", "帮我生成字节跳动的汇报"),
                    ChatTurn("assistant", "好的")
                )
            )
        }
        val gateway = RuntimePluginGateway(
            toolId = PluginToolIds.ARTIFACT_GENERATE,
            contextBuilder = contextBuilder,
            allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
        )
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()

        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints.add(checkpoint)
        }

        try {
            val states = registry.executeTool(
                toolId = PluginToolIds.ARTIFACT_GENERATE,
                request = PluginRequest(
                    rawInput = "帮我生成字节跳动的汇报",
                    parameters = mapOf(
                        "ruleId" to "executive_report",
                        "accountName" to "字节跳动"
                    )
                ),
                gateway = gateway
            ).toList()

            assertTrue(states.any { it is UiState.ExecutingTool && it.toolName.contains("报告上下文") })
            val response = states.last() as UiState.Response
            assertTrue(response.content.contains("字节跳动"))
            assertTrue(response.content.contains("executive_report"))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_CAPABILITY_CALL))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }
}
