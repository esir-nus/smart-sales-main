package com.smartsales.feature.chat.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

enum class MotionState {
    Hidden,
    Idle,      // Gentle harmonic wave
    Listening, // High amplitude, fast
    Thinking,  // Medium amplitude, lateral flow
    Error      // Jitter
}

/**
 * ChromaWave V3: Multi-layered harmonic wave based on the V3 visual reference.
 *
 * Design Intent:
 * - **Organic**: Sinusoidal curves with transparency, not solid blocks.
 * - **Depth**: 3 layers moving at different speeds/phases.
 * - **Fluidity**: Water-like, semi-transparent.
 */
@Composable
fun ChromaWave(
    state: MotionState,
    modifier: Modifier = Modifier.fillMaxWidth().height(100.dp)
) {
    if (state == MotionState.Hidden) return

    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")

    // --- Phase Animations (3 layers) ---
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase2"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase3"
    )

    // --- Amplitude Factors per State ---
    val baseAmplitude = when (state) {
        MotionState.Idle -> 0.15f
        MotionState.Listening -> 0.6f
        MotionState.Thinking -> 0.35f
        MotionState.Error -> 0.5f
        MotionState.Hidden -> 0f
    }

    // --- Speed Multiplier per State ---
    val speedMult = when (state) {
        MotionState.Idle -> 0.5f
        MotionState.Listening -> 1.5f
        MotionState.Thinking -> 2.0f
        MotionState.Error -> 3.0f
        MotionState.Hidden -> 0f
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.5f
        val maxAmplitude = height * 0.4f

        // Layer definitions: (Frequency, Alpha, PhaseOffset, Color)
        data class WaveLayer(
            val frequency: Float,
            val alpha: Float,
            val phaseAnimation: Float,
            val colorStart: Color,
            val colorEnd: Color
        )

        val layers = listOf(
            // Layer 1 (Back): Slow, wide, subtle
            WaveLayer(
                frequency = 1.2f,
                alpha = 0.3f,
                phaseAnimation = phase1 * speedMult,
                colorStart = Color(0xFF007AFF).copy(alpha = 0.4f), // Blue
                colorEnd = Color(0xFFA259FF).copy(alpha = 0.4f)    // Purple
            ),
            // Layer 2 (Middle): Medium
            WaveLayer(
                frequency = 1.8f,
                alpha = 0.5f,
                phaseAnimation = phase2 * speedMult,
                colorStart = Color(0xFFA259FF).copy(alpha = 0.6f), // Purple
                colorEnd = Color(0xFFFF2D55).copy(alpha = 0.5f)    // Pink
            ),
            // Layer 3 (Front): Fast, sharp
            WaveLayer(
                frequency = 2.5f,
                alpha = 0.7f,
                phaseAnimation = phase3 * speedMult,
                colorStart = Color(0xFF00C7BE).copy(alpha = 0.5f), // Teal
                colorEnd = Color(0xFF007AFF).copy(alpha = 0.6f)    // Blue
            )
        )

        for (layer in layers) {
            val path = Path()
            path.moveTo(0f, height) // Start bottom-left

            // Draw wave from left to right
            val step = 8
            for (x in 0..width.toInt() step step) {
                val xPos = x.toFloat()
                val nX = xPos / width // Normalized X (0..1)

                // Window function to taper at edges (sin(PI * nX) is 0 at 0 and 1)
                val window = sin(PI * nX).toFloat()

                // Wave equation: y = A * window * sin(2PI * freq * nX + phase)
                val yOffset = maxAmplitude * baseAmplitude * window *
                        sin(2 * PI * layer.frequency * nX + layer.phaseAnimation).toFloat()

                path.lineTo(xPos, centerY + yOffset)
            }

            path.lineTo(width, height) // Bottom-right
            path.close()

            val brush = Brush.horizontalGradient(
                colors = listOf(layer.colorStart, layer.colorEnd, layer.colorStart)
            )

            drawPath(
                path = path,
                brush = brush,
                style = Fill,
                alpha = layer.alpha
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F7F7)
@Composable
fun PreviewChromaWaveIdle() {
    ChromaWave(state = MotionState.Idle, modifier = Modifier.fillMaxWidth().height(120.dp))
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F7F7)
@Composable
fun PreviewChromaWaveListening() {
    ChromaWave(state = MotionState.Listening, modifier = Modifier.fillMaxWidth().height(120.dp))
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F7F7)
@Composable
fun PreviewChromaWaveThinking() {
    ChromaWave(state = MotionState.Thinking, modifier = Modifier.fillMaxWidth().height(120.dp))
}
