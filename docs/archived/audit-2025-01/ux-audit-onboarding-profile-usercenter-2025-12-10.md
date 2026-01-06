# UX Audit Report: Onboarding, Persistent Profile, and User Center

**Branch:** `UX-polishment`  
**Commit:** `a6300261`  
**Date:** 2025-12-10  
**Scope:** Onboarding (welcome + personal info), Persistent Profile, and User Center

---

## UX Rules (from docs)

From `docs/ux-contract.md` and `docs/Orchestrator-MetadataHub-Mvp-V3.md`:

1. **Onboarding gating:** On first install / first open, onboarding flow appears before Home. Flow includes: Welcome screen → Personal Info screen (display name, role/position, industry) → Device + Wi-Fi Setup (optional, via BLE) → Return to Home Chat.

2. **Onboarding completion:** Onboarding is considered "completed" when personal info is saved and persisted. Once completed, app should not show onboarding again on subsequent launches.

3. **Personal info fields:** Required fields: Display name (required), Role/position (optional), Industry (optional). These fields are collected for: personalized greetings ("你好，{userName}"), export filename `<Username>` component, MetaHub context (better summaries and CRM inferences).

4. **Profile storage:** User profile is stored persistently and shared across Home and User Center. Profile includes: displayName, role, industry, email, organization, phone.

5. **Profile as metadata:** User profile feeds:
   - Home greeting: uses `displayName` from profile (fallback to "用户" or "SmartSales 用户" if empty).
   - Export filename: `<Username>` component in pattern `<Username>_<major person>_<summary limited to 6 Chinese characters>_<timestamp>.<ext>` comes from profile `displayName`.
   - MetaHub context: profile data (role, industry) can inform CRM inferences and summaries.

6. **User Center entry:** User Center entry point is in history drawer footer (tag `HISTORY_USER_CENTER`). No profile/avatar icon in top bar; history drawer footer is the canonical way into User Center from Home.

7. **User Center editing:** User Center allows viewing and editing: display name, role, industry. Changes are saved to the same `UserProfileRepository` used by onboarding and Home.

8. **User Center links:** User Center may contain links to Device Manager and Wi-Fi setup (if documented). Privacy/About links are allowed as placeholders.

9. **Test bypass:** Tests can bypass onboarding via `PersistentOnboardingStateRepository.testOverrideCompleted` to avoid onboarding flow in automated tests.

---

## Findings by Rule

### 1. Onboarding gating

**OK – Onboarding gate logic** – `AiFeatureTestActivity.kt:258-282` – `OnboardingGateViewModel` checks `completed` state from `OnboardingStateRepository`. If `!onboardingCompleted`, shows `OnboardingHost` instead of Home. Gate logic correctly prevents Home from showing until onboarding is complete.

**OK – Onboarding flow sequence** – `OnboardingScreens.kt:38-153` – Flow includes `OnboardingWelcomeScreen` (welcome message + "开始使用" button) and `OnboardingPersonalInfoScreen` (display name, role, industry fields). Sequence matches contract: Welcome → Personal Info.

**MISMATCH – Device + Wi-Fi Setup missing** – `OnboardingScreens.kt:38-153` – Contract specifies onboarding should include "Device + Wi-Fi Setup (via BLE)" step after Personal Info, but current implementation only has Welcome and Personal Info screens. Device/Wi-Fi setup is not part of the onboarding flow.

**OK – Return to Home after completion** – `AiFeatureTestActivity.kt:277-279` – After `onSubmit` completes, `currentPage` is set to `TestHomePage.Home`, correctly returning user to Home.

### 2. Onboarding completion

**OK – Completion persistence** – `OnboardingViewModel.kt:76-101` – `onSubmit` saves profile to `UserProfileRepository` and calls `onboardingStateRepository.markCompleted(true)`, correctly persisting completion state.

**OK – State repository** – `OnboardingStateRepository.kt:18-59` – `PersistentOnboardingStateRepository` uses SharedPreferences to persist `completed` flag. `completedFlow` exposes state as Flow for reactive UI updates.

