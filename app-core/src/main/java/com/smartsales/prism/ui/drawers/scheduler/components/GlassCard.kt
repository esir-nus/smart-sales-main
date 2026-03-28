package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
    Box(
        modifier = modifier
            .background(visuals.cardBackground, RoundedCornerShape(16.dp))
            .border(1.dp, visuals.cardBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp)),
        content = content
    )
}
