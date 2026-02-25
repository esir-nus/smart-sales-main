---
description: Create a standardized Cerb feature specification (Interface + Spec + Wave Plan)
---

# Cerb Spec Scaffolder

> **Purpose**: Create a standardized Cerb feature specification.
> **Output**: `interface.md` + `spec.md` with OS Layer and Wave Plan.
> **Next Step**: Once spec is created, use `/feature-dev-planner` to implement it.

---

## Step 1: Define Context Boundary (Interface)

Create `docs/cerb/[feature-name]/interface.md`.

**Rule**: This is the PUBLIC contract. Consumers read this, not `spec.md`.

```markdown
# [Feature Name] Interface

> **Owner**: [Module]
> **Consumers**: [Who uses this?]

## Public Interface

```kotlin
interface [Feature]Service {
    /**
     * [Description]
     */
    suspend fun doSomething(input: Input): Result
}
```

## Data Models

```kotlin
data class Input(...)
data class Result(...)
```
```

---

## Step 2: Define Internal Implementation (Spec)

Create `docs/cerb/[feature-name]/spec.md`.

### 2.1 OS Layer Declaration (Mandatory)

> **Reference**: `docs/specs/os-model-architecture.md`

Every spec MUST declare its OS layer at the top:

```markdown
> **OS Layer**: [RAM Application | SSD Storage | Kernel | File Explorer]
```

| Layer | Meaning | Data Flow |
|-------|---------|----------|
| **RAM Application** | Logic/Intelligence | Read/write SessionWorkingSet |
| **SSD Storage** | Persistence | Room DB, File System |
| **Kernel** | Lifecycle | ContextBuilder, startup/shutdown |
| **File Explorer** | Dashboard/UI | Direct read-only access |

### 2.2 Wave Plan

Define how the feature will be built incrementally.

```markdown
## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Logic | 🔲 PLANNED | Interface + Fake + RealImpl |
| **2** | Wiring | 🔲 PLANNED | Connect to ViewModel/Orchestrator |
| **3** | Polish | 🔲 PLANNED | Edge cases, error handling |
```

---

## Step 3: Register in Tracker and Interface Map

Update `docs/plans/tracker.md`:

1.  Add new row to **Cerb Spec Index**.
2.  Set State to `SPEC_ONLY`.
3.  Set Next Wave to "Wave 1: [Title]".

Update `docs/cerb/interface-map.md`:
1. Decide which layer the module belongs to
2. Add a new row documenting its ownership and any known reads

---

## Checklist

- [ ] `interface.md` created (Public contract)
- [ ] `spec.md` created (Internal logic)
- [ ] OS Layer declared in `spec.md` header
- [ ] Wave Plan defined in `spec.md`
- [ ] Registered in `tracker.md` as `SPEC_ONLY`
- [ ] Registered in `interface-map.md` (if applicable/new module)

**Done. Now verify with `/cerb-check` and then start building with `/feature-dev-planner`.**
