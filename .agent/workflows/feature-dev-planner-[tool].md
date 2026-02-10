---
description: Gateway workflow for feature development - enforces anti-drift protocol, OS Model alignment, and 3-level testing
---

# Feature Dev Planner

> **Gateway Workflow**: Run `/feature-dev-planner [feature area]` before starting any feature work.

---

## Usage

```
/feature-dev-planner memory center
/feature-dev-planner scheduler
/feature-dev-planner analyst mode
```

---

## Phase 0: Spec Check (Mandatory Gate)

Before ANY planning, read the relevant spec:

1. **Locate spec**: `docs/cerb/[feature]/spec.md`
2. **Read COMPLETELY** — no skimming
3. **Quote** the exact section being implemented

### Spec Alignment Checklist

```markdown
### Spec Quote
> [Paste exact spec text here]

### My Understanding
[Explain what you will build]

### Mismatch?
- [ ] Spec is clear, I understand it
- [ ] Spec is unclear, need clarification from USER
- [ ] Spec doesn't cover this, need spec update FIRST
```

**If mismatch → STOP. Do not proceed.**

> [!IMPORTANT]
> **Docs-First Protocol** (`.agent/rules/docs-first-protocol.md`):
> If spec doesn't exist → CREATE via `/feature-cerb-pattern` before any code.
> If spec doesn't cover this change → UPDATE spec first, then implement.

### ⚠️ Anti-Invention Gate

| Quote Type | Example | Valid Spec? |
|------------|---------|-------------|
| **Wave title** | "Wave 3: Location Conflict" | ❌ NO — summary, not behavior |
| **Behavior spec** | Sequence diagram, I/O examples, data flow | ✅ YES |
| **Tracker milestone** | "Extend overlap logic to shared resources" | ❌ NO — roadmap, not spec |

**If you can only quote a title/summary → spec doesn't exist for this behavior.**

**Rule**: You may only implement behavior that is **explicitly specified**:
- Sequence diagram
- Example I/O
- Data flow description
- Validation rules

**If behavior is not specified**:
1. Ask USER for spec clarification
2. OR request spec update before implementation

**Never invent behavior to fill gaps in spec.**

---

## Phase 0.1: OS Model Alignment (Mandatory Gate)

> **Reference**: `docs/specs/os-model-architecture.md`

Before planning, classify the feature's OS layer:

```markdown
### OS Layer Classification
- **Feature**: [Name]
- **Layer**: [RAM Application | SSD Storage | Kernel | File Explorer]
- **Justification**: [Why this layer?]
```

### Layer-Specific Rules

| If Layer = | Then |
|------------|------|
| **RAM Application** | Feature reads/writes through SessionWorkingSet. No direct repo access. |
| **SSD Storage** | Feature is a passive store. No session awareness. |
| **Kernel** | Feature manages RAM lifecycle. Only ContextBuilder qualifies. |
| **File Explorer** | Feature reads SSD directly for dashboards. No session dependency. |

### OS Alignment Checklist

```markdown
- [ ] OS Layer declared in spec header
- [ ] Data flow matches layer rules
- [ ] No RAM Application accesses repos directly
- [ ] Write-through pattern used (if RAM Application)
- [ ] SessionWorkingSet interaction documented
```

**If OS Layer not declared in spec → STOP. Update spec first.**

---

## Phase 0.5: UI Discovery (Check for Existing UI)

**BEFORE planning to build UI, check if it already exists.**

### UI Audit Checklist

```bash
# Search for UI components by feature name
find app-prism/src/main/java -name "*[FeatureName]*" -type f | grep -i "ui/"

# Search for ViewModels
find app-prism/src/main/java -name "*ViewModel.kt" | xargs grep -l "[FeatureName]"

# Search for Composables
grep -rn "@Composable" app-prism/src/main/java --include="*[FeatureName]*.kt"
```

### Decision Matrix

| UI Status | Action | Plan Type |
|-----------|--------|-----------|
| **UI exists, fully wired** | Skip UI work | Backend/Logic only |
| **UI exists, NOT wired** | Wire existing UI | **UI Wiring** plan |
| **UI partially exists** | Complete + wire | Hybrid plan |
| **UI does not exist** | Build from scratch | Full UI implementation |

### Screenshot Verification (When Provided)

If USER supplies screenshot of existing UI:

1. **Verify file match**: Screenshot → UI file correspondence
2. **Document in plan**: Embed screenshot with caption
3. **Update spec**: If UI undocumented, add to spec's "UI Components" section

**Template for UI Wiring Plan**:

