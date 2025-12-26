# UX 合约（对齐 Orchestrator-V1）

## 版本说明
- 本文对齐 Orchestrator-V1（Disector / Tingwu / MemoryCenter / Publisher）。
- V7 规范已归档；不得再以 V7 作为 UX 依据。
- UI 永不展示任何原始 JSON；JSON 仅允许出现在 HUD 的可复制区块。
- 数据模型以 `docs/orchestrator-v1.schema.json` 为准。

---

## 1) Transcript vs Analysis（V1）

### Transcript（逐字稿视图）
- 由 Publisher 输出，格式：`MM:SS  Speaker: utterance`。
- UI 只展示**连续前缀**（b1..bk），不允许乱序展示。
- 若缺少相对段信息，按“批次块”展示，并接受预热区重复（V1 限制）。

### Analysis（结果/分析视图）
- 由 MemoryCenter 输出结构化 SessionMemory，UI 展示格式化结果。
- 只使用章节级时间线；V1 不做逐行时间戳精修。
- 不依赖 chain-of-thought。

---

## 2) 说话人显示
- 优先使用 SessionMemory 的 speaker mapping：
  - `displayName` / `title` / `confidence` / `effectiveFromBatchId`
- 若无映射，回退到稳定占位符：S1、S2…
- 映射随批次推进更新，但不回写历史。

---

## 3) 时间展示规则
- 所有展示时间为**绝对时间**（相对 sessionStart）。
- 章节时间来自 `captureStartMs + offsetMs` 的校准结果。
- UI 不做二次修正或“抹平”。

---

## 4) 渐进发布规则
- UI 仅接收 Publisher 的“前缀发布”结果。
- 不允许在 UI 侧拼装乱序批次。
- 去重逻辑由 Publisher 统一执行（区间过滤）；UI 不重复计算。

---

## 5) HUD（Debug）三段 copy/paste（强制）

HUD 为调试专用面板，必须提供三个可复制区域：

1) **Section 1：Effective Run Snapshot**
   - 当前配置与关键状态
   - provider lane 选择/禁用原因

2) **Section 2：Raw Transcription Output**
   - Tingwu 原始输出或引用
   - copy-only，不进入正式气泡

3) **Section 3：Preprocessed / Publisher-ready Snapshot**
   - 批次计划（DisectorPlan 摘要）
   - 章节时间线（MemoryCenter 结果）
   - 其它确定性产物

> V1 **不使用** suspicious gap/suspicious boundary 作为润色依据。

---

## 6) 文本清理边界
- UI 层不做复杂清理，不引入二次 LLM 清理。
- 仅允许轻量 sanitize（迁移期兼容），不改变语义。
- 原始标签文本仅允许在 HUD（调试模式）中展示。
