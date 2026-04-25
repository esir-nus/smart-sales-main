# Sprint 07 — badge-switching-lifecycle

## Header

- Project: firmware-protocol-intake
- Sprint: 07
- Slug: badge-switching-lifecycle
- Date authored: 2026-04-25 (revised 2026-04-25 — real-device primary; UX rebuild deferred)
- Author: Claude
- Operator: **Codex** (default operator per project tracker)
- Lane: `develop` (Android dataflow validation; no Harmony work; no UI changes)
- Branch: `develop` (direct-to-develop; no feature branch)
- Worktree: none
- Ship path: one commit on `develop` at sprint close containing evidence artifacts + contract closeout. No production code changes are expected; inline defect patches are included in the same commit if needed.
- Blocked-by: none — two physical badges are available

## Demand

**User ask:** "We may split the tasks wisely, some involves UI/UX works like rebuilding the pairing user flow and UI/UX. Our test can be pure backend-centric dataflow verification. Actually I just got the second badge, we can use real device test instead of fakes."

**Interpretation:** Sprint 07 is a **pure backend dataflow verification sprint** using two real physical badges on a real Android device. The goal is to observe and capture logcat evidence proving that the registry, session, and BLE connection layers behave correctly across the full multi-device lifecycle: initial registration of both badges, switch A → B, post-switch reconnect, and switch B → A. No production code changes, no fake-based unit tests as primary evidence, no UI/UX evaluation.

The `SIM_ADD_DEVICE` pairing UX (screen routing, step sequence, UX polish) is explicitly deferred to a future sprint. This sprint only cares about what happens in the data layer after the second badge is paired.

## Scope

**In scope** (all paths rooted at repo root):

1. **Evidence artifacts** — the primary deliverable. Logcat excerpts committed under `docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/`. At minimum:
   - `registry-switch-A-to-B.txt` — logcat capture of Badge A → Badge B switch
   - `registry-switch-B-to-A.txt` — logcat capture of Badge B → Badge A switch

2. **Inline defect patches** in any of the following files, only if a dataflow defect is discovered during the device pass (document in ledger; no API shape changes):
   - `app-core/.../registry/RealDeviceRegistryManager.kt`
   - `app-core/.../data/connectivity/legacy/DeviceConnectionManagerConnectionSupport.kt`
   - `app-core/.../data/connectivity/legacy/DeviceConnectionManagerReconnectSupport.kt`

3. `docs/projects/firmware-protocol-intake/tracker.md` — flip sprint 07 row to `done` at closeout.

**Out of scope:**

- JVM unit tests with `FakeDeviceConnectionManager` / `InMemoryDeviceRegistry` — real device evidence supersedes these for this sprint
- Any UX evaluation of the `SIM_ADD_DEVICE` pairing flow — screen routing, step sequence, completion behavior — separate future sprint
- Any production UI changes
- `platforms/harmony/**`
- Sprint-08 or sprint-09 work

## References

Operator reads these, not the whole tree:

1. `docs/specs/sprint-contract.md` — sprint schema
2. `docs/projects/firmware-protocol-intake/tracker.md` — project context
3. `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt` — switch implementation; all log calls go through `ConnectivityLogger` (tag `SmartSalesConn`); key log strings: `"🏠 Registry: switching ${current?.macSuffix ?: "none"} → ${target.macSuffix}"` (line 89), `"🏠 Registry: registered"` (line 60)
4. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/ConnectivityLogger.kt:12` — single log tag for all connectivity and registry logs: `TAG = "SmartSalesConn"`; there is no `SmartSalesRegistry` or `SmartSalesBle` tag
5. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerConnectionSupport.kt:130,442` — key log strings: `"🔌 Soft disconnect (session preserved)"` on `disconnectBle()`; `"🔌 connectUsingSession: connected, ip=..."` on reconnect success; `"🔌 connectUsingSession: badge offline"` on reconnect failure
6. `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerReconnectSupport.kt:36–41` — `forceReconnectNow()` calls `launchReconnect(ignoreBackoff = true)` but emits **no log line itself**; reconnect progress is visible only via `AutoReconnecting` state and the subsequent `connectUsingSession` log

## Success Exit Criteria

Literally checkable:

- `ls docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/` exits 0 and lists both `registry-switch-A-to-B.txt` and `registry-switch-B-to-A.txt`
- `grep "Registry: switching" docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-A-to-B.txt` returns a line with two distinct MAC suffixes in the `none/...XX:XX → ...YY:YY` format
- `grep "Soft disconnect" docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-A-to-B.txt` returns at least one line
- `grep -E "connectUsingSession: (connected|badge offline)" docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-A-to-B.txt` returns at least one line
- `grep "Registry: switching" docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-B-to-A.txt` returns a line with the suffixes reversed from the A→B file
- `grep "Soft disconnect" docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-B-to-A.txt` returns at least one line
- `grep -E "connectUsingSession: (connected|badge offline)" docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-B-to-A.txt` returns at least one line
- `./gradlew :app:assembleDebug` exits 0

