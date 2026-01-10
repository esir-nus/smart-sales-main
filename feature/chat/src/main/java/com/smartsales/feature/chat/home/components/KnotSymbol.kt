package com.smartsales.feature.chat.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.theme.AppColors
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
    Canvas(modifier = modifier.size(160.dp, 120.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val scale = 50.dp.toPx() // Scale factor from prototype
        
        val path = Path()
        var first = true
        
        // Parametric equation for Lemniscate of Bernoulli
        // t from 0 to 2*PI
        val steps = 100
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * 2 * PI.toFloat()
            val sinT = sin(t)
            val cosT = cos(t)
            val denom = 1 + sinT.pow(2)
            
            val x = cx + (scale * cosT) / denom
            val y = cy + (scale * sinT * cosT) / denom
            
            if (first) {
                path.moveTo(x, y)
                first = false
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        // Pass 1: Atmosphere (Glow)
        // Note: Compose Canvas doesn't support shadowBlur easily on path without paint object.
        // For standard implementation, we'll draw a wider transparent stroke first.
        drawPath(
            path = path,
            brush = AppColors.WaveIdle,
            alpha = 0.3f,
            style = Stroke(width = 15.dp.toPx())
        )

        // Pass 2: Core (Stroke)
        drawPath(
            path = path,
            brush = AppColors.WaveIdle,
            style = Stroke(width = 8.dp.toPx())
        )
    }
}
