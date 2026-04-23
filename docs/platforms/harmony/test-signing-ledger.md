# Harmony Test And Signing Ledger

> **Purpose**: Keep one simple record for Harmony app identity, AGC naming, signing artifacts, device targeting, and on-device test commands so local packaging does not drift from cloud signing state.
> **Status**: Active
> **Last Updated**: 2026-04-12
> **Primary Trackers**:
> - `docs/plans/harmony-tracker.md`
> - `docs/plans/harmony-ui-translation-tracker.md`

---

## 1. Naming Law

Use one stable naming chain across local code, AGC, and device testing.

Current Harmony lanes:

- backend mini-lab app
  - bundle name: `smartsales.HOS.test`
  - module: `entry`
  - ability: `EntryAbility`
  - branch: `harmony/tingwu-container`
  - role name: `tingwu-container`
- internal UI verification app
  - bundle name: `smartsales.HOS.ui`
  - module: `entry`
  - ability: `EntryAbility`
  - branch: `harmony/tingwu-container`
  - role name: `ui-verification`
- complete native app
  - bundle name: `smartsales.HOS.test`
  - module: `entry`
  - ability: `EntryAbility`
  - branch: `harmony/scheduler-phase-2b`
  - role name: `smartsales-app`

Rule:

- the mini-lab app stays tied to the current AGC-backed signing assets
- the UI verification app uses a distinct bundle identity so it can install separately from the mini-lab app
- do not claim device-install readiness for the UI verification app until a device-accepted signing/profile lane exists for `smartsales.HOS.ui`

---

## 1a. Shared Credential Model

Both Harmony packages share one signing credential lane but use separate package-specific profiles.

Shared material (same `.p12`, same alias, same passwords):

| Asset | Shared | Notes |
|---|---|---|
| `.p12` keystore | yes | one signer identity for both packages |
| key alias | yes | same alias in credentials.txt |
| store password | yes | same store password |
| key password | yes | same key password |

Package-specific material (never shared):

| Asset | mini-lab | ui-verification |
|---|---|---|
| bundle name | `smartsales.HOS.test` | `smartsales.HOS.ui` |
| `.p7b` profile | `smartsales.HOS.test` scoped | `smartsales.HOS.ui` scoped |
| AGC asset directory | `~/.ohos/tingwu-container-signing/agc-debug/` | `~/.ohos/ui-verification-signing/agc-debug/` |

Install rule:

- install ui-verification: use bundle `smartsales.HOS.ui`, use the `smartsales.HOS.ui` `.p7b`
- install mini-lab: use bundle `smartsales.HOS.test`, use the `smartsales.HOS.test` `.p7b`

Mental model: one signer, two app identities, one install target at a time.

Agent instruction:

- use the shared credential lane, but choose the profile by target package: `smartsales.HOS.ui` for UI, `smartsales.HOS.test` for mini-lab
- never infer target package from filename alone
- always verify: target bundle name, matching `.p7b` bundle, launch command bundle name

Script usage:

```bash
# 签名并安装 UI 验证包
./scripts/assemble_device_debug.sh --target ui --mode agc --install

# 签名并安装后端小实验室
./scripts/assemble_device_debug.sh --target minilab --mode agc --install
```

---

## 2. Current Identity Record

| Lane | Bundle | Signing Status | Notes |
|---|---|---|---|
| `tingwu-container` | `smartsales.HOS.test` | active AGC-backed signing lane with shared credential | Shared `.p12` and alias, package-specific `.p7b` |
| `ui-verification` | `smartsales.HOS.ui` | AGC-backed signing lane with shared credential; device install pending operator confirmation | Shared `.p12` and alias, package-specific `.p7b`; local signed HAP verified; AGC preflight wired |
| `smartsales-app` | `smartsales.HOS.test` | AGC-backed device proof recorded for HS-006 | Uses the same bundle lane as the mini-lab but a different app root under `platforms/harmony/smartsales-app/` |

Common values:

| Field | Current Value | Notes |
|---|---|---|
| Branch | `harmony/tingwu-container` | Active Harmony lane |
| Ability | `EntryAbility` | Launch target |
| Module | `entry` | Main entry module |
| Vendor | `smartsales` | From `AppScope/app.json5` |
| Version name | `0.1.0` | Current app package version |
| Version code | `1` | Current app package version code |

