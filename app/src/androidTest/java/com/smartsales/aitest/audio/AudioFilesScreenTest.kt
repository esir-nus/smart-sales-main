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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.feature.media.audiofiles.AudioOrigin
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
                    onDelete = {},
                    onTranscribe = {},
                    onUploadClick = {},
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
                    onDelete = {},
                    onTranscribe = {},
                    onUploadClick = {},
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
                    onDelete = {},
                    onTranscribe = {},
                    onUploadClick = {},
                    onDismissError = { uiState = uiState.copy(errorMessage = null) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onAllNodesWithTag(AudioFilesTestTags.ERROR_BANNER).assertCountEquals(1)
        composeRule.onNodeWithText("知道了").performClick()
        composeRule.onAllNodesWithTag(AudioFilesTestTags.ERROR_BANNER).assertCountEquals(0)
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
                    onDelete = {},
                    onTranscribe = {},
                    onUploadClick = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.DEVICE_STATUS).assertIsDisplayed()
        composeRule.onAllNodesWithText("媒体服务器地址").assertCountEquals(0)
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
                    onDelete = {},
                    onTranscribe = {},
                    onUploadClick = {},
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
                    onDelete = {},
                    onTranscribe = {},
                    onUploadClick = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onAllNodesWithText("photo-1.jpg").assertCountEquals(0)
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
                    onDelete = {},
                    onTranscribe = { transcribedId = it },
                    onUploadClick = {},
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
            status = AudioRecordingStatus.Idle,
            origin = AudioOrigin.DEVICE
        )

    @Test
    fun originLabels_renderForDeviceAndPhone() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    state = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "d1",
                                fileName = "dev.wav",
                                sizeText = "1MB",
                                modifiedAtText = "2025-11-21",
                                status = AudioRecordingStatus.Idle,
                                origin = AudioOrigin.DEVICE
                            ),
                            AudioRecordingUi(
                                id = "p1",
                                fileName = "phone.wav",
                                sizeText = "1MB",
                                modifiedAtText = "2025-11-21",
                                status = AudioRecordingStatus.Idle,
                                origin = AudioOrigin.PHONE
                            )
                        ),
                        baseUrl = "http://192.168.0.8:8000"
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onPlayPause = {},
                    onDelete = {},
                    onTranscribe = {},
                    onUploadClick = {},
                    onDismissError = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithText("来源：设备").assertIsDisplayed()
        composeRule.onNodeWithText("来源：手机").assertIsDisplayed()
    }

    private fun progressMatcher(): SemanticsMatcher =
        SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate
        )
}
