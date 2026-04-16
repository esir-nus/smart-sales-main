# Harmony Test Signing Ledger

> **Role**: Record of signing milestones and device-accepted install evidence for Harmony packages
> **Status**: Active
> **Date**: 2026-04-16
> **Primary Law**: `docs/specs/platform-governance.md`

---

## Purpose

Every Harmony package must have a recorded signing milestone before it can be used for device verification or docking. This ledger is the single surface for that evidence.

---

## Signing Status

| Package | Bundle ID | Status | Last Verified | Device | Notes |
|---------|-----------|--------|---------------|--------|-------|
| Backend Mini-Lab | `smartsales.HOS.test` | Signed, device-accepted | 2026-04-11 | `4NY0225613001090` | Compile -> signing -> hdc deploy -> launch -> hilog chain verified |
| UI Verification | `smartsales.HOS.ui` | Blocked | -- | -- | pkcs7 error 9568257; AGC asset set not yet created |
| Complete Native App | `com.smartsales.harmony.app` | Scaffolded, signing not started | -- | -- | App root committed at `platforms/harmony/smartsales-app/`; AGC profile and signing still pending |

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

### smartsales.HOS.ui (UI Verification)

_Pending — blocked on AGC asset set creation._

### com.smartsales.harmony.app (Complete Native App)

_Pending — scaffold committed; AGC profile, signing config, install, and launch evidence not yet recorded._
