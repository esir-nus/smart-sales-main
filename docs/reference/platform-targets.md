# Platform Targets

> **Purpose**: Plain-language definition of the supported product targets and how they map to the repo.
> **Status**: Active governance reference
> **Last Updated**: 2026-04-04
> **Primary Law**: `docs/specs/platform-governance.md`

---

## 1. Android / AOSP Native

This is the current Kotlin/Gradle app lineage in the repo.

Current status:

- shipped baseline
- beta-maintenance only

Meaning:

- continue to fix bugs and reliability issues
- keep docs, CI, and release hygiene healthy
- do not use this line as the place to quietly invent the native Harmony architecture

---

## 2. Android App on Huawei / Honor / Harmony Devices

This is still part of the Android product lineage.

Meaning:

- the app is still the Android app
- Huawei/Honor/Harmony device support is a compatibility problem for that Android app
- use the same canonical Android package/artifact as other Android devices unless a platform-owned release process explicitly requires otherwise
- this path may require OEM guidance, launch-management handling, and Android-specific reminder hardening

It is **not** the same thing as a native Harmony app.

The governing reference for this compatibility path is:

- `docs/reference/harmonyos-platform-guide.md`

---

## 3. Harmony-native

Harmony-native is now treated as a separate forward platform.

Meaning:

- it inherits product intent, user journeys, and shared contracts from the shared docs spine
- it does not inherit ownership of the current Android implementation tree
- it must land in a dedicated Harmony-native root once the scaffold phase begins

Current status:

- product direction approved
- repo governance/docs lane started
- first transient Harmony Tingwu container root scaffolded at `platforms/harmony/tingwu-container/`
- broader full-capability Harmony app still not scaffolded in this repo

---

## 4. Shared Truth vs Platform Delivery

Shared truth:

- product goals
- core flows
- domain and scheduler semantics
- shared interface ownership

Platform delivery:

- lifecycle
- permissions
- notifications / reminders
- background execution
- BLE / hardware runtime
- packaging and platform-native APIs

Rule:

- if a topic answers "what the product does", keep it shared
- if a topic answers "how this platform delivers it", put it in a platform overlay

---

## 5. Current Repo Mapping

Current Android lineage:

- `app/**`
- `app-core/**`
- `core/**`
- `data/**`
- `domain/**`

Platform overlays:

- `docs/platforms/android/**`
- `docs/platforms/harmony/**`

Current transient Harmony-native code location:

- `platforms/harmony/tingwu-container/**`

Reserved broader native Harmony code location:

- dedicated Harmony-owned root, separate from the current Android lineage

Even with the transient Tingwu container root now present, native Harmony artifacts must not be added to the current Android tree.
