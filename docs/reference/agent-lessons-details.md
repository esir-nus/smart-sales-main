# Agent Lessons Learned (Detailed Reference)

> **Source of Truth**: This document holds the full post-mortem details for the heuristics listed in `.agent/rules/lessons-learned.md`.
> **Agent Instructions**: Read the specific section here when triggered by the index rule.
> **Update Rule**: Only add/update entries when **USER explicitly says "problem fixed"**.  
> ⚠️ **BUILD SUCCESSFUL ≠ Problem Fixed.** Wait for user confirmation.

---

## Entry Format

When adding a new lesson (after USER confirms "problem fixed"):
```markdown
### [SHORT TITLE] — [DATE]

**Symptom**: What the user reported  
**Root Cause**: The actual problem  
**Wrong Approach**: What didn't work  
**Correct Fix**: What worked  
**File(s)**: Where the fix was applied
```

---

## Lessons

### Harmony Build Proof Is Not Device Proof — 2026-04-11

**Symptom**: A Harmony lane reaches `assembleHap` success, but on-device scheduler/backend verification is still blocked or misleading because signing, deployment, or launch does not line up with the same app identity.
**Root Cause**: Compile/build proof, signing/profile readiness, `hdc` deployment, bundle identity, and runtime evidence were treated as separate chores instead of one lane contract. The backend mini-lab `smartsales.HOS.test` path only became reliable when the generated config path, signing lane, deploy target, and launch target all agreed.
**Wrong Approach**:
1. Treating unsigned Harmony build output as if it proved install or device readiness
2. Mixing the backend mini-lab lane with the still-blocked `smartsales.HOS.ui` package assumptions
3. Treating `hvigorfile.ts` config generation, AGC signing, `hdc install`, and `hilog` inspection as disconnected steps
**Correct Fix**:
1. Keep `hvigorfile.ts` as the build-owned generator for the Harmony local config artifact sourced from repo-root `local.properties`
2. Keep bundle identity, signing lane, and `hdc` target aligned to the same Harmony app lane
3. Require the full chain: compile, signed deploy, launch, and runtime log proof before calling the lane device-ready
4. Record the proof in the Harmony signing/operator docs so future agents do not repeat the same partial-proof mistake
**File(s)**:
- `docs/platforms/harmony/test-signing-ledger.md`
- `platforms/harmony/tingwu-container/hvigorfile.ts`
- `docs/plans/harmony-tracker.md`
**Pattern**: Harmony progress is only real when build proof, signing proof, deploy proof, and runtime proof all belong to the same app identity and the same lane.

---

### Android Assumption Leakage into Harmony — 2026-04-11

**Symptom**: A Harmony migration task copies Android behavior, lifecycle, permissions, packaging, or background expectations straight into Harmony planning without proving the platform can support them the same way.
**Root Cause**: The task was treated as ordinary porting instead of platform translation with explicit evidence. Android was used as a convenient template rather than as input truth that still needs Harmony validation.
**Wrong Approach**:
1. Reusing Android-native assumptions as if they were Harmony facts
2. Treating the Android implementation as permission to claim Harmony parity
3. Skipping the Harmony overlay and governance docs because the feature "should work the same"
**Correct Fix**:
1. Start with shared product truth plus the relevant Harmony overlay
2. Separate behavior truth from platform delivery mechanics
3. Require Harmony-native evidence before promising parity for notifications, reminders, lifecycle, background work, scheduler ingress, or onboarding completion
**File(s)**:
- `AGENTS.md`
- `.codex/skills/harmony-translation-guardrail/SKILL.md`
- `docs/platforms/harmony/**`
**Pattern**: Harmony translation starts from shared truth, but every platform-owned claim still needs Harmony evidence.

---

### Lane Contamination Across Android and Harmony — 2026-04-11

**Symptom**: One task or branch mixes Harmony-native artifacts, Android-mainline edits, and shared-truth changes without clearly separating ownership.
**Root Cause**: The work treated "migration" as one blended lane, which makes contamination and accidental contract drift likely.
**Wrong Approach**:
1. Editing Android and Harmony implementation roots together by default
2. Hiding governance or shared-truth changes inside a platform pass
3. Letting Harmony-native files appear inside Android-owned trees
**Correct Fix**:
1. Split work into shared, Android, Harmony, or governance write scopes
2. Keep Harmony-native artifacts in Harmony-owned roots only
3. Record shared contract changes separately from platform adaptation changes
**File(s)**:
- `AGENTS.md`
- `.codex/skills/harmony-translation-guardrail/SKILL.md`
- `docs/specs/platform-governance.md`
**Pattern**: Parallel lanes only stay safe when write scope and ownership are explicit.

---

### Fake Parity Without Harmony Evidence — 2026-04-11

**Symptom**: Docs or implementation claim Harmony supports a feature family because Android supports it, but no Harmony-native evidence exists for the actual operator path.
**Root Cause**: Parity was inferred from product intent instead of demonstrated by platform-owned behavior and verification.
**Wrong Approach**:
1. Saying a feature is "the same on Harmony" without runtime proof
2. Treating copy translation or scaffold presence as evidence of working behavior
3. Leaving unsupported affordances visible to avoid admitting reduced capability
**Correct Fix**:
1. Require platform evidence before claiming parity
2. Use curated-container framing when Harmony supports only a bounded subset
3. Fail closed by hiding or blocking unsupported entrypoints instead of leaving dead promises
**File(s)**:
- `.codex/skills/harmony-translation-guardrail/SKILL.md`
- `docs/platforms/harmony/**`
**Pattern**: Platform parity is earned by evidence, not borrowed from Android.

---

### Harmony Spec Invention from Migration Notes — 2026-04-11

**Symptom**: A Harmony tracker item, migration brief, or TODO note gets treated as if it already defines native behavior or interface rules.
**Root Cause**: Navigation docs were mistaken for implementation contracts.
**Wrong Approach**:
1. Inventing native behavior from tracker bullets or handoff notes
2. Treating "migrate feature X" as enough detail to choose Harmony semantics
3. Using migration momentum to skip the shared Core Flow or spec layer
**Correct Fix**:
1. Use trackers as routing only
2. Read the owning shared contract first, then the Harmony overlay
3. If Harmony behavior truly diverges, document the divergence explicitly instead of inventing it in code
**File(s)**:
- `docs/plans/tracker.md`
- `docs/platforms/harmony/**`
- `docs/cerb/**`
**Pattern**: Migration notes say what to visit, not what to build.

