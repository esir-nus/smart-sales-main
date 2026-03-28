# Dynamic Island UI Spec

> **Context Boundary**: `docs/cerb-ui/dynamic-island/`
> **Status**: Active
> **Visual Role**: Sticky top-header ambient summary surface

## 1. Ownership

This shard owns the `Dynamic Island` shell surface used inside the standard top header on eligible top-level screens.

It owns:

- sticky top placement inside the header
- one-line summary presentation
- center-slot layout behavior
- truncation behavior
- single tap routing payload
- single-item visibility policy

It does not own:

- scheduler task truth
- scheduler drawer internals
- home-shell layout outside the center host slot
- connectivity, agent-activity, or alert logic
- global tokens in `docs/specs/style-guide.md`
- shared layer rules in `docs/specs/ui_element_registry.md`

## 2. Current v1 Contract

The current v1 Dynamic Island is scheduler-scoped only.

Rules:

- it appears on eligible top-level surfaces that use the standard persistent top header
- it mounts inside the header, not below it
- it replaces the previous center title slot only
- left and right header controls remain outside the island
- it is always sticky at the top
- it is always one line
- it shows only one winning item at a time
- it never expands vertically
- tap opens the scheduler drawer and may carry a scheduler date target
- there are no inline buttons, chips, or secondary actions inside the island

Current composition:

```text
┌──────────────────────────────────────────────┐
│ [☰] [Badge]     最近：回访客户 · 15:00      [+] │
└──────────────────────────────────────────────┘
```

## 3. Content Rules

The island renders one scheduler-backed summary line.

Current copy pattern:

- conflict item: `冲突：任务标题 · 时间`
- normal upcoming item: `最近：任务标题 · 时间`
- idle fallback: `暂无待办`

Current fallback rule:

- if there is no pending scheduler item, render `暂无待办`

Current overflow rule:

- content remains single-line
- long content truncates with ellipsis
- long content must not wrap into a second line

Current hue rule:

- conflict-visible item uses yellow warning hue
- most-immediate non-conflict item uses red urgent hue
- idle fallback stays neutral/cool

## 4. Integration Rules

- scheduler remains the owner of task meaning, prioritization, and task selection truth
- the island consumes one scheduler-backed item plus scheduler-target routing metadata
- the island may open the scheduler drawer, but it must not become a second scheduler surface
- shared v1 does not own multi-item rotation; shells such as SIM may layer that presentation locally while still reusing this one-line renderer
- future non-scheduler lanes must be introduced by updating this shard first

## 5. Deferred Scope

The following are explicitly deferred:

- connectivity lane
- agent activity lane
- system alert lane
- multi-lane arbitration beyond the current single scheduler-backed item
- rich inline actions inside the island
