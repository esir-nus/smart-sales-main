# Sprint Contract

The canonical unit of work in this repo. One markdown file per sprint, self-contained enough that the operating agent reads only the contract and its referenced files — no prior conversation context.

## Authorship and Handoff

- **Author:** always Claude. Authoring is a one-time upfront cost. Produces the contract file placed in the owning project's `sprints/` folder.
- **Operator:** Codex by default. User may override explicitly per sprint ("Claude operates this one"). Whoever picks up the contract runs it to completion — no mid-sprint agent handoff.
- **User:** final gate at close. Approves or rejects status, lesson proposals, and CHANGELOG line before they land.

The contract must be readable without access to the authoring conversation. References are explicit file paths, not "see what we discussed."

## Required Sections

Every contract contains these ten sections in order. Sections 9 and 10 are filled by the operator during/at the end of the run; all others are filled at authoring and must not be edited during operation.

1. **Header** — slug, project, date, author, operator, lane (`develop` | `platform/harmony`), branch, worktree (only if the parallel-scope escape valve applies).
2. **Demand** — user ask verbatim, then Claude's interpretation in 2–3 lines.
3. **Scope** — file/module globs the operator may touch. Holistic (code + docs + UI + whatever coheres). Anything outside is out-of-scope; the operator must not edit out-of-scope files.
4. **References** — explicit paths to the authoritative plan, owning specs, interface-map entries, relevant `lessons-learned.md` triggers. Operator reads these, not the whole tree.
5. **Success Exit Criteria** — literally checkable conditions. Each criterion must be verifiable by a command, a file path, a grep, a line count, a test name, or a visible artifact. Prose like "looks good" or "works end-to-end" is not acceptable.
6. **Stop Exit Criteria** — named abort conditions: unresolvable blocker (describe the shape), out-of-scope regression detected, iteration bound hit. On stop the operator halts and surfaces rather than grinding past.
7. **Iteration Bound** — max iterations or wall-clock (e.g., "3 iterations or 30 min, whichever hits first"). Runaway prevention. Hitting the bound without green is a stop, not a force-close.
8. **Required Evidence Format** — the external artifacts the operator must append into the closeout. Minimum: ≥1 external artifact (command output, log excerpt, screenshot path, test run). Agent narration alone is not acceptable evidence.
9. **Iteration Ledger** — appended by the operator once per iteration: what was tried, what the evaluator saw, next action. Not committed mid-sprint.
10. **Closeout** — filled at exit: status (`success` | `stopped` | `blocked`), one-line summary for the project tracker, the required evidence artifacts, optional lesson proposals, optional CHANGELOG line. Lesson proposals and CHANGELOG line are human-gated.

## Commit Discipline

- **No mid-sprint commits.** The working tree stays dirty across iterations within a sprint. Evidence and ledger entries accumulate in the contract file uncommitted.
- **On success:** operator commits everything — code, docs, contract file with closeout filled, evidence artifacts — as a single commit (or squashable series) on the contract's declared branch. Commit message references the contract file path.
- **On stop or blocker:** do NOT commit. Leave the working tree dirty. Fill Closeout with the stop/block reason. Wait for the user to arbitrate (resume, reassign, or abandon via `git restore`/`git checkout`).

## Evidence Rule

Every closeout must contain at least one external artifact. Acceptable forms:

- Command output (e.g., `wc -l <file>`, `git diff --stat`, test runner output).
- Log excerpt (e.g., `adb logcat -d | grep <tag>`, `hdc hilog | grep <tag>`).
- Screenshot path (for UI work; committed under the project's `evidence/<sprint>/` folder).
- File-existence proof (e.g., `ls -la <expected-path>`).

Agent prose ("I verified the tests pass") is not a substitute. The operator's job is to emit evidence; the user's job at close gate is to read it.

## Self-Application

This spec is template-is-the-spec: the section list above IS the contract template. A contract that uses these ten sections in this order conforms to the schema. No separate template file is needed.

The first real instance of this schema lives at `docs/projects/workflow-renovation/sprints/01-specs-bootstrap.md` — read it as the worked exemplar.

## Size Discipline

A well-formed contract is small (typically 100–200 lines). If a contract exceeds 200 lines, the scope is probably too broad — split into two sprints. The contract is a handoff document, not a planning document.

## See Also

- `docs/specs/project-structure.md` — where contracts live and how projects accumulate them.
- `.agent/rules/lessons-learned.md` — index+details pattern this spec propagates.
