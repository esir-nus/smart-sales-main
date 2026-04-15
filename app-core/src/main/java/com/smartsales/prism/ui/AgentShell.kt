// File: app-core/src/main/java/com/smartsales/prism/ui/AgentShell.kt
// Module: :app-core
// Summary: 轻量封装宿主，作为 AgentIntelligenceScreen 的壳层入口，隔离外部调用方与内部视图实现。
// Author: created on 2026-04-13

package com.smartsales.prism.ui

import androidx.compose.runtime.Composable
import com.smartsales.prism.ui.components.DynamicIslandTapAction

/**
 * AgentShell 是 AgentIntelligenceScreen 的包装宿主。
 * 仅负责将外部入口参数透传给内部实现，不包含独立业务逻辑。
 */
@Composable
fun AgentShell(
    viewModel: IAgentViewModel,
    onMenuClick: () -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onSchedulerClick: (DynamicIslandTapAction) -> Unit = {},
    onAudioBadgeClick: () -> Unit = {},
    onAudioDrawerClick: () -> Unit = {}
) {
    AgentIntelligenceScreen(
        viewModel = viewModel,
        onMenuClick = onMenuClick,
        onNewSessionClick = onNewSessionClick,
        onSchedulerClick = onSchedulerClick,
        onAudioBadgeClick = onAudioBadgeClick,
        onAudioDrawerClick = onAudioDrawerClick
    )
}
