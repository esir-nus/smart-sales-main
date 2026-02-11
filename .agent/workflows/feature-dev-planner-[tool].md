---
description: Gateway workflow for feature development - enforces single-spec scope, anti-drift protocol, OS Model alignment, and 3-level testing
---

# Feature Dev Planner

> **Gateway Workflow**: Run before starting any feature work.
> **Core Rule**: ONE task = ONE `spec.md` + ONE `interface.md`. No exceptions.

---

## Execution Order

| Phase | Gate | Action |
|-------|------|--------|
| **-2** | Tracker | Read `docs/plans/tracker.md`, verify feature exists, find owning Cerb shard |
| **-1** | Lessons | Read `.agent/rules/lessons-learned.md`, check for matching patterns |
| **0** | Spec Scope | Read owning spec COMPLETELY, fill Cerb Scope Declaration, quote behavior |
| **0.1** | OS Model | Classify OS layer, verify layer rules |
| **0.5** | UI Discovery | Search for existing UI before building new |
| **1** | Clean Slate | Delete skeleton data from Fakes, TODOs, stubs |
| **2** | First Principles | LLM vs Kotlin decision, anti-pattern check |
| **3** | Plan | Write implementation plan with Anti-Drift Audit |
| **4** | Test | L1 (unit) → L2 (debug HUD, UI only) → L3 (on-device, UI only) |
| **4.5** | Ship Gate | Update spec `state`, tracker index, interface-map, run `/cerb-check` |
| **5** | Lesson Log | If bug fix, log AFTER user confirms |

**STOP at any failed gate. Do not proceed.**

---

## Phase -2: Tracker → Cerb Shard

```bash
cat docs/plans/tracker.md
```

**Tracker is an INDEX, not a spec.** It faithfully reflects spec implementation status — it never invents tasks. Find the owning Cerb shard:

```
Tracker entry → "session-context: PARTIAL"
                     ↓
              Owning shard: docs/cerb/session-context/
                     ↓
              spec.md (state: PARTIAL) → read spec waves → find 🔲 wave
```

| Tracker Pattern | Action |
|----------------|--------|
| Entry references one Cerb shard | ✅ Use that spec |
| Entry is vague / cross-cutting | ⚠️ Ask USER which spec owns it |
| Entry spans multiple shards | ❌ Decompose into per-shard tasks |

> [!WARNING]
> **Tracker = index of spec states. Spec = truth of what to build.**
> Tracker entries are derived FROM specs, never the other way around.

---

## Phase 0: 🔒 Single Spec Scope (CRITICAL)

Read `docs/cerb/[feature]/spec.md` **COMPLETELY**. If no spec exists, run `/cerb-spec-template` first to create one. Then fill:

```markdown
### Cerb Scope Declaration
- **Owning Spec**: `docs/cerb/[feature]/spec.md`
- **Owning Interface**: `docs/cerb/[feature]/interface.md`
- **Spec State**: [SPEC_ONLY | PARTIAL | SHIPPED]
- **Target Wave**: [Wave N: title from spec's wave table]
- **Business logic comes from**: This spec ONLY
```

| Doc Type | Use For | Source Business Logic? |
|----------|---------|------------------------|
| **Owning spec + interface** | Implement | ✅ YES — sole source |
| **Other cerb specs** | Understand communication | ❌ NO — context only |
| **interface-map.md** | Verify edges | ❌ NO — topology only |
| **tracker.md** | Find owning spec | ❌ NO — index only |

### Anti-Invention Gate

| Quote Type | Valid Spec? |
|------------|-------------|
| Behavior spec (sequence diagram, I/O, data flow) | ✅ YES |
| Wave title / tracker entry | ❌ NO — index, not spec |
| Another spec's section | ❌ NO — not your spec |

**If you can only quote a title → spec doesn't exist. Ask USER or update spec first.**

---

## Phase 0.1: OS Model Alignment

| Layer | Rule |
|-------|------|
| **RAM Application** | Read/write through SessionWorkingSet only. No direct repo access. |
| **SSD Storage** | Passive store. No session awareness. |
| **Kernel** | Manages RAM lifecycle. Only ContextBuilder. |
| **File Explorer** | Reads SSD directly for dashboards. |

If OS Layer not in spec → update spec first.

---

## Phase 0.5: UI Discovery

```bash
find app-prism/src/main/java -name "*[Feature]*" -type f | grep -i "ui/"
```

| Exists? | Action |
|---------|--------|
| Fully wired | Skip UI work |
| Exists but not wired | Wire it (don't rebuild) |
| Doesn't exist | Build from scratch |

---

## Phase 2: First Principles

| Needs language understanding? | → LLM |
|-------------------------------|-------|
| Pure math/logic? | → Kotlin |
| Disambiguation/inference? | → LLM |

---

## Phase 3: Implementation Plan

```markdown
### Feature: [Name]
### Owning Spec: docs/cerb/[feature]/spec.md (state: PARTIAL)
### Target Wave: Wave N

### Anti-Drift Audit
- [ ] Cerb Scope Declaration filled (ONE spec + ONE interface)
- [ ] ALL business logic quotes from owning spec ONLY
- [ ] Spec section quoted with line numbers
- [ ] OS Layer classified
- [ ] LLM vs Kotlin justified
- [ ] Fakes clean (no skeleton data)
- [ ] L2 scenario planned (if UI feature)
```

**STOP if any box unchecked.**

---

## Phase 4.5: Ship Gate (Three Parallel Updates)

After tests pass, update THREE docs in parallel:

### A. Spec State Update

Update the spec header's `state` field:

| State | Meaning | When to Set |
|-------|---------|-------------|
| `SPEC_ONLY` | Spec written, no code yet | After spec creation |
| `PARTIAL` | Some waves shipped, some 🔲 | After shipping a wave (not the last) |
| `SHIPPED` | All waves done | After shipping the final wave |

**Format**: Add `> **State**: PARTIAL` to spec header block.

**Also**: Update wave table rows (🔲 → ✅ SHIPPED) and domain models if changed.

### B. Tracker Index Update

Tracker is a **faithful index** — it mirrors spec states, never invents tasks.

```markdown
| Cerb Shard | State | Current Wave | Notes |
|------------|-------|-------------|-------|
| session-context | PARTIAL | Wave 3: Pointer Cache | pathIndex + EntityState machine |
| scheduler | SHIPPED | — | All waves complete |
| memory-center | SPEC_ONLY | Wave 1 | Spec written, no code |
```

**Rule**: Tracker row = one Cerb shard. State matches spec header. Nothing invented.

### C. Interface Map Update

Update `docs/cerb/interface-map.md` when interfaces change:

| Trigger | Action |
|---------|--------|
| New interface method added | Add row to ownership table |
| New cross-module edge | Add note with data flow |
| Interface method removed | Remove row |
| Status changed (📐→✅) | Update status column |

### Ship Gate Checklist

```markdown
- [ ] Spec `state` header updated (SPEC_ONLY / PARTIAL / SHIPPED)
- [ ] Spec wave table rows updated (🔲 → ✅)
- [ ] Tracker index row matches spec state
- [ ] interface-map.md updated (if interface changed)
- [ ] `/cerb-check` PASS on modified spec.md
- [ ] `/cerb-check` PASS on modified interface.md (if changed)
```

**❌ Do NOT mark wave shipped without passing ALL checks.**

---

## Related Rules

- `.agent/rules/anti-drift-protocol.md`
- `.agent/rules/docs-first-protocol.md`
- `.agent/rules/lessons-learned.md`
