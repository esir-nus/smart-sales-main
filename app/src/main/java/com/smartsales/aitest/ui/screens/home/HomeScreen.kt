package com.smartsales.aitest.ui.screens.home

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/home/HomeScreen.kt
// 模块：:app
// 说明：底部导航 Home 页聊天体验，接入 feature/chat 的真实聊天 ViewModel
// 作者：创建于 2025-12-02

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.aitest.ui.components.ChatMessageBubble
import com.smartsales.aitest.ui.components.SkillChips
import com.smartsales.aitest.ui.screens.home.model.SkillSuggestion
import com.smartsales.aitest.ui.screens.home.model.toSkillSuggestion
import com.smartsales.aitest.ui.screens.home.model.toUiMessages
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.HomeScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeScreenViewModel,
    onNavigateToHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val messages = uiState.chatMessages.toUiMessages()
    val skills: List<SkillSuggestion> = uiState.quickSkills.map { it.toSkillSuggestion() }
    val emptySkillOrder = listOf(
        QuickSkillId.SUMMARIZE_LAST_MEETING,
        QuickSkillId.PREP_NEXT_MEETING,
        QuickSkillId.WRITE_FOLLOWUP_EMAIL
    )
    val emptySkills: List<SkillSuggestion> = uiState.quickSkills
        .filter { it.id in emptySkillOrder }
        .sortedBy { emptySkillOrder.indexOf(it.id) }
        .map { quick ->
            val customLabel = when (quick.id) {
                QuickSkillId.SUMMARIZE_LAST_MEETING -> "智能分析"
                QuickSkillId.PREP_NEXT_MEETING -> "生成 PDF"
                QuickSkillId.WRITE_FOLLOWUP_EMAIL -> "生成 CSV"
                else -> quick.label
            }
            quick.toSkillSuggestion().copy(label = customLabel)
        }
    val isLoading = uiState.isStreaming || uiState.isSending
    val isEmptySession = uiState.chatMessages.isEmpty()
    val inputEnabled = !uiState.isInputBusy && !uiState.isBusy
    val canSend = uiState.inputText.isNotBlank() && !uiState.isBusy
    var uploadMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onAudioFilePicked(it) }
    }
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onAudioFilePicked(it) }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImagePicked(it) }
    }

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "新对话") },
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag(HomeScreenTestTags.HISTORY_TOGGLE)
                    ) {
                        Icon(Icons.Filled.History, contentDescription = "历史记录")
                    }
                    IconButton(onClick = { viewModel.onNewChatClicked() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "新建对话")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Column {
                    uiState.chatErrorMessage?.let { error ->
                        ErrorBanner(
                            message = error,
                            onDismiss = { viewModel.onDismissError() }
                        )
                    }
                    ChatInputArea(
                        inputText = uiState.inputText,
                        onInputChange = viewModel::onInputChanged,
                        onSend = {
                            if (canSend) {
                                viewModel.onSendMessage()
                            }
                        },
                        inputEnabled = inputEnabled,
                        canSend = canSend,
                        onUploadClick = { uploadMenuExpanded = true },
                        uploadMenuExpanded = uploadMenuExpanded,
                        onUploadMenuDismiss = { uploadMenuExpanded = false },
                        onUploadFile = {
                            uploadMenuExpanded = false
                            filePicker.launch(arrayOf("*/*"))
                        },
                        onUploadAudio = {
                            uploadMenuExpanded = false
                            audioPicker.launch(arrayOf("audio/*"))
                        },
                        onUploadImage = {
                            uploadMenuExpanded = false
                            imagePicker.launch(arrayOf("image/*"))
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f))
                .testTag("page_home")
        ) {
            if (isEmptySession) {
                EmptyConversationContent(
                    userName = uiState.userName,
                    skills = emptySkills,
                    onSkillClick = { skill -> viewModel.onSelectQuickSkill(skill.id) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageBubble(message = message)
                    }
                    if (isLoading) {
                        item(key = "typing") {
                            TypingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    inputEnabled: Boolean,
    canSend: Boolean,
    onUploadClick: () -> Unit,
    uploadMenuExpanded: Boolean,
    onUploadMenuDismiss: () -> Unit,
    onUploadFile: () -> Unit,
    onUploadAudio: () -> Unit,
    onUploadImage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(
                onClick = onUploadClick,
                enabled = inputEnabled
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "上传或附件")
            }
            DropdownMenu(
                expanded = uploadMenuExpanded,
                onDismissRequest = onUploadMenuDismiss
            ) {
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    text = { Text(text = "上传文件") },
                    onClick = onUploadFile
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.AudioFile, contentDescription = null) },
                    text = { Text(text = "上传音频") },
                    onClick = onUploadAudio
                )
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                    text = { Text(text = "上传图片") },
                    onClick = onUploadImage
                )
            }
        }
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            maxLines = 4,
            enabled = inputEnabled,
            placeholder = { Text(text = "上传文件或输入消息...") }
        )
        FilledIconButton(
            onClick = onSend,
            enabled = canSend
        ) {
            Icon(Icons.Filled.Send, contentDescription = "发送")
        }
    }
}

@Composable
private fun TypingIndicator() {
    Text(
        text = "正在思考...",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭错误"
                )
            }
        }
    }
}

@Composable
private fun EmptyConversationContent(
    userName: String,
    skills: List<SkillSuggestion>,
    onSkillClick: (SkillSuggestion) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        EmptyHeroBlock(userName = userName)
        CapabilitiesBlock()
        Text(
            text = "让我们开始吧",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp)
        )
        SkillChips(
            skills = skills,
            onSkillClick = onSkillClick,
            modifier = Modifier.fillMaxWidth()
        )
        Divider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun EmptyHeroBlock(
    userName: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "LOGO",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "你好, $userName",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "我是您的销售助手",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CapabilitiesBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "我可以帮您：",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "• 分析用户画像、意图、痛点。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "• 生成 PDF、CSV 文档及思维导图。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
