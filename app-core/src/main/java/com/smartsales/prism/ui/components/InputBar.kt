package com.smartsales.prism.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AudioFile
import com.smartsales.prism.ui.theme.*

/**
 * Prism Input Bar (Sleek Glass Version)
 * 
 * Features:
 * - Glass Capsule Container
 * - Breathing Placeholder
 * - Send/Mic Transition
 */
@Composable
fun InputBar(
    text: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onUploadFile: () -> Unit = {},
    onUploadImage: () -> Unit = {},
    onUploadAudio: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showAttachMenu by remember { mutableStateOf(false) }

    // Glass Capsule
    PrismSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(28.dp), // Capsule Shape
        backgroundColor = BackgroundSurface.copy(alpha = 0.8f), // Slightly more opaque for input
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp), // Inner padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Attachment Menu [+]
            Box {
                IconButton(onClick = { showAttachMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach",
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false },
                    modifier = Modifier.background(BackgroundSurfaceActive)
                ) {
                    DropdownMenuItem(
                        text = { Text("文件", color = TextPrimary) },
                        onClick = { showAttachMenu = false; onUploadFile() },
                        leadingIcon = { Icon(Icons.Default.InsertDriveFile, null, tint = TextSecondary) }
                    )
                    DropdownMenuItem(
                        text = { Text("图片", color = TextPrimary) },
                        onClick = { showAttachMenu = false; onUploadImage() },
                        leadingIcon = { Icon(Icons.Default.Image, null, tint = TextSecondary) }
                    )
                    DropdownMenuItem(
                        text = { Text("音频", color = TextPrimary) },
                        onClick = { showAttachMenu = false; onUploadAudio() },
                        leadingIcon = { Icon(Icons.Default.AudioFile, null, tint = TextSecondary) }
                    )
                }
            }

            // 2. Text Input
            TextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    val infiniteTransition = rememberInfiniteTransition(label = "Shimmer")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Alpha"
                    )
                    Text("输入消息...", color = TextMuted.copy(alpha = alpha))
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 3. Mic / Send Action
            val hasText = text.isNotBlank()
            
            if (hasText || isSending) {
                // Send Button (Blue Circle)
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSend()
                    },
                    enabled = hasText && !isSending,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentBlue,
                        disabledContainerColor = BackgroundSurfaceActive
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
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
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Mic Button (Glass/Gray)
                FilledIconButton(
                    onClick = onMicClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = BackgroundSurfaceActive
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                     Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Note",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
