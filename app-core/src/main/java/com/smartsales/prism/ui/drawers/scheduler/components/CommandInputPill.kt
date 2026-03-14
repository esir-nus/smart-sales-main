package com.smartsales.prism.ui.drawers.scheduler.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.ui.theme.AccentDanger
import com.smartsales.prism.ui.theme.TextMuted
import com.smartsales.prism.ui.theme.TextPrimary

@Composable
fun CommandInputPill(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSubmitText: (String) -> Unit,
    isRecordingMic: Boolean,
    onRecordStart: () -> Unit,
    onRecordStop: (Boolean) -> Unit, // success = true
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "❌ 需要录音权限", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF0A0A0F)),
                    startY = 0f,
                    endY = 150f
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF1E1E23).copy(alpha = 0.8f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                .padding(start = 20.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AttachFile,
                contentDescription = "Attach",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
                decorationBox = { innerTextField ->
                    if (inputText.isEmpty()) {
                        Text(
                            text = if (isRecordingMic) "🔴 松开发送..." else "输入消息，或长按麦克风说话...",
                            color = if (isRecordingMic) AccentDanger else TextMuted,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput(Unit) {
                        if (inputText.isEmpty()) {
                            detectTapGestures(
                                onPress = {
                                    val hasPermission = androidx.core.content.ContextCompat
                                        .checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (!hasPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        return@detectTapGestures
                                    }
                                    onRecordStart()
                                    val released = tryAwaitRelease()
                                    onRecordStop(released)
                                }
                            )
                        }
                    }
                    .clickable(enabled = inputText.isNotEmpty()) {
                        if (inputText.isNotEmpty()) {
                            onSubmitText(inputText)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (inputText.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = if (isRecordingMic) AccentDanger else Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
