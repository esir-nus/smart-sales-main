# Sprint 04-b Runtime Status

- Date: 2026-04-30
- Device baseline: `adb devices` sees `fc8ede3e device`
- Session result: local code plus focused/full `:app-core:testDebugUnitTest`
  verification completed, debug APK installed on the attached phone, and the
  debug-gated connectivity manager probes were enabled for installed-runtime
  capture.
- Installed-debug `platform-runtime/L2.5` evidence captured:
  - loop A app-side owner-selection path:
    - `run-07-loop-a-l25-logcat.txt`
    - `run-07-loop-a-l25-ui.xml`
    - `run-07-loop-a-l25.png`
    - Result: PASS for
      `CONNECTIVITY_ACTIVE_ONLY_DUAL_ADVERTISE`, selected MAC stays
      `14:C1:9F:D7:E4:06`
  - loop C app-side suppression path:
    - `run-08-loop-c-l25-logcat.txt`
    - `run-08-loop-c-l25-ui.xml`
    - `run-08-loop-c-l25.png`
    - Result: PASS for
      `CONNECTIVITY_MANUAL_DEFAULT_SUPPRESSION`, selected MAC stays
      `14:C1:9F:D7:E4:06` while the suppressed default card remains
      disconnected-first
- Remaining unclosed runtime branches:
  - loop B: no installed-debug deterministic ingress exists for the
    "latest intended active absent -> chooser with no auto-connect" branch, and
    this session did not have a bounded physical second-badge choreography to
    create that state honestly
  - loop D: no installed-debug deterministic ingress exists for
    "stale mismatch/isolation UI present -> reconnect success clears it", and
    this session did not have a bounded physical mismatch/isolation repro plus
    reconnect collaboration plan
- Factual closeout law:
  - do not claim Sprint 04-b physical L3 closure
  - do not claim full loop A-D runtime closure
  - do record that loops A and C now have installed-debug `platform-runtime/L2.5`
    evidence, while loops B and D remain runtime-blocked
- Physical L3 next-run artifact:
  - `run-09-physical-l3-choreography.md` declares the exact badge identity
    fields, manual collaboration items, capture commands, pass criteria, and
    block criteria for physical loops A-D
