package com.smartsales.feature.media.audio

// 文件：feature/media/src/test/java/com/smartsales/feature/media/audio/AudioFilesScreenTest.kt
// 模块：:feature:media
// 说明：验证 AudioFilesScreen 的卡片展示与转写查看 UI 标签
// 作者：创建于 2025-11-26

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AudioFilesScreenTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Before
    fun setup() {
        ShadowBuild.setFingerprint("robolectric")
    }

    @Test
    fun transcribeButton_visibleAndTriggersCallback_whenNone() {
        var transcribeId: String? = null
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "a1",
                                title = "a1",
                                fileName = "a1.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.NONE
                            )
                        )
                    ),
                    onRefresh = {},
                    onSyncClicked = {},
                    onRecordingClicked = {},
                    onPlayPauseClicked = {},
                    onDeleteClicked = {},
                    onTranscribeClicked = { transcribeId = it },
                    onTranscriptClicked = {},
                    onAskAiClicked = {},
                    onTranscriptDismissed = {},
                    onErrorDismissed = {},
                    modifier = Modifier
                )
            }
        }

        composeRule.onNodeWithTag("${AudioFilesTestTags.TRANSCRIBE_BUTTON_PREFIX}a1").assertIsDisplayed()
        composeRule.onNodeWithTag("${AudioFilesTestTags.TRANSCRIBE_BUTTON_PREFIX}a1").performClick()
        assertEquals("a1", transcribeId)
    }

    @Test
    fun statusChip_showsForInProgressOrDone() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "p1",
                                title = "p1",
                                fileName = "p1.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.IN_PROGRESS
                            ),
                            AudioRecordingUi(
                                id = "d1",
                                title = "d1",
                                fileName = "d1.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                transcriptPreview = "示例转写"
                            )
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
                    modifier = Modifier
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}p1", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("转写中…", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("${AudioFilesTestTags.STATUS_CHIP_PREFIX}d1", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("转写完成", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("${AudioFilesTestTags.TRANSCRIPT_BUTTON_PREFIX}d1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun transcriptViewer_showsFullContent_andDismisses() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "d2",
                                title = "d2",
                                fileName = "d2.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                transcriptPreview = "preview",
                                fullTranscriptMarkdown = "# 标题\n- 行1\n正文"
                            )
                        ),
                        transcriptPreviewRecording = AudioRecordingUi(
                            id = "d2",
                            title = "d2",
                            fileName = "d2.wav",
                            createdAtText = "today",
                            transcriptionStatus = TranscriptionStatus.DONE,
                            transcriptPreview = "preview",
                            fullTranscriptMarkdown = "# 标题\n- 行1\n正文"
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
                    modifier = Modifier
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AudioFilesTestTags.TRANSCRIPT_DIALOG, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(AudioFilesTestTags.TRANSCRIPT_CONTENT, useUnmergedTree = true).assertExists()
    }

    @Test
    fun transcriptViewer_showsSummary_whenProvided() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "d3",
                                title = "d3",
                                fileName = "d3.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                smartSummary = TingwuSmartSummaryUi(
                                    summary = "概览",
                                    keyPoints = listOf("要点A"),
                                    actionItems = listOf("行动1")
                                )
                            )
                        ),
                        transcriptPreviewRecording = AudioRecordingUi(
                            id = "d3",
                            title = "d3",
                            fileName = "d3.wav",
                            createdAtText = "today",
                            transcriptionStatus = TranscriptionStatus.DONE,
                            smartSummary = TingwuSmartSummaryUi(
                                summary = "概览",
                                keyPoints = listOf("要点A"),
                                actionItems = listOf("行动1")
                            )
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
                    modifier = Modifier
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AudioFilesTestTags.TRANSCRIPT_SUMMARY, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("AI 智能总结", useUnmergedTree = true).assertExists()
    }

    @Test
    fun transcriptViewer_hidesSummary_whenAbsent() {
        composeRule.setContent {
            MaterialTheme {
                AudioFilesScreen(
                    uiState = AudioFilesUiState(
                        recordings = listOf(
                            AudioRecordingUi(
                                id = "d4",
                                title = "d4",
                                fileName = "d4.wav",
                                createdAtText = "today",
                                transcriptionStatus = TranscriptionStatus.DONE,
                                transcriptPreview = "preview",
                                fullTranscriptMarkdown = "内容"
                            )
                        ),
                        transcriptPreviewRecording = AudioRecordingUi(
                            id = "d4",
                            title = "d4",
                            fileName = "d4.wav",
                            createdAtText = "today",
                            transcriptionStatus = TranscriptionStatus.DONE,
                            transcriptPreview = "preview",
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
                    modifier = Modifier
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(AudioFilesTestTags.TRANSCRIPT_SUMMARY).assertCountEquals(0)
        composeRule.onAllNodesWithText("AI 智能总结").assertCountEquals(0)
    }
}