```markdown
## Existing UI Components

| Component | File | Status |
|-----------|------|--------|
| [ComponentName] | `ui/path/File.kt` | ✅ Exists |
| [ViewModel] | `ui/path/ViewModel.kt` | ✅ Exists |
| [Service Interface] | `domain/Service.kt` | ✅ Exists |

**Screenshot Verification**: 
![UI Screenshot](path/to/screenshot.png)
*Caption: Existing [FeatureName] modal showing [X, Y, Z]*

### Missing Layer: Glue Implementation

**Need to build**: `Real[Service]` to wire UI → Backend

**Data Flow**:
```
Existing UI → Existing ViewModel → Missing Service Impl → Existing Backend
                                        ↑ BUILD THIS
```
```

### Anti-Pattern: Building UI That Exists

**❌ DON'T**:
- Assume UI doesn't exist without searching
- Build new UI when existing UI is 90% done
- Ignore existing UI because "it's not wired yet"

**✅ DO**:
- Search for UI components FIRST
- Wire existing UI if it's close enough
- Propose spec update if UI exists but undocumented

---

## Phase 1: Contamination Cleanup

Before implementing ANYTHING:

1. **Check Fakes** for hardcoded test data → DELETE
2. **Check for TODO stubs** → DELETE
3. **Check for skeleton implementations** → DELETE

```bash
# Contamination audit
grep -rn "TODO" app-prism/src/main/java/com/smartsales/prism/data/fakes/
grep -rn "Phase 2" app-prism/src/main/java/
```

**Clean slate = correct implementation.**

---

## Phase 2: First Principles Check

### LLM vs Kotlin Decision Tree

| Question | If YES → | If NO → |
|----------|----------|---------|
| Does this require understanding language/context? | LLM | Kotlin |
| Is this pure math/logic? | Kotlin | LLM |
| Does this need disambiguation/inference? | LLM | Kotlin |

**Examples:**
- "Is 3pm free?" → **Kotlin** (interval math)
- "Who is 张总?" → **LLM** (context understanding)
- "Parse 下周三" → **LLM** (NLP)
- "Check for overlap" → **Kotlin** (time math)

### Anti-Pattern Check

| Anti-Pattern | Detection | Fix |
|--------------|-----------|-----|
| Premature abstraction | Interface with 1 impl | Just use the class |
| Kotlin for LLM work | Parsing/disambiguation | Let LLM do it |
| Over-engineering | "This might be useful later" | YAGNI — delete it |

---

## Phase 3: Implementation Plan

Only after Phases 0-2 pass:

```markdown
### Feature: [Name]
### Spec Reference: [File:Line#]

### Deliverables
| File | Purpose | Status |
|------|---------|--------|
| ... | ... | 🔲 |

### Anti-Drift Audit
- [ ] Spec section quoted
- [ ] No skeleton data in Fakes
- [ ] LLM vs Kotlin justified
- [ ] L2 scenario planned
- [ ] OS Layer classified and documented
- [ ] RAM interaction rules verified (if Application layer)
```

---

## Phase 4: Three-Level Testing

### L2 Applicability Matrix

| Feature Type | Example | L1 (Unit) | L2 (Debug HUD) | L3 (On-Device) |
|--------------|---------|-----------|----------------|----------------|
| **UI Surface** | Scheduler, Analyst Mode | ✅ | ✅ Required | ✅ |
| **Invisible Infra** | Memory Center, ScheduleBoard | ✅ | ❌ Skip | ❌ Skip |

**Rule**: L2 tests are ONLY for features with user-visible UI that receives input.

### When to Use L2

| ✅ Use L2 | ❌ Skip L2 |
|----------|-----------|
| Feature has a Drawer/Screen | Feature is pure backend index |
| User can trigger via input bar | Feature is consumed by other features |
| Pipeline goes: Input → LLM → UI | Pipeline is index/cache/query |

### L2 Debug HUD Pattern (UI Features)

For features with UI surface, use the centralized Debug HUD (🐛 icon):

1. **Input bar** for natural language simulation (bypasses hardware)
2. **Full pipeline** via Orchestrator (not just domain layer)
3. **Logcat output** for verification

```bash
# L2 verification command
adb logcat -s L2DebugHud:D
```

**Location**: `ui/debug/L2DebugHud.kt`

### L1-Only Pattern (Invisible Infra)

For invisible infrastructure:

```kotlin
// Just unit tests in src/test/
class ScheduleBoardTest {
    @Test fun `overlap detection works`() { ... }
    @Test fun `index updates correctly`() { ... }
}
```

**No Debug HUD needed.** Unit tests are sufficient.

### Testing Gate

