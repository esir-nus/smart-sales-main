package com.smartsales.prism.data.real.plugins

import com.smartsales.core.context.ChatTurn
import com.smartsales.core.pipeline.CoreModulePermission
import com.smartsales.core.pipeline.PluginRequest
import com.smartsales.core.pipeline.RealToolRegistry
import com.smartsales.core.pipeline.RuntimePluginGateway
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class TestingEmailPluginTest {

    @Test
    fun `email plugin uses runtime history capability and yields contextual draft`() = runTest {
        val registry = RealToolRegistry(setOf(TestingEmailPlugin()))
        val contextBuilder = FakeContextBuilder().apply {
            loadSession(
                "session-1",
                listOf(
                    ChatTurn("user", "跟 Tom 跟进报价"),
                    ChatTurn("assistant", "好的，我来帮你整理")
                )
            )
        }
        val gateway = RuntimePluginGateway(
            toolId = "DRAFT_EMAIL",
            contextBuilder = contextBuilder,
            allowedPermissions = setOf(CoreModulePermission.READ_SESSION_HISTORY)
        )
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()

        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints.add(checkpoint)
        }

        try {
            val states = registry.executeTool(
                toolId = "DRAFT_EMAIL",
                request = PluginRequest(rawInput = "起草邮件", parameters = emptyMap()),
                gateway = gateway
            ).toList()

            assertTrue(states.any { it is UiState.ExecutingTool && it.toolName.contains("匹配邮件模板") })
            assertTrue(states.any { it is UiState.ExecutingTool && it.toolName.contains("最近会话") })
            val response = states.last() as UiState.Response
            assertTrue(response.content.contains("Tom"))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_CAPABILITY_CALL))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.PLUGIN_YIELDED_TO_OS))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }
}
