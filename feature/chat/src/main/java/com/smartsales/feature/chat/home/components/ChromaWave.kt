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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * ChromaWave V5: Floating Ribbon & Neon Glow
 *
 * Design Intent:
 * - **Floating**: Geometry is defined by TWO sine waves (Top and Bottom), creating a ribbon that does not touch the container edges.
 * - **Twist**: The bottom wave has a phase shift relative to the top, creating a 3D twisting illusion.
 * - **Neon**: Top edge is drawn with a bright Stroke. Body is drawn with a faint Aurora Fill.
 * - **Strict Visibility**: Controlled by [ChromaWaveVisualState].
 */
@Composable
fun ChromaWave(
    state: ChromaWaveVisualState,
    modifier: Modifier = Modifier.fillMaxWidth().height(120.dp)
) {
    if (state == ChromaWaveVisualState.Hidden) return

    val infiniteTransition = rememberInfiniteTransition(label = "WaveAnimation")

    // --- Phase Animations (2 main ribbons) ---
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
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase2"
    )

    // --- Visual Parameters ---
    // Amplitude: Height of the wave peaks
    val amplitude = when (state) {
        ChromaWaveVisualState.Listening -> 0.6f
        ChromaWaveVisualState.Thinking -> 0.4f
        ChromaWaveVisualState.Error -> 0.3f
        else -> 0f
    }

    // Ribbon Thickness: How "wide" the ribbon is
    val ribbonThickness = when (state) {
        ChromaWaveVisualState.Listening -> 40f
        ChromaWaveVisualState.Thinking -> 30f
        else -> 20f
    }

    // Speed Multiplier
    val speedMult = when (state) {
        ChromaWaveVisualState.Listening -> 1.5f
        ChromaWaveVisualState.Thinking -> 2.5f // Faster shimmer
        ChromaWaveVisualState.Error -> 5.0f // Jitter
        else -> 1f
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.5f
        val maxAmpPx = height * 0.3f

        // Layer Data
        data class RibbonLayer(
            val frequency: Float,
            val phaseAnim: Float,
            val colorStroke: Color, // Bright Neon
            val colorFillTop: Color, // Aurora Start
            val colorFillBottom: Color, // Aurora End
            val twistFactor: Float // How much the bottom lags the top (creating 3D twist)
        )

        // Colors (iOS/Siri Vibrant Style)
        val cyanNeon = Color(0xFF5AC8FA) // iOS System Cyan
        val purpleNeon = Color(0xFFBF5AF2) // iOS System Purple
        val pinkNeon = Color(0xFFFF2D55) // iOS System Pink
        val blueNeon = Color(0xFF007AFF) // iOS System Blue

        val layers = listOf(
            // Ribbon 1 (Back): Purple/Pink
            RibbonLayer(
                frequency = 1.0f,
                phaseAnim = phase1 * speedMult,
                colorStroke = purpleNeon,
                colorFillTop = purpleNeon.copy(alpha = 0.3f),
                colorFillBottom = pinkNeon.copy(alpha = 0.0f), // Fade to transp
                twistFactor = 0.5f
            ),
            // Ribbon 2 (Front): Cyan/Blue
            RibbonLayer(
                frequency = 1.5f,
                phaseAnim = phase2 * speedMult,
                colorStroke = cyanNeon,
                colorFillTop = cyanNeon.copy(alpha = 0.4f), 
                colorFillBottom = blueNeon.copy(alpha = 0.0f), // Fade to transp
                twistFactor = 0.8f
            )
        )

        for (layer in layers) {
            val pathFill = Path()
            val pathStroke = Path()
            
            // We need to construct the ribbon shape.
            // VerticesTop: (x, yTop)
            // VerticesBottom: (x, yBottom)
            // Path Fill = Top L->R, then Bottom R->L
            
            // Arrays to store points for the return trip
            val bottomPointsX = FloatArray((width.toInt() / 10) + 2)
            val bottomPointsY = FloatArray((width.toInt() / 10) + 2)
            var pointIndex = 0

            pathFill.moveTo(0f, centerY) // Start approx
            pathStroke.moveTo(0f, centerY)

            val step = 10
            for (x in 0..width.toInt() step step) {
                val xPos = x.toFloat()
                val nX = xPos / width

                // Window taper
                val window = sin(PI * nX).toFloat()

                // Top Wave
                val yTopOffset = maxAmpPx * amplitude * window * 
                                 sin(2 * PI * layer.frequency * nX + layer.phaseAnim).toFloat()
                val yTop = centerY + yTopOffset

                // Bottom Wave (Twisting Ribbon)
                // yBottom = yTop + thickness * (variation)
                // Variation creates the "twist" - sometimes thick, sometimes thin
                val twist = sin(2 * PI * layer.frequency * nX + layer.phaseAnim + layer.twistFactor).toFloat()
                val currentThickness = ribbonThickness * (1.2f + 0.4f * twist) // 0.8 to 1.6x thickness
                val yBottom = yTop + currentThickness

                if (x == 0) {
                    pathFill.moveTo(xPos, yTop)
                    pathStroke.moveTo(xPos, yTop)
                } else {
                    pathFill.lineTo(xPos, yTop)
                    pathStroke.lineTo(xPos, yTop)
                }

                if (pointIndex < bottomPointsX.size) {
                    bottomPointsX[pointIndex] = xPos
                    bottomPointsY[pointIndex] = yBottom
                    pointIndex++
                }
            }

            // Draw Stroke (Neon Line) on Top Edge Only
            drawPath(
                path = pathStroke,
                color = layer.colorStroke,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                alpha = 0.9f
            )

            // Complete the Fill Path (Go backwards along bottom edge)
            for (i in pointIndex - 1 downTo 0) {
                pathFill.lineTo(bottomPointsX[i], bottomPointsY[i])
            }
            pathFill.close()

            // Draw Fill (Aurora)
            // Vertical Gradient from Top Color to Bottom Color
            val brush = Brush.verticalGradient(
                colors = listOf(layer.colorFillTop, layer.colorFillBottom),
                startY = centerY - maxAmpPx,
                endY = centerY + maxAmpPx + ribbonThickness
            )

            drawPath(
                path = pathFill,
                brush = brush,
                style = Fill,
                alpha = 1.0f // Alpha controls built into colors
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1E) // Dark mode preview
@Composable
fun PreviewChromaWaveV5() {
    ChromaWave(state = ChromaWaveVisualState.Thinking, modifier = Modifier.fillMaxWidth().height(120.dp))
}
