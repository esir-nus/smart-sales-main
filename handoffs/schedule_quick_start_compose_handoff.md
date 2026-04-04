# Handoff: DTQ-01 Onboarding and Quick Start

> **Lane ID**: `DTQ-01`
> **Scope**: Onboarding and quick-start lane for the current dirty tree, including the staged scheduler quick-start sandbox, onboarding flow progression, and the one-shot handoff back into the runtime shell.

## Scope

This handoff owns the onboarding feature seam end-to-end for the current dirty tree, especially the quick-start staging/commit path and the onboarding-specific shell handoff gate.

## Owned Paths

- `app-core/src/main/java/com/smartsales/prism/data/onboarding/**`
- `app-core/src/main/java/com/smartsales/prism/ui/onboarding/**`
- onboarding-focused tests under `app-core/src/test/**` and `app-core/src/androidTest/**`
- `docs/cerb/onboarding-interaction/**`
- `docs/specs/flows/OnboardingFlow.md`
- onboarding acceptance evidence such as `docs/reports/tests/L3-20260403-onboarding-quick-start-session-recovery.md`
- onboarding prototype sources under `prototypes/onboarding-family/**`

## Current Repo State / Implementation Truth

The current dirty tree includes real onboarding implementation work, not just prototype translation notes. The lane currently spans onboarding flow progression, mic interaction, quick-start staging/commit support, calendar/exact-alarm branches, and the one-shot shell handoff back into home.

The detailed Compose translation notes below remain useful implementation memory, but they are no longer sufficient as the current source of truth by themselves. Treat the repo docs and code listed in this handoff as the controlling truth.

## What Is Finished

- The onboarding lane already has a bounded dirty-file seam in the quarantine tracker.
- The historical Compose handoff notes remain available as detailed UI implementation memory.
- Owning source-of-truth docs and current evidence references are now attached to this handoff so the lane is resumable without rediscovering drift from scratch.
- DTQ-01 audit on 2026-04-04 found no onboarding-specific contradiction between `docs/specs/flows/OnboardingFlow.md`, `docs/cerb/onboarding-interaction/**`, and the live quick-start / commit / shell-handoff code.
- Focused unit verification passed in this audit session:
  - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.onboarding.OnboardingQuickStartServiceTest' --tests 'com.smartsales.prism.ui.onboarding.OnboardingSchedulerQuickStartCommitterTest' --tests 'com.smartsales.prism.ui.onboarding.OnboardingFlowTransitionTest'`
  - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.onboarding.OnboardingInteractionViewModelTest'`

## What Is Still Open

- Keep the live onboarding code, docs, and tests synchronized around quick-start staging, commit, and shell handoff behavior.
- Re-check any onboarding change that starts to depend on shared scheduler routing or connectivity transport internals; split instead of silently broadening the lane.
- Do not treat this lane as `Accepted` yet while the dirty onboarding lane is still unlanded and this audit session does not include a fresh Compose instrumentation or device rerun.

## Doc-Code Alignment

- **Owning source-of-truth docs**:
  - `docs/specs/flows/OnboardingFlow.md`
  - `docs/cerb/onboarding-interaction/spec.md`
  - `docs/cerb/onboarding-interaction/interface.md`
  - `docs/plans/tracker.md`
- **Current alignment state**: `Both pending`
- **Docs still needing sync or final confirmation before `Accepted`**:
  - no doc/code contradiction was found in the 2026-04-04 audit
  - hold this lane at `Both pending` until fresh UI/instrumentation or device evidence closes the remaining verification gap for the still-dirty onboarding scope
- **Rule**: no onboarding code in this lane should be treated as landed while verification evidence for the current dirty scope is still incomplete.

## Required Evidence / Verification

- Focused onboarding unit/instrumentation coverage for the touched quick-start and mic-interaction branches.
- Current audit evidence:
  - `OnboardingQuickStartServiceTest`
  - `OnboardingSchedulerQuickStartCommitterTest`
  - `OnboardingFlowTransitionTest`
  - `OnboardingInteractionViewModelTest`
  - existing device acceptance reference: `docs/reports/tests/L3-20260403-onboarding-quick-start-session-recovery.md`
- Remaining gap: no fresh Compose instrumentation or on-device rerun was executed in this session for the still-dirty onboarding lane.
- If runtime-only diagnosis is involved, capture concrete device evidence rather than inferring from screenshots.

## Safe Next Actions

- Continue work inside the onboarding-owned paths listed above.
- Sync the owning onboarding docs in the same working session as any behavior change.
- Split out any scheduler-core or connectivity-transport dependency instead of letting this handoff absorb it.

## Do Not Touch / Collision Notes

- Do not absorb shared scheduler router ownership; coordinate with `DTQ-02` instead.
- Do not absorb connectivity bridge/service ownership; coordinate with `DTQ-03` instead.
- Do not treat shared shell chrome or governance docs as onboarding-owned implementation territory.

## Detailed Historical Notes

> **Translation of the interactive Guided Sandbox onboarding sequence (Page 3).**

## A. Architecture & Top-Level Context
- **Component Role**: Implements the "learning-by-doing" interactive voice onboarding sequence specifically for the Scheduler feature.
- **Placement**: This is a fullscreen immersive onboarding page hosted on the device shell as part of the initial user journey.
- **Insets/SOT**: Requires full-screen edge-to-edge rendering with `WindowInsets.systemBars` handled correctly. The ambient background should extend beneath standard system bars.

## B. Component Decomposition (HTML to Compose)

