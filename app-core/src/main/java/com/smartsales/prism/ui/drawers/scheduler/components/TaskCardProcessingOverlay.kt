package com.smartsales.prism.ui.drawers.scheduler.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.BackgroundSurface

@Composable
fun TaskCardProcessingOverlay(
    status: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundSurface.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxSize().scale(pulseScale).background(AccentBlue.copy(alpha = pulseAlpha), CircleShape))
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AccentBlue, trackColor = AccentBlue.copy(alpha = 0.2f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = status, fontSize = 13.sp, color = AccentBlue, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}
