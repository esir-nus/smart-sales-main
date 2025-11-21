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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.media.audiofiles.AudioFilesUiState
import com.smartsales.feature.media.audiofiles.AudioRecordingStatus
import com.smartsales.feature.media.audiofiles.AudioRecordingUi
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
                    state = AudioFilesUiState(recordings = emptyList()),
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onTranscribe = {},
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
                        ),
                        baseUrl = "http://192.168.0.5:8000"
                    )
                )
            }
            MaterialTheme {
                AudioFilesScreen(
                    state = uiState,
                    onRefresh = {},
                    onSyncClicked = { uiState = uiState.copy(isSyncing = true) },
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onTranscribe = {},
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
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onTranscribe = {},
                    onDismissError = { uiState = uiState.copy(errorMessage = null) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.ERROR_BANNER).assertIsDisplayed()
        composeRule.onNodeWithText("知道了").performClick()
        composeRule.onNodeWithTag(AudioFilesTestTags.ERROR_BANNER).assertDoesNotExist()
    }

    @Test
    fun deviceStatus_showsDisconnectedWhenNoEndpoint() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    state = AudioFilesUiState(baseUrl = null, recordings = emptyList()),
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onTranscribe = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.DEVICE_STATUS).assertIsDisplayed()
        composeRule.onNodeWithText("媒体服务器地址").assertDoesNotExist()
    }

    @Test
    fun syncButton_disabledWithoutEndpoint() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    state = AudioFilesUiState(baseUrl = null),
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onTranscribe = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.SYNC_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun nonAudioRecordings_notShown() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    state = AudioFilesUiState(
                        recordings = listOf(
                            sampleRecording("voice-1"),
                            sampleRecording("photo-1").copy(fileName = "photo-1.jpg")
                        ),
                        baseUrl = "http://192.168.0.6:8000"
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onTranscribe = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithText("photo-1.jpg").assertDoesNotExist()
        composeRule.onNodeWithText("voice-1.wav").assertIsDisplayed()
    }

    @Test
    fun transcribeButton_invokesCallback() {
        var transcribedId: String? = null
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    state = AudioFilesUiState(
                        recordings = listOf(sampleRecording("voice-2")),
                        baseUrl = "http://192.168.0.7:8000"
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onApply = {},
                    onDelete = {},
                    onTranscribe = { transcribedId = it },
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.transcribe("voice-2")).performClick()
        assertEquals("voice-2", transcribedId)
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
