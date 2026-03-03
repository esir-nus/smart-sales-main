package com.smartsales.prism.ui.drawers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.drawers.audio.AudioViewModel
import com.smartsales.prism.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Audio Drawer — Bottom-Up Sheet (Sleek Glass Version)
 * @see prism-ui-ux-contract.md §1.8
 */
import com.smartsales.prism.domain.model.Mode

@Composable
fun AudioDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onNavigateToChat: (sessionId: String, targetMode: Mode?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioViewModel = hiltViewModel()
) {
    val audioItems by viewModel.audioItems.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Scrim
        androidx.compose.animation.AnimatedVisibility(
            visible = isOpen,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackdropScrim)
                    .clickable { onDismiss() }
            )
        }

        // Drawer Content (Glass Sheet)
        androidx.compose.animation.AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            PrismSurface(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.95f) // 95% height
                    .pointerInput(Unit) {
                        awaitPointerEventScope { while (true) { awaitPointerEvent() } }
                    },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                backgroundColor = BackgroundSurface.copy(alpha = 0.95f), // Slightly more opaque for sheet
                elevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Drag Handle Pill
                    var accumulatedDrag by remember { mutableStateOf(0f) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragEnd = { accumulatedDrag = 0f },
                                    onDragCancel = { accumulatedDrag = 0f }
                                ) { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount
                                    // Drag DOWN to close (since Drawer is at the bottom, dragging down means positive Y)
                                    if (accumulatedDrag > 50) {
                                        onDismiss()
                                        accumulatedDrag = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        )
                    }

                    // 2. Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "录音文件",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )
                        
                        // Sync Status Pill (Glass Green)
                        Surface(
                            color = AccentSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, AccentSecondary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Smartphone,
                                    contentDescription = null,
                                    tint = AccentSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SmartBadge 已同步",
                                    color = AccentSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 3. List
                    var expandedCardId by remember { mutableStateOf<String?>(null) }
                    var showRenameDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
                    val scope = rememberCoroutineScope()

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        if (uri != null) {
                            // We need a ViewModel method to handle the URI. Let's assume it exists or we will create it.
                            viewModel.uploadLocalAudio(uri)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(items = audioItems, key = { it.id }) { item ->
                            val isExpanded = expandedCardId == item.id
                            
                            AudioCard(
                                item = item,
                                isExpanded = isExpanded,
                                viewModel = viewModel,
                                onClick = { expandedCardId = if (isExpanded) null else item.id },
                                onStarClick = { viewModel.toggleStar(item.id) },
                                onAskAi = { id -> 
                                    scope.launch {
                                        val (sessionId, isNew) = viewModel.onAskAi(id)
                                        if (isNew) {
                                            onNavigateToChat(sessionId, Mode.ANALYST)
                                        } else {
                                            onNavigateToChat(sessionId, null)
                                        }
                                    }
                                },
                                onTranscribe = { id -> viewModel.startTranscription(id) },
                                onDelete = { id -> viewModel.deleteAudio(id) },
                                onRename = { id -> showRenameDialog = id to item.filename }
                            )
                        }
                        
                        if (com.smartsales.prism.BuildConfig.DEBUG) {
                            item { 
                                UploadButton(onClick = { launcher.launch("audio/*") }) 
                            }
                        }
                        item { Spacer(modifier = Modifier.height(30.dp)) }
                    }
                    
                    // Dialog
                    if (showRenameDialog != null) {
                        val (_, currentName) = showRenameDialog!!
                        var newName by remember { mutableStateOf(currentName) }
                        
                        AlertDialog(
                            onDismissRequest = { showRenameDialog = null },
                            title = { Text("重命名录音") },
                            text = {
                                OutlinedTextField(
                                    value = newName,
                                    onValueChange = { newName = it },
                                    label = { Text("文件名") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { showRenameDialog = null }) { Text("确认") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
                            },
                            containerColor = BackgroundSurface,
                            titleContentColor = TextPrimary,
                            textContentColor = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadButton(onClick: () -> Unit) {
    val stroke = Stroke(
        width = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color.LightGray,
                    style = stroke,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())
                )
            }
            .background(Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+",
                fontSize = 18.sp,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "上传本地音频 (测试专用)",
                fontSize = 14.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
