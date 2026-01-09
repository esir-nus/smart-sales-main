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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.theme.AppColors
import kotlin.math.PI
import kotlin.math.sin

enum class MotionState {
    Hidden,
    Idle,      // Gentle sine wave
    Listening, // High amplitude, fast frequency
    Thinking,  // Fast phase shift, medium amplitude
    Error      // Jagged or chaotic (simulated with high freq)
}

@Composable
fun ChromaWave(
    state: MotionState,
    modifier: Modifier = Modifier.fillMaxWidth().height(100.dp)
) {
    if (state == MotionState.Hidden) return

    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")

    // Phase animation (moves the wave horizontally)
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    // Amplitude animation (height of the wave)
    // We animate this based on state changes if we want transitions, 
    // but here we map state to target amplitude directly for simplicity or use animateFloatAsState if needed.
    // For now using infinite transition for "breathing" amplitude in Idle.
    
    val amplitudeFactor = when (state) {
        MotionState.Idle -> 0.2f
        MotionState.Listening -> 0.8f
        MotionState.Thinking -> 0.5f 
        MotionState.Error -> 0.6f
        MotionState.Hidden -> 0f
    }
    
    // Frequency factor
    val frequency = when(state) {
        MotionState.Idle -> 1.0f
        MotionState.Listening -> 2.5f
        MotionState.Thinking -> 3.0f // Fast shimmy
        MotionState.Error -> 5.0f // Jittery
        MotionState.Hidden -> 0f
    }
    
    val speedMultiplier = when(state) {
        MotionState.Idle -> 0.5f
        MotionState.Listening -> 1.5f
        MotionState.Thinking -> 2.0f
        MotionState.Error -> 0.2f // Slow but jagged? Or fast? Let's say fast.
        MotionState.Hidden -> 0f
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val maxAmplitude = height / 3

        val effectivePhase = phase * speedMultiplier

        val path = Path()
        path.moveTo(0f, height) // Start bottom left
        path.lineTo(0f, centerY) // Go to center Y start

        // Draw sine wave
        for (x in 0..width.toInt() step 10) {
            val xPos = x.toFloat()
            // Normalized X (0..1)
            val nX = xPos / width
            
            // Angular frequency: 2PI * frequency
            // Wave equation: y = A * sin(wx + phase)
            // We apply a window function (sin(PI*nX)) to taper ends to 0 so it looks like a contained wave
            val window = sin(PI * nX).toFloat() 
            
            val yOffset = maxAmplitude * amplitudeFactor * window * 
                          sin(2 * PI * frequency * nX + effectivePhase).toFloat()
            
            path.lineTo(xPos, centerY + yOffset)
        }

        path.lineTo(width, height) // Bottom right
        path.lineTo(0f, height) // Close loop at bottom left
        path.close()

        val brush = when (state) {
            MotionState.Idle -> AppColors.WaveIdle
            MotionState.Listening -> AppColors.WaveListening
            MotionState.Thinking -> AppColors.WaveThinking
            MotionState.Error -> AppColors.WaveError
            MotionState.Hidden -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
        }

        drawPath(
            path = path,
            brush = brush,
            style = Fill
        )
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
    }
}
