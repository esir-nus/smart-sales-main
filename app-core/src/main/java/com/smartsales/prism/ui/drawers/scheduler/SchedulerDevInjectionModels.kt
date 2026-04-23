package com.smartsales.prism.ui.drawers.scheduler

enum class DevInjectSource {
    BROADCAST,
    DEV_PANEL
}

data class SchedulerDevInjectionRequest(
    val text: String,
    val displayedDateIso: String? = null,
    val scenarioId: String? = null,
    val source: DevInjectSource = DevInjectSource.BROADCAST
)
