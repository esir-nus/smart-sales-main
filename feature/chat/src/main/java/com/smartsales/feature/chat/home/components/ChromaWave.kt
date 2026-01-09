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
    Idle,      // Gentle persistent presence
    Listening, // High amplitude, fast
    Thinking,  // Medium amplitude, lateral flow
    Error      // Jitter
}

/**
 * ChromaWave V4: Ethereal Ribbons
 *
 * Design Intent:
 * - **Ethereal**: Uses VERTICAL gradients to fade out the top/bottom of the wave, creating a floating ribbon effect.
 * - **Organic**: Multiple harmonic sine waves.
 * - **Persistent**: Tuned to be visible but subtle in Idle state.
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
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
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
        MotionState.Idle -> 0.2f       // Increased for visibility
        MotionState.Listening -> 0.7f
        MotionState.Thinking -> 0.4f
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

        // Layer definitions
        data class WaveLayer(
            val frequency: Float,
            val alpha: Float,
            val phaseAnimation: Float,
            val colorCore: Color
        )

        val layers = listOf(
            // Layer 1 (Back): Blue, wide
            WaveLayer(
                frequency = 1.0f,
                alpha = 0.6f, // Higher alpha because gradient fades it out
                phaseAnimation = phase1 * speedMult,
                colorCore = Color(0xFF007AFF) 
            ),
            // Layer 2 (Middle): Purple
            WaveLayer(
                frequency = 1.5f,
                alpha = 0.7f,
                phaseAnimation = phase2 * speedMult,
                colorCore = Color(0xFFA259FF)
            ),
            // Layer 3 (Front): Teal/Cyan, fast
            WaveLayer(
                frequency = 2.2f,
                alpha = 0.8f,
                phaseAnimation = phase3 * speedMult,
                colorCore = Color(0xFF00C7BE)
            )
        )

        for (layer in layers) {
            val path = Path()
            path.moveTo(0f, height) 

            val step = 8
            for (x in 0..width.toInt() step step) {
                val xPos = x.toFloat()
                val nX = xPos / width 

                // Window function to taper at edges
                val window = sin(PI * nX).toFloat()

                // Wave equation
                val yOffset = maxAmplitude * baseAmplitude * window *
                        sin(2 * PI * layer.frequency * nX + layer.phaseAnimation).toFloat()

                // Map Y to vertically centered wave
                // Note: For a "ribbon" we might want to draw a strip, but here we fill from bottom?
                // Actually, V3 filled from bottom. V4 "Ribbon" means we want a strip.
                // Let's draw a FILLED shape from Top to Bottom but shade it with a gradient that hides the top/bottom flat edges?
                // No, standard sine wave fill is usually (x, y) down to (x, height).
                // If we use a vertical gradient that is Transparency -> Color -> Transparency, 
                // and we draw a RECT that covers the wave area, clipped to the wave path?
                // Easier: Draw the wave path filled from (x, y) to (x, height). 
                // But set the gradient to fade out at the bottom.
                
                path.lineTo(xPos, centerY + yOffset)
            }

            path.lineTo(width, height)
            path.lineTo(0f, height)
            path.close()

            // V4 Secret Sauce: Vertical Gradient
            // Top of rect (centerY - amp) -> Transparent
            // Middle (centerY) -> Color
            // Bottom (height) -> Transparent
            
            // Because the path fills down to 'height', we can set the gradient to fade out towards 'height'.
            val brush = Brush.verticalGradient(
                colors = listOf(
                    layer.colorCore.copy(alpha = 0f),      // Top (start of brush)
                    layer.colorCore,                       // Core
                    layer.colorCore.copy(alpha = 0f)       // Bottom (end of brush)
                ),
                startY = centerY - maxAmplitude,
                endY = height
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
fun PreviewChromaWaveV4() {
    ChromaWave(state = MotionState.Idle, modifier = Modifier.fillMaxWidth().height(120.dp))
}
