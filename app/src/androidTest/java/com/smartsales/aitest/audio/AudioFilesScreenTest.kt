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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import com.smartsales.feature.media.audio.AudioFilesScreen
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.media.audio.AudioFilesUiState
import com.smartsales.feature.media.audio.AudioRecordingUi
import com.smartsales.feature.media.audio.TranscriptionStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioFilesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun statusChips_andActions_matchNewCopy() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            sampleRecording("none"),
                            sampleRecording("progress").copy(transcriptionStatus = TranscriptionStatus.IN_PROGRESS),
                            sampleRecording("done").copy(transcriptionStatus = TranscriptionStatus.DONE, transcriptPreview = "预览"),
                            sampleRecording("error").copy(transcriptionStatus = TranscriptionStatus.ERROR)
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag("${AudioFilesTestTags.TRANSCRIBE_BUTTON_PREFIX}none").assertIsDisplayed()
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("转写"))
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("转写中…"))
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("等待转写完成"))
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("查看转写"))
        composeRule.onNodeWithTag(AudioFilesTestTags.RECORDING_LIST)
            .performScrollToNode(hasText("转写失败，重试"))
        val inProgress =
            composeRule.onAllNodesWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}progress", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        val done =
            composeRule.onAllNodesWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}done", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        assertTrue(inProgress && done)
    }

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
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
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
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
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
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = { uiState = uiState.copy(errorMessage = null) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onAllNodesWithTag(AudioFilesTestTags.ERROR_BANNER).assertCountEquals(1)
        composeRule.onNodeWithText("收起提示").performClick()
        composeRule.onAllNodesWithTag(AudioFilesTestTags.ERROR_BANNER).assertCountEquals(0)
    }

    @Test
    fun transcriptSheet_showsAskAiOnlyWhenDone() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            sampleRecording("done").copy(
                                transcriptionStatus = TranscriptionStatus.DONE,
                                fullTranscriptMarkdown = "内容"
                            )
                        ),
                        transcriptPreviewRecording = sampleRecording("done").copy(
                            transcriptionStatus = TranscriptionStatus.DONE,
                            fullTranscriptMarkdown = "内容"
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onNodeWithTag(AudioFilesTestTags.ASK_AI_BUTTON).assertIsDisplayed()
    }

    @Test
    fun transcriptSheet_hidesAskAiWhenNotDone() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            sampleRecording("progress").copy(
                                transcriptionStatus = TranscriptionStatus.IN_PROGRESS,
                                fullTranscriptMarkdown = "内容"
                            )
                        ),
                        transcriptPreviewRecording = sampleRecording("progress").copy(
                            transcriptionStatus = TranscriptionStatus.IN_PROGRESS,
                            fullTranscriptMarkdown = "内容"
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = {},
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeRule.onAllNodesWithTag(AudioFilesTestTags.ASK_AI_BUTTON).assertCountEquals(0)
    }

    private fun sampleRecording(id: String): AudioRecordingUi =
        AudioRecordingUi(
            id = id,
            title = id,
            fileName = "$id.wav",
            createdAtText = "2025-11-21",
            transcriptionStatus = TranscriptionStatus.NONE
        )

    private fun progressMatcher(): SemanticsMatcher =
        SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate
        )
}
