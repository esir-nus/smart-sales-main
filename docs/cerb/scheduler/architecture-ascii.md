# Monolithic Scheduler Architecture (Path A focus)

```text
┌───────────────────────────────────────────────┐
│ 🎤 User Speaks: "remind me to sign contract   │
│                 with legal tomorrow at 2"     │
└───────────────────────────────────────────────┘
                        │
          (Orchestrator routes to Monolithic flow)
                        │
          ▼             
┌───────────────────────────────────────────────┐
│ ⚡ Executor (LLM Semantic Parsing)            │
│ Converts raw audio transcript into JSON with: │
│ - title: "sign contract with legal"           │
│ - startTime: "2026-03-16 14:00"               │
│ - urgency: "L3_NORMAL"                        │
└───────────────────────────────────────────────┘
          │        
          ▼
┌───────────────────────────────────────────────┐
│ 🛡️ SchedulerLinter.lint(json)                │
│ Enforces pure deterministic validation rules: │
│ - Validates ISO format & normalizes           │
│ - Ensures time is not in the past             │
└───────────────────────────────────────────────┘
          │
      (LintResult.Success)
          │
          ▼
┌───────────────────┐
│ Insert Task DB    │  -->  Task Title: "sign contract with legal"
│                   │  -->  Time: 14:00 Tomorrow
└─────────┬─────────┘  
          │
          ▼
┌───────────────────────────────────────────────┐
│ Timeline UI instantly renders card at the     │
│ perfectly correct semantic slot.              │
└───────────────────────────────────────────────┘
```
