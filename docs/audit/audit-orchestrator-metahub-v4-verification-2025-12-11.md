# Orchestrator–MetaHub V4 行为审计报告

**审计日期**: 2025-12-11  
**审计范围**: Orchestrator–MetaHub V4 行为（GENERAL chat、SMART_ANALYSIS、Tingwu 集成）  
**审计模式**: 只读分析，不涉及代码修改

---

## 1. 审计范围重述

本次审计聚焦以下三个方面：

- **GENERAL chat 管道**：首条回复的 prompt 规则、元数据解析职责、UI 流式展示行为
- **SMART_ANALYSIS 管道**：LLM JSON-only 输出、Orchestrator 解析与 Markdown 生成、ViewModel 占位符与流式控制
- **MetaHub 写入规则**：GENERAL 与 SMART_ANALYSIS 的元数据合并策略、分析标记更新

---

## 2. V4 规则（来自文档）

### LLM 职责

1. **GENERAL chat**：返回自然语言文本；可选在末尾追加一个 JSON 元数据块（轻量提示，禁止长篇 scaffold）
2. **SMART_ANALYSIS**：仅输出单个 JSON 对象，禁止 Markdown、解释或多版本草稿

### Orchestrator 职责

3. **GENERAL**：透明透传，不重写文本，不解析 JSON（JSON 解析由 ViewModel 负责）
4. **SMART_ANALYSIS**：
   - 从 LLM 返回文本中提取最后一个 JSON 对象
   - 使用“最后值优先”策略解析字段
   - 构建 `SessionMetadata` 并 `upsert` 到 MetaHub
   - 本地生成用户可见的 Markdown（仅渲染有内容的章节）

### ViewModel / UI 职责

5. **GENERAL**：流式展示所有 Delta 事件；在 Completed 时提取 JSON 并写入 MetaHub
6. **SMART_ANALYSIS**：
   - **必须忽略所有 Delta 事件**，不渲染到 UI
   - 立即插入本地占位符（如“正在智能分析…”）
   - 仅在 Completed 时用 Orchestrator 返回的 Markdown 替换占位符

### MetaHub 写入规则

7. **GENERAL 首条回复**：ViewModel 解析 JSON，至少一个字段有效时 `upsertSession`；非空新值覆盖，空值保留旧值
8. **SMART_ANALYSIS**：
   - JSON 完全失败：不写入 MetaHub，UI 显示友好失败消息
   - JSON 部分成功：写入可用字段，保留缺失字段的旧值，更新 `latestMajorAnalysis*` 标记

### Prompt 规则

9. **GENERAL 首条回复**：必须使用轻量级 JSON 指导，禁止长篇 scaffold、多分节模板、完整 JSON 骨架
10. **SMART_ANALYSIS**：prompt 可包含字段说明，但必须明确要求“仅输出一个 JSON 对象，不要输出其他文字”

---

## 3. 当前行为 vs V4（按规则分类）

### **OK – GENERAL Orchestrator 透传行为**

- `HomeOrchestratorImpl.streamChat` 对 GENERAL 请求（`quickSkillId == null`）直接透传所有事件，不进行 JSON 解析或文本重写。符合 V4 规则 3。

### **OK – SMART_ANALYSIS Orchestrator JSON 解析与 Markdown 生成**

- `HomeOrchestratorImpl.buildSmartAnalysisResult` 实现了：
  - 提取最后一个 JSON 对象（`extractLastSmartJson`）
  - 使用“最后值优先”策略（`parseSmartAnalysisPayload` 中的 `lastOrNull()` 逻辑）
  - 构建 `SessionMetadata` 并 `upsert` 到 MetaHub
  - 本地生成 Markdown（`buildSmartAnalysisMarkdown`），仅渲染有内容的章节
- 符合 V4 规则 4。

### **OK – SMART_ANALYSIS ViewModel 占位符与 Delta 忽略**

- `HomeScreenViewModel.startStreamingResponse` 中：
  - 当 `isSmartAnalysis == true` 时，立即插入占位符（`SMART_PLACEHOLDER_TEXT`）
  - 在 Delta 事件处理中明确跳过 SMART_ANALYSIS（`if (!isSmartAnalysis)`）
  - 在 Completed 时用 Orchestrator 返回的 Markdown 替换占位符
- 符合 V4 规则 6。

### **OK – GENERAL 元数据解析由 ViewModel 负责**

- `HomeScreenViewModel.handleGeneralChatMetadata` 实现了：
  - 从 `rawFullText` 中提取最后一个 JSON 块（`findLastJsonBlock`）
  - 解析字段（`parseGeneralChatMetadata`）
  - 构建 `SessionMetadata` 并 `upsert` 到 MetaHub
- 符合 V4 规则 5 和规则 7。

### **OK – SMART_ANALYSIS 失败处理**

- `HomeOrchestratorImpl.buildSmartAnalysisResult` 在 JSON 解析失败时返回 `SMART_ANALYSIS_FAILURE_MESSAGE`，不写入 MetaHub。
- `HomeScreenViewModel.startStreamingResponse` 在错误事件时用友好失败消息替换占位符。
- 符合 V4 规则 8（JSON 完全失败场景）。

### **MISMATCH – GENERAL 首条回复 prompt 仍使用结构化规则标签**

