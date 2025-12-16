# UX 合约（对齐 Orchestrator-MetadataHub V5.0.0）

## 版本说明
- 与 Orchestrator-MetadataHub-V5.md V5.0.0 对齐：GENERAL 聊天采用标签化输出信道，UI 仅展示 `<Visible2User>`；标签缺失时允许按既有 sanitize 轻量清理。

## 7.2 GENERAL vs SMART 行为对比

- GENERAL：
  - 按普通助手对话流式展示 Delta → Completed，自然语言为主。
  - LLM 输出可包含标签：
    - `<Visible2User>...</Visible2User>`：用户可见文本。
    - `<Metadata>{...}</Metadata>`：可选 JSON 元数据，仅用于命名/摘要与 MetaHub。
  - UI 只展示 `<Visible2User>`，绝不展示 `<Metadata>` JSON 或内部标签；若模型暂未严格产出标签，ViewModel 按 V5 既有规则从整段文本轻量清理（去掉 scaffold / JSON 尾巴）后展示。
- SMART：
  - 不流式展示内容，Completed 后由 Orchestrator 生成 Markdown 卡片；UI 不直接消费 JSON。

## GENERAL 文本清理与标签协作

- 展示层：
  - Home 聊天气泡默认展示 `<Visible2User>` 内的 Markdown/纯文本。
  - 若存在 `<Visible2User>`，其外部任何内容（规则提示、Persona 摘要、“历史对话/最新问题”等）不得展示。
  - 若缺少 `<Visible2User>`，可用简化版 sanitizeAssistantOutput 清理明显的 prompt scaffold 与 JSON 尾巴再展示。
- 调试层：
  - HUD 在“仅调试”模式可展示完整原始文本（含标签），供 QA 对照 Visible2User 与 Metadata。
- 责任边界：
  - 语义结构由 LLM + prompt 决定；
  - 仅在出现“重复同一句/同一要点”或泄漏提示标题时做轻量剪裁；
  - 不用复杂正则或二次 LLM 处理，避免破坏语义。
