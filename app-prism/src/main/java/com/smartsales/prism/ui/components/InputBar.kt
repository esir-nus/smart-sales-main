package com.smartsales.prism.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue // v2.6 Shimmer
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.* // v2.6 Shimmer Animation

/**
 * Prism Input Bar
 * Aligned with Registry v2.5 (Input & Modes)
 *
 * Features:
 * - Attachment Button [📎]
 * - Floating Capsule Integration
 * - Mic [Mic] / Send [➤] Transition
 */
@Composable
fun InputBar(
    text: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit = {}, // New
    onMicClick: () -> Unit = {}     // New
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Attachment Button [📎] (Reusing Add icon as placeholder for Attach)
        IconButton(onClick = onAttachClick) {
            Icon(
                imageVector = Icons.Default.Add, // Placeholder for Clip/Attach
                contentDescription = "Attach",
                tint = Color(0xFF888888)
            )
        }

        // 2. Text Input
        TextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                // v2.6 Shimmering Placeholder
                val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "Shimmer")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                        animation = androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.LinearEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "Alpha"
                )
                Text("输入消息...", color = Color(0xFF666666).copy(alpha = alpha))
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF4FC3F7),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(4.dp))

        // 3. Mic / Send Action
        val hasText = text.isNotBlank()
        
        // Show Send if typing, otherwise Mic
        if (hasText || isSending) {
            Button(
                onClick = onSend,
                enabled = hasText && !isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4FC3F7),
                    disabledContainerColor = Color(0xFF333333)
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // Mic FAB
            FloatingActionButton(
                onClick = onMicClick,
                containerColor = Color(0xFF333333),
                contentColor = Color.White,
                modifier = Modifier.size(40.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                 Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Note",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