## Stop Exit Criteria

Halt and surface:

- The registry switch log line (`Registry: switching`) never appears in logcat after tapping switch — stop, surface the raw `SmartSalesConn` logcat around the tap, and propose adding a targeted log to `switchToDevice()` as a defect patch before retrying
- `switchToDevice()` completes but the evidence window for either switch direction lacks `Soft disconnect` or lacks both `connectUsingSession: connected` and `connectUsingSession: badge offline` within 15 seconds of the switch log — stop, surface the logcat gap and the last state visible in the log; investigate whether `disconnectBle()` or `forceReconnectNow()` was called. `Soft disconnect` alone is not sufficient post-switch evidence.
- Badge B cannot be registered at all via any available path in the current build — stop, surface as `blocked`; note that a pairing UX sprint must be prioritized first
- Any switch results in a crash (ANR, NullPointerException in registry or connection layer) — stop, surface the stacktrace, do not retry without patching
- `./gradlew :app:assembleDebug` fails at close
- Iteration bound hit without registry switch log evidence in both files

## Iteration Bound

- Max 3 iterations OR 60 minutes wall-clock, whichever hits first
- One iteration = install build → device pass (pair + switch + capture logs) → evidence save
- If a defect patch is needed, that patch + re-run counts as the next iteration

## Required Evidence Format

At close, operator appends to Closeout:

1. **Build evidence**
   - `./gradlew :app:assembleDebug` tail showing `BUILD SUCCESSFUL`
   - `adb install` confirmation line

2. **Device dataflow evidence** — primary gate
   - Exact `adb logcat` command used (see Codex Handoff for the correct tag and grep strings)
   - `registry-switch-A-to-B.txt` content (MAC addresses redacted to suffix: `...XX:XX`)
   - `registry-switch-B-to-A.txt` content (MAC addresses redacted to suffix)
   - `ls docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/` output

3. **Defect log** (if any patches were made)
   - `git diff --stat` for inline fixes
   - One-line description of the defect found and how it was patched

## Iteration Ledger

<!-- Operator appends one entry per iteration below this line -->

- **Iteration 1 — 2026-04-25**
  - Built and installed the originally authored `:app` artifact first, but device validation showed that package is `com.smartsales.aitest` and is not the user-facing Smart Sales artifact for this pass.
  - Switched validation to `:app-core` (`app-core/build/outputs/apk/debug/app-core-debug.apk`, package `com.smartsales.prism`) and installed it successfully.
  - Real-device add-device pass exposed that an already registered badge could still appear in post-onboarding discovery while a second unregistered badge remained discoverable. Patched `RealPairingService` to filter registered MACs and `AndroidBleScanner` to accumulate trusted candidates instead of stopping at the first trusted scan result.
  - Real-device switch pass exposed stale session/GATT reuse: switch calls could seed the target but reconnect through the previous active transport. Patched registry switching to pass an explicit target `BleSession`, made reconnect support accept that target session, and made the GATT gateway close a stale persistent session before connecting a different MAC.
  - Result: real-device logcat captured successful A->B and B->A switch lifecycle with `Registry: switching`, `Soft disconnect`, stale GATT close, target GATT establishment, and `connectUsingSession: connected` in both directions.

## Closeout

<!-- Operator fills at sprint exit -->
- **Status:** success
- **Summary for project tracker:** Real-device Sprint 07 closed on `develop`: two physical badges validated registry switch A->B and B->A with explicit target reconnect, stale GATT close, app-core install evidence, focused JVM coverage, and debug builds. Inline patches also hide already registered badges from add-device discovery so the post-onboarding registry flow does not re-offer occupied devices.
- **Evidence artifacts:**
  - Build/test evidence:
    - `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.data.connectivity.legacy.DefaultDeviceConnectionManagerIngressTest' --tests 'com.smartsales.prism.data.pairing.RealPairingServiceTest'` -> `BUILD SUCCESSFUL in 1m 10s`
    - `./gradlew :app-core:assembleDebug` -> `BUILD SUCCESSFUL in 13s`
    - `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL in 3s`
    - `adb install -r app-core/build/outputs/apk/debug/app-core-debug.apk` -> `Success`
    - `adb shell pm list packages | rg 'com\.smartsales\.(prism|aitest)'` -> `package:com.smartsales.prism` and `package:com.smartsales.aitest`
  - Device dataflow command:
    - `adb logcat -v time -s SmartSalesConn:D` captured to `/tmp/07-lifecycle-appcore-final.txt`
  - Device dataflow evidence:
    - `docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-A-to-B.txt`
    - `docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-B-to-A.txt`
  - Evidence directory proof:
    - `ls docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/` -> `registry-switch-A-to-B.txt`, `registry-switch-B-to-A.txt`
  - Contract grep proof:
    - A->B: `Registry: switching ...E3:F6 -> ...8F:96`; `Soft disconnect (session preserved)`; `connectUsingSession: connected, ip=192.168.0.107 ssid=MstRobot`
    - B->A: `Registry: switching ...8F:96 -> ...E3:F6`; `Soft disconnect (session preserved)`; `connectUsingSession: connected, ip=192.168.0.107 ssid=MstRobot`
  - Defect log:
    - `git diff --cached --stat` for scoped Sprint 07 files: 20 files changed, 468 insertions(+), 33 deletions(-)
    - Add-device discovery defect: registered badge remained visible in post-onboarding pairing; patched scanner/pairing filtering so occupied badges remain in registry UI only.
    - Switching lifecycle defect: switch could reconnect through stale session/GATT state; patched explicit target-session reconnect, reconnect job cancellation, synchronous session persistence, and stale GATT close.
