package com.smartsales.prism.ui.sim

enum class SimDrawerType {
    SCHEDULER,
    AUDIO
}

data class SimShellState(
    val activeDrawer: SimDrawerType? = null,
    val activeChatAudioId: String? = null,
    val showHistory: Boolean = false,
    val showConnectivity: Boolean = false,
    val showSettings: Boolean = false
)
