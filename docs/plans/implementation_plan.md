# Implementation Plan - Prism Phase 2: Home Screen (UX Layer)

**Goal**: Implement the Prism Home Screen in Android Compose, aligning with the `SmartSales V12 Prototype` visual design.
**Target Status**: `Phase 2: UX Layer (Page-by-Page)` - P1 Home Screen
**Mode**: Feature Building Mode

---

## 🏗️ Architecture Alignment

This implementation follows the **Lattice Architecture** (Prism V1 §7):
*   **UI Layer Only**: This is a "UX Layer" task phase. We are building `Composables` and `ViewModels` driven by `Fakes`.
*   **Zero Logic**: Visuals and state management only. No real data connection yet (Mock/Fake only).

## 🎨 Visual Spec (From V12 Prototype)

*   **Aurora Background**: Radial gradient mesh (Blue/Indigo/Cyan).
*   **Hero Section**:
    *   Greeting: "Hello, User" (Gradient Text).
    *   Subtitle: Context aware.
    *   Action Pills: Glassmorphic chips ("Quick Start", "History").
*   **Floating Input**: Capsule shape with "Scan Shine" animation.
*   **Zero-Chrome**: Hiding status bars/navigation bars (Edge-to-Edge).

## 📝 Proposed Changes

### 1. Design Tokens Update (`docs/design/design-tokens.json` & `AppColors.kt`)
*   Add `aurora` gradient definitions.
*   Add `glass-border` and `glass-shadow` tokens.
*   Update `input-capsule` dimensions and shadows.

### 2. New Components (`feature/chat/src/main/java/com/smartsales/feature/chat/presentation/components/`)
*   `[NEW] AuroraBackground.kt`: Canvas-based radial gradient renderer.
*   `[NEW] FloatingInputBar.kt`: Capsule input with "Scan Shine" modifier.
*   `[NEW] ActionPill.kt`: Glassmorphic chip.
*   `[NEW] KnotFab.kt`: Breathing animation state.

### 3. Screen Implementation (`feature/chat/src/main/java/com/smartsales/feature/chat/presentation/home/`)
*   `[MODIFY] HomeScreen.kt`: Implement the Scaffold with MobileFrame layout.
*   `[NEW] HomeViewModel.kt`: Manage `SessionListState` (Fake data).

## 🔍 Verification Plan

### Automated Tests
*   `./gradlew :feature:chat:compileDebugKotlin` (Build check)
*   **Screenshot Tests**: (If infrastructure exists, otherwise manual)

### Manual Verification
1.  **Launch App**: Verify strict "Edge-to-Edge" layout (no white bars).
2.  **Visual Check**:
    *   Aurora blobs are breathing/visible behind content.
    *   Input Bar floats at bottom (above nav bar area).
    *   Hero Text has gradient fill.
3.  **Interaction**:
    *   Tap Input Bar -> Toast/State change.
    *   Scroll -> Content moves behind Glass headers (if any).

---

## ❓ Alignment Questions to User
1.  **Mock Data**: Shall we hardcode the "Session List" in `FakeSessionsRepository` now or just use static list in UI? (Proposal: Static list in UI for P2, move to Repo in P3).
2.  **Navigation**: Do you want the "Audio Drawer" accessible yet? (Proposal: Yes, add gesture listener but wire later).
