package com.smartsales.feature.chat.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.theme.AppColors

enum class MotionState {
    Hidden,
    Idle,      // Subtle breathing (Home Hero only)
    Listening, // Expansion ripple / height
    Thinking,  // Lateral flow / Shimmer
    Error
}

@Composable
fun ChromaWave(
    state: MotionState,
    modifier: Modifier = Modifier.fillMaxWidth().height(100.dp) // Default height
) {
    if (state == MotionState.Hidden) return

    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")

    // 1. Thinking Animation (Lateral Flow)
    // We animate the offset of the gradient to create a shimmering flow effect
    val thinkingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f, // Arbitrary large number for offset
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThinkingFlow"
    )

    // 2. Listening Animation (Pulse/Expansion)
    val listeningScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ListeningPulse"
    )

    // 3. Idle Animation (Subtle Breathe)
    val idleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdleBreathe"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val brush = when (state) {
            MotionState.Thinking -> {
                // Animated Linear Gradient for lateral flow
                // We create a brush that shifts its start/end points
                val colors = listOf(
                    Color(0xFFAF52DE), // Purple
                    Color(0xFFFF2D55), // Pink
                    Color(0xFFAF52DE)  // Loop back for seamless restart
                )
                // Use the offset to shift the gradient window
                // Note: Standard linearGradient doesn't "loop" easily with pure offsets without tiling.
                // A simpler "shimmer" is often done by moving the start/end X.
                // Let's construct a dynamic brush for the "shimmer".
                Brush.linearGradient(
                    colors = colors,
                    start = Offset(0f - thinkingOffset % width, 0f),
                    end = Offset(width - thinkingOffset % width, 0f),
                    tileMode = TileMode.Mirror
                )
            }
            MotionState.Listening -> AppColors.WaveListening
            MotionState.Error -> AppColors.WaveError
            MotionState.Idle -> AppColors.WaveIdle
            MotionState.Hidden -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
        }

        // Apply animations via alpha or drawing scale
        when (state) {
            MotionState.Listening -> {
                // Draw a pulsing rectangle expanding from the bottom
                val currentHeight = height * listeningScale
                drawRect(
                    brush = brush,
                    topLeft = Offset(0f, height - currentHeight),
                    size = size.copy(height = currentHeight),
                    alpha = 1.0f
                )
            }
            MotionState.Idle -> {
                drawRect(
                    brush = brush,
                    alpha = idleAlpha
                )
            }
            MotionState.Thinking -> {
                 drawRect(
                    brush = brush,
                    alpha = 1.0f
                )
            }
            else -> {
                drawRect(brush = brush)
            }
        }
    }
}

@Preview
@Composable
fun PreviewChromaWave() {
    androidx.compose.foundation.layout.Column {
        ChromaWave(state = MotionState.Idle, modifier = Modifier.fillMaxWidth().height(80.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        ChromaWave(state = MotionState.Listening, modifier = Modifier.fillMaxWidth().height(80.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        ChromaWave(state = MotionState.Thinking, modifier = Modifier.fillMaxWidth().height(80.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        ChromaWave(state = MotionState.Error, modifier = Modifier.fillMaxWidth().height(80.dp))
    }
}
