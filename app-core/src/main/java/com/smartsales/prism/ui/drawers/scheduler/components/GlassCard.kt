package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.drawers.scheduler.currentSchedulerDrawerVisuals

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val visuals = currentSchedulerDrawerVisuals
    val shape = RoundedCornerShape(visuals.cardCornerRadius)
    Box(
        modifier = modifier
            .background(visuals.cardBackground, shape)
            .border(0.5.dp, visuals.cardBorder, shape)
            .clip(shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(visuals.cardHighlight)
        )
        content()
    }
}