---

## 3. Current Command Set

Backend mini-lab build:

```bash
cd platforms/harmony/tingwu-container
../../commandline-tools-linux-x64-6.0.2.650/command-line-tools/bin/hvigorw --mode module -p product=default -p buildMode=debug assembleHap --stacktrace
```

UI verification build:

```bash
cd platforms/harmony/ui-verification
../../commandline-tools-linux-x64-6.0.2.650/command-line-tools/bin/hvigorw --mode module -p product=default -p buildMode=debug assembleHap --stacktrace
```

Complete native app build:

```bash
cd platforms/harmony/smartsales-app
~/.local/bin/hvigorw assembleHap --mode module -p product=default
```

Complete native app AGC-backed device install:

```bash
java -jar platforms/harmony/commandline-tools-linux-x64-6.0.2.650/command-line-tools/sdk/default/openharmony/toolchains/lib/hap-sign-tool.jar \
  sign-app \
  -mode localSign \
  -keyAlias tingwuAgcDebug \
  -keyPwd "$KEY_PASSWORD" \
  -appCertFile ~/.ohos/tingwu-container-signing/agc-debug/com.smartsales.harmony.app.cer \
  -profileFile ~/.ohos/tingwu-container-signing/agc-debug/com.smartsales.harmony.appDebug.p7b \
  -inFile platforms/harmony/smartsales-app/entry/build/default/outputs/default/entry-default-unsigned.hap \
  -signAlg SHA256withECDSA \
  -keystoreFile ~/.ohos/tingwu-container-signing/agc-debug/tingwu-agc-debug.p12 \
  -keystorePwd "$STORE_PASSWORD" \
  -outFile platforms/harmony/smartsales-app/entry/build/agcDeviceDebug/outputs/agcDeviceDebug/entry-agcDeviceDebug-signed-hs006.hap \
  -compatibleVersion 12 \
  -signCode 1

hdc -t <connect_key> install -r platforms/harmony/smartsales-app/entry/build/agcDeviceDebug/outputs/agcDeviceDebug/entry-agcDeviceDebug-signed-hs006.hap
hdc -t <connect_key> shell aa start -a EntryAbility -b smartsales.HOS.test
```

UI verification signed build (local chain):

```bash
cd platforms/harmony/ui-verification
./scripts/assemble_device_debug.sh --target ui --mode local
```

Mini-lab signed build and deploy (AGC):

```bash
cd platforms/harmony/ui-verification
./scripts/assemble_device_debug.sh --target minilab --mode agc --install
```

Mini-lab launch:

```bash
hdc -t <connect_key> shell aa start -a EntryAbility -b smartsales.HOS.test
```

UI verification signed build and deploy (AGC):

```bash
cd platforms/harmony/ui-verification
./scripts/assemble_device_debug.sh --target ui --mode agc --install
```

UI verification launch:

```bash
hdc -t <connect_key> shell aa start -a EntryAbility -b smartsales.HOS.ui
```

UI verification AGC-backed device lane:

```bash
cd platforms/harmony/ui-verification
./scripts/assemble_device_debug.sh --target ui --mode agc
./scripts/assemble_device_debug.sh --target ui --start
```

Expected AGC asset roots:

```text
~/.ohos/ui-verification-signing/agc-debug/      (for smartsales.HOS.ui)
~/.ohos/tingwu-container-signing/agc-debug/      (for smartsales.HOS.test)
```

---

## 4. Acceptance Checkpoints

For the current Harmony scheduler mini-lab, the required telemetry joints are:

- `INGRESS_ACCEPTED`
- `COMMAND_CLASSIFIED`
- `SNAPSHOT_PERSISTED`
- `PATH_A_COMMITTED`

For the current Harmony UI verification lane, required checkpoints are:

- dedicated package builds from its own root
- current local build proof exists at `platforms/harmony/ui-verification/entry/build/default/outputs/default/entry-default-unsigned.hap`
- current local signed proof exists at `platforms/harmony/ui-verification/entry/build/deviceDebug/outputs/deviceDebug/entry-deviceDebug-signed.hap`
- internal gate stays honest
- page proof is recorded in `docs/plans/harmony-ui-translation-tracker.md`

---