- **Lesson proposals:** Consider adding a repo lesson that Android device evidence must verify the installed `applicationId` before runtime testing when multiple debug APKs exist (`app` vs `app-core`).
- **CHANGELOG line:** Sprint 07: validated two-badge real-device switching lifecycle and fixed add-device discovery plus target reconnect defects found during evidence capture.

---

## Codex Handoff

*Non-schema appendix — operator reference only. Not part of the ten required sprint sections.*

Paste this prompt into Codex to start the operator session:

```
You are the operator for sprint 07 of the firmware-protocol-intake project.

Contract file (read end-to-end before touching any code):
  docs/projects/firmware-protocol-intake/sprints/07-badge-switching-lifecycle.md

Lane: develop. Branch: develop. Iteration bound: 3.
Two real physical badges are available. A real Android device is connected via adb.

This sprint has NO planned production code changes and NO JVM unit tests as
primary evidence. The deliverable is real-device logcat evidence.

--- Setup ---

Build and install:
  ./gradlew :app:assembleDebug
  adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk

Create evidence directory:
  mkdir -p docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle

--- Logcat setup ---

ALL connectivity and registry log lines share a single tag: SmartSalesConn
(ConnectivityLogger.kt:12 — there is no SmartSalesRegistry or SmartSalesBle tag).

Start a scoped capture:
  adb -s <serial> logcat -c
  adb -s <serial> logcat -v time -s SmartSalesConn:D 2>&1 | tee /tmp/07-lifecycle.txt

--- Human flow to exercise while capture runs ---

  A. Open the app. Confirm Badge A is registered and connected.
  B. Navigate to connectivity manager -> tap 添加设备 -> pair Badge B using
     whatever pairing path is available in the current build.
  C. After Badge B is registered, tap to switch the active device to Badge B.
     Wait for reconnect to settle (~10s).
  D. Perform one post-switch badge interaction (e.g., tap the firmware version
     refresh row in UserCenter, or wait for a battery push from Badge B).
  E. Switch back to Badge A. Wait for reconnect to settle.
  F. Stop the capture.

--- Save evidence files ---

For A→B direction (lines from step C through D):
  grep "Registry: switching\|Soft disconnect\|connectUsingSession" \
    /tmp/07-lifecycle.txt | head -30 \
    > docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-A-to-B.txt

For B→A direction (lines from step E):
  grep "Registry: switching\|Soft disconnect\|connectUsingSession" \
    /tmp/07-lifecycle.txt | tail -30 \
    > docs/projects/firmware-protocol-intake/evidence/07-badge-switching-lifecycle/registry-switch-B-to-A.txt

Redact full MAC addresses to suffix only (e.g., "AA:BB:CC:DD:EE:FF" -> "...EE:FF")
before saving.

Key log strings to verify are present:
  - "🏠 Registry: switching ...XX:XX → ...YY:YY"  (RealDeviceRegistryManager:89)
  - "🔌 Soft disconnect (session preserved)"       (ConnectionSupport:130)
  - "🔌 connectUsingSession: connected"            (ConnectionSupport:442)
    OR "🔌 connectUsingSession: badge offline"     (if Wi-Fi unavailable)

Note: forceReconnectNow() emits NO log line. Reconnect progress is only
visible from the subsequent connectUsingSession line.

If "Registry: switching" never appears after a switch tap, stop and surface
the raw SmartSalesConn logcat around the tap — do not retry silently.

--- Commit discipline ---
No mid-sprint commits. Tree stays dirty across iterations.
On success: single commit on develop covering evidence files + any inline
defect patches + contract closeout. Commit message references this contract.
On stop/blocked: do not commit; fill Closeout with the reason.

Append one Iteration Ledger entry per attempt. At close, fill Closeout with
status + summary + the evidence categories from Required Evidence Format.
```
