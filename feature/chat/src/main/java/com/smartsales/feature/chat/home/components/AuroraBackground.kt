package com.smartsales.feature.chat.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Aurora Background.
 * 
 * Draws 3 moving gradient blobs to create a subtle, premium atmosphere.
 * Optimized for performance using a single Canvas.
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
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Blob 1: Tech Blue (Top Left-ish)
        // Moves in a slow circle
        val x1 = w * 0.3f + cos(t) * (w * 0.1f)
        val y1 = h * 0.3f + sin(t) * (h * 0.1f)
        drawAuroraBlob(
            center = Offset(x1, y1),
            radius = w * 0.6f,
            color = Color(0xFF007AFF).copy(alpha = 0.15f)
        )

        // Blob 2: Deep Indigo (Bottom Right-ish)
        // Moves in an opposing ellipse
        val x2 = w * 0.7f + cos(t * 0.8f) * (w * 0.15f)
        val y2 = h * 0.6f + sin(t * 1.2f) * (h * 0.15f)
        drawAuroraBlob(
            center = Offset(x2, y2),
            radius = w * 0.7f,
            color = Color(0xFF5E5CE6).copy(alpha = 0.1f)
        )

        // Blob 3: Slate Cyan (Bottom Left-ish)
        // Adds brightness
        val x3 = w * 0.4f + sin(t * 0.5f) * (w * 0.1f)
        val y3 = h * 0.8f + cos(t) * (h * 0.1f)
        drawAuroraBlob(
            center = Offset(x3, y3),
            radius = w * 0.5f,
            color = Color(0xFF64D2FF).copy(alpha = 0.1f)
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
        radius = radius,
        // Screen blend mode for "light" effect, though SrcOver with low alpha works well for simple gradients
        // blendMode = BlendMode.Screen 
    )
}
