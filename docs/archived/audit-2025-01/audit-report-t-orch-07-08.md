# T-orch-07/08 后实现自审计报告

**审计日期**: 2025-12-06  
**审计范围**: Orchestrator + MetaHub 重构 (T-orch-07) 及后续 Audio/CSV 补丁 (T-orch-08)  
**审计类型**: 代码审查（无代码修改）

---

## 1. ExportOrchestrator / export path

### 1.1 ExportOrchestrator 是否 LLM-free

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:34-39`
  - 构造函数仅注入 `MetaHub`, `ExportManager`, `ExportFileStore`, `DispatcherProvider`
  - 无 `AiChatService` 或其他 LLM 客户端依赖

### 1.2 PDF export

**Status**: ⚠️ **Mismatch**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:396`
  - 调用 `exportOrchestrator.exportMarkdown(sessionId, markdown, format)`
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:27-31`
  - 接口仅定义 `exportPdf(sessionId: String, markdown: String)` 和 `exportCsv(sessionId: String)`
  - **不存在 `exportMarkdown` 方法**

**问题**: HomeScreenViewModel 调用不存在的接口方法，会导致编译错误或运行时错误。

**风险**: 🔴 **High** - 阻塞导出功能

### 1.3 CSV export

**Status**: ✅ **OK** (接口层面)

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:61-94`
  - 从 `SessionMetadata.crmRows` 构建 CSV
  - 空列表时返回仅 header
  - 始终写入 `ExportMetadata`

### 1.4 所有 CSV 导出入口点

**Status**: ✅ **OK**

**Evidence**:
- `grep` 搜索 `ExportManager.*exportCsv` 无匹配
- 生产代码中所有 CSV 导出均通过 `ExportOrchestrator.exportCsv()`

---

## 2. Home export macro + SMART_ANALYSIS

### 2.1 Home export 函数 (PDF/CSV)

**Status**: ⚠️ **Partial**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:353-386`
  - `exportMarkdown()` 检查 `latestAnalysisMarkdown`
  - 如果为空，自动触发 SMART_ANALYSIS（`pendingExportAfterAnalysis`）
  - 有"内容太少"路径检查（`findLatestLongContent()`）

**问题**:
- 使用 `latestAnalysisMarkdown`（VM 本地缓存）而非 MetaHub 的 `latestMajorAnalysis*`
- 未从 MetaHub 读取已存在的分析结果

**风险**: 🟡 **Medium** - 可能重复触发分析

### 2.2 ExportOrchestrator 不触发 LLM

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt` 无 LLM 调用

---

## 3. MetaHub models & merge

### 3.1 SessionMetadata v2 fields

**Status**: ✅ **OK**

**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt:11-24`
  - 包含所有 v2 字段：`mainPerson`, `shortSummary`, `summaryTitle6Chars`, `location`, `stage`, `riskLevel`, `crmRows`, `latestMajorAnalysis*`, `tags`, `lastUpdatedAt`, `analysisSource`

### 3.2 InMemoryMetaHub merge 语义

**Status**: ✅ **OK**

**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt:29-43`
  - `mergeWith()` 使用非空合并：`other.mainPerson ?: mainPerson`
  - Tags 合并：`(tags + other.tags).toSet()`
- `core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt:29-34`
  - `upsertSession()` 调用 `existing?.mergeWith(metadata) ?: metadata`

### 3.3 无并行 v2 类型

**Status**: ✅ **OK**

**Evidence**:
- 所有 orchestrator 使用相同的 `SessionMetadata` 和 `TranscriptMetadata`

---

## 4. HomeOrchestratorImpl & SMART_ANALYSIS

### 4.1 GENERAL_CHAT + SMART_ANALYSIS JSON 提取

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:70-94`
  - `extractJsonBlock()` 支持 fenced blocks 和 inline JSON
  - 不修改 assistant text（仅提取）
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:96-137`
  - `parseSessionMetadata()` 解析 JSON 到 `SessionMetadata` 字段 + `crmRows` + `latestMajorAnalysis*` + `analysisSource`

### 4.2 Streaming path

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:32-40`
  - 仅在 `Completed` 事件时解析元数据
  - 原始 `Completed.fullText` 直接 emit 给 VM（`emit(event)`）

---

## 5. Transcript/Tingwu orchestration

### 5.1 RealTranscriptOrchestrator

**Status**: ⚠️ **Partial**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:33-58`
  - 当前实现仅从 `request.speakerLabels` 构建 `SpeakerMeta`，未调用 `AiChatService`
  - 未实现分段采样逻辑
  - 未实现 JSON 提取和 confidence 限制

