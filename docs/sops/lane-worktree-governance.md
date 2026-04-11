# Lane Worktree Governance SOP

> **Purpose**: Define the repo's lane-first execution harness so dirty-tree mixing is blocked before the tree becomes unsafe.
> **Status**: Active governance SOP
> **Date**: 2026-04-11
> **Primary Laws**:
> - `docs/specs/platform-governance.md`
> - `docs/plans/dirty-tree-quarantine.md`
> **Machine Control Plane**:
> - `ops/lane-registry.json`
> - `scripts/lane_guard.py`
> - `scripts/lane`
> - `.githooks/pre-commit`
> - `.githooks/pre-push`
> **Related Docs**:
> - `docs/sops/tracker-governance.md`
> - `handoffs/README.md`
> - `.github/workflows/platform-governance-check.yml`

---

## 1. Harness Intent

This repo now treats dirty-tree prevention as a harness engineering problem, not a reminder problem.

The harness exists to make safe work the default and ambiguous work impossible to ignore.

Core design goals:

- constrain work to one lane worktree at a time
- fail closed when lane identity or owned scope is unclear
- keep the main repo path integration-only for governance and merge work
- make paused lanes resumable through a real handoff contract
- run the same rules locally and in CI

---

## 2. Hard Invariants

These invariants are non-optional:

1. one feature worktree attaches to one lane lease
2. one active/paused/review lane owns one bounded write scope
3. one changed path may not belong to multiple reserved lanes
4. the integration tree must not carry feature edits
5. a paused lane must have a current handoff file
6. ambiguous ownership is a failure, not a warning
7. local bypass is not accepted; CI re-validates the same rules

Reserved lane statuses are:

- `Active`
- `Paused`
- `Review`

Only reserved lanes block path reuse.

---

## 3. Control Plane vs Data Plane

### 3.1 Control plane

The lane harness control plane is:

- `ops/lane-registry.json`: machine-readable lane ownership and status
- `docs/plans/dirty-tree-quarantine.md`: human-readable lane board and intent ledger
- `handoffs/*.md`: resumable paused-lane state
- `AGENTS.md`, `.codex/**`, `.agent/**`, `.claude/**`, and `CLAUDE.md`: cross-runtime operator registration surfaces for Codex, Antigravity, and Claude
- `docs/specs/platform-governance.md` and this SOP: policy law

### 3.2 Data plane

The lane harness data plane is:

- actual git branches
- actual git worktrees
- current dirty/staged file paths
- current attached lease in `.git/smart-sales/current-lane.json`

Rule:

- the validator must prove that data-plane reality matches control-plane declarations

---

## 4. Main Tree Rule

The repo root worktree is now the integration tree.

Allowed work in the integration tree:

- registry updates
- governance/spec/SOP updates for the harness itself
- tracker/index updates needed to integrate lane state
- hook and CI updates

Disallowed work in the integration tree:

- normal feature edits in Android code
- Harmony feature edits
- mixed docs plus code continuation of a feature lane

If the integration tree becomes dirty with feature files, the lane guard must fail.

---

## 5. Lane Registry Contract

`ops/lane-registry.json` is the machine ledger.

Each lane entry must include:

- `lane_id`
- `title`
- `status`
- `work_class`
- `branch`
- `recommended_worktree`
- `owned_paths`
- `allowed_shared_paths`
- `handoff_path`
- `tracker_refs`
- `alignment`

Rules:

- `owned_paths` must be bounded and reviewable
- `allowed_shared_paths` should stay small and explicit
- if a lane is `Paused`, `handoff_path` must exist
- if `branch` is declared, the attached worktree must actually be on that branch

---

## 6. Local Mechanism

### 6.1 Shared commands

Use the repo wrapper instead of ad-hoc branch/worktree setup when possible:

- `scripts/lane create`
- `scripts/lane attach`
- `scripts/lane resume`
- `scripts/lane pause`
- `scripts/lane status`
- `scripts/lane commit`
- `scripts/lane push`
- `scripts/lane ship`
- `scripts/lane validate`
- `scripts/lane collisions`
- `scripts/lane integrate`
- `scripts/lane install-hooks`

### 6.2 Lease model

Each non-integration worktree carries one local lease:

- path: `.git/smart-sales/current-lane.json`

