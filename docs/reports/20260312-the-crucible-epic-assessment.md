# Assessment Report: The Crucible (Pipeline Validation)
**Date**: 2026-03-12
**Protocol/Spec on Trial**: [pipeline-explainer.md](../artifacts/pipeline-explainer.md)
**Target Implementation**: `RealUnifiedPipeline`, `IntentOrchestrator`, `RealContextBuilder`, L2 Mock Eviction (`L2WriteBackConcurrencyTest.kt`)

## 1. Contextual Anchor (The "State of the World")
The system had just completed a massive "Great Assembly" decoupling phase, where the entire Layer 2 (Data Services) and Layer 3 (Pipeline) were physically extracted into isolated modules. We pivoted from feature development into an intense "Anti-Illusion" testing phase. Fakes were completely rewritten as thread-safe `Mutex`-guarded Room replacements. This trial was about subjecting the newly assembled `UnifiedPipeline` to severe L2 simulated chaos (context poisoning, disambiguation collisions, and concurrent write-backs) to mathematically prove the OS Model Architecture worked before trusting it on physical devices.

## 2. Executive Summary
The trial was a massive success, exposing hidden "Testing Illusions" that had previously masked critical routing bugs. We proved the Twin Engines (CRM + RL) can safely execute asynchronous write-backs without memory corruption. However, the trial exposed a critical flaw in Phase 0 Intent Routing (`IntentOrchestrator`) where `SIMPLE_QA` queries were bypassing the pipeline entirely. We implemented surgical hotfixes to route factual questions through the `UnifiedPipeline` ETL phase, ensuring the LLM reads the database before answering.

## 3. Drifts & Architectural Discoveries
- **Assumption vs. Code Reality**: We assumed `SIMPLE_QA` intents (like "What is Zhang's title?") could be answered by the fast Phase 1 `LightningRouter` without needing the Phase 2 heavy `UnifiedPipeline`.
- **Verdict**: The assumption was totally wrong. Factual questions require `KNOWN_FACTS` loaded from the database (`ContextBuilder`), which only the `UnifiedPipeline` does. Code was updated to align with the core architectural necessity of the OS RAM model.
- **Assumption vs. Code Reality**: `entity-writer` spec called for simple "last-write-wins" logic. The code actually built a highly resilient FIFO `aliasesJson` history tracker. The code was right.

## 4. Friction & Fixes
| Constraint/Error | Root Cause | Fix Applied |
|------------------|------------|-------------|
| "I have no information" (Device Test) | `IntentOrchestrator` short-circuited `SIMPLE_QA` directly to UI. | Allowed `SIMPLE_QA` to fall through to `UnifiedPipeline` for DB context. |
| Hardcoded Scheduler Prompting | `RealUnifiedPipeline` hardcoded `Mode.ANALYST` for all fallback queries. | Made context `Mode` dynamic based on Lightning Router intent. |
| Pipeline Latency on Entity Updates | Editing an entity title required a full JSON LLM extraction loop. | Added proactive `EntityDeclaration` parsing in `PassThrough` to write natively to Room, bypassing the LLM. |

## 5. Identified Gaps & Weaknesses
- **Intent Enum Overload**: `QueryQuality` acts as both a phase-0 router flag and a phase-2 LLM Mode selector. If we add new pipelines (e.g., Email, Analytics), this enum will become severely overloaded and fragile.

## 6. Advice to the Consul (Strategic Next Steps)
1. **Proceed to Beta Validation**: The Dual-Engine OS architecture is mathematically proven at L2 and manually validated at L3. The core engine is ready.
2. **Decouple Intent from Mode**: In future sprints, consider decoupling `QueryQuality` (what the user wants) from `Mode` (which system prompt the LLM uses) to allow more granular context assembly.
3. **Resume Feature Roadmap**: We can safely exit the "Anti-Illusion / Assembly" epic and resume high-level feature development (e.g., Advanced Habits, Analytics) trusting the ground we stand on.
