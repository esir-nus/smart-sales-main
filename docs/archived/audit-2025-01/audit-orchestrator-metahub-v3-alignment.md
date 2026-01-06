# Orchestrator + MetaHub V3 对齐自查报告

> **审计日期**: 2025-01-XX  
> **审计范围**: Orchestrator-MetadataHub-Mvp-V3.md 规范对齐  
> **审计模式**: 只读分析，不改代码

---

## 概览

### 测试执行情况

- **单元测试**: 未执行（环境限制，仅代码审查）
- **总体验证结论**: ⚠️ **Yellow**（部分对齐，存在关键问题需修复）

### 文档对齐摘要

已完整阅读以下文档：
- `Orchestrator-MetadataHub-Mvp-V3.md`（V3 规范）
- `api-contracts.md`
- `AGENTS.md`
- `role-contract.md`
- `tingwu-doc.md`
- `style-guide.md`

**V3 核心约束总结**：
1. **LLM 边界**：仅 `HomeOrchestratorImpl`、`RealTranscriptOrchestrator` 可直接调用 LLM
2. **MetaHub 为唯一真相**：会话/转写元数据统一存到 `SessionMetadata`/`TranscriptMetadata`
3. **分析与导出解耦**：导出前检查分析状态，优先使用缓存，不暗中重跑
4. **分析标记持久化**：`latestMajorAnalysis*` 字段必须写入 MetaHub
5. **转写流规范化**：Tingwu → TranscriptOrchestrator → MetaHub

---

## 分章节检查结果

### 3.1 LLM 边界与调用方

#### ✅ 允许 LLM 的类

| 类名 | 状态 | 证据 |
|------|------|------|
| `HomeOrchestratorImpl` | ✅ OK | `feature/chat/.../HomeOrchestratorImpl.kt:29` 构造函数包含 `aiChatService: AiChatService` |
| `RealTranscriptOrchestrator` | ✅ OK | `data/ai-core/.../TranscriptOrchestrator.kt:46` 构造函数包含 `aiChatService: AiChatService` |

#### ✅ 禁止直接调用 LLM 的类

| 类名 | 状态 | 证据 |
|------|------|------|
| `RealExportOrchestrator` | ✅ OK | `data/ai-core/.../ExportOrchestrator.kt:34-38` 构造函数仅包含 `MetaHub`/`ExportManager`/`ExportFileStore`/`DispatcherProvider`，无 LLM 依赖 |
| `RealTingwuCoordinator` | ✅ OK | `data/ai-core/.../RealTingwuCoordinator.kt:62-69` 构造函数无 `AiChatService`，仅依赖 `TranscriptOrchestrator` |
| `HomeScreenViewModel` | ✅ OK | `feature/chat/.../HomeScreenViewModel.kt:193-208` 构造函数无 `AiChatService`，仅通过 `HomeOrchestrator` 调用 |
| `AudioFilesViewModel` | ✅ OK | `feature/media/.../AudioFilesViewModel.kt:37-44` 构造函数无 LLM 依赖 |

**结论**: LLM 边界清晰，符合 V3 规范。

---

### 3.2 MetaHub 模型 & merge 语义

#### SessionMetadata

| 检查项 | 状态 | 证据 | 风险 |
|--------|------|------|------|
| 字段完整性 | ✅ OK | `core/util/.../SessionMetadata.kt:11-24` 包含 `mainPerson`/`summaryTitle6Chars`/`crmRows`/`latestMajorAnalysis*` 等 | Low |
| mergeWith 非空覆盖 | ✅ OK | `SessionMetadata.kt:29-43` 使用 `other.field ?: this.field` 模式 | Low |
| tags 合并去重 | ✅ OK | `SessionMetadata.kt:37` `(tags + other.tags).filter { it.isNotBlank() }.toSet()` | Low |
| crmRows 合并去重 | ✅ OK | `SessionMetadata.kt:45-50` `mergeCrmRows` 使用 `distinctBy(client + "|" + owner)` | Low |
| lastUpdatedAt 单调增长 | ✅ OK | `SessionMetadata.kt:38` `maxOf(lastUpdatedAt, other.lastUpdatedAt)` | Low |
| latestMajorAnalysis* 处理 | ✅ OK | `SessionMetadata.kt:39-41` 使用 `new ?: old` 语义 | Low |

#### TranscriptMetadata

| 检查项 | 状态 | 证据 | 风险 |
|--------|------|------|------|
| 字段完整性 | ✅ OK | `core/util/.../TranscriptMetadata.kt:11-24` 包含 `speakerMap`/`sessionId`/`diarizedSegmentsCount`/`source`/`extra` | Low |
| speakerMap 合并 | ✅ OK | `TranscriptMetadata.kt:45-65` `mergeSpeakers` 新值覆盖，confidence clamp 到 [0,1] | Low |
| extra 合并 | ✅ OK | `TranscriptMetadata.kt:42` `extra + other.extra` | Low |
| createdAt 单调增长 | ✅ OK | `TranscriptMetadata.kt:34` `maxOf(createdAt, other.createdAt)` | Low |

