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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.media.audio.AudioFilesScreen
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.media.audio.AudioFilesUiState
import com.smartsales.feature.media.audio.AudioRecordingUi
import com.smartsales.feature.media.audio.AudioTranscriptionStatus
import org.junit.Assert.assertEquals
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
                    uiState = AudioFilesUiState(recordings = emptyList()),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onApplyClicked = {},
                    onDeleteClicked = {},
                    onErrorDismissed = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.EMPTY_STATE).assertIsDisplayed()
    }

    @Test
    fun syncButton_showsProgressIndicator() {
        composeRule.setContent {
            val uiState = AudioFilesUiState(
                recordings = listOf(sampleRecording("audio-1")),
                isSyncing = true
            )
            MaterialTheme {
                AudioFilesScreen(
                    uiState = uiState,
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onApplyClicked = {},
                    onDeleteClicked = {},
                    onErrorDismissed = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNode(progressMatcher()).assertIsDisplayed()
    }

    @Test
    fun errorBanner_dismissHidesIt() {
        composeRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    AudioFilesUiState(
                        recordings = listOf(sampleRecording("error-1")),
                        errorMessage = "操作失败"
                    )
                )
            }
            MaterialTheme {
                AudioFilesScreen(
                    uiState = uiState,
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onApplyClicked = {},
                    onDeleteClicked = {},
                    onErrorDismissed = { uiState = uiState.copy(errorMessage = null) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onAllNodesWithTag(AudioFilesTestTags.ERROR_BANNER).assertCountEquals(1)
        composeRule.onNodeWithText("知道了").performClick()
        composeRule.onAllNodesWithTag(AudioFilesTestTags.ERROR_BANNER).assertCountEquals(0)
    }

    @Test
    fun nonAudioRecordings_notShown() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            sampleRecording("voice-1"),
                            sampleRecording("photo-1").copy(fileName = "photo-1.jpg")
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onApplyClicked = {},
                    onDeleteClicked = {},
                    onErrorDismissed = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onAllNodesWithText("photo-1.jpg").assertCountEquals(0)
        composeRule.onNodeWithText("voice-1.wav").assertIsDisplayed()
    }

    private fun sampleRecording(id: String): AudioRecordingUi =
        AudioRecordingUi(
            id = id,
            title = id,
            fileName = "$id.wav",
            createdAtText = "2025-11-21",
            transcriptionStatus = AudioTranscriptionStatus.None
        )

    private fun progressMatcher(): SemanticsMatcher =
        SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate
        )
}
