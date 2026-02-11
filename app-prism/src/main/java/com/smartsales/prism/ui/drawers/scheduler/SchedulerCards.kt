package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.QueryBuilder
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.smartsales.prism.data.audio.PhoneAudioRecorder
import com.smartsales.prism.ui.components.PrismCard
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismButtonStyle
import com.smartsales.prism.ui.theme.*

/**
 * Task Card Component (Sleek Glass Version)
 * @see prism-ui-ux-contract.md §1.3 "Task Card"
 */
@Composable
fun TaskCard(
    state: TimelineItem.Task,
    isExpanded: Boolean = false,
    onExpandToggle: () -> Unit,
    onClick: () -> Unit,
    onReschedule: (String) -> Unit = {},
    onMicRecord: (java.io.File) -> Unit = {},
    onToggleDone: () -> Unit = {}
) {
    // 冲突视觉效果 — 琥珀色边框 + 呼吸发光
    val conflictBorderModifier = when (state.conflictVisual) {
        ConflictVisual.CAUSING -> {
            // Aggressive glow: Background pulse + thicker border
            val infiniteTransition = rememberInfiniteTransition(label = "conflict_glow")
            
            // Background tint pulse (0.0 -> 0.12) - Unmistakable attention grab
            val bgAlpha by infiniteTransition.animateFloat(
                initialValue = 0.0f,
                targetValue = 0.12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bg_alpha"
            )
            
            // Border alpha (0.6 -> 1.0)
            val borderAlpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "border_alpha"
            )
            
            if (!isExpanded) {
                // Pulsing state (when collapsed)
                Modifier
                    .background(Color(0xFFFF9800).copy(alpha = bgAlpha), RoundedCornerShape(12.dp))
                    .border(3.dp, Color(0xFFFF9800).copy(alpha = borderAlpha), RoundedCornerShape(12.dp))
            } else {
                // Static state (when expanded) - stop distraction
                Modifier.border(2.dp, Color(0xFFFF9800), RoundedCornerShape(12.dp))
            }
        }
        ConflictVisual.IN_GROUP -> {
            Modifier.border(2.dp, Color(0xFFFF9800).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
        }
        ConflictVisual.NONE -> Modifier
    }
    
    PrismCard(
        onClick = {
            onExpandToggle()
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(conflictBorderModifier),  // 应用冲突边框
        shape = GlassCardShape,
        elevation = if (isExpanded) 8.dp else 2.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Header Row (Always Visible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Checkbox
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onToggleDone() }
                            .background(
                                if (state.isDone) AccentSecondary else Color.Transparent, 
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.5.dp, 
                                if (state.isDone) Color.Transparent else BorderSubtle, 
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isDone) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Title
                    Text(
                        text = state.title,
                        fontSize = 14.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (state.isDone) TextDecoration.LineThrough else null,
                        color = if (state.isDone) TextMuted else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Alarm Icon / Smart Badge
                    if (state.hasAlarm) {
                        if (state.isSmartAlarm) {
                            // Smart Badge
                            Row(
                                modifier = Modifier
                                    .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QueryBuilder,
                                    contentDescription = "Smart Alarm",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "智能提醒",
                                    fontSize = 11.sp,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            // Standard Icon
                            Icon(
                                imageVector = Icons.Outlined.QueryBuilder,
                                contentDescription = "Alarm",
                                tint = if (state.isDone) TextMuted else AccentBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // Expanded Content
                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 44.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(bottom = 8.dp))
                        
                        // Metadata Rows
                        val metaStyle = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 13.sp
                        )

                        // Date Row (📅)
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = "Date",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = state.dateRange, style = metaStyle)
                        }
                        
                        // Location Row (📍)
                        state.location?.let { loc ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = "Location",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = loc, style = metaStyle)
                            }
                        }
                        
                        // Key Person Row (👥)
                        state.keyPerson?.let { person ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Key Person",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = person, style = metaStyle)
                            }
                        }
                        
                        // Highlights Row (✨)
                        state.highlights?.let { hl ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = "Highlights",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = hl, style = metaStyle.copy(color = AccentBlue))
                            }
                        }
                        
                        // Alarm Cascade Row (⏰)
                        state.alarmCascade?.let { cascade ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = "Alarm Cascade",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = cascade.joinToString(", "), style = metaStyle)
                            }
                        }
                        
                        // Notes Row (📝) - Extra, not in spec ASCII but useful
                        state.notes?.let { note ->
                             Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = "Notes",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = note, style = metaStyle)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Chat Input (Real UI)
                        if (!state.isDone) {
                            var inputText by remember { mutableStateOf("") }
                            var isRecordingMic by remember { mutableStateOf(false) }
                            val cardContext = LocalContext.current
                            val cardRecorder = remember { PhoneAudioRecorder(cardContext) }
                            
                            // 权限请求
                            val cardPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                            ) { granted ->
                                if (!granted) {
                                    android.widget.Toast.makeText(cardContext, "❌ 需要录音权限", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isRecordingMic) AccentDanger.copy(alpha = 0.15f)
                                        else BackgroundSurface.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(color = TextPrimary),
                                    decorationBox = { innerTextField ->
                                        if (inputText.isEmpty()) {
                                            Text(
                                                if (isRecordingMic) "🔴 松开发送..." else "调整时间...",
                                                color = if (isRecordingMic) AccentDanger else TextMuted,
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                if (inputText.isNotEmpty()) {
                                    // 文本模式 → 发送按钮
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = AccentBlue,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                onReschedule(inputText)
                                                inputText = ""
                                            }
                                    )
                                } else {
                                    // 录音模式 → 按住录音
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record",
                                        tint = if (isRecordingMic) AccentDanger else AccentBlue,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onPress = {
                                                        // 检查权限
                                                        val hasPermission = androidx.core.content.ContextCompat
                                                            .checkSelfPermission(
                                                                cardContext,
                                                                android.Manifest.permission.RECORD_AUDIO
                                                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                        
                                                        if (!hasPermission) {
                                                            cardPermissionLauncher.launch(
                                                                android.Manifest.permission.RECORD_AUDIO
                                                            )
                                                            return@detectTapGestures
                                                        }
                                                        
                                                        // 开始录音
                                                        cardRecorder.startRecording()
                                                        isRecordingMic = true
                                                        
                                                        val released = tryAwaitRelease()
                                                        
                                                        isRecordingMic = false
                                                        if (released) {
                                                            val wavFile = cardRecorder.stopRecording()
                                                            onMicRecord(wavFile)
                                                        } else {
                                                            cardRecorder.cancel()
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Processing Overlay
            if (state.processingStatus != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(BackgroundSurface.copy(alpha = 0.8f))
                        .zIndex(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = AccentBlue
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.processingStatus ?: "",
                            fontSize = 12.sp,
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Inspiration Card Component (Sleek Glass Version)
 */
@Composable
fun InspirationCard(
    state: TimelineItem.Inspiration,
    onAskAI: () -> Unit,
    onToggleSelection: () -> Unit = {}
) {
    // Purple Logic for Inspiration
    val isPurple = true
    val purpleColor = Color(0xFFAF52DE) // iOS Purple
    val purpleBackground = purpleColor.copy(alpha = 0.1f)

    PrismCard(
        onClick = {
             if (state.isSelectionMode) {
                onToggleSelection()
             }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = GlassCardShape
    ) {
        // Purple Tint Background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(purpleBackground)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox or Bulb Icon
            if (state.isSelectionMode) {
                 Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            if (state.isSelected) purpleColor else Color.Transparent, 
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp, 
                            if (state.isSelected) Color.Transparent else purpleColor, 
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = purpleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Title
            Text(
                text = state.title,
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            
            // Ask AI Button
            if (!state.isSelectionMode) {
                PrismButton(
                    text = "问AI",
                    onClick = onAskAI,
                    style = PrismButtonStyle.GHOST,
                    modifier = Modifier.height(32.dp),
                    enabled = true
                )
            }
        }
    }
}