- **位置**: `HomeScreenBindings.buildPromptWithHistory`（第 34-70 行）
- **问题**: 当前 prompt 使用 `<RULES>` / `<OUTPUT>` 标签结构，虽然内容精简，但仍属于“结构化指令”而非“轻量级 JSON 指导”。
- **V4 要求**: GENERAL 首条回复应使用轻量级 JSON 指导，禁止长篇 scaffold 或多分节模板。
- **影响**: 可能导致 LLM 输出时仍带有规则标签或结构化痕迹，而非“看起来像人类回答”。

### **UNKNOWN – GENERAL prompt 是否包含完整 JSON 骨架**

- **位置**: `HomeScreenBindings.buildPromptWithHistory`（GENERAL 首条回复部分）
- **问题**: 当前 prompt 结构（`<RULES>` / `<OUTPUT>`）中是否包含完整的 JSON 字段示例或骨架，需要查看完整 prompt 内容才能确定。
- **V4 要求**: 禁止在 GENERAL prompt 中粘贴完整 JSON 模板（所有字段和占位符文本）。

### **OK – SMART_ANALYSIS prompt JSON-only 要求**

- `HomeScreenBindings.buildPromptWithHistory` 中 SMART_ANALYSIS 部分明确要求“只能输出一个 JSON 对象，不要输出 Markdown、解释或多版本草稿”，并提供了字段说明和示例。
- 符合 V4 规则 10。

### **UNKNOWN – SMART_ANALYSIS 部分解析时的 MetaHub 写入行为**

- **位置**: `HomeOrchestratorImpl.buildMergedMetadata`
- **问题**: 当 JSON 部分解析成功（例如只有 `main_person` 和 `short_summary`，数组为空）时，代码会写入可用字段并保留旧值，但未明确验证“至少一个字段有效”的逻辑是否与 V4 规则 8 一致。
- **当前实现**: `parseSmartAnalysisPayload` 中有 `hasContent` 检查，但需要确认是否与 V4 的“部分成功”定义一致。

### **OK – GENERAL UI 流式展示**

- `HomeScreenViewModel.startStreamingResponse` 对 GENERAL 请求处理所有 Delta 事件，符合 V4 规则 5。

### **OK – JSON 不展示给用户**

- `HomeScreenViewModel.stripTrailingJsonFromGeneralReply` 在 GENERAL Completed 时移除尾部 JSON。
- `HomeScreenViewModel.startStreamingResponse` 对 SMART_ANALYSIS 直接使用 Orchestrator 返回的 Markdown，不展示原始 JSON。
- 符合 V4 要求“UI 不展示 JSON”。

---

## 4. 当前可见问题的可能原因（如适用）

### 症状：GENERAL 首条回复可能仍带有结构化痕迹或规则标签

- **可能原因**: `HomeScreenBindings` 中的 `<RULES>` / `<OUTPUT>` 标签结构可能导致 LLM 在输出时引用这些标签或产生结构化痕迹。
- **层级不匹配**: Prompt 层使用了结构化指令而非轻量级 JSON 指导。

### 症状：GENERAL 首条回复可能过长或包含重复内容

- **可能原因**: 虽然 prompt 中包含了长度和重复控制规则，但如果 LLM 未严格遵守，仍可能出现超长或重复输出。
- **层级**: Prompt 层规则执行依赖于 LLM 的遵循程度。

---

## 5. 审计总结

### 规则检查统计

- **总计**: 10 条核心规则
- **OK**: 8 条
- **MISMATCH**: 1 条（GENERAL prompt 结构化标签）
- **UNKNOWN**: 1 条（GENERAL prompt 是否包含完整 JSON 骨架；SMART_ANALYSIS 部分解析的 MetaHub 写入验证）

### 关键发现

1. **架构层面**：
   - ✅ SMART_ANALYSIS 管道已完全符合 V4：Orchestrator 负责 JSON 解析与 Markdown 生成，ViewModel 负责占位符与 Delta 忽略。
   - ✅ GENERAL 元数据解析职责分离正确：ViewModel 负责解析，Orchestrator 负责透传。

2. **Prompt 层面**：
   - ⚠️ GENERAL 首条回复 prompt 使用了 `<RULES>` / `<OUTPUT>` 结构化标签，可能不符合 V4 的“轻量级 JSON 指导”要求。
   - ✅ SMART_ANALYSIS prompt 明确要求 JSON-only 输出。

3. **UI 层面**：
   - ✅ SMART_ANALYSIS 占位符与 Delta 忽略已正确实现。
   - ✅ GENERAL 流式展示已正确实现。
   - ✅ JSON 不展示给用户的规则已遵守。

### 优先级修复建议

1. **高优先级**（Prompt 层）：
   - 审查并简化 GENERAL 首条回复 prompt，移除 `<RULES>` / `<OUTPUT>` 标签结构，改为纯文本轻量级 JSON 指导。
   - 确认 GENERAL prompt 中不包含完整 JSON 骨架（所有字段和占位符文本）。

2. **中优先级**（验证与文档）：
   - 验证 SMART_ANALYSIS 部分解析时的 MetaHub 写入逻辑是否与 V4 规则 8 完全一致（至少一个字段有效时才写入）。

3. **低优先级**（优化）：
   - 考虑在 GENERAL prompt 中加强长度和重复控制的执行力度（如果实际输出仍出现超长或重复）。

---

**审计完成时间**: 2025-12-11  
**审计人**: AI Assistant (Auto)  
**下一步**: 根据 MISMATCH 和 UNKNOWN 项，进行代码审查和 prompt 优化。

