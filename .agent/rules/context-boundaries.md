---
description: Context boundary decision rules (Cerb)
trigger: always_on
---

# Context Boundaries (红绿灯法则)

> Derived from [Anthropic's Building Effective Agents](https://www.anthropic.com/engineering/building-effective-agents)

---

## Anthropic's Core Principles

1. **Maintain simplicity** — Add complexity only when it demonstrably improves outcomes
2. **Prioritize transparency** — Explicitly show the agent's planning steps
3. **Carefully craft interfaces** — Tool documentation must be clear and well-tested

---

## Decision Framework

When starting work, ask: **"Does this need shared context with other work?"**

### 🔴 Keep Together (Same Agent Context)

| Signal | Example |
|--------|---------|
| **Continuous stages** | Planning → Impl → Test for one feature |
| **Shared state** | Both parts need to track "current step" |
| **Frequent sync** | Changes in A immediately affect B |
| **Same lifecycle** | Design, build, verify one component |

**Rule**: Don't hand off mid-feature. Finish the vertical slice.

### 🟢 Can Separate (Different Context / Blackbox)

| Signal | Example |
|--------|---------|
| **Independent path** | Research task unrelated to main work |
| **Clear interface** | Only need API contract, not internals |
| **Review/Verify** | Evaluator that checks work product |
| **Parallel-able** | Multiple similar subtasks at once |

**Rule**: If it has a clean interface, treat it as blackbox.

---

## Anthropic Patterns Mapped

| Pattern | When | Context Implication |
|---------|------|---------------------|
| **Prompt Chaining** | Sequential steps, each depends on previous | 🔴 Same context |
| **Orchestrator-Workers** | Central LLM breaks down dynamic tasks | 🔴 Same context (orchestrator holds state) |
| **Parallelization** | Independent subtasks, aggregate results | 🟢 Separate contexts |
| **Evaluator-Optimizer** | One LLM evaluates another's work | 🟢 Evaluator = Blackbox |
| **Routing** | Classify input → specialized handler | 🟢 Handlers = Separate contexts |

---

## Practical Application

### For Feature Work

```
Feature = ONE context
├── Spec reading
├── Implementation  
├── Testing
└── Verification
```

Don't switch agents mid-feature. Don't hand off without completion.

### For Consultants (Evaluator-Optimizer Pattern)

```
Main work context ───────┐
                         │ Request review
                         ▼
                  /senior-eng  ← Separate context (evaluator)
                         │
                         │ Returns verdict
                         ▼
Main work context ◀──────┘
```

> **Anthropic term:** "Evaluator-Optimizer" — one LLM evaluates another's work.

Consultant sees: Work product only.
Consultant doesn't see: Full implementation history.

### For Shared Infrastructure

```
Feature Agent ──────────┐
                        │ Calls interface
                        ▼
              MemoryRepository.save()  ← Blackbox
                        │
              (Don't read implementation)
```

**Blackbox rule**: Use interface, don't read internals.

---

## Quick Decision Tree

```
Need to do X...
├─ Is X a continuation of current work?
│   └─ YES → 🔴 Same context. Keep going.
│
├─ Does X need to know my current state?
│   └─ YES → 🔴 Same context. Don't hand off.
│
├─ Can X be defined by interface/contract?
│   └─ YES → 🟢 Blackbox. Call & forget.
│
├─ Is X a review/evaluation of finished work?
│   └─ YES → 🟢 Consultant. Hand off artifact only.
│
└─ Unsure?
    └─ 🔴 Default conservative. Same context.
```

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| Hand off mid-feature to "specialist persona" | Complete feature, THEN get review |
| Read blackbox internals "to understand" | Trust the interface contract |
| Split one feature across multiple agents | One feature = one context |
| Create agent per file/component | Create agent per feature lifecycle |
