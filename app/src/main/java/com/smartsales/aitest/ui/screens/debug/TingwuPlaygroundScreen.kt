package com.smartsales.aitest.ui.screens.debug

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/debug/TingwuPlaygroundScreen.kt
// 模块：:app
// 说明：Tingwu 调试界面，可快速验证 FileUrl + 说话人分离开关效果
// 作者：创建于 2025-12-12

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.TingwuJobArtifacts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TingwuPlaygroundScreen(
    onBack: () -> Unit,
    viewModel: TingwuPlaygroundViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Tingwu Playground") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "快速验证 Tingwu 说话人分离", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.fileUrl,
                onValueChange = viewModel::updateFileUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "FileUrl") },
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "启用说话人分离", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "切换 Parameters.Transcription.DiarizationEnabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.diarizationEnabled,
                    onCheckedChange = viewModel::updateDiarizationEnabled
                )
            }
            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (state.isSubmitting) "提交中..." else "提交并开始轮询")
            }
            Text(
                text = "状态：${state.statusLabel}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            state.artifacts?.let { artifacts ->
                ArtifactSection(artifacts = artifacts)
            }
            if (state.diarizedSegments.isNotEmpty()) {
                DiarizedSegmentsSection(
                    speakerLabels = state.speakerLabels,
                    segments = state.diarizedSegments.take(10)
                )
            } else if (!state.transcriptMarkdown.isNullOrBlank()) {
                Text(
                    text = "无说话人分离数据，显示转写文本：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.transcriptMarkdown.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ArtifactSection(artifacts: TingwuJobArtifacts) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "结果链接", style = MaterialTheme.typography.titleSmall)
        listOfNotNull(
            "转写" to artifacts.transcriptionUrl,
            "章节" to artifacts.autoChaptersUrl,
            "CustomPrompt" to artifacts.customPromptUrl
        ).forEach { (label, url) ->
            if (!url.isNullOrBlank()) {
                Text(
                    text = "$label: $url",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DiarizedSegmentsSection(
    speakerLabels: Map<String, String>,
    segments: List<DiarizedSegment>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = "说话人标签", style = MaterialTheme.typography.titleSmall)
        if (speakerLabels.isEmpty()) {
            Text(
                text = "无标签",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            speakerLabels.forEach { (id, label) ->
                Text(
                    text = "$id -> $label",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Text(text = "分段预览（前10条）", style = MaterialTheme.typography.titleSmall)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            items(segments) { segment ->
                val label = speakerLabels[segment.speakerId] ?: (segment.speakerId ?: "未知")
                Text(
                    text = "[${segment.speakerId ?: "(无ID)"}] $label ${segment.startMs}ms-${segment.endMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = segment.text,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
