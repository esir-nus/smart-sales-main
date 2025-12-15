// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunOrderResultParser.kt
// 模块：:data:ai-core
// 说明：解析讯飞 orderResult 并生成用于 UI 展示的 Markdown 文本
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XfyunOrderResultParser @Inject constructor() {

    fun toTranscriptMarkdown(orderResult: String?): String {
        val raw = orderResult?.trim().orEmpty()
        if (raw.isBlank() || raw == "{}") {
            return "## 讯飞转写\n暂无内容"
        }
        val root = parseJsonObject(raw) ?: parseJsonObject(raw.replace("\\\\", "\\")) ?: return "## 讯飞转写\n解析失败"
        val lattice = root.getAsJsonArray("lattice") ?: root.getAsJsonArray("lattice2") ?: JsonArray()
        val segments = buildSegments(lattice)
        if (segments.isEmpty()) {
            return "## 讯飞转写\n暂无内容"
        }
        val hasRole = segments.any { !it.roleId.isNullOrBlank() }
        val header = "## 讯飞转写"
        return if (hasRole) {
            buildString {
                appendLine(header)
                segments.forEach { seg ->
                    val role = seg.roleId?.trim()?.takeIf { it.isNotBlank() } ?: "?"
                    val text = seg.text.trim()
                    if (text.isNotBlank()) {
                        appendLine("- 发言人 $role：$text")
                    }
                }
            }.trimEnd()
        } else {
            val text = segments.joinToString(separator = "") { it.text }.trim()
            if (text.isBlank()) "$header\n暂无内容" else "$header\n$text"
        }
    }

    private fun buildSegments(lattice: JsonArray): List<TranscriptSegment> {
        val result = mutableListOf<TranscriptSegment>()
        for (item in lattice) {
            val obj = item.asJsonObjectOrNull() ?: continue
            val jsonBest = obj.getPrimitiveString("json_1best") ?: continue
            val best = parseJsonObject(jsonBest) ?: parseJsonObject(jsonBest.replace("\\\\", "\\")) ?: continue
            val st = best.getAsJsonObject("st") ?: continue
            val roleId = st.getPrimitiveString("rl")
            val startMs = parseFirstLong(st.getPrimitiveString("bg"))
            val endMs = parseFirstLong(st.getPrimitiveString("ed"))
            val text = extractText(st)
            if (text.isNotBlank()) {
                result += TranscriptSegment(
                    roleId = roleId,
                    startMs = startMs,
                    endMs = endMs,
                    text = text
                )
            }
        }
        return result
    }

    private fun extractText(st: JsonObject): String {
        val rtArray = st.getAsJsonArray("rt") ?: return ""
        val sb = StringBuilder()
        for (rt in rtArray) {
            val rtObj = rt.asJsonObjectOrNull() ?: continue
            val wsArray = rtObj.getAsJsonArray("ws") ?: continue
            for (ws in wsArray) {
                val wsObj = ws.asJsonObjectOrNull() ?: continue
                val cwArray = wsObj.getAsJsonArray("cw") ?: continue
                val top = cwArray.firstOrNull()?.asJsonObjectOrNull() ?: continue
                val word = top.getPrimitiveString("w") ?: continue
                sb.append(word)
            }
        }
        return sb.toString().trim()
    }

    private fun parseJsonObject(raw: String): JsonObject? =
        runCatching { JsonParser.parseString(raw) }
            .getOrNull()
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject

    private fun parseFirstLong(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val match = NUMBER_REGEX.find(raw) ?: return null
        return match.value.toLongOrNull()
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (this.isJsonObject) this.asJsonObject else null

    private fun JsonObject.getPrimitiveString(key: String): String? {
        val element = this.get(key) ?: return null
        return if (element.isJsonPrimitive) element.asString else null
    }

    private data class TranscriptSegment(
        val roleId: String?,
        val startMs: Long?,
        val endMs: Long?,
        val text: String,
    )

    private companion object {
        private val NUMBER_REGEX = Regex("-?\\d+")
    }
}
