# /sprint -- Sprint Contract Authoring

Author sprint contracts for non-trivial work. Claude authors; Codex is the default operator unless the user explicitly overrides at handoff.

## Your job

1. Capture the task declaration from `$ARGUMENTS` or ask once.
2. Resolve the owning project slug.
3. Read `docs/specs/sprint-contract.md` and `docs/specs/project-structure.md`.
4. Author one contract at `docs/projects/<slug>/sprints/NN-<slug>.md`.
5. Present the contract for approval before execution starts.

## Step 1: Collect the task declaration

If `$ARGUMENTS` is empty, ask:
- What are you trying to change?
- Which project does this belong to?
- What should the operator leave untouched?

## Step 2: Resolve the project folder

Contracts live under `docs/projects/<slug>/sprints/NN-<slug>.md`.

- If the project folder already exists, use it.
- If no matching project folder exists, stop and ask the user for the slug before authoring.
- Do not invent a new project folder silently.

## Step 3: Read the governing docs

Read, in order:
1. `docs/specs/sprint-contract.md`
2. `docs/specs/project-structure.md`
3. `docs/plans/tracker.md`
4. The owning project tracker under `docs/projects/<slug>/tracker.md` when it exists
5. The owning spec or SOP for the requested work

If a relevant `docs/core-flow/**` file exists, read it before treating lower docs as final behavior.

## Step 4: Author the contract

Follow `docs/specs/sprint-contract.md` exactly. The contract must contain these ten sections in order:

1. Header
2. Demand
3. Scope
4. References
5. Success Exit Criteria
6. Stop Exit Criteria
7. Iteration Bound
8. Required Evidence Format
9. Iteration Ledger
10. Closeout

Authoring rules:
- Keep the contract self-contained enough that the operator can run from the file plus its references.
- Name the output file `docs/projects/<slug>/sprints/NN-<slug>.md`.
- Keep Scope explicit. The operator may edit only what Scope allows.
- Put verification in literally checkable form: command, grep, test, file, count, or visible artifact.
- Keep Iteration Ledger and Closeout present but unfilled beyond authoring-time placeholders.
- Keep the contract small. If it grows past the size discipline in `docs/specs/sprint-contract.md`, split the work.

## Step 5: Handoff framing

Present the authored contract and state:
- Claude is the author.
- Codex is the default operator unless the user explicitly chooses otherwise.
- The operator runs one contract at a time.
- Commit discipline comes from `docs/specs/sprint-contract.md`: no mid-sprint commits, one close commit on success.

## Step 6: Stop conditions

Stop and surface instead of authoring when:
- the project slug is unknown
- the spec is missing and the task would require inventing behavior
- the requested work obviously exceeds one sprint contract
- the user is asking for execution rather than authoring and no contract exists yet