**OK – No re-show after completion** – `AiFeatureTestActivity.kt:268` – Gate checks `!onboardingCompleted` before showing onboarding, preventing re-show after completion.

### 3. Personal info fields

**OK – Required fields** – `OnboardingPersonalInfoScreen.kt:107-133` – Screen collects: display name (required, validated in ViewModel), role (optional), industry (optional). Fields match contract.

**OK – Field validation** – `OnboardingViewModel.kt:76-81` – `onSubmit` validates `displayName` is not blank, showing error "请填写姓名" if empty. Role and industry are optional (trimmed, blank becomes null).

**OK – Field purpose documented** – `OnboardingPersonalInfoScreen.kt:102-106` – UI text explains: "我们将使用这些信息为你生成更贴合的问候与导出文件。" Matches contract purpose (greetings, exports, MetaHub context).

### 4. Profile storage

**OK – Persistent storage** – `UserProfileRepository.kt:26-126` – `PersistentUserProfileRepository` uses SharedPreferences to persist profile fields (displayName, email, role, industry, organization, phone, isGuest). Storage is persistent across app restarts.

**OK – Shared repository** – `UserProfileRepository.kt:18-23` – Single `UserProfileRepository` interface used by both `OnboardingViewModel` and `UserCenterViewModel`, ensuring shared data source.

**OK – Profile flow** – `UserProfileRepository.kt:52` – `profileFlow: Flow<UserProfile>` exposes reactive updates, allowing Home and User Center to observe profile changes.

### 5. Profile as metadata

**OK – Home greeting uses profile** – `HomeScreenViewModel.kt:1504-1507` – `loadUserProfile()` observes `userProfileRepository.profileFlow` and updates `userName` via `deriveUserName(profile)`. Home greeting displays "你好，{userName}" using this value.

**OK – UserName derivation** – `HomeScreenViewModel.kt:2501-2502` – `deriveUserName` uses `profile.displayName` if not blank, otherwise falls back to "用户" or "SmartSales 用户". Matches contract fallback behavior.

**OK – Export filename uses userName** – `HomeScreenViewModel.kt:452-453` – `exportPdf` and `exportCsv` pass `_uiState.value.userName` to `ExportOrchestrator`, which uses it for `<Username>` component in filename pattern.

**OK – Export orchestrator contract** – `api-contracts.md:26` – `ExportOrchestrator.exportPdf(sessionId, markdown, userName: String? = null)` accepts `userName` parameter for filename construction. Matches contract pattern `<Username>_<major person>_<summary>_<timestamp>.<ext>`.

**UNKNOWN – MetaHub context usage** – No evidence found in code that profile `role` or `industry` fields are passed to MetaHub or used in CRM inferences. Contract mentions "MetaHub context (better summaries and CRM inferences)" but implementation may not yet use these fields for LLM context.

### 6. User Center entry

**OK – History drawer footer entry** – `HomeScreen.kt:1664-1670` – `HistoryUserCenter` composable renders at bottom of history drawer with tag `HISTORY_USER_CENTER`. Entry point matches contract.

**OK – No top bar profile icon** – `HomeScreen.kt:809-876` – `HomeTopBar` does not contain profile/avatar icon. Contract note "顶部栏不再包含 Profile/用户头像" is correctly implemented.

**OK – Navigation wiring** – `HomeScreen.kt:388` – History drawer passes `onUserCenterClick = onProfileClicked` callback, correctly wiring User Center navigation.

### 7. User Center editing

**OK – Profile editing fields** – `UserCenterScreen.kt:46-86` – Screen displays and allows editing: display name, role, industry. Fields match contract.

**OK – Shared repository** – `UserCenterViewModel.kt:24-27` – Uses same `UserProfileRepository` as onboarding, ensuring edits persist and are visible in Home greeting/exports.

