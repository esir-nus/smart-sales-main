package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.runtime.compositionLocalOf

enum class SchedulerDrawerVisualStyle {
    Default,
    SimPrototype
}

val LocalSchedulerDrawerVisualStyle = compositionLocalOf {
    SchedulerDrawerVisualStyle.Default
}
