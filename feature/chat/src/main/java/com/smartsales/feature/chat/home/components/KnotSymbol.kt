package com.smartsales.feature.chat.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.theme.AppColors
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue // Added import
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

/**
 * The "Knot Symbol" - Brand mark for "Living Intelligence".
 * A lemniscate (infinity) figure drawn with the brand gradient.
 * 
 * Ported from `design_system_prototype.html`.
 */
@Composable
fun KnotSymbol(
    isThinking: Boolean = false, // Fix T5: Accept state
    modifier: Modifier = Modifier
) {
    // Animation 1: Breathing (Idle)
    val infiniteTransition = rememberInfiniteTransition(label = "knot_breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animation 2: Spinning (Thinking)
    val spinTransition = rememberInfiniteTransition(label = "knot_spinning")
    val rotation by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // State Selection
    val currentScale = if (isThinking) 1.0f else breathScale
    val currentRotation = if (isThinking) rotation else 0f
    
    // Brush Selection
    val currentBrush = if (isThinking) AppColors.WaveThinking else AppColors.WaveIdle

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = currentScale
            scaleY = currentScale
            rotationZ = currentRotation
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        // Scale Factor: Fits lemniscate within the bounds
        val scalePx = size.minDimension * 0.35f 
        
        val path = Path()
        var first = true
        
        // Parametric equation for Lemniscate of Bernoulli
        val steps = 100
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2 * PI.toFloat()
            val sinT = sin(t)
            val cosT = cos(t)
            val denom = 1 + sinT.pow(2)
            
            val x = cx + (scalePx * cosT) / denom
            val y = cy + (scalePx * sinT * cosT) / denom
            
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        // Pass 1: Atmosphere (Glow)
        drawPath(
            path = path,
            brush = currentBrush, // Dynamic Brush
            alpha = 0.5f, // Increased alpha for glow
            style = Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) // 5dp glow
        )

        // Pass 2: Core (Stroke)
        drawPath(
            path = path,
            brush = currentBrush, // Dynamic Brush
            style = Stroke(width = 2.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) // 2.5dp core
        )
    }
}
