package com.smartsales.feature.chat.home.messages

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.theme.AppRadius

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    // Glass Pill Container
    Surface(
        modifier = modifier
            .wrapContentSize(),
        shape = RoundedCornerShape(
            topStart = AppRadius.BubbleCornerSmall,
            topEnd = AppRadius.BubbleCornerLarge,
            bottomEnd = AppRadius.BubbleCornerLarge,
            bottomStart = AppRadius.BubbleCornerLarge
        ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TypingDot(delayMillis = 0, color = Color(0xFF00C6FF)) // Cyan
            TypingDot(delayMillis = 200, color = Color(0xFF007AFF)) // Blue
            TypingDot(delayMillis = 400, color = Color(0xFF5856D6)) // Purple
        }
    }
}

@Composable
private fun TypingDot(
    delayMillis: Int,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TypingDot")
    
    // Scale Animation (Breathe)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    // Opacity Animation
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(color)
    )
}


