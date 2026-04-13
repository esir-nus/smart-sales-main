# Harmony UI Translation Tracker

> **Purpose**: Page-by-page tracker for Harmony ArkUI-native rewrite, internal gate status, mock-vs-docked readiness, and device UI pass evidence.
> **Status**: Active
> **Last Updated**: 2026-04-11
> **Primary Laws**:
> - `docs/sops/tracker-governance.md`
> - `docs/platforms/harmony/ui-verification.md`
> - `docs/specs/platform-governance.md`
> **Related Docs**:
> - `docs/plans/harmony-tracker.md`
> - `docs/specs/prism-ui-ux-contract.md`
> - relevant shared `docs/cerb-ui/**` and `docs/core-flow/**` docs for each page

---

## 1. Status Model

| Status | Meaning |
|---|---|
| `Proposed` | Page is defined but not yet scaffolded |
| `Active` | Native ArkUI rewrite is in progress |
| `Blocked` | Waiting on signing, capability boundary, source docs, or backend adapter truth |
| `Pass` | Docs, internal gate posture, build/install evidence, and visual proof are aligned for the current phase |
| `Deferred` | Intentionally parked without claiming parity |

---

## 2. Page Pass Rule

A page may move to `Pass` only when all are true:

- the owning shared docs are identified
- the Harmony UI overlay still states the supported and disabled capability honestly
- the page scaffold exists in the internal `ui-verification` package
- mock or real driver status is explicit
- Harmony CLI evidence is recorded for the current page phase
- screenshot or equivalent UI proof is linked when visual behavior matters

---

## 3. Active Board

### HUI-01: Internal Shell And Route Scaffold

- `ID`: `HUI-01`
- `Page ID`: `HUI-01`
- `Title`: Internal Harmony UI shell and gated route scaffold
- `Work Class`: `harmony-native`
- `Platform Lane`: `harmony`
- `Owner`: `platforms/harmony/ui-verification/**`
- `Status`: `Active`
- `Package Target`: `smartsales.HOS.ui`
- `Source of Truth`:
  - `docs/platforms/harmony/ui-verification.md`
  - `docs/specs/prism-ui-ux-contract.md`
- `Required Evidence`:
  - package builds from the dedicated Harmony root
  - shell page renders route cards and internal gate state honestly
- `Mock Driver Status`: shell mock driver landed
- `On-Device Pass Status`: unsigned build and local signed HAP verified on 2026-04-11; AGC mode now fails fast unless a real `smartsales.HOS.ui` asset set exists; the prior install attempt on target `4NY0225613001090` still failed with pkcs7 verification error `9568257`
- `Docking Status`: not applicable yet
- `Hidden / Internal Gate Status`: active internal-only package plus scheduler preview gate
- `Notes / Drift`:
  - this page is the control plane for all later page passes
  - it must stay explicit that the package is internal verification only
- `Last Updated`: `2026-04-11`

### HUI-02: Tingwu Public-Capability Rewrite

- `ID`: `HUI-02`
- `Page ID`: `HUI-02`
- `Title`: Tingwu inventory and artifact page-native rewrite
- `Work Class`: `harmony-native`
- `Platform Lane`: `harmony`
- `Owner`: `platforms/harmony/ui-verification/**`
- `Status`: `Active`
- `Package Target`: `smartsales.HOS.ui`
- `Source of Truth`:
  - `docs/platforms/harmony/tingwu-container.md`
  - shared Tingwu/audio docs referenced there
- `Required Evidence`:
  - adapter-backed Tingwu inventory and artifact cards render cleanly in the internal package
  - page stays within the current public Harmony capability boundary
- `Mock Driver Status`: inline mock cards removed; page now maps repository-shaped seed state through a dedicated Tingwu adapter
- `On-Device Pass Status`: unsigned build and local signed HAP verified on 2026-04-11; AGC mode now rejects missing or wrong-bundle assets before install; real device install is still blocked by pkcs7 verification error `9568257` until a UI-specific AGC profile exists
- `Docking Status`: page adapter landed; docking contract defined at `docs/platforms/harmony/hui-02-tingwu-docking-contract.md`; live Tingwu repository bind still deferred until signing gate and data access prerequisites are met
- `Hidden / Internal Gate Status`: visible inside the internal package only
- `Notes / Drift`:
  - this page can become the first honest public-facing rewrite candidate later because it matches current Harmony capability
  - the docking contract documents the exact field projection from `HarmonyContainerSnapshot` to `HarmonyUiTingwuAdapterSnapshot` and the swap point in `HarmonyUiDriver`
  - no live backend docking is claimed yet; the adapter seam and contract are real, but the bind is not
- `Last Updated`: `2026-04-12`

### HUI-03: Hidden Scheduler Preview Rewrite

- `ID`: `HUI-03`
- `Page ID`: `HUI-03`
- `Title`: Hidden scheduler preview page-native rewrite
- `Work Class`: `harmony-native`
- `Platform Lane`: `harmony`
- `Owner`: `platforms/harmony/ui-verification/**`
- `Status`: `Active`
- `Package Target`: `smartsales.HOS.ui`
- `Source of Truth`:
  - `docs/cerb-ui/scheduler/contract.md`
  - `docs/platforms/harmony/scheduler-backend-first.md`
  - `docs/platforms/harmony/ui-verification.md`
- `Required Evidence`:
  - mock-backed scheduler preview states render natively in ArkUI
  - the page remains hidden behind the internal gate and does not imply public parity
- `Mock Driver Status`: mock scheduler preview driver landed
- `On-Device Pass Status`: unsigned build and local signed HAP verified on 2026-04-11; AGC mode now rejects missing or wrong-bundle assets before install; real device install is still blocked by pkcs7 verification error `9568257` until a UI-specific AGC profile exists
- `Docking Status`: deferred until backend adapter truth is ready
- `Hidden / Internal Gate Status`: hidden by default; unlock required inside internal package
- `Notes / Drift`:
  - this page must not leak into the public Tingwu container surface
  - reminder, onboarding, and public scheduler parity remain out of scope
- `Last Updated`: `2026-04-11`
