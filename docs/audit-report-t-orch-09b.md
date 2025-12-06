# T-orch-09b 后实现审计报告

**审计日期**: 2025-12-28  
**审计范围**: T-orch-09b export-flow fix  
**审计类型**: 代码审查（无代码修改）

---

## 1. Export hotfix checklist

### 1.1 No more phantom exportMarkdown call

**Status**: ⚠️ **Partial**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:389-419`
  - `performExport(format: ExportFormat)` 方法仍调用 `exportOrchestrator.exportMarkdown(sessionId, markdown, format)` (line 397)
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:405-435`
  - `performExport(format: ExportFormat, markdownOverride: String? = null)` 方法正确使用 `when` 分支调用 `exportPdf` 和 `exportCsv` (lines 413-415)
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:360,386`
  - `exportMarkdown()` 调用 `performExport(format)`，匹配到旧签名的方法（line 389），而非新签名的方法（line 405）

**问题**: 存在两个 `performExport` 方法：
- 旧方法（line 389）：调用不存在的 `exportMarkdown` 方法
- 新方法（line 405）：正确使用 `when` 分支调用 `exportPdf`/`exportCsv`

由于 `exportMarkdown()` 调用 `performExport(format)` 时匹配到旧签名，实际执行的是旧方法，仍会调用不存在的 `exportMarkdown`。

**风险**: 🔴 **High** - 导出功能会失败

### 1.2 Export paths branch on format

**Status**: ⚠️ **Partial**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:405-435`
  - `performExport(format: ExportFormat, markdownOverride: String? = null)` 正确实现：
    - Line 413-415: `when (format) { ExportFormat.PDF -> exportPdf(...), ExportFormat.CSV -> exportCsv(...) }`
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:389-419`
  - `performExport(format: ExportFormat)` 仍调用不存在的 `exportMarkdown` 方法

**问题**: 新方法实现正确，但旧方法仍在使用，导致实际执行路径错误。

**风险**: 🔴 **High** - 与 1.1 相同问题

### 1.3 ExportOrchestrator still LLM-free

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:34-39`
  - 构造函数仅注入 `MetaHub`, `ExportManager`, `ExportFileStore`, `DispatcherProvider`
  - 无 `AiChatService` 或其他 LLM 客户端依赖
- `grep` 搜索 `AiChatService|DashScope` 在 `ExportOrchestrator.kt` 中无匹配

### 1.4 Export macro uses MetaHub metadata

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:356-400`
  - `exportMarkdown()` 方法（grep 显示的实际版本）：
    - Line 359: `val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()`
    - Line 360: `val hasMetaAnalysis = meta?.latestMajorAnalysisMessageId != null`
    - Line 361-364: 优先使用 `latestAnalysisMarkdown`，如果存在则直接导出
    - Line 378-379: 如果 MetaHub 有分析记录但缓存缺失，显示 snackbar 提示
    - Line 381-385: 根据 `hasMetaAnalysis` 设置不同的 `autoGoal`

**问题**: 无。实现符合规范：优先使用 MetaHub 的 `latestMajorAnalysisMessageId` 判断，显示提示，安全降级。

### 1.5 Tests align with new behavior

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt:277-289`
  - `RecordingExportOrchestrator` 实现 `exportPdf()` 和 `exportCsv()` 方法
  - 无 `exportMarkdown` 方法
- `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt:182-207`
  - 测试使用 `exportPdf` 和 `exportCsv`，验证缓存重用行为

---

## 2. Sanity check – transcript path unchanged

### 2.1 TranscriptOrchestrator implementation

**Status**: ✅ **OK** (unchanged)

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:46,60`
  - 仍使用 `aiChatService.sendMessage()` 调用 LLM
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:87-119,162-186,188-250`
  - 采样、JSON 提取、解析逻辑未变
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:261-264,276-287`
  - Confidence clamping 和 MetaHub 写入逻辑未变

### 2.2 RealTingwuCoordinator

**Status**: ✅ **OK** (unchanged)

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:1016-1044`
  - `refineSpeakerLabels()` 仍调用 `transcriptOrchestrator.inferTranscriptMetadata()`
  - 合并逻辑：`confidence >= 0.6f || !merged.containsKey(speakerId)`
- `grep` 搜索 `AiChatService` 在 `RealTingwuCoordinator.kt` 中无匹配

---

## 总结

### 状态概览

- ✅ **完全符合**: 4/6 项
- ⚠️ **部分符合**: 2/6 项
- 🔴 **不匹配**: 0/6 项

### 整体状态: 🟡 **Yellow**

存在 1 个高优先级问题：存在两个 `performExport` 方法，旧方法仍在使用并调用不存在的 `exportMarkdown`。

---

## 推荐修复（按优先级）

### 🔴 High Priority (阻塞发布)

1. **删除旧的 `performExport(format: ExportFormat)` 方法**
   - 文件: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:389-419`
   - 问题: 旧方法调用不存在的 `exportOrchestrator.exportMarkdown()` 方法
   - 修复: 删除旧方法（line 389-419），保留新方法（line 405+），确保 `exportMarkdown()` 调用时使用新方法

---

**注意**: 本报告仅关注 T-orch-09b 的 export-flow fix 是否正确实现，不涉及新功能建议或重构建议。

