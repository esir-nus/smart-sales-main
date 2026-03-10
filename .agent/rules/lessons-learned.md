---
description: Quick triggers for known bugs. If symptom matches, read docs/reference/agent-lessons-details.md
trigger: always_on
---

# Agent Lessons Learned (Index)

> **🔴 Senior Engineer Checks**: Loading 400 lines of historical bugs into every prompt burns context and dilutes focus. 
> Below are strictly **Symptoms & Triggers**.
> 
> **When to act**: If your current task hits a trigger below, you MUST read the full context in `docs/reference/agent-lessons-details.md` before writing code.
> **When to update**: Only add to this index + the details doc AFTER the user confirms "problem fixed".

## 🧠 LLM & Prompts
- **Context is empty** → LLM will hallucinate. Add explicit guards. (Ref: *LLM Fabricates History*)
- **Missing JSON fields** → Prompt is likely missing strict requirements. (Ref: *Prompt-Linter Data Gap*)
- **Complex string matching** → Don't use Kotlin math. Let LLM route IDs. (Ref: *LLM Semantic Mapping vs Hardcoded Math*)
- **Implementing "Concepts"** → Verify against `data class` schema, not the name. (Ref: *Conceptual Name → Wrong Implementation*)
- **Forcing JSON for everything** → JSON is for routing (use native API), Markdown/Text is for context. (Ref: *JSON Schema Fragility vs Raw Markdown*)

## 🏗️ Architecture & Specs
- **Compiler-Driven Extraction** → Moving files + hacking build.gradle to pass compilation is NOT modularization. Check target architecture (interface-map) and invert dependencies instead. (Ref: *Physical vs Logical Modularization*)
- **Reading multiple specs** → STOP. One task = ONE spec.md. (Ref: *Multi-Spec Drift*)
- **Tracker wave titles** → These are NOT specs. Do not invent behavior from them. (Ref: *Spec Invention from Wave Titles*)
- **Missing UI details** → Flag as spec gap. Do not invent gestures. (Ref: *Spec Drift: Inventing UI Features*)
- **Inventing UI for Data Hubs** → Agentic apps have invisible data layers. The LLM is the "user" of the hub. (Ref: *Metaphor → Hallucinated UI*)
- **Independent flows sharing a resource** → Serialize at the lowest transport level. (Ref: *Application-Level Coupling*)
- **Fake → Real DB swap** → Fakes hide "empty state" bugs. Audit empty returns. (Ref: *Fake→Real Swap*)
- **Soft-deprecated fields in core pipelines** → Rip them out. They confuse future agents/devs. (Ref: *Soft-Deprecation Rot ("memoryHits")*)

## 🐛 Core Data & Kotlin
- **Stale UI after update** → 90% chance it's a missing Flow trigger, not persistence. (Ref: *Ghost UI After Update*)
- **Unassigned Flow reference** → Calling `asSharedFlow()` without `combine/collect` does nothing. (Ref: *Dead Flow Reference*)
- **Post-insert conflicts** → Insert + refresh = conflict with yourself. Add exclusion ID. (Ref: *Post-Insert Self-Conflict*)
- **Data missing but logs show it** → Check sealed class parameters and Mapper (`toDomain`). (Ref: *Sealed Class Data Gap*, *Entity-Domain Mapping Gap*)

## 🎨 UI & Compose
- **UI Element entirely missing** → Check upstream data pipeline logs first. (Ref: *UI Element Not Appearing*)
- **Drawer scrim issues** → Separate `AnimatedVisibility` for scrim and drawer. (Ref: *Compose Scrim Inside AnimatedVisibility*)
- **Drawer click passthrough** → Modal content must explicitly `consume` pointer events. (Ref: *Modal Drawer Click Passthrough*)
- **Swipe-to-dismiss background** → Use `targetValue != Settled`, not `dismissDirection`. (Ref: *SwipeToDismiss Background Visibility*)
- **Date picker selection missing** → Ensure "selected" vs "today" UI states are distinct. (Ref: *Calendar Selected vs Today State*)

## 🔌 APIs & Network
- **Pre-signed OSS URLs vs REST** → Aliyun V2 returns artifacts as URLs, not REST endpoints. (Ref: *Hallucinated REST Endpoint*)
- **Silent Signature Crashes** → Check build script fallback logic for unified vs dedicated keys. (Ref: *OSS Credentials vs Unified Aliyun Key*)
- **Reconnecting BLE** → Never fire-and-poll with fixed delays. Use suspend & await. (Ref: *Reconnect Race Condition*)
- **Gating on HTTP** → Don't gate BLE connection success on HTTP server reachability. (Ref: *HTTP Gate Conflating Connection Concerns*)

## 🛠️ Tooling & Editor
- **Compiler line number errors** → Do not ignore exact line numbers. Often caused by injecting markdown tags. (Ref: *Markdown Tag Injection & Ignoring Line Numbers*)
- **NoClassDefFoundError on standalone interfaces** → D8 may drop them. Move declaration into consumer file. (Ref: *D8/R8 Silent Interface Dropping*)
