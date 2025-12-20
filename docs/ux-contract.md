# UX 合约（对齐 Orchestrator-MetadataHub V7.0.0）

## 版本说明
- 本文对齐 V7：多编排器/多通道、M1/M2/M3/M4、HUD 三段 copy、转写伪流式（batch-by-batch）。
- UI 永不展示任何原始 JSON；JSON 仅在 HUD 中以“可复制块”方式呈现。

---

## 1) GENERAL vs SMART（V7）

### GENERAL（聊天）
- 仍采用 Delta -> Completed 的流式对话体验。
- UI 只展示 `<Visible2User>` 内容。
- `<Metadata>` 仅供 Orchestrator 解析写入 MetaHub；UI 永不展示。

### SMART（Smart Analysis / 卡片）
- Smart Analysis 由 Agent1 产出结构化结果。
- UI 展示 Smart Analysis 的“格式化 Markdown/卡片”，而不是 JSON。
- 导出（CSV/PDF）必须在 Smart Analysis 成功后才允许触发（gating）。

---

## 2) M1/M2/M3 的 UI 读取原则（V7）

### M1（UserMetadata）
- 原始 M1 为 onboarding/user center JSON。
- UI 若需要展示或参与交互，只能使用“安全投影”（例如 UserUiContext / UserPolishContext），避免泄露与耦合。

### M2（ConversationDerivedState）
- UI 允许读取 M2（为未来可扩展性）。
- 推荐 UI 绑定 `uiSignals`（稳定投影），而不是直接绑定 `rawSignals`，以避免抖动。
- 允许的玩法示例（未来）：
  - progress 映射进度条
  - risk 映射“风险提示/提醒动画”
  - highlights 数量映射徽章

> 未来升级提示：风险/进度等 UI 信号应引入稳定化（hysteresis），避免 “忽高忽低” 闪烁。

### M3（SessionState / RenamingMetadata）
- UI 负责“会话管理”和“命名接受/编辑”：
  - session rename（accepted）
  - export name rename（accepted）
  - session switching/pinning/archiving 等

---

## 3) RenamingMetadata UX（candidate vs accepted）

### 3.1 定义
- candidate：由 first-20 parsing / Smart Analysis 提案生成
- accepted：用户已接受或已编辑的最终值

### 3.2 One-time auto apply（避免“AI 抢控制权”）
Orchestrator 可执行一次性自动命名，仅在满足：
- accepted 为空
- 用户未手动改名（userRenamedAt == null）
- auto-apply 尚未执行（autoRenameAppliedAt == null）
- （可选）candidate confidence >= 阈值

之后只保留 candidate 更新，但不再覆盖 accepted。

---

## 4) 转写伪流式 UX（batch-by-batch）

- UI 可在每个 batch `BatchReleased` 时追加显示文本（伪流式）。
- 说话人标签允许在处理中更新，但 Completed 后冻结。
- 原始 provider JSON 永不进入正常展示；仅出现在 HUD 的 copy-only 区域。

---

## 5) HUD（Debug）三段 copy/paste（强制）

HUD 为调试专用面板（或调试弹层），必须提供三个可复制区域：

### Section 1：Effective Run Snapshot（调试信息）
包含尽可能多关键状态（可复制一键粘贴）：
- AiParaSettings（sanitized）+ effective config
- provider lane 选择、禁用原因
- LLM prompt pack 是否加载、版本/哈希
- diarization 等关键能力：请求 vs 生效
- M4（external knowledge/style）启用状态、pack versions、retrieval traceId
- 注意：禁止泄露任何密钥、签名原文、raw HTTP bodies

### Section 2：Raw Transcription Output（原始转写）
- Tingwu/XFyun 原生输出（或引用）
- copy-only，不进入正式气泡展示

### Section 3：Preprocessed Snapshot（最终润色前）
- first 20 lines summary（rendered）
- suspicious boundaries
- batch plan 概览
- 其它 deterministic preprocess 产物

---

## 6) 文本清理与标签协作（保持 V7 责任边界）

- UI 展示层：
  - 若存在 `<Visible2User>`，仅展示该段。
  - 若缺失 `<Visible2User>`，允许 sanitizeAssistantOutput 做轻量清理后展示（迁移期兼容）。
- 调试层：
  - HUD 可在“仅调试模式”显示完整原始文本（含标签）用于 QA 对照。
- 不使用复杂正则或二次 LLM 清理，以免破坏语义与证据链。

