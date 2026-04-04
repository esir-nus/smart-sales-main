# Harmony-native Notification and Reminder Delivery

> **Purpose**: Harmony-native overlay placeholder for notifications, reminders, and alarm delivery once the native Harmony root exists.
> **Shared Product Truth**: `docs/cerb/notifications/spec.md`
> **Status**: Planned / not yet shipped in this repo
> **Last Updated**: 2026-04-04

---

## Current State

The repo does **not** yet ship a native Harmony reminder implementation.

Current interpretation:

- Harmony-native is an approved forward platform direction
- native Harmony reminder delivery is not yet implemented in this repo
- the existing Huawei/Honor/Harmony behavior for the shipped app is still governed by the Android compatibility path, not by this overlay

For the current shipped Android app on Huawei/Honor/Harmony devices, use:

- `docs/platforms/android/notifications.md`
- `docs/reference/harmonyos-platform-guide.md`

---

## Future Ownership

Once the native Harmony root exists, this overlay must own the Harmony-specific delivery delta for:

- native reminder APIs and permissions
- Harmony lifecycle and background constraints
- Harmony settings entrypoints
- notification delivery differences that do not belong in shared product truth

Do not document speculative implementation details here before the native root and owning code path exist.
