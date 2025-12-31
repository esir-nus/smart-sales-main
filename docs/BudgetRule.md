# Budget Rules (Codex) — Voting-Based Budgets, Directed Reads, High Quality

This file defines the **budgeted directed-read workflow** for Codex in the SmartSales Orchestrator-V1 project.

## Non-negotiable priorities
1) **Code quality is the highest priority.** Budgeting prevents over-reading/over-planning, not correctness.
2) **V1 docs/JSON are the source of truth.** If code diverges: mark **MISMATCH**, don’t silently adapt.
3) **Small, executable slices are preferred.** But “too small to be correct” is also unacceptable.

---

## Two-phase mechanism (binding)

### Phase 1 — Audit (read-only) + Budget Voting Proposal (required)
Codex must:
- Perform a targeted read-only audit with evidence.
- Produce **2–4 budget options** (not one) for the next step.
- Stop and wait for unlock: **“Self-eval complete, awaiting unlock.”**

**Phase 1 must include**
- Directed Read Log (what was searched/opened + key windows)
- Findings (OK / MISMATCH / UNKNOWN)
- Proposed next micro-task scope (what exactly will be changed)
- **Budget Options (Voting Proposal)** (template below)

### Phase 2 — Implementation (only after Human decision)
Codex may implement only after Orchestrator issues an unlock prompt that includes:
- **UNLOCK: YES**
- **BUDGET DECISION** (the option chosen by the Human)
- **BUDGET ALLOWANCE** (the exact numeric limits)
- Any additional constraints / “do not touch” boundaries

If Codex needs more budget during Phase 2, it must stop and request escalation (see below).

---

## Budget options (Voting Proposal) — required format

Codex must propose multiple options so the Human can choose.
Each option must include concrete numeric ceilings and tradeoffs.

### BUDGET OPTIONS (Codex)
Option A — (tight / minimal)
- rg_search_max: N
- file_open_max: N
- read_window_max: N
- lines_per_window_max: N
- planning_max_bullets: N
- estimated_files_to_edit: N (recommended)
- estimated_new_tests: N (recommended)
- pros:
- cons:
- risk_if_too_tight:

Option B — (balanced / recommended by Codex)
- (same fields)

Option C — (wider / safety buffer)
- (same fields)

(Option D allowed only if truly needed; keep it rare.)

**Definitions**
- `rg_search_max`: number of ripgrep searches allowed.
- `file_open_max`: number of distinct files allowed to open/read.
- `read_window_max`: number of separate code “windows/snippets” allowed across files.
- `lines_per_window_max`: max lines per window.
- `planning_max_bullets`: plan length cap (prevents over-planning).

---

## Orchestrator review + Human decision (voting)

After Codex submits options:
1) **Orchestrator** responds with:
   - quick critique of each option (too tight / too wide / just right)
   - suggested edits (e.g., “Option B but bump file_open_max by +1”)
2) **Human (you)** chooses the final budget (A/B/C or modified).
3) **Orchestrator** issues Phase 2 unlock prompt including the final chosen allowance.

---

## Phase 2 unlock prompt requirements (Orchestrator → Codex)

The unlock prompt must include:

UNLOCK: YES  
BUDGET DECISION: (Human choice, e.g., “Option B (modified)”)  
BUDGET ALLOWANCE:
- rg_search_max: ...
- file_open_max: ...
- read_window_max: ...
- lines_per_window_max: ...
- planning_max_bullets: ...
- (optional) max_files_to_edit: ...
- (optional) max_new_tests: ...

Then:
- Intended behavior (from V1 docs)
- Areas to inspect (search terms / packages)
- Ordered plan + tests
- Constraints (AGENTS.md + style-guide + Chinese comments near non-obvious logic)

---

## Budget escalation (Phase 2)
If Codex cannot proceed safely within allowance:
- Stop immediately (do not continue exploring).
- Submit:

### BUDGET ESCALATION REQUEST
- why_needed: (specific correctness risk)
- what_blocked: (symbol/file seam)
- additional_allowance_requested:
  - rg_search_max: +N
  - file_open_max: +N
  - read_window_max: +N
  - lines_per_window_max: +N
  - planning_max_bullets: +N
- files_to_open_next (max 3):
- expected_findings:

Orchestrator reviews; Human decides.

---

## Failure reporting (preferred over guessing)
If Codex hits overload/tool failure or missing plumbing, it must produce a report to help split tasks.

### FAILURE REPORT (required fields)
- why_failed:
- files_read: [ ... ]
- hotspots: [file:lineRange or symbols]
- what_you_tried: (1–3 bullets)
- suggested_split: (2–4 smaller tasks)
- next_step_budget_options: (new BUDGET OPTIONS block)

This report is considered a valid outcome when the task is too chunky.

---

## Large-file hygiene (non-prescriptive)
- Avoid reading huge files end-to-end by default.
- Prefer: search → open small windows → modify localized seams.
- If full-file comprehension is required for correctness, request more budget (escalation) rather than “wing it”.
