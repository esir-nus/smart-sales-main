package com.smartsales.prism.ui.sim

enum class SimDrawerType {
    SCHEDULER,
    AUDIO
}

enum class SimAudioDrawerMode {
    BROWSE,
    CHAT_RESELECT
}

enum class SimConnectivitySurface {
    MODAL,
    SETUP,
    MANAGER
}

data class SimShellState(
    val activeDrawer: SimDrawerType? = null,
    val audioDrawerMode: SimAudioDrawerMode = SimAudioDrawerMode.BROWSE,
    val activeConnectivitySurface: SimConnectivitySurface? = null,
    val showHistory: Boolean = false,
    val showSettings: Boolean = false,
    val isForcedFirstLaunchOnboarding: Boolean = false
)
