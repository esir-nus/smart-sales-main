package com.smartsales.feature.chat.title

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/title/SessionTitleGenerator.kt
// 模块：:feature:chat
// 说明：根据首轮对话生成会话标题（MM/DD_主要对象_简短场景）
// 作者：创建于 2025-12-09

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionTitleGenerator {
    private val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val honorificPattern = Regex("([\\p{IsHan}]{1})(总|经理|老师|总监|老板)")
    private val companyPattern = Regex("给([\\p{IsHan}A-Za-z0-9]{2,})公司|为([\\p{IsHan}A-Za-z0-9]{2,})做|和([\\p{IsHan}A-Za-z0-9]{2,})[客户|伙伴|合作]")
    private val companyWritePattern = Regex("给([\\p{IsHan}A-Za-z0-9]{2,})写")
    private val summaryKeywords = listOf(
        "会议纪要" to "会议纪要",
        "纪要" to "会议纪要",
        "展会项目" to "展会项目",
        "展会" to "展会项目",
        "报价跟进邮件" to "报价跟进邮件",
        "报价跟进" to "报价跟进",
        "跟进邮件" to "跟进邮件",
        "报价" to "报价跟进",
        "邮件" to "跟进邮件",
        "异议" to "客户异议处理",
        "销售话术优化" to "销售话术优化",
        "话术" to "销售话术优化",
        "方案" to "方案优化",
        "总结" to "销售咨询",
        "咨询" to "销售咨询"
    )

    fun deriveSessionTitle(
        updatedAtMillis: Long,
        firstUserMessage: String,
        firstAssistantMessage: String?
    ): String {
        val datePart = dateFormatter.format(Date(updatedAtMillis))
        val majorName = extractMajorName(firstUserMessage) ?: extractMajorName(firstAssistantMessage ?: "")
        val summary = extractSummary(firstUserMessage) ?: extractSummary(firstAssistantMessage ?: "")
        val safeName = majorName ?: "未知客户"
        val safeSummary = summary ?: "销售咨询"
        return "${datePart}_${safeName}_${safeSummary}"
    }

    private fun extractMajorName(text: String): String? {
        if (text.isBlank()) return null
        honorificPattern.find(text)?.let { match ->
            val surname = match.groupValues[1]
            val honorific = match.groupValues[2]
            return "$surname$honorific"
        }
        companyWritePattern.find(text)?.let { match ->
            val candidate = match.groupValues.getOrNull(1)?.trim().orEmpty()
            if (candidate.isNotBlank()) return candidate
        }
        companyPattern.find(text)?.let { match ->
            for (i in 1..3) {
                val candidate = match.groupValues.getOrNull(i)?.trim().orEmpty()
                if (candidate.isNotBlank()) return candidate
            }
        }
        return null
    }

    private fun extractSummary(text: String): String? {
        if (text.isBlank()) return null
        summaryKeywords.forEach { (key, value) ->
            if (text.contains(key)) return value
        }
        return null
    }
}
