package com.smartsales.aitest.testing

// 文件：app/src/androidTest/java/com/smartsales/aitest/testing/TestWaiter.kt
// 模块：:app
// 说明：Compose UI 测试等待工具，提供通用的 Tag 轮询方法
// 作者：创建于 2025-11-27

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import org.junit.rules.TestRule

fun <R : TestRule, A : ComponentActivity> waitForAnyTag(
    composeRule: AndroidComposeTestRule<R, A>,
    vararg tags: String,
    extraFallbackTags: Array<String> = emptyArray(),
    timeoutMillis: Long = 15_000,
    useUnmergedTree: Boolean = true
) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    val allTags = buildList {
        addAll(tags)
        addAll(extraFallbackTags)
    }
    while (System.currentTimeMillis() < deadline) {
        val idleOk = runCatching { composeRule.waitForIdle() }.isSuccess
        if (!idleOk) {
            Thread.sleep(200)
            continue
        }
        val found = allTags.any { tag ->
            runCatching {
                composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isNotEmpty() ||
                    composeRule.onAllNodesWithTag(tag, useUnmergedTree = !useUnmergedTree).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        if (found) return
        Thread.sleep(200)
    }
    throw AssertionError("Tags ${allTags.joinToString()} not found within timeout")
}
