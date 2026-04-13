# Harmony Platform Overlays

> **Purpose**: Harmony-native delivery notes for the current bounded Harmony roots and future native Harmony platform work.
> **Status**: Active Stage 2 Harmony overlay set
> **Primary Law**: `docs/specs/platform-governance.md`
> **Operational Trackers**:
> - `docs/plans/harmony-tracker.md`
> - `docs/plans/harmony-ui-translation-tracker.md`

---

## Scope

This folder owns native Harmony delivery deltas only.

It does **not** own:

- the current Android app running on Huawei/Honor/Harmony devices
- generic product journeys
- shared business or scheduler semantics

Those remain governed by the shared docs spine plus the Android compatibility guidance where applicable.

---

## Current Overlays

- `native-development-framework.md`
- `scheduler-backend-first.md`
- `tingwu-container.md`
- `ui-verification.md`
- `test-signing-ledger.md`

Use `docs/plans/harmony-tracker.md` for Harmony program-summary state such as capability boundaries, restore posture, and backend/dataflow summary.

Use `docs/plans/harmony-ui-translation-tracker.md` for page-by-page ArkUI rewrite and page-pass evidence.

Human operator workflow lives in `docs/sops/harmony-operator-runbook.md`. Use that runbook when you need to classify a Harmony slice, brief an agent, interpret Cerb interfaces, or review delivery evidence.