### 1. Ambient Background & Header
- **HTML Element**: `.ambient-bg`, `.aurora-blob`, `#p3-header`
- **Compose Mapping**: `Box` for ambient background containing `Canvas` or `ImageFilter` blurred shapes. `Column` for the header text.
- **Surface Styling**: 
  - Background (Base Layer): `Color(0xFF111111)`
  - Aurora Blob 1: `Color(0x2638BDF8)` (15% Alpha of #38BDF8) with `80.dp` blur.
  - Aurora Blob 2: `Color(0x26818CF8)` (15% Alpha of #818CF8) with `80.dp` blur.
- **Typography**: Header `Text` is `28.sp`, font-weight Medium. Subtitle is `15.sp`, font-weight Light (`Color(0x99FFFFFF)`).

### 2. Schedule Card (The Sandbox)
- **HTML Element**: `.glass-card.schedule-preview`
- **Compose Mapping**: `ElevatedCard` or `Box` with `Modifier.clip(RoundedCornerShape(24.dp))` and frosted glass modifiers.
- **Layout / Spacing**: `Modifier.padding(vertical = 48.dp, horizontal = 24.dp).fillMaxWidth()`. Internal card padding `16.dp`.
- **Surface Styling**: 
  - Background: `Color(0x08FFFFFF)` (3% White)
  - Border: `BorderStroke(1.dp, Color(0x14FFFFFF))` (8% White)
  - Blur Radius: `20.dp`
  - Shape: `RoundedCornerShape(24.dp)`

### 3. Schedule Item Row 
- **HTML Element**: `.schedule-item`
- **Compose Mapping**: `Row` with `Arrangement.Start` and `Alignment.CenterVertically`.
- **Layout**: `Modifier.padding(start = 20.dp, end = 16.dp, vertical = 12.dp)`.
- **Sub-components**:
  - **Urgency Bar** (`.urgency-bar`): Absolute-positioned `Box` at the leading edge (`Modifier.width(4.dp).fillMaxHeight()`).
  - **Time** (`.schedule-time`): Fixed width `45.dp`, `Color(0x99FFFFFF)`. Use `fontFeatureSettings = "tnum"` for tabular figures.
  - **Description** (`.schedule-desc`): `Modifier.weight(1f)`, text-overlow `TextOverflow.Ellipsis`, max lines 1.
  - **Bells Cascade** (`.schedule-bells`): `Row` with `horizontalArrangement = Arrangement.spacedBy(0.dp)`. Icon size `14.dp`.
  - **Date** (`.schedule-date`): `Color(0x99FFFFFF)`, `12.sp`.

### 4. Color Tokens (Urgency Tiers)
- **L1 (Critical)**: `Color(0xFFFF453A)` (Bar includes slight offset drop shadow)
- **L2 (High)**: `Color(0xFFFF9F0A)` (Bar includes slight offset drop shadow)
- **L3 (Normal)**: `Color(0xFF0A84FF)`

## C. State Machine & Interaction Logic
- **Component State**: 
  - `var onboardingRound by remember { mutableIntStateOf(1) }` (Rounds 1 through 3)
  - `var scheduleItems by remember { mutableStateListOf(...) }`
- **Interactions**:
  - **Round 1 (Initial)**: User taps mic, speaks. Simulation produces the base 3-item list.
  - **Permission Gate**: The UI execution of the first schedule item MUST immediately invoke the Android `SCHEDULE_EXACT_ALARM` permission API organically. The system OS prompt temporarily arrests interaction.
  - **Round 2 (Append)**: User actively adds "赶飞机" (L2 urgency). A new row dynamically appends with `AnimatedVisibility`.
  - **Round 3 (Update)**: User modifies "赶飞机" to "推迟后的飞机" and changes the date to "大后天". This modifies the *existing* row properties, triggering a recomposition highlighting the targeted item.

## D. Animation & Motion Choreography
- **List Entrance (Staggered)**: The initial `.schedule-item` entries translate horizontally off-screen `offsetX = 10.dp` to `0.dp` and fade in, staggered by `100ms` each using `tween(400, easing = FastOutSlowInEasing)`.
- **Mutation Highlight**: When an item is updated (Round 3 state shift), its background color pulses `Color(0x33FFD60A)` (20% Alpha of #FFD60A) decaying to `Color.Transparent` over `1200ms` using `animateColorAsState` or a standard keyframe sequence.
- **Voice Handshake Glow**: The bottom audio wave hints pulse at an idle height of `20.dp` normally, accelerating and expanding to `40.dp` during active mic capture using an infinite `tween`.

## E. Anti-Drift & Defensive Implementation Rules
- ⚠️ **Native System Permission Integration**: Do NOT build a custom UI for the alarm permission in-app. The prototype specifies this as an OS-level prompt. It must be an organic native query (`ActivityCompat.requestPermissions`), not a custom Compose dialog.
- ⚠️ **Text Left-Alignment Guarantee**: Do NOT put the urgency bells inside the task description text container. Follow the explicit sibling structure `[Time(fixed)] [Description(weight=1)] [Bells] [Date]` to ensure descriptions perfectly vertical-align to the left margin regardless of how many bells exist (🔔🔔🔔 vs 🔔).
- ⚠️ **Component Ownership (Staged Sandbox)**: This onboarding experience must mutate its own local state representation and NOT trigger global persistence side-effects until the user explicitly presses the final "Enter App" button. Keep `Repository` additions isolated in an in-memory staging domain during this sandbox.
