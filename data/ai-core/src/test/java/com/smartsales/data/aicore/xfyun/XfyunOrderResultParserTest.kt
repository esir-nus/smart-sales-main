// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunOrderResultParserTest.kt
// 模块：:data:ai-core
// 说明：验证讯飞 orderResult 解析与 Markdown 产出
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import org.junit.Assert.assertTrue
import org.junit.Test

class XfyunOrderResultParserTest {

    @Test
    fun `parser builds diarized markdown when rl exists`() {
        val parser = XfyunOrderResultParser()
        val orderResult = """
            {
              "lattice": [
                {
                  "json_1best": "{\\"st\\":{\\"bg\\":\\"0\\",\\"ed\\":\\"1000\\",\\"rl\\":\\"1\\",\\"rt\\":[{\\"ws\\":[{\\"cw\\":[{\\"w\\":\\"你好\\",\\"wp\\":\\"n\\"}]}]}]}}"
                },
                {
                  "json_1best": "{\\"st\\":{\\"bg\\":\\"1000\\",\\"ed\\":\\"2000\\",\\"rl\\":\\"2\\",\\"rt\\":[{\\"ws\\":[{\\"cw\\":[{\\"w\\":\\"再见\\",\\"wp\\":\\"n\\"}]}]}]}}"
                }
              ]
            }
        """.trimIndent()

        val markdown = parser.toTranscriptMarkdown(orderResult)

        assertTrue(markdown.contains("## 讯飞转写"))
        assertTrue(markdown.contains("发言人 1"))
        assertTrue(markdown.contains("你好"))
        assertTrue(markdown.contains("发言人 2"))
        assertTrue(markdown.contains("再见"))
    }
}

