package com.smartsales.prism.ui.sim

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SimAudioDrawerContent(
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
    onDeleteAudio: (String) -> Unit,
    onSelectForChat: (SimChatAudioSelection) -> Unit,
    onImportTestAudio: () -> Unit,
    onSeedDebugFailureScenario: () -> Unit,
    onSeedDebugMissingSectionsScenario: () -> Unit,
    onSeedDebugFallbackScenario: () -> Unit,
    onReplayOnboarding: () -> Unit,
    showTestImportAction: Boolean,
    showDebugScenarioActions: Boolean
) {
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
                if (showDebugScenarioActions) {
                    SimDrawerHeaderIconAction(
                        onClick = onReplayOnboarding,
                        contentDescription = "重新开始设备引导"
                    )
                }
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
                onDelete = { onDeleteAudio(entry.item.id) },
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
                SimTestImportButton(onClick = onImportTestAudio)
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
private fun SimDrawerHeaderIconAction(
    onClick: () -> Unit,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
            .border(1.dp, SimDrawerDivider, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ViewCarousel,
            contentDescription = contentDescription,
            tint = SimDrawerTextMuted,
            modifier = Modifier.size(15.dp)
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
