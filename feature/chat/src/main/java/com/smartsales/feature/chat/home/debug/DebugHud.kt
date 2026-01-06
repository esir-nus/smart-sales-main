// File: feature/chat/src/main/java/com/smartsales/feature/chat/home/debug/DebugHud.kt
// Module: :feature:chat
// Summary: Debug HUD components extracted from HomeScreen
// Author: created on 2026-01-06

package com.smartsales.feature.chat.home.debug

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.smartsales.data.aicore.debug.TingwuTraceSnapshot
import com.smartsales.data.aicore.debug.XfyunTraceSnapshot
import com.smartsales.feature.chat.BuildConfig
import com.smartsales.feature.chat.home.CHAT_DEBUG_HUD_ENABLED
import com.smartsales.feature.chat.home.HomeScreenTestTags
import java.io.File

@Composable
internal fun DebugSessionMetadataHud(
    snapshot: com.smartsales.data.aicore.debug.DebugSnapshot?,
    xfyunTrace: XfyunTraceSnapshot?,
    tingwuTrace: TingwuTraceSnapshot?,
    onRefreshXfyun: () -> Unit,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    showRawAssistantOutput: Boolean,
    onToggleRawAssistantOutput: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenHalfHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
    val scrollState = rememberScrollState()
    val section1Text = snapshot?.section1EffectiveRunText ?: "加载中..."
    val section2Text = snapshot?.section2RawTranscriptionText ?: "加载中..."
    val section3Text = snapshot?.section3PreprocessedText ?: "加载中..."
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = screenHalfHeight)
            .testTag(HomeScreenTestTags.DEBUG_HUD_PANEL),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "调试信息",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag(HomeScreenTestTags.DEBUG_HUD_CLOSE)
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "关闭 HUD")
                }
                }
            }
        if (BuildConfig.DEBUG) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "显示原始助手回复（仅调试）",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "切换后气泡显示原始模型文本，不影响元数据或正式体验。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showRawAssistantOutput,
                        onCheckedChange = onToggleRawAssistantOutput
                    )
                }
            }
            // 重要：HUD 内容由 Orchestrator 生成，UI 只渲染字符串，不做二次拼装。
            DebugSnapshotBlock(
                title = "Section 1 — Effective Run Snapshot",
                content = section1Text,
                onCopy = onCopy,
                testTag = HomeScreenTestTags.DEBUG_HUD_COPY,
            )
            DebugSnapshotBlock(
                title = "Section 2 — Raw Transcription Output",
                content = section2Text,
                onCopy = onCopy,
                testTag = null,
            )
            DebugSnapshotBlock(
                title = "Section 3 — Preprocessed Snapshot",
                content = section3Text,
                onCopy = onCopy,
                testTag = null,
            )

            if (CHAT_DEBUG_HUD_ENABLED) {
                TingwuTraceSection(
                    trace = tingwuTrace,
                )
                XfyunTraceSection(
                    trace = xfyunTrace,
                    onRefresh = onRefreshXfyun,
                )
            }
        }
    }
}

@Composable
private fun DebugSnapshotBlock(
    title: String,
    content: String,
    onCopy: (String) -> Unit,
    testTag: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = { onCopy(content) },
                enabled = content.isNotBlank(),
                modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
            ) {
                Text(text = "复制")
            }
        }
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(10.dp),
        )
    }
}

@Composable
private fun TingwuTraceSection(
    trace: TingwuTraceSnapshot?,
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Tingwu 调试",
            style = MaterialTheme.typography.titleMedium
        )
        if (trace == null) {
            Text(
                text = "暂无 Tingwu 调用记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }
        val rawDumpPath = trace.transcriptionDumpPath
        val rawFile = rawDumpPath?.let { path -> File(path) }?.takeIf { it.exists() && it.isFile }
        val rawStatus = if (rawFile == null) "missing" else "available"
        Text(
            text = "raw.json: $rawStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                val file = rawFile ?: return@TextButton
                runCatching {
                    // 重要：调试导出使用 FileProvider，避免复制大文本或内联展示原始内容。
                    val authority = "${context.packageName}.chatfileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, "导出 ${file.name}").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }.onFailure { error ->
                    Toast.makeText(context, error.message ?: "导出失败", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = rawFile != null,
        ) {
            Text(text = "导出 Raw JSON")
        }
        val transcriptPath = trace.transcriptDumpPath
        val transcriptFile = transcriptPath?.let { path -> File(path) }?.takeIf { it.exists() && it.isFile }
        val transcriptStatus = if (transcriptFile == null) "missing" else "available"
        Text(
            text = "transcript.txt: $transcriptStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = {
                val file = transcriptFile ?: return@TextButton
                runCatching {
                    // 重要：调试导出使用 FileProvider，避免复制大文本或内联展示原始内容。
                    val authority = "${context.packageName}.chatfileprovider"
                    val uri = FileProvider.getUriForFile(context, authority, file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(intent, "导出 ${file.name}").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                }.onFailure { error ->
                    Toast.makeText(context, error.message ?: "导出失败", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = transcriptFile != null,
        ) {
            Text(text = "导出 Transcript")
        }
    }
}

@Composable
private fun XfyunTraceSection(
    trace: XfyunTraceSnapshot?,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    var confirmShare by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "XFyun 调试",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRefresh) { Text(text = "刷新") }
                if (trace?.rawDumpPath?.isNotBlank() == true) {
                    TextButton(onClick = { confirmShare = true }) {
                        Text(text = "分享 dump")
                    }
                }
            }
        }
        if (trace == null) {
            Text(
                text = "暂无 XFyun 调用记录",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val rawDumpPath = trace.rawDumpPath
        val rawFile = rawDumpPath?.let { path -> File(path) }?.takeIf { it.exists() && it.isFile }
        val rawStatus = if (rawFile == null) "missing" else "available"
        Text(
            text = "raw.json: $rawStatus",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        trace.orderId?.takeIf { it.isNotBlank() }?.let { orderId ->
            Text(
                text = "orderId: ${orderId.take(64)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (trace != null && confirmShare) {
        AlertDialog(
            onDismissRequest = { confirmShare = false },
            title = { Text(text = "分享 XFyun 原始返回") },
            text = {
                Text(text = "将导出并分享 XFyun 原始返回 JSON，可能包含转写文本。是否继续？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmShare = false
                        val path = trace.rawDumpPath
                        if (path.isNullOrBlank()) {
                            Toast.makeText(context, "暂无可分享的 dump 文件", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        val file = File(path)
                        if (!file.exists() || !file.isFile) {
                            Toast.makeText(context, "dump 文件不存在，无法分享", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        runCatching {
                            // 重要：
                            // - 使用 FileProvider 生成 content:// Uri，避免 file:// 在新系统上被拦截崩溃。
                            // - 授予临时读权限，便于微信等第三方 App 读取文件内容。
                            val authority = "${context.packageName}.chatfileprovider"
                            val uri = FileProvider.getUriForFile(context, authority, file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(intent, "分享 ${file.name}").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                        }.onFailure { throwable ->
                            Toast.makeText(context, throwable.message ?: "分享失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text(text = "继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmShare = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}
