# UI Architecture V1 Specification

**Status**: DRAFT
**Owner**: UI Designer / Senior Reviewer
**Scope**: All Compose UI features (`feature:chat`, `feature:media`, etc.)

## 1. Core Principles

### 1.1 The "No God File" Rule
*   **Rule**: No file shall exceed **400 lines**.
*   **Enforcement**: Any screen exceeding this must delegate to sub-components.
*   **Rationale**: Maintainability, readability, and avoiding git conflicts.

### 1.2 Component Hierarchy (Atomic Design)
We adhere to a strict 3-layer structural hierarchy:

| Level | Directory | Purpose | Dependencies |
| :--- | :--- | :--- | :--- |
| **Atoms** | `components/atoms` | Primitive building blocks (Buttons, Inputs, Icons). | `Theme`, `Modifiers` |
| **Molecules** | `components/molecules` | Functional units (MessageBubble, ActionCard). | `Atoms`, `UiModels` |
| **Organisms** | `components/organisms` | Complex UI sections (HomeHero, ChatList). | `Molecules`, `UiState` |
| **Screens** | Root (`Home.kt`) | Layout scaffolding only. | `Organisms`, `ViewModel` |

### 1.3 The Strangler Fig Pattern for Refactoring
When migrating legacy monoliths (e.g., `HomeScreen.kt`):
1.  **Freeze**: Do not edit logic inside the monolith.
2.  **Build**: Create the new component in `components/...`.
3.  **Inject**: Replace the inline code in the monolith with the new component call.
4.  **Repeat**: Until the monolith is just a scaffold.

## 2. Theme System

### 2.1 Single Source of Truth
*   All colors MUST come from `AppTheme.colorScheme` or `AppColors` (if custom token).
*   All type MUST come from `AppTheme.typography`.
*   Hardcoded hex codes (e.g., `Color(0xFF000000)`) are **FORBIDDEN** in components.

### 2.2 Tokens
*   **Spacing**: Use `AppSpacing` tokens (`4.dp`, `8.dp`, `16.dp`, etc.). Magic numbers are banned.
*   **Shapes**: Use `AppShapes` tokens.

## 3. Directory Structure Standard

```
feature/name/src/main/java/com/smartsales/feature/name/
├── FeatureScreen.kt            # The Scaffold (Screen)
├── FeatureViewModel.kt         # The State Holder
├── FeatureUiState.kt           # The State Contract (Immutable)
│
├── components/                 # UI Components
│   ├── atoms/                  # Buttons, Inputs, Chips
│   ├── molecules/              # ListItems, Cards, Bubbles
│   └── organisms/              # Complex Sections (Hero, Lists)
│
├── theme/                      # Feature-specific theme extensions
│   ├── Theme.kt
│   └── Color.kt
│
└── navigation/                 # Navigation Logic
    └── FeatureNavigation.kt
```
