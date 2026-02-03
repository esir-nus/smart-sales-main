# Audio & Entity Flows

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md §3 (Audio & Entity Flows)

---

## 3.13 Local Audio Upload
Tap `[+]` in Audio Drawer → System Picker → File added to list → Auto-transcribe starts.

## 3.14 Badge Sync (Pull-to-Refresh)
Pull down Audio Drawer → Sync Indicator activates → New files populate list.

## 3.15 Entity Recognition (Snakebar)
**Scenario:** Real-time extraction during analysis.
**UI:** Unobtrusive Snackbar at bottom:
`✨ Learned new entity: "Project A3" (Project)`
**Logic:** Updates `RelevancyLibrary` in background.

## 3.16 Ambiguous Entity (Disambiguation)

**Scenario:** User mentions "张总", system knows multiple matches.

**Two UI States** (based on scoring from Prism-V1 §5.4):

---

**STATE A: Auto-Resolved (High Confidence)**

When `topScore > 0.85 AND secondScore < 0.3`, system auto-resolves with disclosure:

```
┌────────────────────────────────────────────────────────────┐
│ [AI - Coach]                                               │
│                                                            │
│  我理解您指的是张总（A3项目负责人）                         │ ← Inline disclosure
│  根据最近的A3项目讨论...                                   │
│                                                            │
│  [ 更改 ]                                                  │ ← Override button
│                                                            │
└────────────────────────────────────────────────────────────┘
```

| Element | Behavior |
|---------|----------|
| **Inline disclosure** | Bold entity name + context hint |
| **[更改] button** | Tap → expands to picker (STATE B) |

---

**STATE B: Picker (Multiple Candidates)**

When confidence is low or user taps `[更改]`:

```
┌────────────────────────────────────────────────────────────┐
│  您指的是哪位张总？                                        │
│                                                            │
│  ┌────────────────────────────────────────────────────────┐│
│  │ ○ 张总（A3项目负责人）                      ⭐ 常联系   ││ ← Sorted by score
│  ├────────────────────────────────────────────────────────┤│
│  │ ○ 张总（B2项目）                                       ││
│  ├────────────────────────────────────────────────────────┤│
│  │ ○ 张司机                                               ││
│  └────────────────────────────────────────────────────────┘│
│                                                            │
│                                             [ 确认 ]       │
└────────────────────────────────────────────────────────────┘
```

| Element | Behavior |
|---------|----------|
| **⭐ 常联系 badge** | Shown if `confirmationCount >= 5` |
| **Sort order** | Highest score first (Prism §5.4 algorithm) |
| **[确认]** | Persists selection → triggers reinforcement (+1 count) |

---

**Reinforcement Feedback:**

| User Action | System Response |
|-------------|-----------------|
| Selects from picker | Toast: "已记住偏好" (fade after 1.5s) |
| Overrides auto-resolution | Toast: "已更正" + counter adjustment |
