# M2: UI Architecture Clean-Up Plan

**Objective**: Migrate `feature:chat` from a monolithic God File (`HomeScreen.kt`) to Clean UI Architecture V1.
**Strategy**: Strangler Fig Pattern (Build new -> Inject -> Delete old).

## Phase 1: Foundation (Zero Risk)
Create the system tokens that enable "Clean" development.

- [ ] **Specs**: Create `docs/specs/ui-architecture-v1.md`. (Done)
- [ ] **Theme**: Create `components/theme/Theme.kt` wrapping Material3.
- [ ] **Spacing**: Create `components/theme/AppSpacing.kt`.

## Phase 2: Atoms & Molecules (The New Assets)
Build the "Living Intelligence" components in isolation.

- [ ] **Atom**: `KnotSymbol.kt` (Canvas Implementation).
    - *Dependency*: `AppColors`, `AppSpacing`.
- [ ] **Atom**: `MonochromeIcon.kt` (Vector wrapper).
- [ ] **Molecule**: `ActionCard.kt` (Icon + Label + Click).
- [ ] **Molecule**: `HomeInputBar.kt` (Floating Pill).

## Phase 3: Organisms (The Logical Units)
Combine atoms/molecules into section-level components.

- [ ] **Organism**: `HomeHero.kt` (Knot + Greeting + ActionGrid).
    - *Replaces*: `EmptyStateContent` logic.
- [ ] **Organism**: `ActiveChatLayer.kt` (Wave + Input).

## Phase 4: Injection (The Strangler)
Wire it up in `HomeScreen.kt`.

- [ ] **Inject Theme**: Wrap `HomeScreen` content in `AppTheme`.
- [ ] **Inject Hero**: Replace `EmptyStateContent` with `HomeHero`.
- [ ] **Inject Input**: Replace `HomeInputArea` with `HomeInputBar`.
- [ ] **Cleanup**: Delete unused private functions in `HomeScreen.kt`.

## Verification
- **Visual**: Compare `design_system_prototype.html` vs Android Screenshot.
- **Structural**: Verify `HomeScreen.kt` lines reduced < 500.
