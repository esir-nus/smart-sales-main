package com.smartsales.prism.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.domain.model.ChatMessage
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.ui.components.*
import com.smartsales.prism.domain.activity.AgentActivity
import com.smartsales.prism.domain.activity.ThinkingPolicy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.SolidColor
import com.smartsales.prism.ui.theme.*

/**
 * Prism Chat Screen (Sleek Glass Version)
 * 
 * - Removes opaque background (relies on Shell)
 * - Glass Chat Header
 * - Glass Mode Toggle
 * - Aurora Home Hero
 */
@Composable
fun PrismChatScreen(
    viewModel: PrismViewModel = hiltViewModel(),
    onMenuClick: () -> Unit = {},
    onNewSessionClick: () -> Unit = {},
    onAudioBadgeClick: () -> Unit = {},
    onTingwuClick: () -> Unit = {},
    onArtifactsClick: () -> Unit = {}
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val history by viewModel.history.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val sessionTitle by viewModel.sessionTitle.collectAsState()
    val agentActivity by viewModel.agentActivity.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val taskBoardItems by viewModel.taskBoardItems.collectAsState()

    val context = LocalContext.current
    
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    /* 
     * Pro Max Layout Strategy:
     * - Everything floats in a Box (The "Void")
     * - Header: Absolute Top
     * - Content: Padding for Header/Dock
     * - Dock: Absolute Bottom
     * - FABs: Absolute Right center
     */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFAFAFC), // Top: Very light gray
                        Color(0xFFF5F0F8), // Middle: Soft lavender hint
                        Color(0xFFEDE5F2)  // Bottom: Subtle purple aurora
                    )
                )
            )
    ) {
        // --- Layer 1: Main Scrollable Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 80.dp) // Space for Floating Header
                .navigationBarsPadding()
                .padding(bottom = 140.dp) // Space for Dock
        ) {
            // Hero (Only if empty history)
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.TopCenter // Fixed: Top alignment
                ) {
                    HomeHero()
                }
            } else {
                // V2: Task Board for Analyst Mode — STICKY
                if (currentMode == Mode.ANALYST && taskBoardItems.isNotEmpty()) {
                    com.smartsales.prism.ui.analyst.TaskBoard(
                        items = taskBoardItems,
                        onItemClick = viewModel::selectTaskBoardItem
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Chat List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    reverseLayout = true
                ) {
                    // (Chat Content Logic - Same as before)
                    // 1. Agent Activity
                    agentActivity?.let { activity ->
                        item {
                            val maxLines = ThinkingPolicy.maxTraceLines(currentMode)
                            val autoCollapse = ThinkingPolicy.shouldAutoCollapse(currentMode)
                            AgentActivityBanner(activity, maxLines, autoCollapse)
                        }
                    }
                    
                    // Legacy Fallback
                    val currentUiState = uiState
                    if (agentActivity == null && currentUiState !is UiState.Idle && currentUiState !is UiState.Error) {
                        item {
                            when (currentUiState) {
                                is UiState.Response -> ResponseBubble(uiState = currentUiState)
                                is UiState.PlannerTableState -> com.smartsales.prism.ui.analyst.PlannerTableBubble(table = currentUiState.table)
                                else -> {}
                            }
                        }
                    }

                    // 2. Message History
                    items(history.reversed()) { message ->
                        when (message) {
                            is ChatMessage.User -> UserBubble(text = message.content)
                            is ChatMessage.Ai -> {
                                when (val state = message.uiState) {
                                    is UiState.Response -> ResponseBubble(uiState = state)
                                    is UiState.PlannerTableState -> com.smartsales.prism.ui.analyst.PlannerTableBubble(table = state.table)
                                    is UiState.Thinking -> {
                                         Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                                            Text("🧠", fontSize = 12.sp) 
                                            Spacer(Modifier.width(8.dp))
                                            Text(state.hint ?: "Thinking...", color = TextMuted, fontSize = 12.sp)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // --- Layer 2: Floating Header (5-Button Layout) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            ProMaxHeader(
                sessionTitle = sessionTitle,
                onMenuClick = onMenuClick,
                onNewSessionClick = onNewSessionClick,
                onDebugClick = viewModel::cycleDebugState,
                // Pass new analytics handler if needed, currently reusing logic inside header
            )
        }

        // --- Layer 3: Floating Right Toolbar (FABs) ---
        // Hidden when keyboard is open or if preferred, kept strict.
        // For now, assume always visible but pushed up by IME if needed.
        if (history.isEmpty()) {
             Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Documents (List -> Description/File)
                GlassFab(icon = Icons.AutoMirrored.Filled.TextSnippet, onClick = { /* Specs */ }) 
                // Cube (Inventory -> Science/ViewInAr)
                GlassFab(icon = Icons.Outlined.Science, onClick = onArtifactsClick) // Using Science as Cube proxy
            }
        }

        // --- Layer 4: Floating Dock (Switcher + Input) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding() // Float above keyboard
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode Switcher (Floating above Input)
                GlassModeSwitcher(
                    currentMode = currentMode,
                    onModeSwitch = viewModel::switchMode
                )
                
                // Input Bar (Glass Capsule)
                GlassInputCapsule(
                    text = inputText,
                    onTextChanged = viewModel::updateInput,
                    onSend = viewModel::send
                )
            }
        }
        
        // Error Toast
         errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
                containerColor = AccentDanger
            ) { Text(error, color = Color.White) }
        }
    }
}

