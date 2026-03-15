# Pipeline Valve Audit Workflow

> **Core Principle**: If you can't trace the data payload across the `PipelineValve` checkpoints in Logcat, the pipeline is broken. This workflow audits the "Google Maps" telemetry established in `project-mono-master-guide.md`.

## When to Use

| Scenario | Use This |
|----------|----------|
| A "Passenger" (data) is vanishing between layers (e.g., UI shows blank card) | ✅ Yes |
| After decoupling a new plugin (e.g., Scheduler, CRM) | ✅ Yes |
| Updating the `pipeline-valves.md` tracker | ✅ Yes |
| General Android crashes or UI glitches | ❌ Use `/SOP-debugging` |

---

## Phase 1: Valve Check (The Blueprint Audit)

**Goal**: Verify the current codebase actually implements the checkpoints declared in the Tracker.
**Evidence Required**: Direct `grep` outputs of the codebase.

```markdown
### 1. Blueprint Audit Results
Run `grep -rn "PipelineValve.tag" app-core/ core/ data/`

**Expected vs Reality:**
- [ ] `[INPUT_RECEIVED]`: (Found at [File:Line] / ❌ Missing)
- [ ] `[ROUTER_DECISION]`: (Found at [File:Line] / ❌ Missing)
- [ ] `[ALIAS_RESOLUTION]`: (Found at [File:Line] / ❌ Missing)
- [ ] `[LIVING_RAM_ASSEMBLED]`: (Found at [File:Line] / ❌ Missing)
- [ ] `[LLM_BRAIN_EMISSION]`: (Found at [File:Line] / ❌ Missing)
- [ ] `[LINTER_DECODED]`: (Found at [File:Line] / ❌ Missing)

**Domain Plugins Checked:**
- [ ] Scheduler Plugin: (Missing / Found)
- [ ] CRM Plugin: (Missing / Found)
```

**If any core checkpoints are missing**: Stop. Implement them according to `project-mono-master-guide.md` before proceeding.

---

## Phase 2: Manual Sync (The Tracker Update)

**Goal**: Ensure `docs/plans/telemetry/pipeline-valves.md` perfectly matches reality.

1. Read `docs/plans/telemetry/pipeline-valves.md`.
2. Compare its `[x] SHIPPED` statuses against the **Blueprint Audit** above.
3. If a valve is in code but marked `🔲 PENDING` in the tracker → Update the tracker to `✅ SHIPPED`.
4. If a valve is marked `✅ SHIPPED` in the tracker but missing in code → Update the tracker to `🔲 PENDING` (and flag it as Architectural Drift).

---

## Phase 3: Effectiveness Evaluation (The Live GPS Audit)

**Goal**: Prove the valves actually capture the shape of the data accurately at runtime, or identify logical gaps where the "car drove off the map."

**Agent Instructions:**
To evaluate the effectiveness, you must trace a single intent (UnifiedId) through the pipeline conceptually or literally via device logs.

### The Evaluation Matrix

Assess the current implementation against these three Antigravity criteria:

1. **One Currency Check**: Do the `payloadSize` metrics represent structurally meaningful data (e.g., `Entity Count: 3`) or just arbitrary string lengths (e.g., `String Length: 450`)? 
   - *Senior Reviewer Standard: String length is weak. Node counts, task counts, and ID counts are strong.*
2. **Blind Spot Analysis**: Is there a massive logical hop between two valves where a data transformation occurs invisibly? (e.g., from `LINTER_DECODED` to `UI_STATE_EMITTED` with zero DB logging in between).
3. **Ghost Passenger Check**: Look at the `rawDataDump` fields implemented. Do they dump the *actual* parsed data, or just a hardcoded summary? If it dumps `classification` but not `tasks`, it's an ineffective valve.

```markdown
### 3. Effectiveness Report

**Strengths (What's working):**
- [e.g., The Router Valve perfectly captures the early short-circuit conditions.]

**Blind Spots (Where the car goes off-map):**
- [e.g., We have a blind spot between `LINTER_DECODED` and the UI displaying the card. We need a `[DB_WRITE_EXECUTED]` valve.]

**Metric Quality:**
- [e.g., `LLM_BRAIN_EMISSION` logs string length, which is fine for raw JSON, but `LIVING_RAM_ASSEMBLED` needs to log the exact `EntityID` keys, not just the count.]
```

---

## Output Generation

The Agent must output the final report matching the above markdown structures, directly replying to the user. Do not execute code changes unless the user explicitly requests you to fix the identified blind spots.
