package com.smartsales.prism.data.real.session

import android.util.Log
import com.smartsales.prism.domain.session.SessionTitleGenerator
import com.smartsales.prism.domain.session.TitleResult
import org.json.JSONObject
import javax.inject.Inject

/**
 * 语义增强的会话标题生成器 — 基于 Input Parser 提取的 JSON
 * 
 * @see docs/cerb/input-parser/spec.md Wave 4
 * 不再重复调用 LLM，直接复用已解析的 `temporal_intent`。
 */
class SemanticSessionTitleGenerator @Inject constructor() : SessionTitleGenerator {

    companion object {
        private const val TAG = "SemanticTitleGen"
    }

    override fun generateTitle(rawParsedJson: String, resolvedNames: List<String>): TitleResult {
        Log.d(TAG, "Generating title synchronously from JSON and names: $resolvedNames")
        
        // 1. 尝试从 JSON 中提取意图
        var intentStr = "新会话"
        if (rawParsedJson.isNotBlank()) {
            try {
                // 简单的容错处理，应对包含 markdown 标记的情况
                val jsonStart = rawParsedJson.indexOf("{")
                val jsonEnd = rawParsedJson.lastIndexOf("}")
                if (jsonStart != -1 && jsonEnd != -1) {
                    val cleanJson = rawParsedJson.substring(jsonStart, jsonEnd + 1)
                    val jsonObj = JSONObject(cleanJson)
                    val temporalIntent = jsonObj.optString("temporal_intent", "")
                    if (temporalIntent.isNotBlank()) {
                        // 截断到最大 12 个字符 (稍微宽松点给意图)
                        intentStr = if (temporalIntent.length > 12) {
                            temporalIntent.substring(0, 11) + "…"
                        } else {
                            temporalIntent
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse json for title generation", e)
            }
        }

        // 2. 结合解析到的实体名称
        val clientLabel = if (resolvedNames.isNotEmpty()) {
            resolvedNames.joinToString("、").take(8) // 限制长度
        } else {
            "客户"
        }

        return TitleResult(clientName = clientLabel, summary = intentStr)
    }
}
