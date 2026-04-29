# Run 23-26 Rec Switch Follow-Up Verdict

Date: 2026-04-29

Scenario:
- Build and install the updated debug APK without clearing app data, preserving
  registered badge state.
- Launch `MainActivity`.
- Capture filtered logcat, UI XML, and screenshots for the restored badge/audio
  state.
- Try to locate an available UI path for two-badge active switch while a live
  `rec#` auto-download notification is in flight.

Artifacts:
- `run-23-rec-switch-entry-logcat.txt`
- `run-23-rec-switch-entry-ui.xml`
- `run-23-rec-switch-entry.png`
- `run-24-drawer-logcat.txt`
- `run-24-drawer-ui.xml`
- `run-24-drawer.png`
- `run-25-drawer-retry-ui.xml`
- `run-26-settings-logcat.txt`
- `run-26-settings-ui.xml`
- `run-26-settings.png`

Observed:
- Device `fc8ede3e` was connected.
- Updated APK installed successfully.
- App restored registered badge `14:C1:9F:D7:E4:06`.
- BLE/GATT session established.
- Manager reached `Ready`.
- Firmware notification reported `Ver#1.0.0.1`.
- Auto sync ran `/list` against `http://192.168.0.101:8088`.
- Badge-owned auto sync/download attempted `rec_19700101_000145.wav`.

Verdict:
- L3 proves the rebuilt APK still restores the badge, reaches ready, and runs
  badge-owned auto sync/download.
- L3 does not prove the exact live `rec#` notification interrupted by
  active-device switch. That branch remains blocked by hardware event
  availability in this run.
- Code-level closure is covered by focused L1/L2:
  `SimAudioRepositorySyncSupportTest.active device change cancels active rec
  auto download`.