The lease is local, not repo-tracked.

It exists only to answer one question quickly and deterministically:

- which lane is this worktree allowed to edit right now?

### 6.3 Hook model

Local hooks live in `.githooks/` and are activated by:

- `scripts/install-hooks.sh`

Current behavior:

- `pre-commit`: validates staged paths against the lane harness and preserves the existing dashboard regeneration behavior for `docs/cerb/interface-map.md`
- `pre-push`: validates current dirty paths so unsafe local bypasses are caught before push

### 6.4 Commit and push autopilot

The lane harness now provides a lane-aware ship surface:

- `scripts/lane status`: prove the current worktree, branch, lane, staged set, and generated commit message
- `scripts/lane commit`: commit the currently staged lane-owned files with an auto-generated lane-aware message
- `scripts/lane push`: push the attached lane branch, publishing the upstream if needed
- `scripts/lane ship`: run the full lane-local commit + push sequence

Rules:

- these commands must run from the lane worktree, not the integration tree
- they must fail closed if the worktree lease, branch, lane status, or staged paths do not satisfy the harness
- an intentional operator invocation is sufficient authorization; the command should not stop for a second human confirmation step
- the generated commit message should include the lane id and lane-scoped summary by default, with optional explicit override via `--message`

---

## 7. Operator Workflow

### 7.1 Start lane

1. define the lane in `ops/lane-registry.json` or use `scripts/lane create`
2. create or attach the dedicated `git worktree`
3. attach the lane lease inside that worktree
4. run `scripts/lane validate`
5. only then start editing

### 7.2 Resume lane

1. open the lane worktree
2. ensure the handoff file still reflects reality
3. run `scripts/lane resume <lane-id>` if the lane was paused
4. run `scripts/lane validate`
5. continue only inside the lane's owned scope

### 7.3 Pause lane

1. update the handoff file
2. run `scripts/lane pause <lane-id> <handoff-path>`
3. leave the lane resumable, with current drift/evidence state recorded

### 7.4 Integrate lane

1. keep feature edits in the lane worktree, not the integration tree
2. run `scripts/lane validate`
3. run lane-specific verification
4. run `scripts/lane integrate <lane-id>` to move the lane to `Review`
5. perform the final governance/tracker updates in the integration tree if needed

### 7.5 Commit and push lane

1. stage only the files that belong to the attached lane
2. run `scripts/lane status` to confirm branch, lane id, staged set, and generated message
3. run `scripts/lane commit` or `scripts/lane ship`
4. if only the branch publication step remains, run `scripts/lane push`
5. if the command fails, fix the harness violation instead of bypassing it with raw git commands

---

## 8. Failure Semantics

The harness must fail with concrete evidence.

Minimum failure output should identify:

- current branch
- current worktree role
- current lane lease, if any
- offending path
- conflicting owner lane or missing declaration
- required next repair action

Examples of required failures:

- no lane declared in a feature worktree
- owned paths empty or malformed
- path belongs to another reserved lane
- integration tree is used for feature edits
- paused lane has no handoff
- lane branch does not match attached branch

---

## 9. CI Backstop

CI must run the same validator logic as local workflows.

Current backstop:

- `.github/workflows/platform-governance-check.yml`
- `python3 scripts/lane_guard.py validate-registry`
- `python3 scripts/lane_guard.py validate-ci ...`

Rule:

- a local bypass is still a failure if CI can prove that branch/path ownership is invalid

---

## 10. Maintenance Rules

Treat the harness as production control-plane code.

Rules:

- do not change enforceable policy without updating both docs and validator logic
- keep the registry schema small; add fields only for real operator decisions
- keep hook scripts thin and push rule logic into `scripts/lane_guard.py`
- every new harness rule should gain at least one validator test
- friction complaints should be investigated with evidence, not by silently weakening rules

---

## 11. Rollout Posture

### Phase 1

Current repo posture is Phase 1:

- hard block integration-tree feature edits
- hard block lane collisions
- hard block missing lane lease in feature worktrees
- CI rejects unregistered branch/path ownership

### Phase 2

Future hardening may add:

- stricter required branch naming such as `lane/<lane-id>/<slug>`
- shell/editor startup guard integration
- stricter lane-cleanliness requirements before `Review`

Phase 2 should tighten the same invariants, not replace them.
