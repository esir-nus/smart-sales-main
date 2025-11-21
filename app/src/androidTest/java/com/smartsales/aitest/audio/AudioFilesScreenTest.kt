package com.smartsales.aitest.audio

// 文件：app/src/androidTest/java/com/smartsales/aitest/audio/AudioFilesScreenTest.kt
// 模块：:app
// 说明：验证 AudioFilesScreen 的空状态、同步按钮与错误提示行为
// 作者：创建于 2025-11-21

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.media.audiofiles.AudioFilesUiState
import com.smartsales.feature.media.audiofiles.AudioRecordingStatus
import com.smartsales.feature.media.audiofiles.AudioRecordingUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioFilesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyState_isVisible() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    state = AudioFilesUiState(recordings = emptyList()),
                    onBaseUrlChanged = {},
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun syncButton_showsProgressIndicator() {
        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    AudioFilesUiState(
                        recordings = listOf(
                            sampleRecording("audio-1")
                        )
                    )
                )
            }
            MaterialTheme {
                AudioFilesScreen(
                    state = uiState,
                    onBaseUrlChanged = {},
                    onRefresh = {},
                    onSyncClicked = { uiState = uiState.copy(isSyncing = true) },
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.SYNC_BUTTON).performClick()
        composeRule.onNode(progressMatcher()).assertIsDisplayed()
    }

    @Test
    fun errorBanner_dismissHidesIt() {
        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    AudioFilesUiState(
                        recordings = emptyList(),
                        errorMessage = "操作失败"
                    )
                )
            }
            MaterialTheme {
                AudioFilesScreen(
                    state = uiState,
                    onBaseUrlChanged = {},
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onDismissError = { uiState = uiState.copy(errorMessage = null) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.ERROR_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("知道了").performClick()
        composeRule.onNodeWithTag(AudioFilesTestTags.ERROR_BANNER).assertDoesNotExist()
    }

    private fun sampleRecording(id: String): AudioRecordingUi =
        AudioRecordingUi(
            id = id,
            fileName = "$id.wav",
            sizeText = "1.0MB",
            modifiedAtText = "2025-11-21 10:00",
            status = AudioRecordingStatus.Idle
        )

    private fun progressMatcher(): SemanticsMatcher =
        SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate
        )
}