// ===================================================================
// Pro Max Components (Local Implementation to match Prototype specs)
// ===================================================================

@Composable
private fun ProMaxHeader(
    sessionTitle: String,
    onMenuClick: () -> Unit,
    onNewSessionClick: () -> Unit,
    onDebugClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Group
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(icon = Icons.Filled.Menu, onClick = onMenuClick)
            // Analytics/Chart - Yellow Tint
             GlassCircleButton(
                 icon = Icons.AutoMirrored.Filled.ShowChart, 
                 onClick = { /* TODO */ },
                 tint = AccentYellow 
             )
        }

        // Center Title (Floating Badge)
        // Center Title (Floating Text - Assumption Removed)
        Box(
            modifier = Modifier.height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sessionTitle.ifEmpty { "New Session" },
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }

        // Right Group
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCircleButton(icon = Icons.Filled.BugReport, onClick = onDebugClick, tint = TextMuted)
            GlassCircleButton(icon = Icons.Filled.Add, onClick = onNewSessionClick)
        }
    }
}

@Composable
private fun HomeHero() {
    // Target Design: Just greeting text near TOP, NO center icon box
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp) // Near top
    ) {
        // Greeting only - no icon box
        Text(
            text = "✨ 上午好, Frank",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun GlassModeSwitcher(currentMode: Mode, onModeSwitch: (Mode) -> Unit) {
    PrismSurface(
        shape = CircleShape,
        backgroundColor = BackgroundSurface.copy(alpha = 0.9f),
        modifier = Modifier.height(36.dp) // Reduced from 44.dp to match target
    ) {
        val modes = listOf(Mode.COACH, Mode.ANALYST)
        
        BoxWithConstraints(modifier = Modifier.padding(4.dp)) {
            val width = maxWidth
            val segmentWidth = width / modes.size
            
            // Animated Indicator (White Pill)
            val indicatorOffset by animateDpAsState(
                targetValue = if (currentMode == Mode.COACH) 0.dp else segmentWidth,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "indicator"
            )

            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .padding(vertical = 2.dp) // Slight inset
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape)
            )

            // Text/Icon Layer
            Row(modifier = Modifier.fillMaxSize()) {
                modes.forEach { mode ->
                    val isSelected = mode == currentMode
                    // Web Spec: Coach=Purple, Analyst=Blue
                    val color = if (isSelected) {
                        if (mode == Mode.COACH) AccentTertiary else AccentBlue
                    } else TextSecondary
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null // Disable ripple as pill is the feedback
                            ) { onModeSwitch(mode) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (mode == Mode.COACH) Icons.Outlined.ChatBubble else Icons.Outlined.Science, // Or Icons.Outlined.CenterFocusStrong for Analyst
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (mode == Mode.COACH) "Coach" else "Analyst",
                            color = color,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassInputCapsule(text: String, onTextChanged: (String) -> Unit, onSend: () -> Unit) {
    PrismSurface(
        shape = CircleShape, // Fully rounded capsule
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plus Button
            IconButton(onClick = { /* Check specs */ }) {
                Icon(Icons.Filled.AttachFile, "Attach", tint = TextPrimary) // Web: Paperclip
            }
            
            // Input Field
            Box(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (text.isEmpty()) {
                     Text("输入消息...", color = TextMuted)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(AccentBlue)
                )
            }
            
            // Mic/Send Button
             Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black, CircleShape) // Web: Black Button
                    .clickable { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (text.isEmpty()) Icons.Filled.Mic else Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassCircleButton(
    icon: ImageVector, 
    onClick: () -> Unit, 
    tint: Color = TextPrimary
) {
    PrismSurface(
        shape = CircleShape,
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

@Composable
private fun GlassFab(icon: ImageVector, onClick: () -> Unit) {
     PrismSurface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
         contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
    }
}
