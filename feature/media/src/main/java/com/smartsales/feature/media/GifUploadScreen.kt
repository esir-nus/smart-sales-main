package com.smartsales.feature.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

// 文件：feature/media/src/main/java/com/smartsales/feature/media/GifUploadScreen.kt
// 模块：:feature:media
// 说明：GIF上传到徽章的Compose UI
// 作者：创建于 2026-01-10

/**
 * Screen for uploading GIF/image to ESP32 badge.
 * 
 * States (per ux-experience.md):
 * - idle: "发送到徽章" button
 * - preparing: "处理中..." spinner
 * - connecting: "正在连接徽章..." spinner
 * - uploading: Progress bar with "正在上传 {n}/{total}..."
 * - complete: Success toast
 * - error: Error card with retry
 */
@Composable
fun GifUploadScreen(
    viewModel: GifUploadViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else "image.gif"
            } ?: "image.gif"
            viewModel.selectImage(it, fileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // File selection
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.selectedFileName ?: "未选择图片",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (uiState.selectedFileName != null) {
                        Text(
                            text = "点击更换",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = { imagePicker.launch("image/*") }) {
                    Text(if (uiState.selectedFileName == null) "选择" else "更换")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status / Progress
        AnimatedVisibility(visible = uiState.step != GifUploadStep.Idle && uiState.step != GifUploadStep.Error) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState.step) {
                    GifUploadStep.Complete -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.statusMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Progress bar for upload
                if (uiState.step == GifUploadStep.Uploading && uiState.total > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progress.toFloat() / uiState.total },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Error state
        AnimatedVisibility(visible = uiState.step == GifUploadStep.Error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.errorMessage ?: "上传失败",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重试",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action button
        Button(
            onClick = { viewModel.startUpload() },
            enabled = uiState.isActionEnabled && uiState.step == GifUploadStep.Idle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("发送到徽章")
        }
    }
}
