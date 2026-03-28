package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartsales.prism.domain.scheduler.UrgencyLevel
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
    val color = taskCardIndicatorColor(
        urgencyLevel = urgencyLevel,
        isDone = isDone
    )

    Box(
        modifier = modifier
            .padding(vertical = 16.dp)
            .padding(start = 16.dp)
            .width(2.dp)
            .fillMaxHeight()
            .background(color, RoundedCornerShape(1.dp))
    )
}
