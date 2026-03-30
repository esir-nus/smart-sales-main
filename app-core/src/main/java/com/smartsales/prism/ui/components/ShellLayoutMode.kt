package com.smartsales.prism.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class ShellLayoutMode {
    TALL,
    COMPACT,
    TIGHT
}

internal fun resolveShellLayoutMode(
    availableWidth: Dp,
    availableHeight: Dp
): ShellLayoutMode {
    val heightMode = when {
        availableHeight < 700.dp -> ShellLayoutMode.TIGHT
        availableHeight < 820.dp -> ShellLayoutMode.COMPACT
        else -> ShellLayoutMode.TALL
    }

    if (availableWidth >= 380.dp) {
        return heightMode
    }

    return when (heightMode) {
        ShellLayoutMode.TALL -> ShellLayoutMode.COMPACT
        ShellLayoutMode.COMPACT -> ShellLayoutMode.TIGHT
        ShellLayoutMode.TIGHT -> ShellLayoutMode.TIGHT
    }
}
