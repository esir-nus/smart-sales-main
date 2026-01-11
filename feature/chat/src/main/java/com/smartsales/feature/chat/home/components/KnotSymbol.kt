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
    modifier: Modifier = Modifier
) {
    // Authentic Breathing Animation (Idle State)
    val infiniteTransition = rememberInfiniteTransition(label = "knot_breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f, // Slightly more pronounced breathing
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        
        // Scale Factor: Fits lemniscate within the bounds
        // For 40dp size (approx 120px), we need radius around 15-20px
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
            brush = AppColors.WaveIdle,
            alpha = 0.4f,
            style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Pass 2: Core (Stroke)
        drawPath(
            path = path,
            brush = AppColors.WaveIdle,
            style = Stroke(width = 1.5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}
