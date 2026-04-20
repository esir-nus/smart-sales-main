> **OS Layer**: RAM Application

# Model Routing Specification

## Architecture Goal
Transition from scattered strings and hardcoded parameters to a single-source-of-truth `LlmProfile` registry. 
**Crucial Rule**: The actual routing logic is very simple and manually selected by the caller, not a smart automated router. Services know what their task is (e.g. parsing vs executing). They must manually pull the appropriate profile.

## Core Implementations

### The LlmProfile Entity
The registry provides static `LlmProfile` objects (not strings) acting as a "Kotlin model management hub". This encapsulates:
1. `modelId` (String)
2. `temperature` (Float)
3. `skillTags` (Set<String>)

### Execution Tiers

| Profile | Task Profile | Target Model | Context Needed | Opt. Temp |
|---------|--------------|--------------|----------------|-----------|
| **EXTRACTOR** | Fast parsing, Named Entity Recognition, Intent extraction | `qwen-turbo` | 100K | `0.0f` |
| **SCHEDULER_EXTRACTOR** | Scheduler-only extraction, create fallback, reschedule fallback | `qwen-plus` | 1M | `0.0f` |
| **PLANNER** | Retrieving memory, reading large context, strategy formulation | `qwen-plus` | 1M | `0.5f` |
| **EXECUTOR** | Generating tool execution, strict JSON structure | `qwen3-max-2026-01-23` | 32k | `0.0f` |
| **ONBOARDING_CONSULTATION** | First-run consultation reply with fast natural language | `qwen-turbo` | 100K | `0.4f` |
| **ONBOARDING_PROFILE_EXTRACTION** | First-run structured profile extraction JSON | `qwen-turbo` | 100K | `0.0f` |

## Scheduler Lane Split

Scheduler parsing no longer shares the generic extractor lane.

- shared `EXTRACTOR` remains on `qwen-turbo` for general bounded extraction work
- scheduler-owned fallback services use `SCHEDULER_EXTRACTOR`
- this is both a lane split and a scheduler-only model upgrade from `qwen-turbo` to `qwen-plus`
- deterministic scheduler parsing such as `ExactTimeCueResolver` remains unchanged; this change only affects the scheduler LLM extraction lane

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Architecture Schema | ✅ SHIPPED | `interface.md` + `spec.md` definition |
| **2** | Kotlin Registry Refactor | ✅ SHIPPED | Introduce `LlmProfile.kt` and rewrite `ModelRegistry.kt`, delete `ModelRouter.kt` |
| **3** | Direct Provider Wiring | ✅ SHIPPED | Route `ModelRegistry` constants directly into Dashscope builder, bypassing smart logic. |
| **4** | Two-Stage Analyst Pipeline | 🔲 PLANNED | Split Analyst logic: `PLANNER` (plus) -> `EXECUTOR` (max) |
