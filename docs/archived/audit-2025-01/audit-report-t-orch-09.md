# T-orch-09 后实现审计报告

**审计日期**: 2025-12-28  
**审计范围**: T-orch-09 hotfix (export + transcript orchestrator)  
**审计类型**: 代码审查（无代码修改）

---

## 1. Export flow API & behavior

### 1.1 No more exportMarkdown phantom call

**Status**: 🔴 **Mismatch**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:397`
  - 调用 `exportOrchestrator.exportMarkdown(sessionId, markdown, format)`
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:27-31`
  - 接口仅定义 `exportPdf(sessionId: String, markdown: String)` 和 `exportCsv(sessionId: String)`
  - **不存在 `exportMarkdown` 方法**

**问题**: HomeScreenViewModel 调用不存在的接口方法，会导致编译错误或运行时错误。

**风险**: 🔴 **High** - 阻塞导出功能

### 1.2 PDF path calls exportPdf, CSV path calls exportCsv

**Status**: ⚠️ **Partial**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:310-315`
  - `onExportPdfClicked()` 调用 `exportMarkdown(ExportFormat.PDF)`
  - `onExportCsvClicked()` 调用 `exportMarkdown(ExportFormat.CSV)`
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:397`
  - 统一调用 `exportOrchestrator.exportMarkdown(sessionId, markdown, format)`

**问题**: 代码意图是分别调用 PDF/CSV，但实际调用的是不存在的 `exportMarkdown` 方法。

**风险**: 🔴 **High** - 与 1.1 相同问题

### 1.3 ExportOrchestrator remains LLM-free

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:34-39`
  - 构造函数仅注入 `MetaHub`, `ExportManager`, `ExportFileStore`, `DispatcherProvider`
  - 无 `AiChatService` 或其他 LLM 客户端依赖
- `grep` 搜索 `AiChatService|DashScope` 在 `ExportOrchestrator.kt` 中无匹配

### 1.4 Analysis-first export macro + MetaHub consult

**Status**: ⚠️ **Partial**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:354-386`
  - `exportMarkdown()` 检查 `latestAnalysisMarkdown`（VM 本地缓存）
  - 如果为空，自动触发 SMART_ANALYSIS（`pendingExportAfterAnalysis`）
  - 有"内容太少"路径检查（`findLatestLongContent()`）
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:219`
  - 使用 `latestAnalysisMarkdown: String?` 本地缓存
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:356`
  - 检查 `latestAnalysisMarkdown` 而非 MetaHub 的 `latestMajorAnalysis*`

**问题**:
- 使用 `latestAnalysisMarkdown`（VM 本地缓存）而非 MetaHub 的 `latestMajorAnalysis*`
- 未从 MetaHub 读取已存在的分析结果
- 当 MetaHub 有分析但缓存缺失时，可能重复触发分析

**风险**: 🟡 **Medium** - 可能重复触发分析，但不会崩溃

