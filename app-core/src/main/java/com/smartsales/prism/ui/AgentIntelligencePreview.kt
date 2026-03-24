package com.smartsales.prism.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.smartsales.prism.ui.fakes.FakeAgentViewModel

@Preview(showBackground = true, name = "1. Idle (Hero)")
@Composable
fun PreviewAgentIntelligence_Idle() {
    AgentIntelligenceScreen(viewModel = FakeAgentViewModel().apply { debugRunScenario("IDLE") })
}

@Preview(showBackground = true, name = "2. Thinking State")
@Composable
fun PreviewAgentIntelligence_Thinking() {
    AgentIntelligenceScreen(
        viewModel = FakeAgentViewModel().apply {
            debugRunScenario("THINKING")
            updateInput("安排明天会议")
            send()
        }
    )
}

@Preview(showBackground = true, name = "3. Awaiting Clarification")
@Composable
fun PreviewAgentIntelligence_Clarification() {
    AgentIntelligenceScreen(
        viewModel = FakeAgentViewModel().apply {
            debugRunScenario("WAITING_CLARIFICATION")
            updateInput("开会")
            send()
        }
    )
}

@Preview(showBackground = true, name = "4. Streaming State")
@Composable
fun PreviewAgentIntelligence_Streaming() {
    AgentIntelligenceScreen(
        viewModel = FakeAgentViewModel().apply {
            debugRunScenario("STREAMING")
            updateInput("写一份总结")
            send()
        }
    )
}

@Preview(showBackground = true, name = "5. Scheduler Task Created")
@Composable
fun PreviewAgentIntelligence_Scheduler() {
    AgentIntelligenceScreen(
        viewModel = FakeAgentViewModel().apply {
            debugRunScenario("SCHEDULER_TASK")
            updateInput("安排与张总技术会议")
            send()
        }
    )
}