## 5. UI Verification Evidence Note: Local Signing vs Device Acceptance

This evidence note applies to the internal UI lane `smartsales.HOS.ui`.

Resolved local chain on 2026-04-11:

1. compile the unsigned package from `platforms/harmony/ui-verification` with `hvigorw ... assembleHap`
2. generate a UI-only local debug keypair, cert chain, and signed profile through `platforms/harmony/ui-verification/scripts/assemble_device_debug.sh --mode local`
3. verify the signed profile and signed HAP locally with `hap-sign-tool`

Real device result on 2026-04-11:

- target connect key: `4NY0225613001090`
- command path: `platforms/harmony/ui-verification/scripts/assemble_device_debug.sh --start`
- install result: `failed to install bundle. code:9568257 error: fail to verify pkcs7 file`
- launch result: `10104001` because the app was not installed

Interpretation:

- the UI lane is no longer blocked on unsigned package assembly
- the live blocker is device-side acceptance of the current local profile chain
- the scripted local signing helper is useful for repeatable package verification, but it is not yet accepted by the attached device as the final install path

AGC-lane hardening on 2026-04-11:

1. `platforms/harmony/ui-verification/scripts/assemble_device_debug.sh` now separates `--mode local` from `--mode agc`
2. `--install` and `--start` now default to AGC mode and refuse the local pkcs7 lane
3. AGC mode now requires a dedicated UI asset set under `~/.ohos/ui-verification-signing/agc-debug/` unless explicit overrides are provided
4. AGC mode decodes the `.p7b` before signing and fails fast unless the issuer is `app_gallery`, the bundle is `smartsales.HOS.ui`, an `app-identifier` exists, and the resolved device id is present

Shared credential model resolved on 2026-04-12:

- both packages now share the same `.p12` keystore, alias, and passwords
- each package has its own AGC-issued `.p7b` profile scoped to its bundle name
- the signing script now accepts `--target ui|minilab` to select the correct package and profile
- the script resolves the project directory, bundle name, and AGC signing directory based on the target
- shared credential material can live in either AGC asset directory since the `.p12`, alias, and passwords are identical

Current AGC evidence on 2026-04-11:

- `./scripts/assemble_device_debug.sh --mode agc` stopped before install because the expected UI asset set does not exist yet under `~/.ohos/ui-verification-signing/agc-debug/`
- `HARMONY_UI_AGC_SIGNING_DIR=~/.ohos/tingwu-container-signing/agc-debug ./scripts/assemble_device_debug.sh --mode agc` failed preflight because the signed profile bundle is `smartsales.HOS.test`

Operator reminder:

- keep the scripted local signing helper for repeatable package verification
- do not treat the current local pkcs7 chain as proof that HarmonyOS hardware accepts `smartsales.HOS.ui`
- the shared `.p12` credential is valid for both packages, but always verify the `.p7b` matches the target bundle before install
- use `--target ui` or `--target minilab` to select the correct package; never rely on filename inference
- the next honest step is confirming that the `smartsales.HOS.ui` AGC profile installs cleanly on device

---

## 6. Mini-Lab Evidence Note: Scheduler Backend Experiment

This evidence note applies to the backend mini-lab lane `smartsales.HOS.test`, not to the still device-blocked `smartsales.HOS.ui` UI verification lane.

Resolved experiment chain:

1. compile the mini-lab from `platforms/harmony/tingwu-container` with `hvigorw ... assembleHap`
2. sign the mini-lab against the active AGC-backed `smartsales.HOS.test` lane
3. deploy the signed HAP with `hdc -t <connect_key> install -r ...`
4. launch with `hdc -t <connect_key> shell aa start -a EntryAbility -b smartsales.HOS.test`
5. inspect runtime behavior and telemetry with `hdc shell hilog`

Resolved symptom:

- Harmony compile proof alone was not enough to get the scheduler backend mini-lab onto device cleanly; signing and deployment still had to agree with the mini-lab bundle lane.

Root cause:

- the build lane, bundle identity, generated local config path, AGC-backed signing lane, and `hdc` deploy target must all point at the same mini-lab app contract
- treating unsigned build output or a generic Harmony package as if it proved device readiness hid the real lane mismatch

Wrong approach:

