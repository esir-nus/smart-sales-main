package com.smartsales.aitest.onboarding

// 文件：app/src/main/java/com/smartsales/aitest/onboarding/OnboardingScreens.kt
// 模块：:app
// 说明：首屏欢迎与个人信息表单
// 作者：创建于 2025-12-10

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

object OnboardingTestTags {
    const val WELCOME = "onboarding_welcome"
    const val BUTTON_START = "onboarding_button_start"
    const val PERSONAL = "onboarding_personal"
    const val FIELD_NAME = "onboarding_field_name"
    const val FIELD_ROLE = "onboarding_field_role"
    const val FIELD_INDUSTRY = "onboarding_field_industry"
    const val FIELD_MAIN_CHANNEL = "onboarding_field_main_channel"
    const val FIELD_EXPERIENCE = "onboarding_field_experience"
    const val FIELD_STYLE = "onboarding_field_style"
    const val BUTTON_SAVE = "onboarding_button_save"
    const val ERROR = "onboarding_error"
}

@Composable
fun OnboardingWelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag(OnboardingTestTags.WELCOME),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "欢迎使用 SmartSales 助手",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "帮你总结对话、生成报告，并快速导出 PDF/CSV。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onStart,
                modifier = Modifier.testTag(OnboardingTestTags.BUTTON_START)
            ) {
                Text(text = "开始使用")
            }
        }
    }
}

@Composable
fun OnboardingPersonalInfoScreen(
    state: OnboardingUiState,
    onDisplayNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onMainChannelChange: (String) -> Unit,
    onExperienceLevelChange: (String) -> Unit,
    onStylePreferenceChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag(OnboardingTestTags.PERSONAL),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "完善个人信息",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "我们将使用这些信息为你生成更贴合的问候与导出文件。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("姓名") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.FIELD_NAME)
            )
            OutlinedTextField(
                value = state.role,
                onValueChange = onRoleChange,
                label = { Text("职位/角色") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.FIELD_ROLE)
            )
            OutlinedTextField(
                value = state.industry,
                onValueChange = onIndustryChange,
                label = { Text("行业") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.FIELD_INDUSTRY)
            )
            OutlinedTextField(
                value = state.mainChannel,
                onValueChange = onMainChannelChange,
                label = { Text("主要沟通渠道") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.FIELD_MAIN_CHANNEL)
            )
            OutlinedTextField(
                value = state.experienceLevel,
                onValueChange = onExperienceLevelChange,
                label = { Text("经验水平") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.FIELD_EXPERIENCE)
            )
            OutlinedTextField(
                value = state.stylePreference,
                onValueChange = onStylePreferenceChange,
                label = { Text("表达风格偏好") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.FIELD_STYLE)
            )
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(OnboardingTestTags.ERROR)
                )
            }
            OutlinedButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(OnboardingTestTags.BUTTON_SAVE)
            ) {
                Text(text = if (state.isSaving) "保存中…" else "保存并进入首页")
            }
        }
    }
}
