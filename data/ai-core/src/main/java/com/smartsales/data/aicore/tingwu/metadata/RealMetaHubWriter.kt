package com.smartsales.data.aicore.tingwu.metadata

import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.ChapterMeta
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.metahub.TranscriptSource
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuMetadataInput
import com.smartsales.data.aicore.TingwuSessionMetadataMapper
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.metahub.TingwuPreprocessPatchBuilder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MetaHubWriter"

/**
 * RealMetaHubWriter: Production implementation of MetaHubWriter.
 *
 * Extracted from TingwuRunner to create a pure Lattice box.
 */
@Singleton
class RealMetaHubWriter @Inject constructor(
    private val metaHub: MetaHub,
    private val tingwuTraceStore: TingwuTraceStore
) : MetaHubWriter {

    override suspend fun writeSessionMetadata(input: SessionMetadataWriteInput): Result<Unit> =
        runCatching {
            val smartSummary = input.artifacts?.smartSummary
            val allChapters = (input.artifacts?.chapters ?: input.chapters).orEmpty()
            val chapterTitle = allChapters.firstOrNull()?.title

            val summary = smartSummary?.summary
                ?: input.transcriptMeta?.shortSummary
                ?: chapterTitle
            val titleHint = chapterTitle ?: input.transcriptMeta?.summaryTitle6Chars
            val mainPerson = input.transcriptMeta?.mainPerson
            val tags = buildSet {
                smartSummary?.keyPoints?.forEach { add(it) }
                smartSummary?.actionItems?.forEach { add(it) }
            }.filter { it.isNotBlank() }.toSet().takeIf { it.isNotEmpty() }

            val metadataInput = TingwuMetadataInput(
                sessionId = input.sessionId,
                callSummary = summary,
                shortTitleHint = titleHint,
                mainPersonName = mainPerson,
                tags = tags,
                completedAt = System.currentTimeMillis()
            )

            val patch = TingwuSessionMetadataMapper.toMetadataPatch(metadataInput)
                ?: return Result.success(Unit)

            val patchWithSource = patch.copy(
                latestMajorAnalysisSource = AnalysisSource.TINGWU,
                latestMajorAnalysisAt = metadataInput.completedAt,
                lastUpdatedAt = maxOf(patch.lastUpdatedAt, metadataInput.completedAt)
            )

            val merged = runCatching { metaHub.getSession(input.sessionId) }.getOrNull()
                ?.mergeWith(patchWithSource) ?: patchWithSource

            runCatching {
                metaHub.upsertSession(merged)
            }.onFailure {
                AiCoreLogger.w(TAG, "Tingwu 元数据写入 MetaHub 失败：${it.message}")
            }

            // V1 §6.2: Write chapters to M2B TranscriptMetadata with source pointers
            if (allChapters.isNotEmpty()) {
                val chapterMetas = allChapters.mapIndexed { index, tingwuChapter ->
                    ChapterMeta(
                        title = tingwuChapter.displayTitle(),
                        startMs = tingwuChapter.startMs,
                        endMs = tingwuChapter.endMs ?: tingwuChapter.startMs,
                        summary = tingwuChapter.summary,
                        audioAssetId = input.audioAssetId,
                        recordingSessionId = input.sessionId,
                        chapterId = "${input.jobId}_ch${index + 1}"
                    )
                }
                val transcriptUpdate = TranscriptMetadata(
                    transcriptId = input.jobId,
                    sessionId = input.sessionId,
                    source = TranscriptSource.TINGWU,
                    shortSummary = summary,
                    summaryTitle6Chars = titleHint,
                    mainPerson = mainPerson,
                    chapters = chapterMetas
                )
                runCatching {
                    metaHub.upsertTranscript(transcriptUpdate)
                }.onFailure {
                    AiCoreLogger.w(TAG, "Tingwu M2B 章节写入 MetaHub 失败：${it.message}")
                }
            }
        }

    override suspend fun writePreprocessPatch(input: PreprocessPatchInput): Result<Unit> =
        runCatching {
            val createdAt = System.currentTimeMillis()
            val traceSnapshot = tingwuTraceStore.getSnapshot()
                .takeIf { it.lastTaskId == input.jobId }

            val patch = TingwuPreprocessPatchBuilder.build(
                sessionId = input.sessionId,
                jobId = input.jobId,
                transcriptMarkdown = input.transcriptMarkdown,
                createdAt = createdAt,
                traceBatchPlan = traceSnapshot?.batchPlan,
                traceSuspiciousBoundaries = traceSnapshot?.suspiciousBoundaries
            )
            runCatching {
                metaHub.appendM2Patch(input.sessionId, patch)
            }.onFailure {
                AiCoreLogger.w(TAG, "Tingwu 预处理补丁写入 MetaHub 失败：${it.message}")
            }
        }
}
