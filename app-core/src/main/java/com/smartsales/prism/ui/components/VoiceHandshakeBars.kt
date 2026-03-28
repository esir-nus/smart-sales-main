package com.smartsales.prism.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun rememberVoiceHandshakeWaveProgress(
    isRecording: Boolean,
    labelPrefix: String
): Float {
    val motion = rememberInfiniteTransition(label = "${labelPrefix}MicMotion")
    val cycleDurationMs = if (isRecording) 600 else 3000
    val waveProgress by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "${labelPrefix}HandshakeProgress"
    )
    return waveProgress
}

@Composable
internal fun VoiceHandshakeBars(
    isRecording: Boolean,
    waveProgress: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val minHeight = if (isRecording) 10f else 8f
    val maxHeight = if (isRecording) 40f else 20f
    val cycleMs = if (isRecording) 600f else 3000f
    val phaseOffsetsMs = if (isRecording) {
        floatArrayOf(0f, 100f, 200f, 50f, 250f, 150f)
    } else {
        floatArrayOf(0f, 400f, 800f, 200f, 600f, 1000f)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.height(50.dp)
    ) {
        phaseOffsetsMs.forEach { offsetMs ->
            val phase = (waveProgress + (offsetMs / cycleMs)) % 1f
            val pulse = (sin((phase * 2f * PI.toFloat()) - (PI.toFloat() / 2f)) + 1f) / 2f
            val barHeight = (minHeight + ((maxHeight - minHeight) * pulse)).dp
            val alpha = if (isRecording) 1f else 0.4f + (0.3f * pulse)
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor.copy(alpha = alpha))
            )
        }
    }
}