**问题**: 实现不完整，缺少 LLM 调用和元数据提取逻辑。

**风险**: 🔴 **High** - 说话人识别功能缺失

### 5.2 RealTingwuCoordinator

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:83-173`
  - 保持 Tingwu 网络调用（CreateTask, GetTaskInfo）
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:346-351`
  - 转写完成后调用 `upsertTranscriptMetadata()`，传递 `request?.sessionId`
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:1016-1044`
  - `refineSpeakerLabels()` 调用 `transcriptOrchestrator.inferTranscriptMetadata()`
  - 合并逻辑：`confidence >= 0.6f || !merged.containsKey(speakerId)`
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:836-900`
  - `buildMarkdown()` 使用合并后的 labels，无 JSON 或调试字符串

### 5.3 Retry button

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:178-212`
  - `retrySpeakerInference()` 使用 `force = true`
  - 重用 `completedArtifacts[jobId]?.diarizedSegments`

---

## 6. Audio upload vs Audio Sync session binding

### 6.1 Home upload path

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:529-533`
  - `submitTranscription()` 传递当前 `sessionId`（通过 `onTranscriptionRequested` 设置）
- `app/src/main/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinator.kt:41-55`
  - `submitTranscription()` 接收 `sessionId` 并传递给 `TingwuRequest`

### 6.2 Audio Sync ("转写并打开聊天") path

**Status**: ✅ **OK**

**Evidence**:
- `feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesViewModel.kt:123`
  - 创建新 `sessionId = "session-${id}-${System.currentTimeMillis()}"`
- `feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesViewModel.kt:174-180`
  - `submitTranscription()` 传递 `sessionId`
- `feature/media/src/main/java/com/smartsales/feature/media/audio/AudioFilesViewModel.kt:53`
  - `jobSessionIds` 存储 jobId -> sessionId 映射

### 6.3 Fallback sessionId

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:348`
  - 使用 `request?.sessionId`，如果为 null 则使用 `jobId` 作为 transcriptId

---

## 7. CSV path consistency

**Status**: ✅ **OK**

**Evidence**:
- 无直接 `ExportManager.exportCsv` 调用
- 所有 CSV 导出通过 `ExportOrchestrator.exportCsv()`

---

## 8. No spec violations / no extra LLM calls

### 8.1 新增 LLM 调用检查

**Status**: ✅ **OK**

**Evidence**:
- `grep` 搜索 `AiChatService` 在 `ExportOrchestrator` 和 `RealTingwuCoordinator` 中无匹配
- ExportOrchestrator 无 LLM 调用
- RealTingwuCoordinator 仅调用 `transcriptOrchestrator`（但该实现不完整）

### 8.2 无新并行 orchestrator

**Status**: ✅ **OK**

**Evidence**:
- 无新的 v2 entity 类或并行 orchestrator

---

## 优先级风险列表

### 🔴 High Priority (阻塞发布)

1. **HomeScreenViewModel 调用不存在的 `exportMarkdown` 方法**
   - 文件: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:396`
   - 问题: 接口不匹配，会导致编译/运行时错误
   - 修复: 改为调用 `exportPdf()` 或 `exportCsv()`

2. **RealTranscriptOrchestrator 实现不完整**
   - 文件: `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:33-58`
   - 问题: 缺少 LLM 调用、分段采样、JSON 提取
   - 修复: 实现完整的 `inferTranscriptMetadata()` 逻辑

### 🟡 Medium Priority (功能缺陷)

3. **Home export 未使用 MetaHub 的 latestMajorAnalysis**
   - 文件: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:353-386`
   - 问题: 仅使用 VM 本地缓存，可能重复触发分析
   - 修复: 从 MetaHub 读取 `latestMajorAnalysis*` 字段

---

## 总结

- ✅ **通过项**: 6/8 项完全符合规范
- ⚠️ **部分通过**: 2/8 项存在实现不完整或接口不匹配
- 🔴 **阻塞项**: 2 个高优先级问题需要修复后才能发布

**建议**: 优先修复 High Priority 问题，然后处理 Medium Priority 改进。

