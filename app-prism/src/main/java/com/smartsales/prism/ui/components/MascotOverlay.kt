package com.smartsales.prism.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.domain.mascot.MascotInteraction
import com.smartsales.prism.domain.mascot.MascotState
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.TextPrimary

@Composable
fun MascotOverlay(
    state: MascotState,
    onInteract: (MascotInteraction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state is MascotState.Active,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { -it }, // slide down from above
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f)
        ),
        exit = fadeOut() + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 200f)
        ),
        modifier = modifier
    ) {
        val activeState = state as? MascotState.Active ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp), // Some padding from screen edges
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.85f)) // Glassmorphism base
                    .clickable { onInteract(MascotInteraction.Tap) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 300.dp), // Don't let it stretch too wide
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playful avatar indicator
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (activeState.emotion == "happy") "✨" else "👀",
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Mascot Message
                Text(
                    text = activeState.message,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
