package com.smartsales.feature.chat.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.smartsales.feature.chat.home.theme.AppColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Aurora Background.
 * 
 * Draws 3 moving gradient blobs to create a subtle, premium atmosphere.
 * Optimized for performance using a single Canvas.
 * 
 * Fix T1: Updated colors to match AppColors tokens and increased visibility.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora_animation")
    
    // Time variable for movement
    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f, // 2*PI
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing), // 10s cycle (was 20s)
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Blob 1: Top Left-ish
        val x1 = w * 0.3f + cos(t) * (w * 0.1f)
        val y1 = h * 0.3f + sin(t) * (h * 0.1f)
        drawAuroraBlob(
            center = Offset(x1, y1),
            radius = w * 0.7f,
            color = AppColors.AuroraTopLeft.copy(alpha = 0.80f) // Boosted to 0.80
        )

        // Blob 2: Center Right-ish
        val x2 = w * 0.7f + cos(t * 0.8f) * (w * 0.15f)
        val y2 = h * 0.6f + sin(t * 1.2f) * (h * 0.15f)
        drawAuroraBlob(
            center = Offset(x2, y2),
            radius = w * 0.8f,
            color = AppColors.AuroraCenterRight.copy(alpha = 0.75f) // Boosted to 0.75
        )

        // Blob 3: Bottom Left-ish
        val x3 = w * 0.4f + sin(t * 0.5f) * (w * 0.1f)
        val y3 = h * 0.8f + cos(t) * (h * 0.1f)
        drawAuroraBlob(
            center = Offset(x3, y3),
            radius = w * 0.6f,
            color = AppColors.AuroraBottomLeft.copy(alpha = 0.70f) // Boosted to 0.70
        )
    }
}

private fun DrawScope.drawAuroraBlob(
    center: Offset,
    radius: Float,
    color: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}