#### InMemoryMetaHub

| 检查项 | 状态 | 证据 | 风险 |
|--------|------|------|------|
| upsertSession 使用 mergeWith | ✅ OK | `core/util/.../InMemoryMetaHub.kt:29-35` 先读 existing，再 `existing?.mergeWith(metadata) ?: metadata` | Low |
| upsertTranscript 使用 mergeWith | ✅ OK | `InMemoryMetaHub.kt:40-51` 同样使用 mergeWith 语义 | Low |

**结论**: MetaHub 模型与 merge 语义实现正确。

---

### 3.3 Home Orchestrator + Streaming 行为

| 检查项 | 状态 | 证据 | 风险 |
|--------|------|------|------|
| 仅在 Completed 事件解析 JSON | ✅ OK | `feature/chat/.../HomeOrchestratorImpl.kt:35` `if (event is ChatStreamEvent.Completed && shouldParseMetadata(request))` | Low |
| 不修改 Completed.fullText | ✅ OK | `HomeOrchestratorImpl.kt:36-38` 仅 emit 原 event，不修改 | Low |
| JSON 解析 fail-soft | ✅ OK | `HomeOrchestratorImpl.kt:36` `runCatching { maybeUpsertSessionMetadata(...) }` | Low |
| 流式路径避免重复追加 | ⚠️ Partial | `HomeScreenViewModel.kt:1074-1110` 使用 `StreamingDeduplicator` 去重，但需验证是否完全避免 Delta+Completed 重复 | Medium |
| 首条助手回复后更新标题 | ✅ OK | `HomeScreenViewModel.kt:1114` `maybeGenerateSessionTitle(request, cleaned)` | Low |
| SMART_ANALYSIS gating | ✅ OK | `HomeScreenViewModel.kt:356-411` `exportMarkdown` 检查缓存 → MetaHub → 自动分析 | Low |

**结论**: Home Orchestrator 行为基本符合 V3，流式去重需进一步验证。

---

### 3.4 ExportOrchestrator + 导出宏

| 检查项 | 状态 | 证据 | 风险 |
|--------|------|------|------|
| 接口仅暴露 exportPdf/exportCsv | ✅ OK | `data/ai-core/.../ExportOrchestrator.kt:27-31` 接口定义仅两个方法 | Low |
| RealExportOrchestrator LLM-free | ✅ OK | `ExportOrchestrator.kt:34-38` 构造函数无 LLM 依赖 | Low |
| HomeScreenViewModel 导出逻辑 | ✅ OK | `HomeScreenViewModel.kt:312-318` `onExportPdfClicked`/`onExportCsvClicked` → `exportMarkdown` → `performExport` | Low |
| performExport 只调用 exportPdf/exportCsv | ✅ OK | `HomeScreenViewModel.kt:414-446` 仅分支调用 `exportOrchestrator.exportPdf`/`exportCsv` | Low |
| 分析优先逻辑 | ✅ OK | `HomeScreenViewModel.kt:364-410` 有缓存 → 直接导出；MetaHub 有但缓存空 → 提示不重跑；都无 → 自动分析 | Low |
| 测试覆盖 | ✅ OK | `feature/chat/.../HomeExportActionsTest.kt:209-239` 覆盖缓存复用、MetaHub 提示、自动分析场景 | Low |

**结论**: ExportOrchestrator 实现符合 V3 规范。

---

### 3.5 TranscriptOrchestrator + Tingwu 协调

| 检查项 | 状态 | 证据 | 风险 |
|--------|------|------|------|
| RealTranscriptOrchestrator 职责 | ✅ OK | `data/ai-core/.../TranscriptOrchestrator.kt:49-70` 负责采样、构造 prompt、调 LLM、解析 JSON、写 MetaHub | Low |
| RealTingwuCoordinator 不直接调 LLM | ✅ OK | `data/ai-core/.../RealTingwuCoordinator.kt:62-69` 仅依赖 `TranscriptOrchestrator` | Low |
| Tingwu 完成后调用 TranscriptOrchestrator | ✅ OK | `RealTingwuCoordinator.kt:1016-1045` `refineSpeakerLabels` 调用 `transcriptOrchestrator.inferTranscriptMetadata` | Low |
| confidence 阈值合并 | ✅ OK | `RealTingwuCoordinator.kt:1040` `if (name != null && (confidence >= 0.6f || !merged.containsKey(speakerId)))` | Low |
| Markdown 不泄露 JSON | ✅ OK | `RealTingwuCoordinator.kt:836-900` `buildMarkdown` 仅使用最终 labels，无 JSON 泄露 | Low |
| Home 上传复用 sessionId | ✅ OK | `HomeScreenViewModel.kt:557-582` `onAudioFilePicked` 使用当前 `sessionId` | Low |
| Audio Sync 生成专属 sessionId | ✅ OK | `feature/media/.../AudioFilesViewModel.kt:123` `val sessionId = "session-${id}-${System.currentTimeMillis()}"` | Low |