### 1.5 Tests in HomeExportActionsTest

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt:210-223`
  - 测试覆盖：首次导出使用分析，后续导出不重复运行分析
  - `second export reuses cached analysis without rerun` 测试验证缓存行为

---

## 2. RealTranscriptOrchestrator behavior

### 2.1 Real LLM-based inference, not stubbed

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:46`
  - 构造函数注入 `aiChatService: AiChatService`
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:60`
  - 调用 `aiChatService.sendMessage(AiChatRequest(prompt = prompt))`
- `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTranscriptOrchestratorTest.kt:31-75`
  - 测试验证 LLM 调用和 JSON 解析

### 2.2 Sampling, JSON extraction, parsing

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:87-119`
  - `sampleSegments()` 实现：限制段数（maxSegments=30）、每段字符数（maxCharsPerSegment=200）、覆盖所有说话人
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:162-186`
  - `extractJsonBlock()` 实现：优先 fenced blocks（```json），fallback 到 brace-depth scan
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:188-250`
  - `parseMetadata()` 使用 `JSONObject.opt*` 容忍解析，失败返回 null

### 2.3 Confidence clamping and merging

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:261-264`
  - confidence 使用 `value.toFloat().coerceIn(0f, 1f)` 限制到 [0,1]
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:216`
  - `TranscriptMetadata` 创建时 `source = TranscriptSource.TINGWU_LLM`
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:227-248`
  - 如果 `sessionId` 存在，构建 `SessionMetadata` delta 并通过 `mergeWith` 合并
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:276-287`
  - `persistMetadata()` 使用 `mergeWith` 进行非空合并

### 2.4 force + cache semantics

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:72-85`
  - `readCachedMetadata()` 检查：`force == false` 且存在非默认 speakerMap 时返回缓存
  - 非默认判断：`displayName` 非空且不以"发言人"/"speaker"开头
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:81`
  - `if (request.force || !hasReadableSpeaker) return null` - force=true 时跳过缓存
- `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTranscriptOrchestratorTest.kt:78-109`
  - 测试验证缓存命中
- `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTranscriptOrchestratorTest.kt:112-150`
  - 测试验证 force=true 绕过缓存

### 2.5 Tests in RealTranscriptOrchestratorTest

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTranscriptOrchestratorTest.kt`
  - 测试覆盖：happy path（31-75）、cache hit（78-109）、force bypass（112-150）、JSON failure（153-172）、confidence clamping（175-202）

---

## 3. No regressions to core contracts

### 3.1 SessionMetadata/TranscriptMetadata structure unchanged

**Status**: ✅ **OK**

**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt:11-24`
  - 包含所有 v2 字段，无重复 v2 类型
- `core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt:11-24`
  - 包含所有 v2 字段，无重复 v2 类型
- `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt:29-43`
  - `mergeWith()` 使用非空合并语义
- `core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt:29-43`
  - `mergeWith()` 使用非空合并语义

### 3.2 No new off-spec LLM calls

**Status**: ✅ **OK**

**Evidence**:
- `grep` 搜索 `AiChatService` 在 `ExportOrchestrator` 中无匹配
- `grep` 搜索 `AiChatService` 在 `RealTingwuCoordinator` 中无匹配
- 仅 `HomeOrchestratorImpl` 和 `RealTranscriptOrchestrator` 调用 LLM（符合规范）

---

## 总结

### 状态概览

- ✅ **完全符合**: 5/9 项
- ⚠️ **部分符合**: 2/9 项
- 🔴 **不匹配**: 2/9 项

### 整体状态: 🔴 **Red**

存在 2 个高优先级阻塞问题需要立即修复。

---

## 推荐修复（按优先级）

### 🔴 High Priority (阻塞发布)

1. **修复 HomeScreenViewModel 的 exportMarkdown 调用**
   - 文件: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:397`
   - 问题: 调用不存在的 `exportOrchestrator.exportMarkdown()` 方法
   - 修复: 改为分别调用 `exportOrchestrator.exportPdf(sessionId, markdown)` 和 `exportOrchestrator.exportCsv(sessionId)`
   - 相关代码:
     - `onExportPdfClicked()` → `exportOrchestrator.exportPdf(sessionId, markdown)`
     - `onExportCsvClicked()` → `exportOrchestrator.exportCsv(sessionId)`
     - `performExport()` 中根据 format 分别调用

### 🟡 Medium Priority (功能改进)

2. **Home export 使用 MetaHub 的 latestMajorAnalysis**
   - 文件: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:354-386`
   - 问题: 仅使用 VM 本地缓存 `latestAnalysisMarkdown`，未从 MetaHub 读取
   - 修复: 在 `exportMarkdown()` 中先检查 MetaHub 的 `latestMajorAnalysis*` 字段，如果存在且 `latestAnalysisMarkdown` 为空，尝试从 messageId 恢复或提示用户

---

**注意**: 本报告仅关注 T-orch-09 的实现是否符合规范，不涉及新功能建议或重构建议。

