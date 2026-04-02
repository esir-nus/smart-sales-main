package com.smartsales.prism.ui.shell

enum class BaseRuntimeDrawerType {
    HISTORY,
    SCHEDULER,
    AUDIO
}

enum class BaseRuntimeAudioDrawerMode {
    BROWSE,
    CHAT_RESELECT
}

enum class BaseRuntimeConnectivitySurface {
    MODAL,
    SETUP,
    MANAGER
}

data class BaseRuntimeShellCoreState(
    val activeDrawer: BaseRuntimeDrawerType? = null,
    val audioDrawerMode: BaseRuntimeAudioDrawerMode = BaseRuntimeAudioDrawerMode.BROWSE,
    val activeConnectivitySurface: BaseRuntimeConnectivitySurface? = null,
    val showSettings: Boolean = false
)

fun openBaseRuntimeScheduler(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = BaseRuntimeDrawerType.SCHEDULER,
    activeConnectivitySurface = null,
    showSettings = false
)

fun openBaseRuntimeHistory(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = BaseRuntimeDrawerType.HISTORY,
    audioDrawerMode = BaseRuntimeAudioDrawerMode.BROWSE,
    activeConnectivitySurface = null,
    showSettings = false
)

fun openBaseRuntimeAudioDrawer(
    state: BaseRuntimeShellCoreState,
    mode: BaseRuntimeAudioDrawerMode
): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = BaseRuntimeDrawerType.AUDIO,
    audioDrawerMode = mode,
    activeConnectivitySurface = null,
    showSettings = false
)

fun openBaseRuntimeConnectivityModal(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = null,
    audioDrawerMode = BaseRuntimeAudioDrawerMode.BROWSE,
    activeConnectivitySurface = BaseRuntimeConnectivitySurface.MODAL,
    showSettings = false
)

fun openBaseRuntimeConnectivitySetup(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = null,
    audioDrawerMode = BaseRuntimeAudioDrawerMode.BROWSE,
    activeConnectivitySurface = BaseRuntimeConnectivitySurface.SETUP,
    showSettings = false
)

fun openBaseRuntimeConnectivityManager(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = null,
    audioDrawerMode = BaseRuntimeAudioDrawerMode.BROWSE,
    activeConnectivitySurface = BaseRuntimeConnectivitySurface.MANAGER,
    showSettings = false
)

fun openBaseRuntimeSettings(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = null,
    audioDrawerMode = BaseRuntimeAudioDrawerMode.BROWSE,
    activeConnectivitySurface = null,
    showSettings = true
)

fun closeBaseRuntimeConnectivitySurface(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeConnectivitySurface = null
)

fun closeBaseRuntimeSettings(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    showSettings = false
)

fun closeBaseRuntimeOverlays(state: BaseRuntimeShellCoreState): BaseRuntimeShellCoreState = state.copy(
    activeDrawer = null,
    audioDrawerMode = BaseRuntimeAudioDrawerMode.BROWSE,
    activeConnectivitySurface = null,
    showSettings = false
)

fun shouldShowBaseRuntimeScrim(state: BaseRuntimeShellCoreState): Boolean =
    state.activeDrawer == BaseRuntimeDrawerType.AUDIO ||
        state.activeDrawer == BaseRuntimeDrawerType.HISTORY ||
        state.showSettings ||
        state.activeConnectivitySurface == BaseRuntimeConnectivitySurface.MODAL

fun resolveBaseRuntimeScrimAlpha(state: BaseRuntimeShellCoreState): Float = when {
    state.activeDrawer == BaseRuntimeDrawerType.HISTORY -> 0.56f
    shouldShowBaseRuntimeScrim(state) -> 0.4f
    else -> 0f
}

fun shouldShowBaseRuntimeIdleComposerHint(
    state: BaseRuntimeShellCoreState,
    isImeVisible: Boolean
): Boolean = state.activeDrawer == null &&
    state.activeConnectivitySurface == null &&
    !state.showSettings &&
    !isImeVisible
