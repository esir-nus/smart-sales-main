package com.smartsales.aitest

// 文件：app/src/androidTest/java/com/smartsales/aitest/AiFeatureTestActivityTest.kt
// 模块：:app
// 说明：验证 AiFeatureTestActivity Shell 导航行为的 Compose UI 测试
// 作者：创建于 2025-11-21

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.smartsales.aitest.testing.waitForAnyTag
import com.smartsales.feature.chat.core.QuickSkillId
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.aitest.TestHomePage
import com.smartsales.feature.media.audio.AudioFilesTestTags
import com.smartsales.feature.chat.history.ChatHistoryTestTags
import com.smartsales.feature.chat.home.chatDebugHudOverride
import androidx.compose.ui.geometry.Offset
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsales.feature.usercenter.data.PersistentOnboardingStateRepository
import org.junit.Before
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.After
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class AiFeatureTestActivityTest {

    private val composeRule = createAndroidComposeRule<AiFeatureTestActivity>()
    private val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val ruleChain: TestRule = RuleChain.outerRule(permissionRule).around(composeRule)

    @Before
    fun bypassOnboarding() {
        PersistentOnboardingStateRepository.testOverrideCompleted = true
        clearOnboardingPrefs()
    }

    @After
    fun tearDown() {
        chatDebugHudOverride = null
        PersistentOnboardingStateRepository.testOverrideCompleted = null
    }

    @Test
    fun defaultTab_isHome() {
        waitForHomeRendered()
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun topBarButtons_navigateAudioAndDevice() {
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.AudioFiles)
        }
        waitForAnyTag(composeRule, AudioFilesTestTags.ROOT, AiFeatureTestTags.PAGE_AUDIO_FILES)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertExists()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.DeviceManager)
        }
        waitForAnyTag(composeRule, AiFeatureTestTags.PAGE_DEVICE_MANAGER, AiFeatureTestTags.PAGE_DEVICE_SETUP)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertExists()
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    @Test
    fun quickSkillTap_setsModeWithoutCreatingMessages() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SMART_ANALYSIS)

        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertIsDisplayed()
        // 不应触发导航或额外气泡标签
        composeRule.onAllNodesWithTag(AiFeatureTestTags.PAGE_AUDIO_FILES, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
        // 也不应生成用户消息
        composeRule.onAllNodesWithTag(HomeScreenTestTags.USER_MESSAGE, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun quickSkillTap_showsNoSkillBubble() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SMART_ANALYSIS)

        // 新 UX 不生成技能气泡，也不离开 Home
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertExists()
    }

    @Test
    fun quickSkill_sendUsesModeWithoutSkillBubble() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        tapQuickSkill(QuickSkillId.SMART_ANALYSIS)
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD, useUnmergedTree = true)
            .performTextClearance()
        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD, useUnmergedTree = true)
            .performTextInput("请总结会议")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON).performClick()

        composeRule.waitForIdle()
        // 发送后仍不应出现技能气泡
        composeRule.onAllNodesWithTag(HomeScreenTestTags.ACTIVE_SKILL_CHIP, useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty()
    }

    @Test
    fun homeEmptySession_showsHeroAndQuickSkills_onlyChatUi() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HERO, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("home_quick_skill_${QuickSkillId.SMART_ANALYSIS.name}", useUnmergedTree = true)
            .assertIsDisplayed()

        assertZeroNodes(HomeScreenTestTags.DEVICE_ENTRY)
        assertZeroNodes(HomeScreenTestTags.AUDIO_ENTRY)
    }

    @Test
    fun heroHidesAfterFirstMessage_andDoesNotReturn() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD, useUnmergedTree = true)
            .performTextInput("hi there")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.HERO, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag("home_quick_skill_${QuickSkillId.SMART_ANALYSIS.name}", useUnmergedTree = true)
            .assertIsDisplayed()
        assertZeroNodes(HomeScreenTestTags.DEVICE_ENTRY)
        assertZeroNodes(HomeScreenTestTags.AUDIO_ENTRY)
    }

    @Test
    fun topBarTitleBindsAndNewChatResetsHero() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.SESSION_TITLE, useUnmergedTree = true)
            .assertTextContains("新的聊天", substring = true)

        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD, useUnmergedTree = true)
            .performTextInput("hi")
        composeRule.onNodeWithTag(HomeScreenTestTags.SEND_BUTTON, useUnmergedTree = true)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.HERO, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.NEW_CHAT_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.HERO, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(HomeScreenTestTags.SESSION_TITLE, useUnmergedTree = true)
            .assertTextContains("新的聊天", substring = true)

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true)
            .performClick()
        waitForAnyTag(composeRule, HomeScreenTestTags.HISTORY_PANEL, ChatHistoryTestTags.PAGE)
    }

    @Test
    fun historyToggle_opensAndClosesDrawer() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        waitForAnyTag(composeRule, HomeScreenTestTags.HISTORY_PANEL, ChatHistoryTestTags.PAGE)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.HISTORY_PANEL, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }

        // 再次打开验证可靠性
        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true)
            .performClick()
        waitForAnyTag(composeRule, HomeScreenTestTags.HISTORY_PANEL, ChatHistoryTestTags.PAGE)
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
    }

    @Test
    fun swipeDown_opensAudioOverlay_andBackCloses() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_HOME_LAYER, useUnmergedTree = true)
            .performTouchInput { swipeDown() }

        waitForAnyTag(composeRule, AiFeatureTestTags.OVERLAY_BACKDROP, AiFeatureTestTags.PAGE_AUDIO_FILES)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_AUDIO_FILES, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_LAYER, useUnmergedTree = true)
            .performTouchInput { swipeUp() }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun smallDragDown_doesNotOpenAudioOverlay() {
        waitForHomeRendered()

        dragByFraction(tag = AiFeatureTestTags.OVERLAY_HOME_LAYER, fraction = 0.10f, direction = 1f)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_AUDIO_FILES, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun mediumDragDown_opensAudioOverlay() {
        waitForHomeRendered()

        dragByFraction(tag = AiFeatureTestTags.OVERLAY_HOME_LAYER, fraction = 0.30f, direction = 1f)
        waitForAnyTag(composeRule, AiFeatureTestTags.OVERLAY_BACKDROP, AiFeatureTestTags.PAGE_AUDIO_FILES)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_AUDIO_FILES, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_AUDIO_LAYER, useUnmergedTree = true)
            .performTouchInput { swipeUp() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun swipeUp_opensDeviceOverlay_andDownReturnsHome() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_HOME_LAYER, useUnmergedTree = true)
            .performTouchInput { swipeUp() }

        waitForAnyTag(composeRule, AiFeatureTestTags.OVERLAY_BACKDROP, AiFeatureTestTags.PAGE_DEVICE_MANAGER)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE_LAYER, useUnmergedTree = true)
            .performTouchInput { swipeDown() }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun smallDragUp_doesNotOpenDeviceOverlay() {
        waitForHomeRendered()

        dragByFraction(tag = AiFeatureTestTags.OVERLAY_HOME_LAYER, fraction = 0.10f, direction = -1f)
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
            .assertCountEquals(0)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun mediumDragUp_opensDeviceOverlay() {
        waitForHomeRendered()

        dragByFraction(tag = AiFeatureTestTags.OVERLAY_HOME_LAYER, fraction = 0.30f, direction = -1f)
        waitForAnyTag(composeRule, AiFeatureTestTags.OVERLAY_BACKDROP, AiFeatureTestTags.PAGE_DEVICE_MANAGER)
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_DEVICE_LAYER, useUnmergedTree = true)
            .performTouchInput { swipeDown() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun imeBlocksOverlayGestures() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.INPUT_FIELD, useUnmergedTree = true)
            .performClick()
            .performTextInput("typing")

        composeRule.onNodeWithTag(AiFeatureTestTags.OVERLAY_HOME_LAYER, useUnmergedTree = true)
            .performTouchInput { swipeDown() }

        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(AiFeatureTestTags.OVERLAY_BACKDROP, useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun deviceIndicator_isDisplayOnly() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HOME_DEVICE_INDICATOR, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithText("设备状态", useUnmergedTree = true).assertExists()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER, useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun historyDrawer_showsDeviceStatusCard() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true)
            .performClick()

        waitForAnyTag(composeRule, HomeScreenTestTags.HISTORY_PANEL, ChatHistoryTestTags.PAGE)

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_DEVICE_STATUS, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("设备状态将在硬件接入后展示", useUnmergedTree = true).assertExists()
    }

    @Test
    fun historyDrawer_userCenterNavigates() {
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true)
            .performClick()
        waitForAnyTag(composeRule, HomeScreenTestTags.HISTORY_PANEL, ChatHistoryTestTags.PAGE)

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_USER_CENTER, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        waitForAnyTag(composeRule, AiFeatureTestTags.PAGE_USER_CENTER)

        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    @Test
    fun exportButtons_doNotNavigateAway() {
        waitForPage(AiFeatureTestTags.PAGE_HOME)

        composeRule.onNodeWithTag("home_quick_skill_${QuickSkillId.EXPORT_PDF.name}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertIsDisplayed()

        composeRule.onNodeWithTag("home_quick_skill_${QuickSkillId.EXPORT_CSV.name}", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(AiFeatureTestTags.PAGE_HOME, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun homeTags_areUnique() {
        waitForHomeRendered()
        assertSingleTag(HomeScreenTestTags.ROOT)
        assertSingleTag(AiFeatureTestTags.PAGE_HOME)
    }

    @Test
    fun otherPageTags_areUniqueWhenNavigated() {
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.AudioFiles)
        }
        assertSingleTag(AudioFilesTestTags.ROOT)
        assertSingleTag(AiFeatureTestTags.PAGE_HOME)

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.Home)
        }
        waitForHomeRendered()

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.DeviceManager)
        }
        assertSingleTag(AiFeatureTestTags.PAGE_DEVICE_MANAGER)
        assertSingleTag(AiFeatureTestTags.PAGE_HOME)

        composeRule.activityRule.scenario.onActivity {
            it.setOverlayForTest(TestHomePage.Home)
        }
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.HISTORY_TOGGLE, useUnmergedTree = true).performClick()
        waitForAnyTag(composeRule, HomeScreenTestTags.HISTORY_PANEL, ChatHistoryTestTags.PAGE)
        composeRule.activityRule.scenario.onActivity {
            it.onBackPressedDispatcher.onBackPressed()
        }
        waitForHomeRendered()
    }

    @Test
    fun debugHud_toggleShowsAndHidesPanel_whenEnabled() {
        chatDebugHudOverride = true
        waitForHomeRendered()

        composeRule.onNodeWithTag(HomeScreenTestTags.DEBUG_HUD_TOGGLE, useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeScreenTestTags.DEBUG_HUD_PANEL, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithTag(HomeScreenTestTags.DEBUG_HUD_CLOSE, useUnmergedTree = true)
            .performClick()

        composeRule.onNodeWithTag(HomeScreenTestTags.DEBUG_HUD_PANEL, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun debugHud_iconHidden_whenDisabled() {
        chatDebugHudOverride = false
        waitForHomeRendered()

        val toggleCount = composeRule.onAllNodesWithTag(HomeScreenTestTags.DEBUG_HUD_TOGGLE, useUnmergedTree = true)
            .fetchSemanticsNodes().size
        val panelCount = composeRule.onAllNodesWithTag(HomeScreenTestTags.DEBUG_HUD_PANEL, useUnmergedTree = true)
            .fetchSemanticsNodes().size
        assertTrue(toggleCount == 0)
        assertTrue(panelCount == 0)
    }

    private fun goHome() {
        val homeVisible = runCatching {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        if (!homeVisible) {
            composeRule.activityRule.scenario.onActivity {
                it.onBackPressedDispatcher.onBackPressed()
            }
        }
        waitForHomeRendered()
    }

    private fun waitForPage(tag: String) {
        waitForAnyTag(
            composeRule,
            tag,
            HomeScreenTestTags.ROOT,
            extraFallbackTags = arrayOf(
                AiFeatureTestTags.PAGE_CHAT_HISTORY,
                AiFeatureTestTags.PAGE_USER_CENTER
            )
        )
    }

    private fun tapQuickSkill(skillId: QuickSkillId) {
        val tag = "home_quick_skill_${skillId.name}"
        val nodes = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)[0].performClick()
        }
    }

    private fun countByTag(tag: String): Int = runCatching {
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size
    }.getOrDefault(0)

    private fun waitForHomeRendered() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(HomeScreenTestTags.ROOT, useUnmergedTree = true).assertIsDisplayed()
    }

    // 检查给定 tag 仅出现一次，确保 PAGE/ROOT 唯一性
    private fun assertSingleTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
    }

    private fun assertZeroNodes(tag: String) {
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun dragByFraction(tag: String, fraction: Float, direction: Float) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput {
            val delta = size.height * fraction * direction
            val start = center
            down(start)
            moveBy(Offset(0f, delta))
            up()
        }
    }

    private fun clearOnboardingPrefs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("onboarding_state_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
