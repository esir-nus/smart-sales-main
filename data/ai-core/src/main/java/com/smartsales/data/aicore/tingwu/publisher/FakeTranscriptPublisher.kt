// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/publisher/FakeTranscriptPublisher.kt
// Module: :data:ai-core
// Summary: Fake implementation of Publisher for testing (Lattice pattern)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.publisher

import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuSmartSummary

/**
 * FakeTranscriptPublisher: Test double for Publisher.
 */
class FakeTranscriptPublisher : Publisher {
    
    var stubTranscriptionUrl: String? = null
    var stubAutoChaptersUrl: String? = null
    var stubSmartSummaryUrl: String? = null
    var stubChapters: List<TingwuChapter>? = null
    var stubSmartSummary: TingwuSmartSummary? = null
    
    val extractCalls = mutableListOf<String>()
    val downloadCalls = mutableListOf<String>()
    
    override fun extractTranscriptionUrl(resultLinks: Map<String, String>?): String? {
        extractCalls.add("transcription")
        return stubTranscriptionUrl ?: resultLinks?.get("Transcription")
    }
    
    override fun extractAutoChaptersUrl(resultLinks: Map<String, String>?): String? {
        extractCalls.add("autoChapters")
        return stubAutoChaptersUrl ?: resultLinks?.get("AutoChapters")
    }
    
    override fun extractSmartSummaryUrl(resultLinks: Map<String, String>?): String? {
        extractCalls.add("smartSummary")
        return stubSmartSummaryUrl ?: resultLinks?.get("MeetingAssistance")
    }
    
    override fun fetchChaptersSafe(url: String, jobId: String): List<TingwuChapter>? {
        downloadCalls.add("chapters:$jobId")
        return stubChapters
    }
    
    override fun downloadChapters(url: String, jobId: String): List<TingwuChapter> {
        downloadCalls.add("chapters:$jobId")
        return stubChapters ?: emptyList()
    }
    
    override fun fetchSmartSummarySafe(resultLinks: Map<String, String>?, jobId: String): TingwuSmartSummary? {
        downloadCalls.add("smartSummary:$jobId")
        return stubSmartSummary
    }
    
    override fun downloadSmartSummary(url: String, jobId: String): TingwuSmartSummary? {
        downloadCalls.add("smartSummary:$jobId")
        return stubSmartSummary
    }
    
    fun reset() {
        stubTranscriptionUrl = null
        stubAutoChaptersUrl = null
        stubSmartSummaryUrl = null
        stubChapters = null
        stubSmartSummary = null
        extractCalls.clear()
        downloadCalls.clear()
    }
}
