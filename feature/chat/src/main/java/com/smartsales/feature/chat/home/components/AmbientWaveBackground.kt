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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A subtle, continuously animating background wave.
 *
 * Visual Specs:
 * - Shape: Soft sine wave, no hard edges.
 * - Gradient: Sky Blue (0xFF87CEEB) -> Orchid (0xFFDA70D6).
 * - Animation: Infinite horizontal flow.
 */
@Composable
fun AmbientWaveBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmbientWave")
    
    // Animate phase for continuous movement
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height * 0.5f

        // Wave Parameters
        val amplitude = height * 0.15f // Subtle curve
        val frequency = 1.0f 

        val path = Path()
        path.moveTo(0f, centerY)

        val step = 10
        for (x in 0..width.toInt() step step) {
            val xPos = x.toFloat()
            val nX = xPos / width // Normalized 0..1

            // Sine wave calculation
            val angle = 2 * PI * frequency * nX + phase
            val yOffset = amplitude * sin(angle).toFloat()
            
            path.lineTo(xPos, centerY + yOffset)
        }
        
        // Close the path to the bottom for filling
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        // Gradient Brush (Sky Blue -> Orchid)
        val gradientBrush = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF87CEEB).copy(alpha = 0.6f), // Sky Blue
                Color(0xFFDA70D6).copy(alpha = 0.6f)  // Orchid
            )
        )

        // Draw Fill ONLY (No Strokes)
        drawPath(
            path = path,
            brush = gradientBrush
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewAmbientWaveBackground() {
    AmbientWaveBackground(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    )
}
