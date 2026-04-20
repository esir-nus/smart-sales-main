# Pull Request

## Summary

<!-- 1-3 bullets describing what this PR does and why. -->

-
-

## User-Visible Surface

<!-- What surface(s) does this change reach? Be explicit about runtime path. -->

- [ ] SIM runtime
- [ ] Full runtime
- [ ] `full` flavor only
- [ ] `harmony` flavor only
- [ ] Backend / pipeline only (no UI)
- [ ] Docs / governance only

**Surface description:**

<!-- e.g., "Long-press copy on chat bubbles in agent intelligence (SIM + full)" -->

## Active Runtime Path Check

- [ ] I verified this feature is wired into every runtime path named above.
- [ ] If a component was added but not yet wired (latent infra), I labeled the PR `chore(infra):` instead of `feat(...)`.

## Test Plan

- [ ] CI passes (android-tests, platform-governance-check)
- [ ] Built and installed on device; verified user-visible behavior
- [ ] Device build version stamp (`Settings → About` or `dumpsys package <id> | grep versionName`) matches this branch + commit
- [ ] Feature-specific checks:
  -

## Branch Hygiene

- [ ] Branch is < 5 days old since first commit, or declared as integration trunk in `docs/plans/tracker.md` (per `docs/specs/platform-governance.md` freshness rule)
- [ ] If multi-platform: confirmed this PR does not require pausing in-flight work on the other platform

## Doc Alignment

- [ ] Owning spec / SOP updated alongside code (cerb-compliant)
- [ ] Tracker entry moved to ✅
- [ ] Changelog entry names user-visible surface, not just component