**OK – Save action** – `UserCenterViewModel.kt:89-106` – `onSaveProfile()` saves edited profile to `UserProfileRepository`, updating persistent storage. Save action exists and works.

**OK – Profile observation** – `UserCenterViewModel.kt:39-45` – `observeProfile()` collects `repository.profileFlow` and updates UI state, ensuring User Center displays current profile data.

### 8. User Center links

**OK – Device Manager link** – `UserCenterScreen.kt:79-83` – `ShortcutMenuCard` includes `onOpenDeviceManager` callback. `UserCenterViewModel.kt:65-69` emits `UserCenterEvent.DeviceManager` event. Link exists and is wired.

**OK – Privacy link** – `UserCenterScreen.kt:81` – `ShortcutMenuCard` includes `onOpenPrivacy` callback. `UserCenterViewModel.kt:71-75` emits `UserCenterEvent.Privacy` event. Link exists as placeholder.

**UNKNOWN – Wi-Fi setup link** – No evidence found in User Center for Wi-Fi setup link. Contract mentions "Device + Wi-Fi Setup" in onboarding but does not explicitly require Wi-Fi link in User Center. May be intentional or future feature.

### 9. Test bypass

**OK – Test override mechanism** – `OnboardingStateRepository.kt:56-57` – `testOverrideCompleted: Boolean?` static field allows tests to override completion state. Mechanism exists and is documented.

**OK – Test usage** – `OnboardingFlowTest.kt:34,41` – Test sets `testOverrideCompleted = null` in `@Before` and `@After` to ensure clean state. Other tests can set it to `true` to bypass onboarding.

**OK – Test cleanup** – `OnboardingFlowTest.kt:76-82` – Test clears SharedPreferences to ensure fresh onboarding state. Cleanup is thorough.

---

## Likely Causes of Visible Issues

1. **Device + Wi-Fi Setup missing from onboarding flow**
   - **Cause:** Contract specifies onboarding should include "Device + Wi-Fi Setup (via BLE)" step after Personal Info, but implementation only has Welcome and Personal Info screens.
   - **Location:** `OnboardingScreens.kt:38-153` – Only Welcome and Personal Info screens exist.
   - **Contract requirement:** Section 9 of `ux-contract.md` specifies: "3. **Device + Wi-Fi Setup (via BLE)**" step in onboarding flow.
   - **Note:** This may be intentional if device setup is deferred to later (e.g., via User Center or Device Manager), but contract explicitly lists it as step 3 in onboarding.

2. **Profile role/industry not used in MetaHub context**
   - **Cause:** While profile fields are collected and stored, there is no evidence that `role` or `industry` are passed to MetaHub or used in LLM context for better summaries/CRM inferences.
   - **Location:** `HomeScreenViewModel.kt:1504-1507` – Only `displayName` is used for greeting and exports.
   - **Contract requirement:** Contract states profile is collected for "MetaHub context (better summaries and CRM inferences)", but implementation may not yet use these fields.
   - **Note:** This may be a future enhancement not yet implemented, or the contract may be aspirational.

---

## Summary

**Total rules checked:** 9  
**OK:** 7  
**MISMATCH:** 1  
**UNKNOWN:** 2

**Critical issues:**
1. Device + Wi-Fi Setup step missing from onboarding flow (contract specifies it as step 3, but implementation only has Welcome + Personal Info).

**Minor issues:**
- None identified.

**Unknown areas:**
1. Whether profile `role`/`industry` are used in MetaHub/LLM context for better summaries (contract mentions this purpose but no implementation found).
2. Whether User Center should have explicit Wi-Fi setup link (contract mentions Wi-Fi in onboarding but not explicitly in User Center).

**Positive findings:**
- Onboarding gate logic correctly prevents Home until completion ✓
- Profile persistence and sharing between onboarding/Home/User Center works ✓
- Home greeting uses profile displayName ✓
- Export filename uses profile displayName ✓
- User Center entry point in history drawer footer ✓
- User Center editing saves to shared repository ✓
- Test bypass mechanism exists and works ✓

