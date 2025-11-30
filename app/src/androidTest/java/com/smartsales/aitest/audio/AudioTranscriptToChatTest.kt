package com.smartsales.aitest.audio

// 文件：app/src/androidTest/java/com/smartsales/aitest/audio/AudioTranscriptToChatTest.kt
// 模块：:app
// 说明：验证 AudioFiles 转写完成后通过“用 AI 分析本次通话”创建通话会话并显示转写内容
// 作者：创建于 2025-11-28

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.aitest.AiFeatureTestActivity
import com.smartsales.aitest.AiFeatureTestTags
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.media.audio.AudioFilesTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@androidx.test.filters.LargeTest
class AudioTranscriptToChatTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()

    @Test
    fun transcriptFlow_pushesToHomeChat() {
        // 进入音频库
        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_HANDLE, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(AudioFilesTestTags.ROOT, useUnmergedTree = true).assertIsDisplayed()

        // 模拟已完成转写的录音并触发“用 AI 分析本次通话”
        composeRule.onAllNodesWithTag("${AudioFilesTestTags.TRANSCRIPT_BUTTON_PREFIX}d1", useUnmergedTree = true)
            .onFirst()
            .performClick()
        composeRule.onNodeWithText("用 AI 分析本次通话", substring = true, useUnmergedTree = true)
            .performScrollTo()
            .performClick()

        // 回到 Home，确认创建了通话分析会话并展示转写内容
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("已为你加载录音", substring = true, useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}
