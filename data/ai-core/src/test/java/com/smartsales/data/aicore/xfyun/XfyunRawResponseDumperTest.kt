// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunRawResponseDumperTest.kt
// 模块：:data:ai-core
// 说明：验证 XFyun raw 响应落盘与保留最近 5 份的清理逻辑
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XfyunRawResponseDumperTest {

    @Test
    fun dumpRawXfyunResponse_writesVerbatimAndLeavesNoTmp() {
        val dir = Files.createTempDirectory("xfyun_raw_dump_test").toFile().apply { deleteOnExit() }
        val dumper = XfyunRawResponseDumper(
            directoryProvider = object : XfyunRawDumpDirectoryProvider {
                override fun directory(): File = dir
            }
        )

        val raw = "{\"code\":\"000000\",\"descInfo\":\"success\",\"content\":{\"orderInfo\":{\"orderId\":\"o1\"}}}"
        val info = dumper.dumpRawXfyunResponse(orderId = "o1", rawJson = raw)

        val dumped = File(info.filePath)
        assertTrue(dumped.exists())
        assertTrue(dumped.name.startsWith("xfyun_o1_"))
        assertTrue(dumped.name.endsWith(".raw.json"))
        assertEquals(raw, dumped.readText(Charsets.UTF_8))
        assertEquals(dumped.length(), info.bytes)
        assertEquals(info.savedAtMillis, dumped.lastModified())

        val tmpLeft = dir.listFiles().orEmpty().any { it.name.endsWith(".tmp") }
        assertFalse(tmpLeft)
    }

    @Test
    fun dumpRawXfyunResponse_prunesToNewestFiveAndIgnoresNonMatchingFiles() {
        val dir = Files.createTempDirectory("xfyun_raw_dump_prune").toFile().apply { deleteOnExit() }
        val dumper = XfyunRawResponseDumper(
            directoryProvider = object : XfyunRawDumpDirectoryProvider {
                override fun directory(): File = dir
            }
        )

        // 非匹配文件：不应被删除
        val keepOther = File(dir, "not_a_dump.json").apply {
            writeText("x", Charsets.UTF_8)
            setLastModified(1L)
        }

        // 先造 6 个符合命名规则的旧文件，lastModified 越大越新
        val base = 1_000_000L
        val created = (0 until 6).map { index ->
            File(dir, "xfyun_order_${index}.raw.json").apply {
                writeText("old-$index", Charsets.UTF_8)
                setLastModified(base + index)
            }
        }

        // 触发一次真实 dump，让 prune 逻辑跑起来
        dumper.dumpRawXfyunResponse(orderId = "latest", rawJson = "{\"ok\":true}")

        val remainingDumps = dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith("xfyun_") && it.name.endsWith(".raw.json") }
            .sortedByDescending { it.lastModified() }

        assertEquals(5, remainingDumps.size)
        assertTrue(keepOther.exists())

        // 旧的那批中至少应有 2 个被清理（因为新增 1 个 dump 后总数 >= 7）
        val oldRemaining = remainingDumps.count { it.name.startsWith("xfyun_order_") }
        assertTrue(oldRemaining <= 4)

        // newest 5 中应该包含 lastModified 最大的那个旧文件（base+5）除非它被顶掉；至少要保证最旧的(base+0)被删掉
        assertFalse(created.first().exists())
    }
}