---

### Harmony Greenfield Scaffolding Without Contract Sync — 2026-04-11

**Symptom**: A new Harmony root, container, service seam, or scaffold appears before ownership, capability limits, and contract docs are updated.
**Root Cause**: Greenfield implementation moved faster than the docs-first contract, so future agents cannot tell what the scaffold is allowed to do.
**Wrong Approach**:
1. Creating Harmony scaffolding first and promising to document it later
2. Leaving capability boundaries implicit
3. Adding native seams without updating interface ownership or platform overlays
**Correct Fix**:
1. Sync the owning docs in the same session as the scaffold
2. Record supported and unsupported capability sets for curated containers
3. Update ownership docs when new platform seams change module boundaries
**File(s)**:
- `docs/platforms/harmony/**`
- `docs/cerb/interface-map.md`
- `docs/plans/tracker.md`
**Pattern**: Greenfield speed without contract sync creates future migration hallucinations.

---

### Harmony Evidence Gap — 2026-04-11

**Symptom**: A Harmony-native diagnosis is stated confidently from Android analogy, screenshots, or expectation, but no Harmony log, runtime artifact, or platform-native evidence supports it.
**Root Cause**: Evidence standards did not move with the platform. Android intuition was used as proof.
**Wrong Approach**:
1. Calling root cause before collecting Harmony-native evidence
2. Treating Android logs or prior Android failures as enough to explain Harmony behavior
3. Accepting screenshots or user recollection as primary diagnosis evidence
**Correct Fix**:
1. Gather Harmony-native logs, telemetry, or runtime artifacts first
2. Lower confidence explicitly when Harmony evidence is unavailable
3. Add targeted logging or operator-visible traces instead of guessing
**File(s)**:
- `AGENTS.md`
- `docs/platforms/harmony/**`
- `docs/sops/**`
**Pattern**: Platform migration changes the evidence surface; diagnostics must move with it.

---

### Cold-Start Empty State Masquerading as Layout Regression — 2026-03-19

**Symptom**: After fixing SIM history row actions, the on-device history drawer reopened as a white sheet with header/footer but no visible cards. It looked like the new row layout had broken the drawer.  
**Root Cause**: The real failure was **cold-start state, not layout math**. A fresh reinstall reset SIM-local sessions, and `HistoryDrawer` had no explicit empty state. The shell therefore rendered a valid but visually misleading blank drawer.  
**Wrong Approach**:  
1. Treating the screenshot as proof that the new card layout had collapsed  
2. Validating only on warm state, where old sessions still existed  
3. Shipping a drawer with no empty-state contract, so "no data" looked identical to "broken UI"  
**Correct Fix**:  
1. Seed stable local sample sessions for SIM Wave 1 after cold start so history acceptance always has real cards to exercise  
2. Add an explicit empty-state card to `HistoryDrawer` so empty data never masquerades as a rendering regression  
3. Re-run on-device acceptance from a fresh install, not only from an already-populated runtime  
**File(s)**:  
- [SimAgentViewModel.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt)  
- [HistoryDrawer.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/HistoryDrawer.kt)  
**Pattern**: If a drawer/screen suddenly "looks broken" after a reinstall, check whether the runtime lost its local seed data and whether the empty-state path is visually explicit before assuming the new layout code is wrong.  
**Status**: ✅ CONFIRMED 2026-03-19

---

### Compose Scrim Inside AnimatedVisibility — 2026-02-02

