# Harmony Test Signing Ledger

> **Role**: Record of signing milestones and device-accepted install evidence for Harmony packages
> **Status**: Active
> **Date**: 2026-04-21
> **Primary Law**: `docs/specs/platform-governance.md`

---

## Purpose

Every Harmony package must have a recorded signing milestone before it can be used for device verification or docking. This ledger is the single surface for that evidence.

---

## Paved Road: Signed HAP and Device-Proof Lane

Treat Harmony verification as one lane contract, not separate chores.

Required chain:

1. compile the same Harmony root you intend to verify
2. produce a signed HAP for the same bundle identity
3. install that exact signed HAP onto the target device
4. launch that exact bundle
5. capture `hilog` evidence from that same runtime

Do not claim L3/device readiness from any of these partial states by themselves:

- unsigned `.hap`
- stale previously signed `.hap`
- install proof without launch proof
- launch proof without `hilog` proof
- proof gathered from a different bundle ID than the lane under test

### Canonical Current Lane

Use this lane when the task needs a real signed Harmony device pass today:

- `Package`: Backend Mini-Lab
- `Bundle ID`: `smartsales.HOS.test`
- `Device`: `4NY0225613001090`
- `Evidence Standard`: signed HAP -> `hdc install` -> `aa start` -> `hilog`

Current rule:

- `smartsales.HOS.test` is the only device-accepted Harmony signing lane currently proven in this repo
- `smartsales.HOS.ui` remains blocked until it has its own AGC asset set
- `com.smartsales.harmony.app` must not borrow proof from `smartsales.HOS.test`

### Canonical Operator Commands

Resolve the exact signed artifact path for a declared lane:

```bash
scripts/harmony-lane-proof.sh hs-test --path-only
```

Print the matching install, launch, and log commands for that same lane:

```bash
scripts/harmony-lane-proof.sh hs-test
```

Current helper behavior:

- `hs-test` resolves the current proven `smartsales.HOS.test` signed HAP
- `app` fails closed until a real `com.smartsales.harmony.app` signed HAP exists
- `hui` fails closed until the UI verification root and signing lane exist locally

Build:

```bash
cd platforms/harmony/smartsales-app
~/.local/bin/hvigorw assembleHap --mode module -p product=default
```

Install the exact signed HAP you intend to verify:

```bash
hdc -t 4NY0225613001090 install -r <path-to-signed-hap>
```

Launch the matching bundle:

```bash
hdc -t 4NY0225613001090 shell aa start -a EntryAbility -b <bundle-id>
```

Capture runtime proof:

```bash
hdc -t 4NY0225613001090 shell hilog -r
hdc -t 4NY0225613001090 shell hilog -G 16M
hdc -t 4NY0225613001090 shell hilog -x | rg 'HS-006|Scheduler|Reminder'
```

Acceptance rule:

- if the installed binary does not expose the requested surface, the lane is not ready even if install and launch succeed
- if the signed HAP is older than the requested feature slice, rebuild/sign before claiming device evidence
- always record the exact signed artifact path and bundle ID used for the pass
- use `scripts/harmony-lane-proof.sh` to resolve the artifact path instead of guessing among stale `.hap` outputs

---

## Signing Status

| Package | Bundle ID | Status | Last Verified | Device | Notes |
|---------|-----------|--------|---------------|--------|-------|
| Backend Mini-Lab | `smartsales.HOS.test` | Signed, device-accepted | 2026-04-21 | `4NY0225613001090` | Canonical current paved-road lane. Only proven Harmony signed-HAP device lane in repo today. |
| UI Verification | `smartsales.HOS.ui` | Blocked | -- | -- | pkcs7 error 9568257; AGC asset set not yet created |
| Complete Native App | `com.smartsales.harmony.app` | Scaffolded, signing not started | -- | -- | App root committed at `platforms/harmony/smartsales-app/`; AGC profile and signing still pending. Must not inherit device proof from `smartsales.HOS.test`. |

---

## Resolution Path for smartsales.HOS.ui

Current blocker: pkcs7 verification error `9568257` on device install.

Steps to resolve:

1. Create `smartsales.HOS.ui` profile in AGC (AppGallery Connect)
2. Generate valid signing certificate for the UI package
3. Configure `build-profile.json5` with signing config
4. Build signed HAP: `hvigorw assembleHap --mode module -p product=default`
5. Install on device: `hdc install <path-to-signed.hap>`
6. Verify launch: `hdc shell aa start -a EntryAbility -b smartsales.HOS.ui`
7. Record evidence below

**This is a manual/operator step requiring AGC console access.**

---

## Resolution Path for com.smartsales.harmony.app

The `platforms/harmony/smartsales-app/` scaffold now exists. Signing work has not started yet.

Steps to resolve:

1. Create `com.smartsales.harmony.app` profile in AGC
2. Configure signing in `platforms/harmony/smartsales-app/build-profile.json5`
3. Follow same build/install/verify chain as above
4. Record evidence below

---

## Evidence Records

### smartsales.HOS.test (Backend Mini-Lab)

- **Date**: 2026-04-11
- **Device**: `4NY0225613001090`
- **Build**: local `hvigorw` build
- **Install**: `hdc install` succeeded
- **Launch**: `hdc shell aa start` succeeded
- **Logs**: `hdc shell hilog` confirmed runtime entry and telemetry events
- **Signing**: local certificate, not AGC-distributed

### Lane Contract Reminder

- This row proves only the `smartsales.HOS.test` lane.
- Do not reuse this proof for `smartsales.HOS.ui`.
- Do not reuse this proof for `com.smartsales.harmony.app`.
- If the requested feature is absent from the installed signed HAP, the operator must rebuild/sign the target lane before claiming device verification.

### smartsales.HOS.ui (UI Verification)

_Pending — blocked on AGC asset set creation._

### com.smartsales.harmony.app (Complete Native App)

_Pending — scaffold committed; AGC profile, signing config, install, and launch evidence not yet recorded._
