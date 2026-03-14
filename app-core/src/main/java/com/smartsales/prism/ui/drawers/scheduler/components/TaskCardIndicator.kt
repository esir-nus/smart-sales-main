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
import com.smartsales.prism.ui.theme.AccentAmber
import com.smartsales.prism.ui.theme.AccentBlue

enum class IndicatorState {
    NORMAL, CONFLICT, DONE
}

@Composable
fun TaskCardIndicator(
    state: IndicatorState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        IndicatorState.NORMAL -> AccentBlue
        IndicatorState.CONFLICT -> AccentAmber
        IndicatorState.DONE -> Color(0xFF10B981) // Green
    }
    
    Box(
        modifier = modifier
            .padding(vertical = 16.dp)
            .padding(start = 16.dp)
            .width(2.dp)
            .fillMaxHeight()
            .background(color, RoundedCornerShape(1.dp))
    )
}
