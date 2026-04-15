# Harmony UI Verification Package

> **Purpose**: Define the internal Harmony-native ArkUI verification package used for page-by-page native rewrite, internal gate control, and later backend docking.
> **Status**: Active internal verification lane
> **Last Updated**: 2026-04-11
> **Primary Law**: `docs/specs/platform-governance.md`
> **Primary Trackers**:
> - `docs/plans/harmony-tracker.md`
> - `docs/plans/harmony-ui-translation-tracker.md`
> **Shared Truth Sources**:
> - `docs/specs/prism-ui-ux-contract.md`
> - relevant `docs/core-flow/**`, `docs/cerb-ui/**`, and `docs/cerb/**` docs for each page

---

## Variant Role

This is a dedicated internal Harmony-native app/package for UI verification.

It is:

- a clean Harmony-owned root for page-native ArkUI rewriting
- a page-by-page verification lane with mock drivers first and real docking later
- a separate package so on-device UI checks do not collide with the backend mini-lab app

It is not:

- the public Harmony product app
- a hidden second product-truth owner
- permission to widen public Harmony capability without updating the overlays and trackers honestly

Current package identity:

- Harmony local bundle name: `smartsales.HOS.ui`
- Harmony module name: `entry`
- Harmony ability name: `EntryAbility`
- Harmony app role name: `ui-verification`

---

## Supported Capability Set

The current `ui-verification` package supports:

- internal shell and route scaffold for the Harmony UI lane
- internal Tingwu page-native rewrite through a dedicated page adapter and repository-shaped seed input
- hidden scheduler preview page-native rewrite with mock data
- Harmony-owned package, lifecycle, and ArkUI page structure for isolated UI verification

---

## Disabled Capability Set

The current `ui-verification` package explicitly hides and does not support:

- public scheduler parity
- reminder/alarm delivery parity
- onboarding scheduler handoff parity
- public claims that mock-backed pages are backend-complete
- any use as the production Harmony package before dedicated device/signing proof exists

These limits are internal-lane honesty rules, not shared product-truth changes.

---

## Delivery Rule

This package rewrites pages natively in ArkUI from shared docs and Android behavior evidence.

Current rules:

- page contracts are page-facing Harmony UI contracts, not direct bindings to the backend mini-lab repository types
- mock drivers land first so page structure can stabilize before backend docking
- when a page moves beyond inline mock cards, the next step is a page-local adapter seam rather than a silent direct bind to backend internals
- scheduler preview stays behind an explicit internal gate until a later docking pass and capability review
- a page only becomes `Pass` through the Harmony UI tracker after docs, build/install evidence, and visual proof align

---

## Root Ownership

The owning Harmony-native root for this internal package is:

- `platforms/harmony/ui-verification/**`

All Harmony-native files for this package must stay inside that root.

They must not land under:

- `app/**`
- `app-core/**`
- `core/**`
- `data/**`
- `domain/**`

Current scaffold landing:

- root config files: `oh-package.json5`, `build-profile.json5`, `hvigorfile.ts`, `AppScope/app.json5`
- entry module files: `entry/oh-package.json5`, `entry/build-profile.json5`, `entry/src/main/module.json5`
- entry ability and page shell: `entry/src/main/ets/entryability/EntryAbility.ets`, `entry/src/main/ets/pages/Index.ets`
- page models and drivers: `entry/src/main/ets/model/HarmonyUiModels.ets`, `entry/src/main/ets/services/HarmonyUiDriver.ets`
- UI-lane signing helper: `scripts/assemble_device_debug.sh`
- Tingwu page adapter seam: `entry/src/main/ets/services/HarmonyUiTingwuPageAdapter.ets`, `entry/src/main/ets/services/HarmonyUiTingwuSeedRepository.ets`

---

## Current Verification Posture

Current phase:

- source scaffold is active
- shell and scheduler preview still use mock drivers
- HUI-02 now uses a dedicated Tingwu page adapter over repository-shaped seed data instead of inline mock cards
- dedicated bundle identity is active
- local Harmony build proof succeeded on 2026-04-11 with unsigned output at `platforms/harmony/ui-verification/entry/build/default/outputs/default/entry-default-unsigned.hap`
- local debug signing still produces a signed HAP at `platforms/harmony/ui-verification/entry/build/deviceDebug/outputs/deviceDebug/entry-deviceDebug-signed.hap` through `platforms/harmony/ui-verification/scripts/assemble_device_debug.sh --mode local`
- the signing helper now has an explicit AGC lane for `smartsales.HOS.ui`; `--install` and `--start` default to `--mode agc` and refuse the known-bad local pkcs7 lane
- on 2026-04-11, `scripts/assemble_device_debug.sh --mode agc` correctly stopped before install because no dedicated AGC asset set exists yet under `~/.ohos/ui-verification-signing/agc-debug`
- on 2026-04-11, forcing AGC mode to read the mini-lab asset dir `~/.ohos/tingwu-container-signing/agc-debug` failed preflight because the profile bundle is `smartsales.HOS.test`, not `smartsales.HOS.ui`
- the earlier real install attempt against target `4NY0225613001090` still stands: local-profile install failed with `code:9568257 error: fail to verify pkcs7 file`
- do not call this lane device-ready until a device-accepted signing/profile path exists for `smartsales.HOS.ui`

Current page order:

- `HUI-01` shell and internal gate
- `HUI-02` Tingwu page rewrite
- `HUI-03` hidden scheduler preview rewrite

---

## Future Direction

This package is the most likely candidate to evolve into the Harmony app shell if the following gates are cleared in order:

1. **Signing gate**: a device-accepted install of `smartsales.HOS.ui` is recorded in `docs/platforms/harmony/test-signing-ledger.md`
2. **HUI-02 docking**: the Tingwu page binds to real backend data through the docking contract defined in `docs/platforms/harmony/hui-02-tingwu-docking-contract.md`, replacing seed data with projected backend state
3. **Shell evaluation**: after successful HUI-02 docking, evaluate whether this package should take on shell-management ownership (route scaffold, page registration, shell-wide state) or remain a verification lane

Until the signing gate is cleared and at least one page is docked to real backend data, this package remains an internal verification lane. Its current role label does not change.

This section does not widen the current capability set or authorize public parity claims.
