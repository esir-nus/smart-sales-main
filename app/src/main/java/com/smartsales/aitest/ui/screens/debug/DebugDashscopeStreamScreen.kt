// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/debug/DebugDashscopeStreamScreen.kt
// 模块：:app
// 说明：DashScope streaming 调试界面，便于对比全量前缀与增量 chunk
// 作者：创建于 2025-12-11
package com.smartsales.aitest.ui.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashscopeStreamScreen(
    onBack: () -> Unit,
    viewModel: DebugStreamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "DashScope 流式调试") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(text = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.prompt,
                onValueChange = viewModel::updatePrompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text(text = "调试提示（发送给 DashScope）") }
            )
            ModeSelector(
                current = uiState.mode,
                onSelected = viewModel::updateMode
            )
            Button(
                onClick = viewModel::startStreaming,
                enabled = !uiState.isStreaming,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (uiState.isStreaming) "Streaming..." else "开始流式请求")
            }
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            AggregatedCard(text = uiState.aggregatedText)
            LogCard(logs = uiState.rawLogs)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ModeSelector(
    current: DebugStreamMode,
    onSelected: (DebugStreamMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "模式选择",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeRadio(
                label = DebugStreamMode.FullSequence.label,
                selected = current == DebugStreamMode.FullSequence,
                onClick = { onSelected(DebugStreamMode.FullSequence) }
            )
            ModeRadio(
                label = DebugStreamMode.Incremental.label,
                selected = current == DebugStreamMode.Incremental,
                onClick = { onSelected(DebugStreamMode.Incremental) }
            )
        }
        Text(
            text = "全量前缀模式：Chunk 即当前完整文本；增量模式：Chunk 为追加段，需要拼接。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun ModeRadio(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = if (selected) "● $label" else "○ $label",
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AggregatedCard(text: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "聚合后的可见文本",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (text.isBlank()) "暂无输出" else text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LogCard(logs: List<String>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "原始 chunk 日志",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (logs.isEmpty()) {
                Text(
                    text = "暂无日志",
                    color = MaterialTheme.colorScheme.outline
                )
            } else {
                logs.forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        textAlign = TextAlign.Start
                    )
                    if (index != logs.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