| Level | What | UI Features | Infra Features |
|-------|------|-------------|----------------|
| **L1** | `./gradlew testDebugUnitTest` | ✅ Required | ✅ Required |
| **L2** | Debug HUD simulation | ✅ Required | ❌ Skip |
| **L3** | Full on-device with real UX | ✅ Required | ❌ Skip |

**L2 MUST pass before L3** (for UI features only).

---

## Phase 4.5: Doc Sync (Mandatory After Code)

> **Rule**: `.agent/rules/docs-first-protocol.md` — Gate 3 (Post-Flight)

After implementation AND after tests pass:

```markdown
### Doc Sync Checklist
- [ ] Domain models in spec match code
- [ ] UI states in spec match code (state tables)
- [ ] Wave status updated (🔲 → ✅ SHIPPED)
- [ ] Ship criteria boxes checked
- [ ] Implementation status table updated
- [ ] Tracker.md tech debt updated (if applicable)
```

**Run `/cerb-check` on updated docs to verify compliance.**

| If You Changed... | Update This |
|-------------------|-------------|
| Sealed class variants | Spec → Domain Models section |
| UI enum values | Spec → States table |
| Interface signatures | `interface.md` |
| Any ship criteria met | Spec → Ship Criteria checkboxes |
| New tech debt found | `tracker.md` → Tech Debt section |

**❌ Do NOT mark a wave as shipped without updating docs.**

---

## Enforcement

**Do NOT proceed to implementation if:**

- [ ] ❌ Lessons-learned.md NOT read
- [ ] Spec not read completely
- [ ] OS Layer not classified
- [ ] Fakes contain hardcoded test data
- [ ] LLM vs Kotlin decision not justified
- [ ] No L2 scenario planned

---

## Phase -2: READ TRACKER (FIRST!)

**MANDATORY**: Before ANY feature work, read `docs/plans/tracker.md`:

```bash
# MANDATORY — verify feature is tracked
cat docs/plans/tracker.md
```

### Tracker Gate Checklist

| Check | Action if FAIL |
|-------|----------------|
| Feature exists in tracker? | STOP. Propose tracker update first. |
| Wave is 🔲 (not already shipped)? | STOP. Pick next wave. |
| Dependencies satisfied? | STOP. Build dependencies first. |

**If feature NOT in tracker → Do NOT build. Propose tracker update first.**

---

## Phase -1: READ LESSONS LEARNED

**BEFORE touching any code, read the lessons file:**

```bash
# MANDATORY — do this SECOND
cat .agent/rules/lessons-learned.md
```

| Check | What to Look For |
|-------|------------------|
| **Similar symptom** | Has this bug pattern been seen before? |
| **Same file/component** | Is there a known gotcha for this area? |
| **UI issue** | Check Compose patterns (scrim, swipe, click) |
| **Data flow issue** | Check sealed class, parser, pipeline lessons |

**If a lesson matches → apply the documented fix IMMEDIATELY. Do NOT reinvent.**

---

## Phase 5: LOG FIX (After User Confirms)

**After USER says "problem fixed" (not just BUILD SUCCESSFUL):**

1. Open `.agent/rules/lessons-learned.md`
2. Add entry using the template:

```markdown
### [SHORT TITLE] — [DATE]

**Symptom**: What the user reported  
**Root Cause**: The actual problem  
**Wrong Approach**: What didn't work  
**Correct Fix**: What worked  
**File(s)**: Where the fix was applied  
**Status**: ✅ CONFIRMED [DATE]
```

3. Add ABOVE the `<!-- Add new lessons above this line -->` marker

**⚠️ Do NOT log until user confirms. BUILD SUCCESS ≠ FIXED.**

---

## Quick Reference

| Phase | Gate |
|-------|------|
| **-2** | ✅ Tracker read, feature exists |
| **-1** | ✅ Lessons read |
| **0** | Spec quoted |
| **0.1** | ✅ OS Layer classified, alignment verified |
| **0.5** | ✅ UI Discovery — existing UI found or confirmed absent |
| **1** | Fakes clean |
| **2** | First principles checked |
| **3** | Plan approved |
| **4** | L1 → L2 → L3 pass |
| **4.5** | ✅ Docs synced (spec + tracker) |
| **5** | ✅ Lesson logged (if bug fix) |

---

## Related Rules

- `.agent/rules/anti-drift-protocol.md` — Full anti-drift rules
- `.agent/rules/docs-first-protocol.md` — **Docs-first enforcement (3 gates)**
- `.agent/rules/lessons-learned.md` — **READ FIRST, UPDATE AFTER FIX**