- stopping at unsigned `assembleHap` success and calling the lane ready
- mixing the backend mini-lab identity with the blocked `smartsales.HOS.ui` package assumptions
- treating repo-root `local.properties` generation, signing readiness, and `hdc` deploy as separate chores instead of one contract chain

Correct fix:

- keep `hvigorfile.ts` as the build-owned generator for the Harmony local config artifact
- keep the backend mini-lab on the AGC-backed `smartsales.HOS.test` signing lane
- deploy only the signed mini-lab artifact that matches that bundle identity
- verify success on device through launch plus `hilog`, not compile output alone

Operator reminder:

- `platforms/harmony/ui-verification/**` now has unsigned-build proof plus local signed-HAP proof, but it still does not have device-accepted install proof; do not reuse this mini-lab evidence to claim UI-package install readiness

---

## 6a. Complete Native App Evidence Note: HS-006 Scheduler Harness

This evidence note applies to `platforms/harmony/smartsales-app` on branch `harmony/scheduler-phase-2b`.

Resolved chain on 2026-04-20:

1. build the complete native app from `platforms/harmony/smartsales-app`
2. sign the unsigned HAP against the active AGC-backed `smartsales.HOS.test` lane
3. install on device `4NY0225613001090`
4. launch `EntryAbility`
5. run the HS-010-style harness loop with screenshots, layout dumps, relaunch, and `hilog`

Recorded evidence:

- create path rendered on device through the real form submit path using the deterministic `Load HS-006 Demo Values` preset and `Create Task`
- reminder attempt stayed honest via the deferred HS-011 path for a future start time
- cold restore logs showed:
  - `[HS-006][FileStore] loadTasks success count=1`
  - `[HS-006][SchedulerRepository] initialize restored count=1`
- the first device pass exposed an ArkUI row-reuse bug where completion succeeded but the row kept the stale `Complete` button; the fix was to key the task row by `id + updatedAt + completion state`
- after the UI fix, the restored task row rendered `Completed · 2030-01-15 09:30`
- after delete plus cold restart, `hilog` showed:
  - `[HS-006][FileStore] loadTasks success count=0`
  - `[HS-006][SchedulerRepository] initialize restored count=0`
- the final scheduler screenshot returned to `0 tasks saved locally`

Operator caution:

- Harmony `uitest` transport and foreground focus remained unstable during the pass
- do not claim raw IME automation reliability from this run; the deterministic form preset exists specifically to keep the harness on the real create pipeline without trusting Harmony text entry

## 6b. Complete Native App Evidence Note: HS-006 Scheduler Harness Phase 2C

This evidence slot applies to `platforms/harmony/smartsales-app` on branch `harmony/scheduler-phase-2c` or the Phase 2C continuation on `harmony/scheduler-phase-2b`.

Planned operator chain for 2026-04-21:

1. build the complete native app from `platforms/harmony/smartsales-app`
2. sign the unsigned HAP against the active AGC-backed `smartsales.HOS.test` lane
3. install on device `4NY0225613001090`
4. launch `EntryAbility`
5. run the Phase 2C mini-lab loop with screenshots plus `hilog | grep HS-006`

Required evidence to append after the operator pass:

- Scenario A: overlapping future tasks show conflict only on the later non-`FIRE_OFF` row
- Scenario B: reschedule clears conflict on both rows while preserving the task `id` and bumping `updatedAt`
- Scenario C: reschedule back onto the occupied slot restores the conflict banner on the later row
- Scenario D: overlapping `FIRE_OFF` create bypasses conflict flags entirely
- Scenario E: cold relaunch after Scenario B restores the rescheduled time and the cleared conflict state
- Reminder seam proof stays honest through deferred HS-011 logs for both `cancelReminder` on the old slot and `publishReminder` on the new slot during reschedule

Current status:

- implementation landed in code and docs on 2026-04-21
- local Harmony build proof still required for this exact revision
- device screenshots and `hilog` proof still pending; do not mark Phase 2C device-verified until this section is filled with the actual log lines and operator artifacts

---

## 7. Drift Guardrail

Before changing any of the following, update this ledger first:

- local bundle name
- AGC app name or profile name
- signing file names
- target device or connect key assumptions
- install or launch command

If the UI verification package receives a device-accepted signing lane, update this ledger in the same session before claiming on-device `Pass` for any page.
