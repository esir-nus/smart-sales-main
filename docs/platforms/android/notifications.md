# Android Notification and Reminder Delivery

> **Purpose**: Android-owned delivery overlay for notifications, reminders, alarms, and OEM reminder hardening.
> **Shared Product Truth**: `docs/cerb/notifications/spec.md`
> **Related Compatibility Guide**: `docs/reference/harmonyos-platform-guide.md`
> **Status**: Active
> **Last Updated**: 2026-04-04

---

## This Overlay Owns

- Android `POST_NOTIFICATIONS` behavior
- Android alarm / reminder delivery through the current app
- full-screen-intent and exact-alarm recovery specifics
- OEM settings guidance for Xiaomi / HyperOS and Huawei / Honor / Harmony-device compatibility
- Android-side settings deep links and background execution constraints

---

## Current Repo Posture

The shipped reminder path in this repo is still Android-owned:

- Android devices use the existing Android reminder stack
- Huawei/Honor/Harmony devices are still treated as Android-app compatibility targets
- the current repo does not ship a native Harmony reminder implementation

When shared reminder semantics change, update the shared notification spec first or in the same change.
When only Android delivery mechanics change, update this overlay and the owning Android docs/SOPs instead of duplicating shared semantics.
