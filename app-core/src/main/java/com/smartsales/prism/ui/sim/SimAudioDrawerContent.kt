package com.smartsales.prism.ui.sim

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smartsales.prism.data.audio.PhoneAudioRecorder

@Composable
internal fun SimAudioDrawerContent(
    isDrawerOpen: Boolean,
    entries: List<SimAudioEntry>,
    viewModel: SimAudioDrawerViewModel,
    mode: SimAudioDrawerMode,
    expandedAudioIds: Set<String>,
    currentChatAudioId: String?,
    isSyncing: Boolean,
    onSyncFromBadge: () -> Unit,
    onOpenConnectivity: () -> Unit,
    onArtifactOpened: (String, String) -> Unit,
    onAskAi: (SimAudioDiscussion) -> Unit,
    onSelectForChat: (SimChatAudioSelection) -> Unit,
    onImportTestAudio: () -> Unit,
    onSeedDebugFailureScenario: () -> Unit,
    onSeedDebugMissingSectionsScenario: () -> Unit,
    onSeedDebugFallbackScenario: () -> Unit,
    showTestImportAction: Boolean,
    showDebugScenarioActions: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recorder = remember { PhoneAudioRecorder(context) }
    var isRecordingTestAudio by remember { mutableStateOf(false) }

    fun cancelRecordingIfNeeded() {
        if (!isRecordingTestAudio) return
        runCatching { recorder.cancel() }
        isRecordingTestAudio = false
    }

    fun startRecording() {
        runCatching {
            recorder.startRecording()
            isRecordingTestAudio = true
        }.onFailure {
            Toast.makeText(context, it.message ?: "启动测试录音失败", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            Toast.makeText(context, "需要录音权限", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(isDrawerOpen, mode) {
        if (!isDrawerOpen || mode != SimAudioDrawerMode.BROWSE) {
            cancelRecordingIfNeeded()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                cancelRecordingIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cancelRecordingIfNeeded()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (mode == SimAudioDrawerMode.CHAT_RESELECT) "选择要讨论的录音" else "录音笔记",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            ),
            color = SimDrawerTextPrimary,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mode == SimAudioDrawerMode.BROWSE) {
                Text(
                    text = "${entries.size} 项",
                    color = SimDrawerTextMuted,
                    fontSize = 12.sp
                )
                SimDrawerHeaderAction(
                    text = "工牌连接",
                    onClick = onOpenConnectivity
                )
                SimDrawerHeaderAction(
                    text = if (isSyncing) "同步中..." else "同步徽章",
                    onClick = onSyncFromBadge,
                    enabled = !isSyncing
                )
            }
        }
    }

    if (mode == SimAudioDrawerMode.CHAT_RESELECT) {
        Text(
            text = "点击录音卡片切换当前聊天",
            color = SimDrawerTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 6.dp)
        )
    }

    HorizontalDivider(color = SimDrawerDivider)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        items(entries, key = { it.item.id }) { entry ->
            SimAudioCard(
                entry = entry,
                viewModel = viewModel,
                mode = mode,
                expanded = expandedAudioIds.contains(entry.item.id),
                currentChatAudioId = currentChatAudioId,
                onToggleExpanded = { viewModel.toggleExpanded(entry.item.id) },
                onToggleStar = { viewModel.toggleStar(entry.item.id) },
                onTranscribe = { viewModel.startTranscription(entry.item.id) },
                onArtifactOpened = onArtifactOpened,
                onAskAi = {
                    viewModel.createDiscussion(entry.item.id)?.let(onAskAi)
                },
                onSelectForChat = {
                    onSelectForChat(
                        SimChatAudioSelection(
                            audioId = entry.item.id,
                            title = entry.item.filename,
                            summary = entry.item.summary ?: entry.preview,
                            status = entry.item.status
                        )
                    )
                }
            )
        }

        if (showTestImportAction && mode == SimAudioDrawerMode.BROWSE) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SimTestRecordButton(
                        isRecording = isRecordingTestAudio,
                        onClick = {
                            if (isRecordingTestAudio) {
                                val recordedFile = runCatching { recorder.stopRecording() }.getOrNull()
                                isRecordingTestAudio = false
                                if (recordedFile == null) {
                                    Toast.makeText(
                                        context,
                                        "停止测试录音失败",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    viewModel.importRecordedTestAudio(recordedFile)
                                }
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    startRecording()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    )
                    SimTestImportButton(onClick = onImportTestAudio)
                }
            }
        }

        if (showDebugScenarioActions && mode == SimAudioDrawerMode.BROWSE) {
            item {
                SimDebugScenarioPanel(
                    onSeedFailureScenario = onSeedDebugFailureScenario,
                    onSeedMissingSectionsScenario = onSeedDebugMissingSectionsScenario,
                    onSeedFallbackScenario = onSeedDebugFallbackScenario
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SimDrawerHeaderAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
            .border(1.dp, SimDrawerDivider, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Text(
            text = text,
            color = SimDrawerTextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SimTestImportButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color.Transparent, RoundedCornerShape(12.dp))
            .border(1.dp, SimDrawerTextFaint, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "+",
                fontSize = 18.sp,
                color = SimDrawerTextPrimary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = "导入测试音频",
                fontSize = 13.sp,
                color = SimDrawerTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SimTestRecordButton(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isRecording) {
        Color(0xFFFF6B6B)
    } else {
        SimDrawerAccent.copy(alpha = 0.72f)
    }
    val backgroundColor = if (isRecording) {
        Color(0x26FF453A)
    } else {
        Color.White.copy(alpha = 0.02f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRecording) "■" else "●",
                fontSize = 15.sp,
                color = borderColor
            )
            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
            Text(
                text = if (isRecording) "停止测试录音" else "REC 测试录音",
                fontSize = 13.sp,
                color = SimDrawerTextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SimDebugScenarioPanel(
    onSeedFailureScenario: () -> Unit,
    onSeedMissingSectionsScenario: () -> Unit,
    onSeedFallbackScenario: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "调试验证场景",
            color = SimDrawerTextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
        Text(
            text = "仅调试版可见。点击后会在当前 SIM 录音列表中生成对应验证卡片。",
            color = SimDrawerTextMuted,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
        SimDebugScenarioButton(
            text = "Seed Failure Scenario",
            onClick = onSeedFailureScenario
        )
        SimDebugScenarioButton(
            text = "Seed Missing Sections Scenario",
            onClick = onSeedMissingSectionsScenario
        )
        SimDebugScenarioButton(
            text = "Seed Fallback Scenario",
            onClick = onSeedFallbackScenario
        )
    }
}

@Composable
private fun SimDebugScenarioButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, SimDrawerAccent.copy(alpha = 0.72f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = SimDrawerAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
