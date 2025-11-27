

Here’s a **high-level guide** you can stick into a ticket / doc and forget about for now.

---

## 1. Capture the current decision

When you pause this:

* Add a short note (in code or ticket):

  > Instrumentation tests in `AiFeatureTestActivityTest` are temporarily ignored due to unstable async behavior.
  > Refactor plan: slim smoke tests + nav tests + fake-backed AI flow tests once UI integration is stable.

That’s enough context so future you remembers *why* they were disabled.

---

## 2. When revisiting: redefine the test layers

When you come back, the first step is deciding **who tests what**, not writing code.

### Instrumentation tests (device / emulator)

Only cover:

* **Smoke**:

  * Activity launches
  * `overlay_shell` + one main page (e.g. `page_home`) is visible.
* **Navigation**:

  * Tapping chips (`chip_home`, `chip_wifi_ble`, `chip_device_setup`, etc.) switches visible overlay/page.
* **Very basic AI UI flow** (with fake backend):

  * Quick skill → `active_skill_chip` appears.
  * Typing + Send → some visible UI change (user bubble / pending state).

Nothing about real networking or real LLM responses here.

### Unit / host tests (no device)

* ViewModel / state logic:

  * Sending messages, quick skills, device state, etc.
* Layout & semantics checks for `HomeOverlayShell` and its pages using `createComposeRule()` + fakes.

---

## 3. Make the harness boring and reliable

When refactoring the tests:

* Keep **one** canonical root tag:

  * `overlay_shell` as the host for the home shell.
* Prefer **direct assertions** over custom wait loops:

  * `onNodeWithTag(...).assertIsDisplayed()` after natural idling.
* If you still need diagnostics:

  * Use a *single* JUnit rule that dumps `onRoot(useUnmergedTree = true).printToLog("AiShellFailure")` on test failure, instead of sprinkling try/catch everywhere.

Goal: tests read like “click → assert”, not “waitUntil loops everywhere”.

---

## 4. Introduce fakes for async behavior

To avoid the timeouts you’re hitting now:

* Add a **fake AI backend / repository** for tests (via DI/Hilt test module or similar).
* Instrumentation tests:

  * Don’t wait for real network / real responses.
  * Only assert that the UI reacts to the *intent* (quick skill tapped, message sent, etc.).
* ViewModel/unit tests:

  * Cover the “what happens when a response arrives” part.

So instrumentation tests never depend on “something remote eventually answers”.

---

## 5. Re-enable gradually

When the UI integration is stable and you’re ready:

1. Re-enable a **single smoke test** (“Ai shell renders and shows home page”).
2. Add **chip navigation tests**.
3. Finally add **quick-skill/AI flow tests** with the fake backend.

If flakiness comes back, you can spot it early because each piece is isolated.

---
