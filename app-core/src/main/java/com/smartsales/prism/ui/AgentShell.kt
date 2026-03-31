package com.smartsales.prism.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.toDayOffset
import com.smartsales.prism.ui.drawers.HistoryViewModel
import com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModel

/**
 * Prism Shell — The Core Container (Sleek Glass Version)
 *
 * 仅保留宿主职责：依赖获取、生命周期观察、顶层状态与回调装配。
 * @see prism-ui-ux-contract.md §1.1
 */
@Composable
fun AgentShell(
    onNavigateToSetup: () -> Unit = {}
) {
    var shellState by remember { mutableStateOf(AgentShellState()) }
    val agentViewModel: AgentViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val schedulerViewModel: SchedulerViewModel = hiltViewModel()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                shellState = handleAgentShellForegroundStart(shellState)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val groupedSessions by historyViewModel.groupedSessions.collectAsState()
    val mascotState by agentViewModel.mascotState.collectAsStateWithLifecycle()

    fun openSchedulerFromIsland(action: DynamicIslandTapAction) {
        when (action) {
            is DynamicIslandTapAction.OpenSchedulerDrawer -> {
                action.target
                    ?.toDayOffset()
                    ?.let(schedulerViewModel::onDateSelected)
            }
        }
        shellState = openAgentShellScheduler(shellState)
    }

    AgentShellContent(
        shellState = shellState,
        agentViewModel = agentViewModel,
        historyViewModel = historyViewModel,
        schedulerViewModel = schedulerViewModel,
        groupedSessions = groupedSessions,
        mascotState = mascotState,
        onNavigateToSetup = onNavigateToSetup,
        onSchedulerClick = ::openSchedulerFromIsland,
        mutateShellState = { transform ->
            shellState = transform(shellState)
        }
    )
}
