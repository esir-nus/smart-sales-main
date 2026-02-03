# Chat Interface Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L126-174

---

## Overview

Dual-mode chat interface supporting Coach and Analyst modes via a toggle.

---

## Layout Structure

### Coach Mode

Fast, conversational. No visible "thinking" trace.

```
┌────────────────────────────────────────────────────────────┐
│  [☰]  [📱]   销售技巧交流                          [+]    │ ← Header
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [USER]                                                 ││
│  │ 帮我分析一下张总这个客户的购买意向                      ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [AI - Coach]                                           ││
│  │                                                        ││
│  │ 根据您与张总的交流记录，我发现他对A3打印机方案表现出   ││
│  │ 较高兴趣，但对售后服务有顾虑。建议下次拜访时重点强调   ││
│  │ 我们的本地化服务队...                                ││
│  │                                                        ││
│  │ ┌─────────────────────────────────────────────────┐   ││
│  │ │ 💡 这看起来需要深度分析，建议切换到分析师模式     │   ││ ← Analyst suggestion
│  │ │          [ 切换到分析师 ]                        │   ││
│  │ └─────────────────────────────────────────────────┘   ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│          [  💬 Coach▼ |  🔬 Analyst  ]                    │ ← Mode Toggle
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ [+] │    输入消息...                            [➤]  ││ ← Input Bar
│  └────────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────┘
```

---

## Coach Mode Logic

1. Parse user intent
2. Query Relevancy Library if needed (no UI feedback)
3. Generate response directly
4. Decide if deeper analysis is warranted → append suggestion block

> **Note:** Coach does NOT show ThinkingBox. ThinkingBox is reserved for **Analyst Mode**.

---

## Component Registry

> 🧩 See [ui_element_registry.md](../ui_element_registry.md) for states and physics.
