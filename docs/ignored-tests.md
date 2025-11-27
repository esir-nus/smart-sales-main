<!-- 文件：docs/ignored-tests.md -->
<!-- 说明：记录当前被 @Ignore 的测试项、原因与后续修复计划 -->

# Ignored Tests Tracker

## :app androidTest

- `com.smartsales.aitest.devicemanager.DeviceManagerScreenTest.loadingState_showsProgressAndDisablesButtons`
  - Reason: Grid refactor causes loading row/tag not considered displayed in instrumentation; needs deterministic visibility or relaxed assertion once UI stabilizes.
- `com.smartsales.aitest.devicemanager.DeviceManagerScreenTest.listState_rendersFilesAndTriggersActions`
  - Reason: Grid refactor makes file items/duration visibility flaky; needs stable tags/visibility and reliable button taps.
- (Previously ignored in AiFeatureTestActivityTest/NavigationSmokeTest) Overlay and quick-skill/navigation cases — still @Ignore for readiness/timing; re-enable after adding explicit waits and ensuring inputs are enabled.

## Next Steps
- After UI refactors, re-enable the above tests one by one:
  1) Make loading/banner/list items deterministically visible in tests (or adjust assertions).
  2) Add explicit waits for overlay/navigation tests and drop the @Ignore markers.
- Keep this list short; remove entries once tests are fixed and green.