**🔴 发现的问题**：

1. **TranscriptMetadataRequest 参数不匹配**（High）
   - **位置**: `RealTingwuCoordinator.kt:1023-1029`
   - **问题**: `refineSpeakerLabels` 构造 `TranscriptMetadataRequest` 时使用了 `segments` 和 `fileName`，但实际定义需要 `diarizedSegments` 且无 `fileName` 字段
   - **证据**:
     ```kotlin
     // RealTingwuCoordinator.kt:1026
     segments = segments,  // ❌ 应为 diarizedSegments
     fileName = jobRequests[transcriptId]?.audioAssetName,  // ❌ 字段不存在
     ```
   - **修复建议**: 将 `segments` 改为 `diarizedSegments`，移除 `fileName` 参数

**结论**: TranscriptOrchestrator 职责清晰，但存在参数不匹配的编译错误（需修复）。

---

### 3.6 latestMajorAnalysis* 持久化（V3 关键点）

| 检查项 | 状态 | 证据 | 风险 |
|--------|------|------|------|
| onAnalysisCompleted 更新 VM 缓存 | ✅ OK | `HomeScreenViewModel.kt:1435-1437` `latestAnalysisMarkdown = summary` / `latestAnalysisMessageId = messageId` | Low |
| 构造 SessionMetadata delta | ✅ OK | `HomeScreenViewModel.kt:1445-1450` 构造只填写 `latestMajorAnalysis*` 的 delta | Low |
| 通过 metaHub.upsertSession 写入 | ✅ OK | `HomeScreenViewModel.kt:1451` `runCatching { metaHub.upsertSession(delta) }` | Low |
| 使用 runCatching + fail-soft | ✅ OK | `HomeScreenViewModel.kt:1451-1456` 使用 `runCatching` 和 `onFailure` 日志 | Low |
| SessionMetadata.mergeWith 正确处理 | ✅ OK | `SessionMetadata.kt:39-41` `latestMajorAnalysis*` 使用 `new ?: old` | Low |
| 测试覆盖 USER vs AUTO | ⚠️ Partial | `HomeExportActionsTest.kt` 未显式测试 `latestMajorAnalysisSource` 的 USER/AUTO 区分 | Medium |

**结论**: `latestMajorAnalysis*` 持久化实现正确，但测试覆盖可加强。

---

## 风险清单

| 类别 | 状态 | 风险等级 | 文件+行号 | 修复建议 |
|------|------|----------|-----------|----------|
| **TranscriptMetadataRequest 参数不匹配** | 🔴 Mismatch | **High** | `RealTingwuCoordinator.kt:1026-1027` | 将 `segments` 改为 `diarizedSegments`，移除 `fileName` 参数 |
| **流式去重完整性验证** | ⚠️ Partial | Medium | `HomeScreenViewModel.kt:1074-1110` | 需运行时验证是否完全避免 Delta+Completed 重复追加 |
| **latestMajorAnalysisSource 测试覆盖** | ⚠️ Partial | Medium | `HomeExportActionsTest.kt` | 添加显式测试验证 USER vs AUTO source 写入 MetaHub |

---

## 结论与建议

### 立即修复项

1. **🔴 High**: `RealTingwuCoordinator.refineSpeakerLabels` 中 `TranscriptMetadataRequest` 参数不匹配
   - 当前代码会导致编译错误或运行时异常
   - 修复：将 `segments` 改为 `diarizedSegments`，移除 `fileName`

### 建议加强项

1. **⚠️ Medium**: 流式去重验证
   - 当前实现使用 `StreamingDeduplicator`，建议添加集成测试验证 Delta+Completed 不重复

2. **⚠️ Medium**: `latestMajorAnalysisSource` 测试覆盖
   - 建议在 `HomeExportActionsTest` 中添加显式断言验证 USER/AUTO source 写入 MetaHub

### 设计缺陷但可接受项

- 无

### 无法确认项

- **测试执行**: 未执行单元测试，仅代码审查。建议运行 `./gradlew testDebugUnitTest` 验证所有测试通过。

---

## 附录：代码引用

### 关键实现位置

- `HomeOrchestratorImpl`: `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt`
- `HomeScreenViewModel`: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
- `SessionMetadata`: `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt`
- `TranscriptMetadata`: `core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt`
- `MetaHub`/`InMemoryMetaHub`: `core/util/src/main/java/com/smartsales/core/metahub/`
- `ExportOrchestrator`: `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt`
- `TranscriptOrchestrator`: `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt`
- `RealTingwuCoordinator`: `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`

---

**报告生成时间**: 2025-01-XX  
**审计模式**: 只读代码审查  
**下一步**: 修复 High 风险项后重新验证

