# Home Shell UI Spec

> **Context Boundary**: `docs/cerb-ui/home-shell/`
> **Status**: Active
> **Visual Source of Truth**: Local home-shell contract beneath `docs/core-flow/base-runtime-ux-surface-governance-flow.md`

## 1. Ownership

This shard owns the local home-shell presentation contract for `HomeShell` and `ChatWelcome` beneath `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, especially `UX.HOME.*`.

It is the visual source of truth for:

- persistent home-shell chrome
- empty-state canvas composition
- current empty-state inclusion/exclusion rules
- the Dynamic Island host slot inside the home header
- the shared top/bottom monolith identity that remains stable as the center canvas moves from empty home into active discussion
- the current shell-chrome inclusion/exclusion rule for normal non-Mono work
- the fact that current home-shell truth is mounted only through `MainActivity -> RuntimeShell`, not through retired split-era shell hosts

It does not own:

- chat-state rendering after the session leaves the empty state
- the shared home/here active-discussion family after the session leaves the empty state; current ownership routes through `docs/core-flow/base-runtime-ux-surface-governance-flow.md`, then the relevant lower shell/dynamic-island/interface docs
- drawer inner contents
- backend, routing, session model, or plugin behavior
- Dynamic Island internal behavior, which is owned by `docs/cerb-ui/dynamic-island/spec.md`
- token definitions that remain global in `docs/specs/style-guide.md`
- shared trigger/layer invariants that remain global in `docs/specs/ui_element_registry.md`
- retired split-era shell hosts such as `AgentShell.kt` and `SimShell.kt`, which are no longer visual truth owners

## 2. Current Empty-State Contract

### HomeShell (persistent chrome)

The current empty-state shell includes only:

- left: hamburger trigger
- left: Smart Badge / device status badge
- center: Dynamic Island host slot with scheduler as the default lane and RuntimeShell-local connectivity takeover when that lane is active
- connected-state shell chrome may add decorative ambient flank icons around the centered island without changing the side utility layout
- right: new-session `+`
- floor: aurora background
- bottom: floating input capsule

### ChatWelcome (empty-state canvas)

The empty-state canvas includes only:

- greeting: `你好, SmartSales 用户`
- subtitle: `我是您的销售助手`

Rule:

- `ChatWelcome` mounts only when the session is empty.
- Leaving the empty state removes the greeting canvas and transitions to the owning chat surface.
- On compact Android heights, the shell must preserve hierarchy by compressing decorative spacing before changing chrome.
- When the center gap becomes tight, the greeting stage shifts to an upper-center composition instead of colliding with the bottom composer.
- Runtime adaptation must be driven by actual Compose constraints and insets, not by whitelisting specific resolutions.

### Shared Shell Continuity After Empty State

When the session leaves the empty state, this shard still owns the shared shell chrome rules:

- top monolith remains the shell header family
- bottom monolith remains the composer foundation family
- the center canvas is the part that swaps between greeting, discussion, and system-sheet content
- the Dynamic Island host slot stays centered in the header family
- RuntimeShell may temporarily swap the centered island content from scheduler to connectivity without moving the side utilities or redefining the header family
- scheduler-open chrome may temporarily suppress side utility actions while keeping the island mounted
- visible-lane tap follows the rendered island item, while downward drag remains scheduler-only

This shard does **not** replace `docs/core-flow/base-runtime-ux-surface-governance-flow.md` for shared surface behavior or `docs/core-flow/sim-shell-routing-flow.md` for routing behavior; it refines the local shell chrome and continuity contract beneath those layers.

## 3. Current Exclusions

The following ideas are not part of the current home empty-state contract:

- hero skill pills such as `Smart Analysis`, `PDF`, or `CSV`
- external Knot FAB
- debug toggle in the home header
- empty-state right-toolbar utility surfaces such as `Insights` or `Files`
- coach/analyst mode toggle on the empty home shell

If any of these return in the future, they must be reintroduced by an owning feature spec rather than inferred from historical docs.

## 4. Integration Rules

- Use `docs/core-flow/base-runtime-ux-surface-governance-flow.md` first for `UX.HOME.*` and shared shell continuity rules.
- Use `docs/specs/style-guide.md` for tokens, material treatment, and motion primitives.
- Use `docs/specs/ui_element_registry.md` for shared interaction and Z-layer invariants.
- Use `docs/cerb-ui/dynamic-island/spec.md` for the center header surface behavior.
- Use `docs/core-flow/sim-shell-routing-flow.md` for shell routing behavior and support-surface transitions.
- Use `docs/specs/base-runtime-unification.md` and `docs/cerb/interface-map.md` for the wrapper-debt rule on legacy full-side hosts.
- shared home-shell surfaces may consume shell-owned adjunct state only through explicit RuntimeShell wiring, never through shared-UI downcasts to a concrete ViewModel.
- Do not treat those global docs as the owner of the home empty-state composition.

## 5. Doc Alignment Rules

When home empty-state visuals change:

1. update `docs/core-flow/base-runtime-ux-surface-governance-flow.md` first for the affected `UX.HOME.*` IDs
2. update this shard for local home-shell refinement
3. update the global UI index if the owning path changes
4. update compatibility docs that still point to older home-screen descriptions
