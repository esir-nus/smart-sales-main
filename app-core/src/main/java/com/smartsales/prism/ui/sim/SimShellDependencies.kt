package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.IAgentViewModel
import com.smartsales.prism.ui.drawers.scheduler.ISchedulerViewModel

interface SimShellDependencies {
    val chatViewModel: IAgentViewModel
    val schedulerViewModel: ISchedulerViewModel
}

data class DefaultSimShellDependencies(
    override val chatViewModel: SimAgentViewModel,
    override val schedulerViewModel: SimSchedulerViewModel,
    val audioViewModel: SimAudioDrawerViewModel
) : SimShellDependencies
