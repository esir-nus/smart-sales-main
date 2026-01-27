package com.smartsales.feature.chat.prism.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🚧 PRISM SKELETON (MOL-06)
 * Registry Status: ✅ Verified Placeholder
 *
 * THIS IS A PLACEHOLDER.
 * Intended for Phase 1.9 verification only.
 * Awaiting replacement in Phase 3 (Real Implementation).
 *
 * Registry Contract: `text: String`, `isSending: Bool`, `onInputChanged: Fn`, `onSend: Fn`
 * Pending: `onAudio: Fn`, `onImage: Fn`
 */
@Composable
fun InputBar(
    text: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    // TODO: Add onAudioRequest / onImageRequest when skeletons are ready
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("输入消息...", color = Color(0xFF666666))
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

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSend,
            enabled = text.isNotBlank() && !isSending,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4FC3F7),
                disabledContainerColor = Color(0xFF333333)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("➤", color = Color.Black, fontSize = 16.sp)
            }
        }
    }
}
