# Home Shell UI Spec

> **Context Boundary**: `docs/cerb-ui/home-shell/`
> **Status**: Active
> **Visual Source of Truth**: Current home empty-state shell

## 1. Ownership

This shard owns the current home empty-state presentation contract for `HomeShell` and `ChatWelcome`.

It is the visual source of truth for:

- persistent home-shell chrome
- empty-state canvas composition
- current empty-state inclusion/exclusion rules
- the Dynamic Island host slot inside the home header

It does not own:

- chat-state rendering after the session leaves the empty state
- the SIM home/here active-discussion family, which is owned by `docs/cerb/sim-shell/spec.md`
- drawer inner contents
- backend, routing, session model, or plugin behavior
- Dynamic Island internal behavior, which is owned by `docs/cerb-ui/dynamic-island/spec.md`
- token definitions that remain global in `docs/specs/style-guide.md`
- shared trigger/layer invariants that remain global in `docs/specs/ui_element_registry.md`

## 2. Current Empty-State Contract

### HomeShell (persistent chrome)

The current empty-state shell includes only:

- left: hamburger trigger
- left: Smart Badge / device status badge
- center: Dynamic Island host slot
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

## 3. Current Exclusions

The following ideas are not part of the current home empty-state contract:

- hero skill pills such as `Smart Analysis`, `PDF`, or `CSV`
- external Knot FAB
- debug toggle in the home header
- empty-state right-toolbar utility surfaces such as `Insights` or `Files`
- coach/analyst mode toggle on the empty home shell

If any of these return in the future, they must be reintroduced by an owning feature spec rather than inferred from historical docs.

## 4. Integration Rules

- Use `docs/specs/style-guide.md` for tokens, material treatment, and motion primitives.
- Use `docs/specs/ui_element_registry.md` for shared interaction and Z-layer invariants.
- Use `docs/cerb-ui/dynamic-island/spec.md` for the center header surface behavior.
- Do not treat those global docs as the owner of the home empty-state composition.

## 5. Doc Alignment Rules

When home empty-state visuals change:

1. update this shard first
2. update the global UI index if the owning path changes
3. update compatibility docs that still point to older home-screen descriptions
