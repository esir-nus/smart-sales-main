# Prism Memory Architecture: The OS Model

> **Purpose**: The north star for all memory-related spec upgrades.  
> **Scope**: Conceptual guidance only. Individual specs implement the details.  
> **Status**: ✅ APPROVED — 2026-02-10 (Review Conference)

---

## The Mental Model

Prism's memory system operates like an OS:

| OS Concept | Prism Equivalent | Role |
|------------|------------------|------|
| **RAM** | Session Working Set | Per-session workspace. All modules operate here. |
| **SSD** | Room DB (all repos) | Permanent storage. Source of truth. |
| **Kernel** | ContextBuilder | Loads data from SSD into RAM. Manages what's active. |
| **Applications** | RL Module, EntityWriter, Executor | Read/write through RAM. Never poke SSD directly. |
| **File Explorer** | CRM Hub | Reads SSD directly for dashboard views (not session-scoped). |

---

## Why This Model

**The current silo design** requires every module to know WHERE data lives and WHO to ask for it. When Coach mode forgets to pass `entityIds`, client habits go missing. When a new mode is added, it must learn the same wiring.

**The OS model** eliminates this: if an entity is mentioned in conversation, its data is on the RAM. Period. Every module just reads what's in front of it.

---

## The RAM: Three Sections

The Session Working Set is incrementally built by the Kernel (ContextBuilder) as conversation progresses.

### Section 1 — Distilled Memory
**What**: Resolved entity pointers + active memory references.  
**Built by**: ContextBuilder (entity resolution, memory search).  
**Key property**: This section's entity IDs drive auto-population of Section 3.

### Section 2 — User Habits (Global)
**What**: The user's own preferences (mode-independent).  
**Loaded**: Once at session start.  
**Example**: "I prefer 30min meetings", "Reply in Chinese."

### Section 3 — Client Habits (Contextual)
**What**: Per-entity behavioral data.  
**Loaded**: Automatically when Section 1 gains a new entity pointer.  
**Key property**: As conversation shifts from 张总 to 李总, Section 3 evolves — no explicit wiring needed.  
**This is the breakthrough**: RL Module always operates on the correct client context because the canvas already points there.

---

## Interaction Rules

### 1. Applications Work on RAM
No module reads from or writes to SSD directly during a session. The RAM is the workspace.

### 2. Write-Through, Never Write-Back
Every RAM write simultaneously persists to SSD. No flush, no sync, no crash risk. This is a mobile app — Room writes are cheap.

### 3. Kernel Owns the RAM Lifecycle
Only ContextBuilder decides what gets loaded into RAM. Applications don't load data themselves — they request it or react to what's already there.

### 4. SSD Remains Source of Truth
RAM is a working copy. CRM Hub (the File Explorer) reads SSD directly because dashboards show historical data, not just the current session.

---

## What Each Spec Must Adopt

> [!IMPORTANT]  
> Each spec upgrade should be done in its own session using this document as the guiding reference.

| Current Spec | Guidance |
|-------------|----------|
| **session-context** | Evolve from "alias cache" into the full RAM with 3 sections. This is the biggest change — it becomes the central workspace. |
| **rl-module** | Stop reading/writing UserHabitRepo directly. Operate through RAM sections 2 & 3. The RL Module becomes an "application" that runs on the workspace. |
| **entity-writer** | Stop reading/writing EntityRepo directly. Operate through RAM section 1. Updates flow through the workspace. |
| **memory-center** | No change. It's the SSD. It stays dumb and permanent. |
| **entity-registry** | Resolution results land in RAM section 1 as rich data, not just IDs. |
| **client-profile-hub** | Becomes the "File Explorer" — reads SSD for dashboard/history views. NOT session-scoped. Simpler role. |
| **coach** | Deletes all special `entityIds` handling. Just reads RAM. The bug goes away. |
| **scheduler** | Deletes `buildWithClues` entity extraction. Just reads RAM. Simpler pipeline. |

---

## What This Kills

| Problem | Why It Dies |
|---------|-------------|
| Coach mode misses client habits | RAM always has them if entity was mentioned |
| Each mode must manually wire entityIds | Kernel loads them; apps just read the canvas |
| RL Module needs explicit "who is the current client?" | Section 3 already points to the right habits |
| Adding a new mode requires learning silo wiring | New modes just read RAM — zero integration cost |

---

## Boundaries

| ✅ RAM Handles | ❌ RAM Does NOT Handle |
|----------------|----------------------|
| Active session data | Historical analysis (CRM Hub reads SSD) |
| Current entity context | Full entity search (Entity Registry hits SSD) |
| Write-through persist | Batch operations or migrations |
| Per-session optimization | Cross-session learning (future wave) |
