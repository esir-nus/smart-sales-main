package com.smartsales.feature.chat.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
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

            // V9: Floating Volumetric Ribbons (The "Siri Standard")
            // Implements the approved Web Prototype logic:
            // 1. 3 Distinct Ribbons (Cyan, Pink, Blue) for Alpha Stacking
            // 2. Floating Geometry (Dual Sine Waves) - No flat bottom
            // 3. Vertical Alpha Gradient per ribbon for soft edges

            // Layer Config (Matches Prototype)
            data class V9Layer(
                val color: Color,
                val alpha: Float,
                val freq: Float,
                val speed: Float,
                val amp: Float,
                val twist: Float
            )

            val v9Layers = listOf(
                // Back: Deep Blue (Slow, Wide)
                V9Layer(Color(0xFF0057FF), 0.3f, 0.8f, 0.02f, 0.4f, 0.5f),
                // Mid: Neon Pink (Body)
                V9Layer(Color(0xFFBD00FF), 0.4f, 1.0f, 0.03f, 0.6f, 1.0f),
                // Front: Cyan (Fast, Detail)
                V9Layer(Color(0xFF00C6FF), 0.5f, 1.5f, 0.05f, 0.5f, 1.5f)
            )

            for (layer in v9Layers) {
                // Determine Phase (State-based speed + Layer intrinsic speed)
                val layerPhase = (if (layer.freq > 1.0f) phase2 else phase1) * layer.speed * 20f

                val path = Path()
                
                // Point lists for constructing the closed shape
                val topPoints = ArrayList<Offset>()
                val bottomPoints = ArrayList<Offset>()
                
                val step = 10
                for (x in 0..size.width.toInt() step step) {
                    val xPos = x.toFloat()
                    val nX = xPos / size.width
                    
                    // Gaussian Window (Bell Curve) to fade edges to 0
                    val xCentered = nX - 0.5f
                    // Note: In Compose we can use a simpler sine window for performance if needed, 
                    // but the prototype used this math for the specific "Siri" look.
                    // Converting JS: 4.0 / (2.0 + 9.0 * (xCentered * xCentered)) - 0.2
                    val window = maxOf(0.0f, 4.0f / (2.0f + 9.0f * (xCentered * xCentered)) - 0.2f)
                    
                    // Wave Math
                    val sineTop = sin(2 * PI * layer.freq * nX + layerPhase).toFloat()
                    val sineBottom = sin(2 * PI * layer.freq * nX + layerPhase + layer.twist).toFloat()

                    // Top Edge
                    val yTop = centerY + (maxAmpPx * layer.amp) * window * sineTop
                    
                    // Bottom Edge (Floating!)
                    // Thickness tapers with the window too
                    // ribbonThickness is already Float (pixels), do not use .toPx()
                    val thickness = (ribbonThickness * 3f) * window 
                    val yBottom = yTop + thickness + (10f * window * sineBottom)

                    topPoints.add(Offset(xPos, yTop))
                    bottomPoints.add(Offset(xPos, yBottom))
                }
                
                // Construct Path: Left->Right (Top), then Right->Left (Bottom)
                if (topPoints.isNotEmpty()) {
                    path.moveTo(topPoints.first().x, topPoints.first().y)
                    for (p in topPoints) path.lineTo(p.x, p.y)
                    
                    for (i in bottomPoints.indices.reversed()) {
                        val p = bottomPoints[i]
                        path.lineTo(p.x, p.y)
                    }
                    path.close()
                }

                // Vertical Gradient Brush (Transparent -> Color -> Transparent)
                // This creates the "Glow" within the ribbon itself
                val ribbonBrush = Brush.verticalGradient(
                    colors = listOf(
                        layer.color.copy(alpha = 0f),
                        layer.color.copy(alpha = layer.alpha * 1.5f), // Boost blend
                        layer.color.copy(alpha = 0f)
                    ),
                    startY = centerY - maxAmpPx,
                    endY = centerY + maxAmpPx
                )

                drawPath(path, brush = ribbonBrush)
            }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun PreviewChromaWaveV5() {
    // Preview the "Thinking" state to verify interweaving
    ChromaWave(state = ChromaWaveVisualState.Thinking, modifier = Modifier.fillMaxWidth().height(200.dp))
}
