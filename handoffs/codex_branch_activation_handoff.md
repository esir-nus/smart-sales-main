# Codex Handoff: Branch Model Activation and CI Repair

**Date**: 2026-04-15
**From**: Claude (governance simplification, local execution)
**To**: Codex (git operator — push, CI, PRs, branch protection)
**Status**: All local work complete. Network was down. Codex picks up when online.

---

## What Was Done (locally, not yet pushed)

- `develop` branch created from master with all 6 DTQ lanes merged + NO-MATCH dirty files committed
- `platform/harmony` branch created with DTQ-08 merged + develop synced
- DTQ lane system fully decommissioned: `ops/`, `scripts/lane_guard.py`, `.githooks/`, lane worktrees, lane branches all removed
- CODEOWNERS simplified, CLAUDE.md rewritten for new branch model
- tracker.md updated with governance simplification shipped slice

## What Codex Must Do

### Step 1: Update CI workflows before pushing

The `platform-governance-check.yml` will FAIL because it checks for deleted files. Fix it BEFORE pushing.

**File**: `.github/workflows/platform-governance-check.yml`

Remove the entire `lane-harness` job (lines 77-107) — lane_guard.py no longer exists.

In the `governance-docs` job, remove these `test -f` lines (files deleted):
```
test -f docs/sops/lane-worktree-governance.md
test -f handoffs/README.md
test -f ops/lane-registry.json
test -f scripts/lane_guard.py
test -f scripts/lane
test -f scripts/install-hooks.sh
test -f .githooks/pre-commit
test -f .githooks/pre-push
```

Remove the `ops/**`, `handoffs/**`, `.githooks/**`, `scripts/**` entries from the `on.push.paths` and `on.pull_request.paths` triggers (if desired — not strictly necessary but keeps triggers clean).

**File**: `.github/workflows/android-tests.yml`

Update the Gradle cache line to also write on `develop`:
```yaml
cache-read-only: ${{ github.ref != 'refs/heads/master' && github.ref != 'refs/heads/develop' }}
```

Commit:
```
fix(ci): remove lane-harness checks for decommissioned DTQ system
```

### Step 2: Push branches

```bash
git push -u origin develop
git push -u origin platform/harmony
```

### Step 3: Set branch protection on master

```bash
gh api repos/esir-nus/smart-sales-main/branches/master/protection \
  -X PUT \
  -H "Accept: application/vnd.github+json" \
  --input - <<'EOF'
{
  "required_pull_request_reviews": {
    "required_approving_review_count": 0
  },
  "enforce_admins": false,
  "restrictions": null,
  "required_status_checks": null
}
EOF
```

`enforce_admins: false` lets the owner bypass in emergencies.

### Step 4: Delete remote lane branches

```bash
for branch in $(git branch -r | grep "origin/lane/DTQ-" | sed 's|origin/||'); do
  git push origin --delete "$branch"
done
```

Also clean up other stale remote branches if desired:
```bash
# These are pre-DTQ era branches that are no longer active
git push origin --delete dtq-04/island-reconnecting-breathe
git push origin --delete test/evidence-sweep
git push origin --delete wip/20260402-worktree-quarantine
```

### Step 5: Create initial promotion PR

```bash
gh pr create --base master --head develop \
  --title "promote: initial develop integration post-DTQ" \
  --body "$(cat <<'BODY'
## Summary

First promotion from develop after governance simplification.

- All 6 active DTQ lanes merged into develop
- Multi-device registry package (DeviceRegistry, ConnectivityModal rewrite, pairing integration)
- Agent Intelligence Wave 6 (suggestion cards, streaming bubble, thinking banner)
- Connectivity Bridge download progress callback
- DTQ system decommissioned, replaced by develop + platform branch model
- CI updated: lane-harness checks removed

## Test plan

- [ ] CI passes (android-tests, platform-governance-check, prism-clean-check)
- [ ] `./gradlew :app-core:compileDebugKotlin` succeeds
- [ ] No lane-guard references remain in active code
BODY
)"
```

---

## New Branch Model Reference

```
master (protected, PR-only)
  └── develop (Android + shared, daily work)
        └── platform/harmony (HarmonyOS, syncs from develop)
```

- Feature work: branch from develop, PR back to develop
- Promotion: PR from develop to master when stable
- HarmonyOS: branch from platform/harmony, PR back; sync from develop via merge
- Shared contracts flow: develop → platform/*, never reverse