**Symptom**: Grey screen covers drawer, clicking anywhere dismisses  
**Root Cause**: **Double scrim conflict** — AgentShell has global scrim at `zIndex(Scrim)`, but SchedulerDrawer had its own internal scrim AND was not wrapped in `zIndex(Drawer)`.  
**Quick Fix**: 1) Remove internal scrim from drawer (rely on AgentShell global scrim), 2) Wrap drawer call in `Box(zIndex(Drawer))`.  
**Deep Dive**: [compose-scrim-drawer-pattern.md](file:///home/cslh-frank/main_app/.agent/rules/compose-scrim-drawer-pattern.md)  
**Status**: ✅ CONFIRMED 2026-02-02

---

### UI Element Not Appearing → Check Pipeline Logs — 2026-02-02

**Symptom**: Scheduler card not appearing in timeline after tapping simulation button (toast appeared ✓, but no task card)  
**Diagnostic**: `adb logcat -s SchedulerVM:D` → revealed `Pipeline result: Error(message=JSON 解析失败...)`  
**Root Cause**: Pipeline silently returning `UiState.Error` — UI correctly showed nothing because there was no data.  
**Key Insight**: **When UI doesn't render expected elements, the bug is often upstream in the data pipeline, not the UI itself.**  
**Correct Fix**: Added `buildSchedulerSystemPrompt()` with explicit JSON schema.  
**File(s)**: [DashscopeExecutor.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/real/DashscopeExecutor.kt#L227-L268)  
**Pattern**: Always instrument ViewModel with tagged logs (`Log.d("SchedulerVM", "Pipeline result: $result")`) to trace data flow.  
**Status**: ✅ CONFIRMED 2026-02-02

---

### Sealed Class Data Gap — 2026-02-03

**Symptom**: Scheduler task created on wrong date (always "tomorrow" instead of parsed date like "next Friday")  
**Root Cause**: **Parsed data exists upstream but sealed class doesn't propagate it**  
- `LintResult.Success.task.startTime` had correct parsed date
- `UiState.Response(content: String)` only passed title string
- `SchedulerViewModel` hardcoded `dayOffset = 1` instead of using actual value  
**Diagnostic Pattern**: Log showed `structuredJson=null` — missing data, not wrong data  
**Correct Fix**:  
1. Add new sealed class variant: `UiState.SchedulerTaskCreated(title, dayOffset)`
2. Producer calculates: `ChronoUnit.DAYS.between(today, taskDate)`
3. Consumer uses: `result.dayOffset` instead of hardcoded value
4. Don't forget exhaustive `when` handling in all consumers  
**File(s)**:  
- [UiState.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/domain/model/UiState.kt)
- [PrismOrchestrator.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/real/PrismOrchestrator.kt)
- [SchedulerViewModel.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt)  
**Pattern**: When adding fields to sealed class, grep for all `when` expressions that match on it  
**Status**: ✅ CONFIRMED 2026-02-03

---

### Post-Insert Self-Conflict — 2026-02-03

**Symptom**: Every new task triggers conflict warning, even with empty schedule  
**Root Cause**: **refresh() → new item in index → checkConflict() finds itself**  
- After `insertTask()`, we call `refresh()` which adds the new task to `ScheduleBoard`
- `checkConflict(startTime, duration)` then finds the task we just inserted → false positive  
**Wrong Approach**: Checking conflict AFTER insert without exclusion  
**Correct Fix**:  
1. Add `excludeId: String?` parameter to `checkConflict()`
2. Add `taskId: String` field to `UiState.SchedulerTaskCreated`
3. Pass `excludeId = result.taskId` in conflict check  
**File(s)**:  
- [ScheduleBoard.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/domain/memory/ScheduleBoard.kt)
- [RealScheduleBoard.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/memory/RealScheduleBoard.kt)
- [UiState.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/domain/model/UiState.kt)
- [SchedulerViewModel.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt)  
**Pattern**: Any "check for duplicates/conflicts after insert" needs **exclusion logic** for the just-inserted entity  
**Status**: ✅ CONFIRMED 2026-02-03

### Spec Drift: Inventing UI Features — 2026-02-03

**Symptom**: Implementation plan included "Swipe Right to Reschedule" gesture  
**Root Cause**: Agent saw a gap (how to trigger reschedule?) and **invented** a UI pattern without spec backing  
**Wrong Approach**: Making up features to fill perceived gaps  
**Correct Fix**:  
1. Check if spec covers the interaction
2. If not covered, **flag as spec gap** — don't invent
3. Ask user: "Spec doesn't define how to trigger reschedule. Should I propose a spec update?"  
**Pattern**: **Docs > Code > Guessing** — Never invent features, always verify against spec  
**Status**: ✅ CONFIRMED 2026-02-03

---

### Spec Invention from Wave Titles — 2026-02-03

**Symptom**: Wave 3 implementation plan prescribed Kotlin location conflict logic that user rejected  
**Root Cause**: **Quoted wave TITLE as if it were behavior SPEC**  
- Tracker said "Extend overlap logic to shared resources (Meeting Rooms)"
- I invented string equality matching conflict detection
- I assumed "location conflict" meant blocking like time conflicts  
**Wrong Approach**: 
- Treating roadmap milestones as implementation specs
- Prescribing business logic when spec was silent
- Assuming semantic matching could be done in Kotlin  
**Correct Fix**:
1. Wave titles/milestones are NOT behavior specs
2. If only a title exists → spec doesn't exist for this behavior
3. Ask USER before implementing unspecified behavior
4. Let RelevancyLib/LLM decide semantic questions, don't prescribe in Kotlin  
**File(s)**: `.agent/workflows/feature-dev-planner-[tool].md` (added Anti-Invention Gate)  
**Pattern**: **Faithfully pull relevant info. Don't prescribe, don't assume.**  
**Status**: ✅ CONFIRMED 2026-02-03

---

### Dead Flow Reference in flatMapLatest — 2026-02-04

**Symptom**: UI requires manual refresh (navigate away/back) to see changes after insert/update  
**Root Cause**: **SharedFlow referenced but not combined** — line exists but has no effect  
```kotlin
// BROKEN: This line does nothing
flatMapLatest { offset ->
    _refreshTrigger.asSharedFlow()  // ← DEAD CODE
    taskRepository.getTimelineItems(offset)
}
```
**Why It's Subtle**: Code compiles, builds pass, no runtime errors. Only visible in L2 testing.  
**Correct Fix**: Use `combine()` to actually merge the flows:  
```kotlin
combine(_activeDayOffset, _refreshTrigger.asSharedFlow()) { offset, _ -> offset }
    .flatMapLatest { offset -> taskRepository.getTimelineItems(offset) }
```
**File(s)**: [SchedulerViewModel.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt)  
**Pattern**: In reactive chains, a Flow reference on its own line **does nothing**. Must be combined/collected.  
**Heuristic**: If you see a Flow/SharedFlow reference that isn't assigned, collected, or combined → **DEAD CODE**.  
**Status**: ✅ CONFIRMED 2026-02-05

---

### Ghost UI After Update ≠ Persistence Bug — 2026-02-04

**Symptom**: Old card persists after reschedule; looks like persistence is broken  
**Initial Hypothesis**: `updateEvent()` not working, CalendarProvider issue  
**Actual Root Cause**: **UI stale, not data stale** — persistence was fine, UI just didn't refresh  
**Diagnostic Pattern**: Don't assume persistence bug first. Always verify:  
1. Check logs: Did update actually go through?  
2. Query DB directly: Is old data still there?  
3. Force refresh: Does UI update after?  
**Correlation**: Ghost card symptom **disappeared after fixing refresh trigger** (see above)  
**Pattern**: When UI shows stale data, check refresh flow BEFORE debugging persistence layer  
**Status**: ✅ CONFIRMED 2026-02-05

---

### LLM Prompt-Linter Data Gap — 2026-02-04

**Symptom**: Inspiration cards show only 💡 emoji, no title text visible  
**Root Cause**: **Prompt told LLM to omit content for inspiration classification**  
- Uni-C extractor prompt allowed inspiration output without durable content semantics
- LLM/Extractor returns inspiration classification with missing or blank content
- SchedulerLinter extracts blank content and persistence stores an empty string
- InspirationRepository stores empty string → UI renders empty text  
**Wrong Approach**: Assuming LLM would include the inspiration text automatically  
**Correct Fix**:  
1. Update the real Uni-C prompt owner to require non-empty inspiration content
2. Add a worked example using `以后想学吉他`
3. Update the linter fallback chain to read content, then title, then the raw utterance
4. Drop blank inspiration writes before repository insert and add persistence coverage
**File(s)**:  
- [PromptCompiler.kt](file:///home/cslh-frank/main_app/core/pipeline/src/main/java/com/smartsales/core/pipeline/PromptCompiler.kt)
- [RealUniCExtractionService.kt](file:///home/cslh-frank/main_app/core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUniCExtractionService.kt)
- [SchedulerLinter.kt](file:///home/cslh-frank/main_app/domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt)
- [SchedulerLinterParsingSupport.kt](file:///home/cslh-frank/main_app/domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinterParsingSupport.kt)
- [FastTrackMutationEngine.kt](file:///home/cslh-frank/main_app/domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/FastTrackMutationEngine.kt)
- [RealInspirationRepository.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/scheduler/RealInspirationRepository.kt)
- [RealInspirationRepositoryTest.kt](file:///home/cslh-frank/main_app/app-core/src/test/java/com/smartsales/prism/data/scheduler/RealInspirationRepositoryTest.kt)  
**Pattern**: **When LLM classifies + extracts data, ensure prompt REQUIRES all fields that downstream code expects.** Don't rely on optional fields.  
**Heuristic**: If Linter extracts field X from LLM JSON, grep prompt for requirement of field X.  
**Status**: Fixed in the current Uni-C extractor/linter path on 2026-04-21; keep the prompt/linter contract alignment check in place

---

### Reconnect Race Condition: Fire-and-Poll — 2026-02-07

**Symptom**: `reconnect()` returns `DeviceNotFound` despite badge being powered on and nearby  
**Root Cause**: **Fire-and-poll pattern with insufficient delay** — `forceReconnectNow()` + `delay(1500ms)` + poll `state.value`. BLE GATT timeout is ~10s, so 1.5s was always too early → state was still `AutoReconnecting` → mapped to `DeviceNotFound`.  
**Wrong Approach**: Increasing the delay (fragile), polling in a loop (wasteful)  
**Correct Fix**: Replace fire-and-poll with `suspend fun reconnectAndWait()` that directly calls `connectUsingSession()` and returns the actual outcome. No delay, no polling.  
**File(s)**:  
- [DeviceConnectionManager.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt) — new `reconnectAndWait()` 
- [RealConnectivityService.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityService.kt) — calls `reconnectAndWait()`  
**Pattern**: **Never fire-and-poll async operations with fixed delays.** Use suspend functions that return actual results. If you see `fire() → delay(N) → poll`, it's a race condition waiting to happen.  
**Status**: ✅ CONFIRMED 2026-02-07

---

### HTTP Gate Conflating Connection Concerns — 2026-02-07

**Symptom**: Reconnect returns `EndpointUnreachable("设备服务不可达")` even though BLE connects and badge reports valid IP  
**Root Cause**: **`connectUsingSession()` gates on HTTP reachability** — it does BLE GATT → query WiFi → get IP → HTTP HEAD to `:8088`. If HTTP server isn't running (firmware state), reconnect fails. But HTTP is only needed for WAV file operations, not for BLE connectivity.  
**Wrong Approach**: Treating HTTP unreachable as "badge not connected"  
**Correct Fix**: Remove HTTP gate from `connectUsingSession()` — check only for valid IP (not `0.0.0.0`). HTTP check stays in `ConnectivityBridge.isReady()` which is the spec-defined pre-flight for media operations.  
**Key Insight**: **The spec already had the right separation** — `isReady()` was designed for BLE+HTTP pre-flight. The HTTP check was in the wrong layer.  
**Diagnostic**: `adb logcat | grep "RX \[NetworkResponse\]"` → `IP#192.168.0.106` proved BLE worked, HTTP was the sole blocker.  
**File(s)**:  
- [DeviceConnectionManager.kt L366-384](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt#L366-L384)  
**Pattern**: **Don't gate connection state on downstream service availability.** BLE connected ≠ HTTP server running. Separate "can I talk to the device?" from "can I download files?"  
**Heuristic**: If a function gates on multiple protocols (BLE + HTTP), ask: *"Do ALL callers need ALL these checks?"* If not, the function is conflating concerns.  
**Status**: ✅ CONFIRMED 2026-02-07

---

### LLM Fabricates History When Memory Is Empty — 2026-02-08

**Symptom**: Coach mode invents specific dates, quotes, and past conversations that never happened. When challenged ("你确定?"), doubles down with more fabricated details.  
**Root Cause**: **System prompt had no anti-hallucination instruction + empty memory context provided no grounding.** LLM fills the vacuum with plausible fiction.  
- `buildCoachSystemPrompt()` said "you're a sales coach" but never said "don't make up history"
- `buildPrompt()` logged `no memory hits` but didn't inject that fact into the LLM prompt
- With zero `memoryHits`, the LLM had no guardrails against fabrication  
**Wrong Approach**: Assuming the LLM would naturally say "I don't have records" when context is empty  
**Correct Fix**: Two-layer defense:
1. System prompt: Add explicit rule `绝不编造历史 — 不要引用或捏造任何以前的对话内容`
2. Runtime: When `memoryHits.isEmpty()`, inject `注意: 当前没有任何历史对话记录` into the prompt body  
**File(s)**: [DashscopeExecutor.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/real/DashscopeExecutor.kt) — L193 (runtime), L232 (Coach), L255 (Analyst)  
**Pattern**: **Every LLM system prompt must include anti-hallucination rules for empty context.** Don't assume the model will be honest about what it doesn't know.  
**Heuristic**: When adding a new LLM mode/pipeline, ask: "What happens when context is empty?" If the answer is "LLM freestyles" → add guardrail.  
**Status**: ⏳ PENDING L3 — 2026-02-08

---

### Fake→Real Swap Removes Implicit Behavior — 2026-02-08

**Symptom**: Same as above — Coach worked fine with Fakes but hallucinated after Room swap  
**Root Cause**: **FakeMemoryRepository had seed data. RoomMemoryRepository starts empty.** The Fake's seed data was an implicit guardrail — the LLM always had *some* context to reference, so it never needed to fabricate. When swapped to Real (empty DB), the LLM had zero grounding data.  
**Deeper Insight**: Fakes don't just provide test data — they implicitly define behavior boundaries. When you remove a Fake, you're also removing whatever implicit behavior its data provided.  
**Correct Fix**: When swapping Fake→Real, audit:
1. What seed data did the Fake provide?
2. Does any downstream consumer assume data is always present?
3. What happens when the Real implementation returns empty?  
**Pattern**: **Fake→Real swap checklist:**
- [ ] What does the consumer do with empty results?
- [ ] Does the LLM behave differently with empty vs populated context?
- [ ] Are there UI states for "no data"?  
**File(s)**: Applies broadly — any `Fake*Repository` → `Room*Repository` swap  
**Status**: ⏳ PENDING L3 — 2026-02-08

---

### Entity-Domain Mapping Gap (Computed Fields) — 2026-02-09

**Symptom**: Scheduler card expanded view showed blank date range, despite UI code and data model supporting it.
**Root Cause**: **Computed field `dateRange` was hardcoded to `""` in `ScheduledTaskEntity.toDomain()`**. 
- Entity doesn't store computed fields (good), but Mapper failed to reconstruct them (bad).
- Pipeline layers (LLM, Linter, UI) were correct; only the persistence mapper dropped the data.
**Wrong Approach**: Assuming UI bug or LLM extraction failure without checking the mapper.
**Correct Fix**: Reconstruct `dateRange` in `toDomain()` using stored timestamps, duplicating the formatting logic from `SchedulerLinter`.
**File(s)**: [ScheduledTaskEntity.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/persistence/ScheduledTaskEntity.kt)
**Pattern**: When a specific field is missing in UI but present in data model, **check the Mapper** (`toDomain`/`toUiState`) first.
**Status**: ✅ CONFIRMED 2026-02-09

### Application-Level Coupling vs Transport-Level Serialization — 2026-02-09

**Symptom**: `tim#get` auto-response interfered with WiFi provisioning writes → `写入特征失败`  
**Root Cause**: Two coroutines calling `GattContext.writeCharacteristic()` concurrently on the same BLE characteristic. Android BLE rejects concurrent GATT writes.  
**Wrong Approach (3 iterations)**:
1. `sessionLock.tryLock()` in `respondToTimeSync()` — failed (timing gaps between coroutine dispatches)
2. `AtomicBoolean` flag `isExecutingOperation` — worked but over-coupled (application logic gating a transport concern)
3. `AtomicBoolean` + deferred `pendingTimeSync` flush — worked but even MORE over-coupled (added a second flag)

**Why All 3 Were Wrong**: They solved a **transport problem** (concurrent BLE writes) with **application logic** (provisioning-aware flags in `respondToTimeSync`). This created false coupling between two independent protocol flows:
- Time sync: `Badge → tim#get → Phone → tim#YYYYMMDDHHMMSS`  
- Provisioning: `Phone → SD#ssid → Phone → PD#password`  

These flows are **logically independent**. The only shared resource is the BLE write pipe.  
**Correct Fix**: Add `writeLock = Mutex()` inside `GattContext.writeCharacteristic()` — serialize ALL BLE writes at the transport layer. Then `respondToTimeSync()` becomes a simple, standalone handler with zero awareness of provisioning.  
**Result**: Removed ~40 lines of flag/defer complexity. Net simpler, net more correct.  
**File(s)**: [GattBleGateway.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGateway.kt) — `GattContext.writeLock`  
**Pattern**: **When two independent flows share a resource, serialize at the resource level, not the flow level.** If you're adding flags like `isDoingX` to block `Y`, ask: "Is the real problem that X and Y share a pipe?" If yes, lock the pipe, not the logic.  
**Heuristic**: If your fix requires one feature to "know about" another feature → you're coupling at the wrong layer. Push the lock DOWN to the shared resource.  
**Corollary (from Frank)**: "Why should we respond tim# when badge sends SD#?" — if your mental model requires explaining why Feature A cares about Feature B's state, you've coupled them wrong.  
**Status**: ✅ CONFIRMED 2026-02-09

---

### Conceptual Name → Wrong Implementation (Label ≠ Schema) — 2026-02-11

**Symptom**: Spec updates told agents to dump raw `memory_entries` into the LLM prompt as "CRM Snapshot"  
**Root Cause**: **Built from the NAME ("CRM Snapshot") instead of verifying against the data MODEL (`EntityEntry`)**  
- Frank said "CRM Snapshot — Kotlin loads, LLM searches"
- I interpreted: "Load everything from CRM tables (entities + memories) into a flat JSON"
- I never checked what `EntityEntry` actually contains: `attributesJson`, `metricsHistoryJson`, `relatedEntitiesJson`, `decisionLogJson` — already structured intelligence
- Raw `memory_entries` are conversation logs (rote memorization), NOT intelligence  
**Wrong Approach**: Taking a conceptual name literally and building the implementation from it  
**Correct Fix**: When Frank gives a conceptual name or description:
1. **Read the data model** — what fields exist? What structured data is already there?
2. **Ask: "Is this structured intelligence or raw data?"** — structured wins, raw is noise
3. **Verify the payload against the schema**, not the label
4. **"Rote memorization ≠ intelligence"** — recalling "Feb 8 CEO had meeting" is trivia. Knowing "CEO → company X → deal stage Y → buying role Z" is intelligence  
**Pattern**: **Frank's conceptual descriptions are human shorthand. Always verify against `data class` definitions before building.** The schema is the spec, the name is just a hint.  
**Corollary (from Frank)**: "I sometimes describe my thought conceptually or simply use cool names — that could be misleading to agents."  
**Agent-Friendly Check**: Before implementing any named concept, ask: `grep "data class [ConceptName]" app-core/` — does the actual model match what you're about to build?  
**Status**: ⏳ PENDING — 2026-02-11

---

### Multi-Spec Drift (Cerb Scope Violation) — 2026-02-11

**Symptom**: Entity Knowledge Context implementation dumped all entities as JSON into LLM prompt — didn't match Frank's pointer-based incremental cache design  
**Root Cause**: **Agent sourced business logic from 4 specs instead of 1.**  
- Tracker said "Wave 3: Entity Knowledge Context"
- Agent read memory-center, session-context, entity-registry, and coach specs
- Cherry-picked from all 4 to assemble a "reasonable" plan
- Never identified which ONE spec owns the behavior
- `session-context/spec.md` already defined the pointer cache (pathIndex, EntityState machine) — agent ignored it  
**Wrong Approach**: Reading tracker wave title → pulling from multiple specs → inventing behavior to fill gaps  
**Correct Fix**:  
1. Tracker wave → identify ONE owning Cerb shard
2. Read that ONE spec completely
3. Implement ONLY what that spec says
4. If spec is incomplete → ask USER, don't invent
5. Other specs are READ-ONLY context for communication patterns  
**File(s)**: `.agent/workflows/feature-dev-planner-[tool].md` (added Single Spec Scope Rule + Cerb Scope Declaration)  
**Relation**: Third instance of "Spec Invention from Wave Titles" (2026-02-03). Pattern now has workflow enforcement.  
**Pattern**: **ONE task = ONE spec.md + ONE interface.md. Period.**  
**Status**: ✅ CONFIRMED 2026-02-11

---

### Markdown Tag Injection & Ignoring Line Numbers — 2026-02-25

**Symptom**: Compilation fails with `Expecting a top level declaration` and `imports are only allowed in the beginning of file` at an exact start line (e.g., `2:29`). Agent attempts to fix `}` brackets at the end of the file. 
**Root Cause**: **Markdown code blocks (` ```kotlin `) were pasted into the actual source file.** 
This happens when an agent uses a tool to write or replace file content and mistakenly includes the markdown formatting wrapper.  
**Wrong Approach**: Ignoring the compiler's line numbers and trying to guess the syntax error (e.g., deleting/adding `}` braces at the bottom of the file).  
**Correct Fix**: 
1. **Read the compiler error carefully.** It gives the exact line number (e.g., `AudioDrawer.kt:2:29`).
2. **View the file at that specific line number** instead of guessing.
3. Remove the extraneous ` ```kotlin ` or other markdown tags from the source file.  
**File(s)**: [AudioDrawer.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/AudioDrawer.kt)  
**Pattern**: **Never ignore the compiler's line numbers.** If the error is on line 2, the bug is on line 2, not line 279.  
**Status**: ✅ CONFIRMED 2026-02-25

---

### OSS Credentials vs Unified Aliyun Key — 2026-02-26

**Symptom**: `[OSS_NETWORK_ERROR] Compute signature failed!` crashing the Audio Upload flow.
**Root Cause**: User deleted `OSS_ACCESS_KEY` specific fields from `local.properties` to rely entirely on the unified `ALIBABA_CLOUD_ACCESS_KEY_ID`. However, `data/oss/build.gradle.kts` strictly required the OSS variants, silently falling back to empty strings when compiling `BuildConfig`. empty strings = automatic signature crash.
**Wrong Approach**: Telling the user to put the redundant keys back (rejected by user as they wanted a unified key).
**Correct Fix**: Refactor `data/oss/build.gradle.kts` to check for `OSS_ACCESS_KEY_ID`, and if blank, `.ifBlank { localProperties.getProperty("ALIBABA_CLOUD_ACCESS_KEY_ID", "") }`. This safely supports both dedicated and unified key schemas.
**File(s)**: [build.gradle.kts](file:///home/cslh-frank/main_app/data/oss/build.gradle.kts)
**Pattern**: When migrating modules to unified credentials, explicitly wire the fallback in the build script so `BuildConfig` generation does not fail silently.
**Status**: ✅ CONFIRMED 2026-02-26

---

### Hallucinated REST Endpoint (/tasks/{taskId}/transcription) — 2026-02-26

**Symptom**: Tingwu transcription successfully polling `GetTaskInfo` to `COMPLETED`, but immediately crashing with `HTTP 404 Not Found`.
**Root Cause**: Code attempted to fetch the transcription artifact via a REST call to `api.getTaskResult(taskId = jobId)` (which mapped to `/tasks/{taskId}/transcription`). **This endpoint does not exist in Aliyun's V2 OpenAPI.**
**Wrong Approach**: Trying to find the "missing parameter" for the 404 endpoint.
**Correct Fix**: 
1. Check Aliyun's V2 OpenAPI documentation for `GetTaskInfo`.
2. Observe that results are NOT returned via an API endpoint, but as a **Pre-Signed OSS URL** embedded inside the `Result` payload of the `GetTaskInfo` response itself (e.g., `statusResponse.data?.resultLinks?.get("Transcription")`).
3. Refactor the pipeline to grab that dynamic URL and download the JSON blob directly using a vanilla `OkHttpClient`.
**File(s)**: [RealTingwuPipeline.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/tingwu/RealTingwuPipeline.kt)
**Pattern**: When dealing with Aliyun V2 APIs, large artifacts (transcripts, summaries, chapters) are returned as Pre-Signed OSS URLs `ResultLinks`, never as direct REST endpoints.
**Status**: ✅ CONFIRMED 2026-02-26

---

### LLM Semantic Mapping vs Hardcoded Math — 2026-02-28

**Symptom**: Building complex O(N) string-matching engines (Levenshtein distance, substring math) to handle CRM entity disambiguation (e.g., aliases, nicknames, homophones).
**Root Cause**: **Treating the LLM solely as a syntax text extractor rather than a semantic reasoning engine.** This leads to over-engineered, rigid Kotlin logic trying to natively solve things the LLM already understands natively.
**Wrong Approach**: Using Kotlin math to find the connection between "CEO" and "Tim Cook" (which fails 100%), or trying to maintain custom dictionary lookup logic.
**Correct Fix**: Dump a lightweight "Contact Sheet" (ID mapped to Name/Aliases) directly into the LLM System Prompt. Instruct the LLM to route the parsed intent directly to the known ID.
**File(s)**: [RealInputParserService.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/parser/RealInputParserService.kt) (Deleted `AliasIndex.kt`)
**Pattern**: **For semantic issues (nicknames, homophones, conceptual mapping), do not seek hardcoded math solutions.** Pass the targeted context boundaries to the LLM and let it solve the mapping natively.
**Status**: ✅ CONFIRMED 2026-02-28

---

### Metaphor → Hallucinated UI (Agentic Architecture) — 2026-03-02

**Symptom**: Explaining a backend data aggregation service ("Client Profile Hub") by inventing non-existent UI screens ("When you tap on a client's name").
**Root Cause**: **Taking an architectural metaphor ("File Explorer") literally and assuming a traditional click-and-scroll CRM UI.** Smart Sales is voice/chat-first. The "user" of the hub is the LLM, not the human.
**Wrong Approach**: Explaining data pipelines using GUI interactions. Inventing screens to explain data aggregation.
**Correct Fix**: 
1. Remember the Agentic OS Model: Humans talk to Agents. Agents read from Hubs.
2. If explaining a data layer, explicitly state it is **invisible** to the human.
**File(s)**: N/A (Documentation/Explanation issue)
**Pattern**: In an agent-first architecture, never assume traditional CRUD screens exist unless explicitly specified in the UI/UX contract.
**Status**: ✅ CONFIRMED 2026-03-02

---

### D8/R8 Silent Interface Dropping (NoClassDefFoundError) — 2026-03-04

**Symptom**: `NoClassDefFoundError: Failed resolution of: Lcom/.../HiltComponentProvider;` crashes app on launch, but interface is clearly in the codebase.  
**Root Cause**: **D8/Kapt silently dropping standalone interfaces during DEX merging.** When an `@EntryPoint` interface is declared in its own file and only referenced via reflection/Hilt generated code, Dalvik AOT compilation (`dex2oat`) fails to find it, causing the entire `SingletonC` or `Application` component to fail verification and crash on boot.  
**Wrong Approach**: Searching for missing dependencies, clearing caches, or assuming compilation failed.  
**Correct Fix**: Move the interface definition directly into the Kotlin file that consumes it (e.g., the UI file doing the EntryPointAccessors call). This forces the compiler to package the interface with strongly referenced code, preventing it from being silently dropped during DEX merging.  
**File(s)**: [ConflictCard.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/ConflictCard.kt)  
**Pattern**: If a standalone interface class throws `NoClassDefFoundError` at runtime despite successful compilation, move its declaration into the file of its primary consumer to bypass D8 exclusion bugs.  
**Status**: ✅ CONFIRMED 2026-03-04

---

### Physical vs Logical Modularization (Compiler-Driven Extraction) — 2026-03-10

**Symptom**: A module extraction is marked "complete" because the Gradle build passes, but the domain dependency graph remains a monolithic hairball (e.g., `:core:pipeline` depending on `:data:ai-core`).
**Root Cause**: **Compiler-Driven Extraction.** The agent hoisted a file into a new module, hit unresolved reference errors, and bypassed them by injecting the legacy module into `build.gradle.kts` instead of rewriting the logic to use the new isolated interface.
**Wrong Approach**: "Silencing the compiler" by adding `implementation(project(":data:..."))` just to get a green checkmark. Abandoning test files in the old module because they didn't cleanly migrate.
**Correct Fix**: 
1. **The Audit**: Read the code to understand *what* it does. Map its usage of legacy components.
2. **The Rewrite**: Rewrite it in the new module targeting the pure interface (e.g., `Executor`).
3. **The Cleanse**: Rip the legacy dependency entirely out of `build.gradle.kts`.
4. **The Test Port**: Rescue the abandoned tests, move them to the new module, and rewrite them using Fakes.
**Pattern**: Physical modularization (moving files) without logical modularization (inverting dependencies) is a failure. **Don't decide by compiler errors. Decide by alignment and coupling.**
**Status**: ✅ CONFIRMED 2026-03-10

---

### Intelligent Orchestration vs Manual Choreography — 2026-03-10

**Symptom**: Agents/Users constantly forgetting to run `/cerb-check`, or choosing the wrong template, leading to hallucinated implementations that bypass the OS Model boundaries.
**Root Cause**: **Manual DX Choreography.** We built strict linters and templates, but forced the human/agent to manually string them together in the right order. This puts the cognitive load of architectural routing completely on the practitioner.
**Wrong Approach**: Adding more templates and slash commands without a central entry point, assuming the agent will perfectly guess the routing.
**Correct Fix**: **Paved Road Orchestration.** 
1. Make the Planner (`/feature-dev-planner`) the single intelligent entry point.
2. The Planner reads the PRD and global topologies (`tracker.md`, `interface-map.md`) first.
3. The Planner dynamically routes the task to the correct template (`/cerb-ui-template`, etc.).
4. The Planner runs the linter (`/cerb-check`) internally.
**Pattern**: **Don't ask the engineer to run 5 linters; give them one entry point that enforces the SOP under the hood.** The easiest path must be the compliant path.
**Status**: ✅ CONFIRMED 2026-03-10

---

### SIMPLE_QA Bypassing UnifiedPipeline (Empty AI Answers) — 2026-03-12

**Symptom**: User asks a factual question (e.g., "What is Zhang Wei's job title?"), and the LLM responds "I have no information" even though the data exists in the database.
**Root Cause**: The `IntentOrchestrator` identified the question as `SIMPLE_QA` but treated it like `VAGUE`—instantly short-circuiting to the UI without loading context. The query *never reached* the `UnifiedPipeline` to actually load the Entity Knowledge.
**Wrong Approach**: Assuming the LLM failed to understand the user, or that the database write failed over concurrency issues.
**Correct Fix**: Separate the routing branches in `IntentOrchestrator`. `VAGUE` should still short-circuit to the UI for clarification. `SIMPLE_QA` must fall through to the heavy `UnifiedPipeline` so that `ContextBuilder` can load `KNOWN_FACTS` before answering.
**File(s)**: [IntentOrchestrator.kt](file:///home/cslh-frank/main_app/core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
**Status**: ✅ CONFIRMED 2026-03-12

---

<!-- Add new lessons above this line -->

### JSON Schema Fragility vs Raw Markdown — 2026-03-02

**Symptom**: Agent over-corrected by forcing LLM to output rigid ````json ... ```` formatting for every extraction task, including simple text context generation like Session Titles.
**Root Cause**: **Treating human-facing text generation (summaries) as machine-routing data (IDs).** Forcing JSON extraction requires regex stripping and try-catch blocks that are fragile in production edge cases.
**Wrong Approach**: Forcing `{"clientName": "张总", "summary": "报价讨论"}` for generic context, then parsing it out.
**Correct Fix**: 
1. **JSON is for Machines**: Reserve strict JSON Schema mapping for routing/IDs where the next node is a rigid Kotlin linter or DB insert. Use the provider's native Structured Outputs API for this.
2. **Markdown/Text is for Context**: If the output is just going to be read by humans or injected into another LLM prompt as context, let the LLM output raw text. "Title: 张总 - 报价讨论" is cheaper to generate and impossible to crash on parsing.
**Status**: ✅ CONFIRMED 2026-03-02

---

### Soft-Deprecation Rot ("memoryHits") — 2026-03-02

**Symptom**: `ContextBuilder` continued wasting DB calls to populate `memoryHits` even though the architecture had already moved to the new `entityKnowledge` graph.
**Root Cause**: **"Soft-deprecation" is a lie we tell ourselves.** Fields were left in `EnhancedContext` and `SessionWorkingSet` to "satisfy" `RealCoachPipeline` without actually taking the time to migrate it.
**Wrong Approach**: Leaving dead fields in core domain pipelines, which misleads future agents/developers into thinking those fields are actively maintained or architecturally relevant.
**Correct Fix**: When replacing a core data structure (like memory search → knowledge graphs), rip the old one fully out. If downstream consumers break, migrate them immediately. Never leave a "soft-deprecated" field in a core pipeline context—it pollutes the clean OS boundaries.
**Status**: ✅ CONFIRMED 2026-03-02

---

### Doc+Code Dual-Check Alignment (Rigorous Verification) — 2026-03-06

**Symptom**: Automated tools blindly passed features because the code matched the local `spec.md`, ignoring the fact that the `spec.md` contradicted the global `interface-map.md` (OS Model boundaries). This created a false sense of security and architectural drift.
**Root Cause**: **Auditing isolated components without cross-referencing the global architecture contract.** A spec that is perfectly implemented but architecturally illegal is a time-bomb.
**Wrong Approach**: Running verification (like compiler checks and `spec.md` grep) on local files without validating the local spec against the Source of Truth (`interface-map.md`).
**Correct Fix**:
1. **The Trinity Check**: Read `spec.md`, `interface-map.md`, and the `.kt` implementation together.
2. **Resolution Rules**: If code is a hack → Rewrite code to match OS Model spec. If code evolved correctly but spec is old → Update spec.
3. Update `tracker.md` with a `✅ [DUAL-CHECKED]` label ONLY when all 3 align perfectly.
**Pattern**: **Never audit code against a siloed document.** Always verify the local contract against the global architecture map before writing or validating code.
**Status**: ✅ CONFIRMED 2026-03-06

---### SwipeToDismiss Background Visibility — 2026-02-02

**Symptom**: Red delete background persists after swipe, requires navigating away to clear  
**Root Cause**: Used `dismissDirection != null` which is always true inside `backgroundContent` lambda  
**Wrong Approach**: `dismissState.progress > 0f` — also unreliable  
**Correct Fix**: Use `dismissState.targetValue != SwipeToDismissBoxValue.Settled`  
**File(s)**: [SchedulerTimeline.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerTimeline.kt)  
**Pattern**: Inside SwipeToDismissBox lambdas, check `targetValue` state, not `dismissDirection`  
**Status**: ✅ CONFIRMED 2026-02-02

---

### DateRange Format Mismatch — 2026-02-02

**Symptom**: Tasks saved with wrong time (defaulted to "now" instead of scheduled time)  
**Root Cause**: Linter outputs `"2026-02-03 03:00 - ..."` but `parseDateRange()` expected `~` delimiter with 2 parts  
**Correct Fix**: Handle 3 formats: `~` (full range), `- ...` (open-ended), ` - ` (same-day)  
**File(s)**: [RealScheduledTaskRepository.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/scheduler/RealScheduledTaskRepository.kt)  
**Pattern**: When parsing formats between layers, test with ALL format variants the upstream can produce  
**Status**: ✅ CONFIRMED 2026-02-02

---

### Modal Drawer Click Passthrough — 2026-02-02

**Symptom**: Tapping empty area in drawer dismisses it (clicks pass through to scrim)  
**Root Cause**: Drawer content Column doesn't consume pointer events  
**Correct Fix**: Add `pointerInput(Unit) { awaitPointerEventScope { while(true) { awaitPointerEvent() } } }`  
**File(s)**: [SchedulerDrawer.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/SchedulerDrawer.kt)  
**Pattern**: Modal content must explicitly consume events to prevent passthrough  
**Reference**: [compose-scrim-drawer-pattern.md](file:///home/cslh-frank/main_app/.agent/rules/compose-scrim-drawer-pattern.md)  
**Status**: ✅ CONFIRMED 2026-02-02

---

### Calendar Selected vs Today State — 2026-02-02

**Symptom**: Tapped date has no visual highlight; only "today" is highlighted  
**Root Cause**: Only tracked `isToday`, never passed `selectedDayOfMonth` down to CalendarRow  
**Correct Fix**: 
- Calculate `selectedDayOfMonth = todayDayOfMonth + activeDay`  
- Pass to CalendarRow, apply border ring for selected (not today)  
- Visual: Today = filled circle, Selected = border ring  
**File(s)**: [SchedulerCalendar.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerCalendar.kt)  
**Pattern**: Date pickers need 3 visual states: normal, today, selected (and selected+today)  
**Status**: ✅ CONFIRMED 2026-02-02

---
