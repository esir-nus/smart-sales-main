package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.TranscriptBlock
import com.smartsales.domain.prism.core.tools.TingwuRunner

class FakeTingwuRunner : TingwuRunner {
    
    override suspend fun transcribe(audioPath: String): List<TranscriptBlock> {
        return listOf(
            TranscriptBlock(
                text = "这是模拟的转写结果。今天我们来讨论一下项目进展。",
                speakerId = "speaker_0",
                startMs = 0,
                endMs = 3000
            ),
            TranscriptBlock(
                text = "好的，我来汇报一下当前的情况。",
                speakerId = "speaker_1",
                startMs = 3000,
                endMs = 5500
            )
        )
    }
}
