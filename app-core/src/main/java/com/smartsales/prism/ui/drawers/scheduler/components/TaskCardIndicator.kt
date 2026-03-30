package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisuals
import com.smartsales.prism.ui.theme.AccentAmber
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.AccentDanger
import com.smartsales.prism.ui.theme.TextMuted

internal fun taskCardIndicatorColor(
    urgencyLevel: UrgencyLevel,
    isDone: Boolean
): Color {
    val baseColor = when (urgencyLevel) {
        UrgencyLevel.L1_CRITICAL -> AccentDanger
        UrgencyLevel.L2_IMPORTANT -> AccentAmber
        UrgencyLevel.L3_NORMAL -> AccentBlue
        UrgencyLevel.FIRE_OFF -> TextMuted
    }

    return if (isDone) {
        baseColor.copy(alpha = 0.45f)
    } else {
        baseColor
    }
}

@Composable
fun TaskCardIndicator(
    urgencyLevel: UrgencyLevel,
    isDone: Boolean,
    modifier: Modifier = Modifier
) {
    val visuals = currentSchedulerDrawerVisuals
    val color = taskCardIndicatorColor(
        urgencyLevel = urgencyLevel,
        isDone = isDone
    )

    Box(
        modifier = modifier
            .width(visuals.cardIndicatorWidth)
            .fillMaxHeight()
            .background(color, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
    )
}
