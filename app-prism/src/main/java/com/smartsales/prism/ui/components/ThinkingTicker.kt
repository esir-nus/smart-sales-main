package com.smartsales.prism.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Thinking Ticker - Visualizes Perception (Parsing Phase)
 *
 * Shows organic parsing progress like "Reading PDF pg 3/12..."
 * Used in parallel with Thinking Box.
 *
 * @see prism-ui-ux-contract.md §6.2
 */
@Composable
fun ThinkingTicker(
    text: String,
    progress: Float, // 0.0 - 1.0 (Optional usage for visuals)
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 24.dp), // Indented more than chat bubbles
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple pulsing dot or gear icon could go here
        Text(
            text = "⚙️",
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        // Typewriter effect logic handled by FSM updating the text state
        AnimatedContent(
            targetState = text,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            }, label = "ticker_text"
        ) { targetText ->
            Text(
                text = targetText,
                color = Color(0xFF88CCFF), // Light Blue
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun TickerPreview() {
    ThinkingTicker(
        text = "📄 Reading Q3_Report.pdf (Pg 5/12)...",
        progress = 0.4f
    )
}
