package com.smartsales.prism.ui.sim

enum class RuntimeDrawerType {
    SCHEDULER,
    AUDIO
}

enum class RuntimeAudioDrawerMode {
    BROWSE,
    CHAT_RESELECT
}

enum class RuntimeConnectivitySurface {
    MODAL,
    SETUP,
    MANAGER,
    ADD_DEVICE
}

data class RuntimeShellState(
    val activeDrawer: RuntimeDrawerType? = null,
    val audioDrawerMode: RuntimeAudioDrawerMode = RuntimeAudioDrawerMode.BROWSE,
    val activeConnectivitySurface: RuntimeConnectivitySurface? = null,
    val showHistory: Boolean = false,
    val showSettings: Boolean = false,
    val showSchedulerIslandHint: Boolean = true,
    val isForcedFirstLaunchOnboarding: Boolean = false
)
