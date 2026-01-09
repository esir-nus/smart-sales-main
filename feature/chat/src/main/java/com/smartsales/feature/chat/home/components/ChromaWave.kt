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
import androidx.compose.ui.graphics.StrokeJoin
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
    // Phase 1 (Purple/Back): Slower, base wave
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase1"
    )

    // Phase 2 (Cyan/Front): Slightly faster, creates the "crossing" effect
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing), // 3s vs 4s creates interference pattern
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase2"
    )

    // --- Visual Parameters ---
    // Amplitude: Height of the wave peaks
    val amplitude = when (state) {
        ChromaWaveVisualState.Listening -> 0.7f // Higher energy
        ChromaWaveVisualState.Thinking -> 0.5f  // Balanced flow
        ChromaWaveVisualState.Error -> 0.3f
        else -> 0f
    }

    // Ribbon Thickness: How "wide" the ribbon is
    val ribbonThickness = when (state) {
        ChromaWaveVisualState.Listening -> 35f
        ChromaWaveVisualState.Thinking -> 25f   // Elegant thin ribbon
        else -> 20f
    }

    // Speed Multiplier
    val speedMult = when (state) {
        ChromaWaveVisualState.Listening -> 2.0f // Energetic
        ChromaWaveVisualState.Thinking -> 1.0f  // Smooth, hypnotic
        ChromaWaveVisualState.Error -> 5.0f     // Jitter
        else -> 1f
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.5f
        val maxAmpPx = height * 0.35f // Use more vertical space

        // Layer Data
        data class RibbonLayer(
            val frequency: Float,
            val phaseAnim: Float,
            val colorStroke: Color, // Bright Neon
            val colorFillTop: Color, // Aurora Start
            val colorFillBottom: Color, // Aurora End
            val twistFactor: Float, // Shift between top/bottom of SAME ribbon
            val strokeWidth: Float
        )

        // Colors (AuraFlow Neon Palette)
        val cyanNeon = Color(0xFF00F0FF) // Cyber Cyan
        val purpleNeon = Color(0xFFBD00FF) // Cyber Purple
        val blueDeep = Color(0xFF0057FF)   // Deep Blue for depth

        val layers = listOf(
            // Ribbon 1 (Back): Purple/Magenta -> Deep Blue
            RibbonLayer(
                frequency = 1.0f,
                phaseAnim = phase1 * speedMult,
                colorStroke = purpleNeon,
                colorFillTop = purpleNeon.copy(alpha = 0.3f),
                colorFillBottom = blueDeep.copy(alpha = 0.0f),
                twistFactor = 0.5f,
                strokeWidth = 3.dp.toPx()
            ),
            // Ribbon 2 (Front): Cyan -> Transparent
            RibbonLayer(
                frequency = 1.5f, // Higher freq creates the "weaving" look
                phaseAnim = phase2 * speedMult,
                colorStroke = cyanNeon,
                colorFillTop = cyanNeon.copy(alpha = 0.4f),
                colorFillBottom = cyanNeon.copy(alpha = 0.0f),
                twistFactor = 0.8f, // More twist on the front ribbon
                strokeWidth = 4.dp.toPx()
            )
        )

        for (layer in layers) {
            val pathFill = Path()
            val pathStroke = Path()
            
            // Arrays to store points for the return trip (bottom edge)
            val bottomPointsX = FloatArray((width.toInt() / 10) + 5)
            val bottomPointsY = FloatArray((width.toInt() / 10) + 5)
            var pointIndex = 0

            pathFill.moveTo(0f, centerY)
            pathStroke.moveTo(0f, centerY)

            val step = 10
            // Draw slightly past width to avoid gaps
            for (x in 0..width.toInt() + step step step) {
                val xPos = x.toFloat()
                val nX = xPos / width

                // Window taper (Bell curve) to fade edges smoothly
                val window = sin(PI * nX).toFloat()

                // Top Wave (The "spine" of the ribbon)
                val yTopOffset = maxAmpPx * amplitude * window * 
                                 sin(2 * PI * layer.frequency * nX + layer.phaseAnim).toFloat()
                val yTop = centerY + yTopOffset

                // Bottom Wave (The "belly" of the ribbon)
                // Twist logic: The bottom creates a visual "turn" by lagging/leading the top
                val twist = sin(2 * PI * layer.frequency * nX + layer.phaseAnim + layer.twistFactor).toFloat()
                
                // Variable thickness enhances the 3D twist illusion
                val currentThickness = ribbonThickness * (1.0f + 0.5f * twist) 
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

            // 1. Draw Fill (Aurora) First (Behind the strokes)
            val pathFillComplete = Path()
            pathFillComplete.addPath(pathFill)
            for (i in pointIndex - 1 downTo 0) {
                pathFillComplete.lineTo(bottomPointsX[i], bottomPointsY[i])
            }
            pathFillComplete.close()

            // Vertical Brush for "hanging light" look - Make it more opaque for ambient feel
            val brush = Brush.verticalGradient(
                colors = listOf(
                    layer.colorFillTop.copy(alpha = 0.35f), // Stronger fill
                    layer.colorFillBottom
                ),
                startY = centerY - maxAmpPx,
                endY = centerY + maxAmpPx + (ribbonThickness * 2.5f)
            )

            drawPath(
                path = pathFillComplete,
                brush = brush,
                style = Fill
            )

            // 2. Multi-Pass Bloom (Ambient Glow Effect)
            // Draw the SAME path 3 times at decreasing widths = faux Gaussian blur
            
            // Pass 1: Outer Glow (Wide, Faint)
            drawPath(
                path = pathStroke,
                color = layer.colorStroke.copy(alpha = 0.12f),
                style = Stroke(
                    width = 28.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Pass 2: Mid Glow (Medium)
            drawPath(
                path = pathStroke,
                color = layer.colorStroke.copy(alpha = 0.25f),
                style = Stroke(
                    width = 14.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Pass 3: Core (Sharp, Bright)
            drawPath(
                path = pathStroke,
                color = layer.colorStroke,
                style = Stroke(
                    width = layer.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                alpha = 0.95f
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewChromaWaveV5() {
    // Preview the "Thinking" state to verify interweaving
    ChromaWave(state = ChromaWaveVisualState.Thinking, modifier = Modifier.fillMaxWidth().height(200.dp))
}
