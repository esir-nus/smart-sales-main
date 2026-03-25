# Onboarding Flow

> Type: Flow
> Status: Active
> Last Updated: 2026-03-25

## Overview

The production onboarding flow now follows the approved 5-wave prototype family.

This flow has two hosts:

1. `FULL_APP` for cold-start product onboarding
2. `SIM_CONNECTIVITY` for SIM setup reuse inside the connectivity route

The same Compose coordinator owns both hosts. Host selection changes the visible sequence, not the pairing runtime owner.

Behavior authority for the pairing runtime remains:

- `docs/cerb/device-pairing/spec.md`
- `docs/cerb/device-pairing/interface.md`
- `docs/cerb/sim-connectivity/spec.md` for SIM routing

## Host Sequences

### FULL_APP

1. `WELCOME`
2. `PERMISSIONS_PRIMER`
3. `VOICE_HANDSHAKE`
4. `HARDWARE_WAKE`
5. `SCAN`
6. `DEVICE_FOUND`
7. `PROVISIONING`
8. `COMPLETE`

### SIM_CONNECTIVITY

1. `PERMISSIONS_PRIMER`
2. `HARDWARE_WAKE`
3. `SCAN`
4. `DEVICE_FOUND`
5. `PROVISIONING`
6. `COMPLETE`

SIM must skip `WELCOME` and `VOICE_HANDSHAKE`.

## Wave Intent

### Wave 1: Tone Foundation

- `WELCOME` presents the calm premium introduction with `您的 AI 销售教练`
- `PERMISSIONS_PRIMER` explains microphone, Bluetooth, and exact-alarm needs before any native prompt appears

Rule:

- the primer is informational first
- Android permission prompts still happen at point-of-use

### Wave 2: Embodied Trust

- `VOICE_HANDSHAKE` is abstract, not fake-chat theater
- `HARDWARE_WAKE` teaches the 3-second badge wake ritual

### Wave 3: Operational Pairing

- `SCAN` uses a technical search presentation
- `DEVICE_FOUND` requires explicit manual tap to connect
- `PROVISIONING` combines Wi-Fi entry plus visible pairing/progress states

### Wave 4: Recovery and Handoff

- scan and provisioning failures stay calm and recoverable
- timeout/retry states should use muted amber recovery rather than panic-red treatment
- `COMPLETE` is one shared success wrapper with host-specific action copy

### Wave 5: Host Split Routing

- `FULL_APP` completion enters the main app home and marks onboarding complete
- `SIM_CONNECTIVITY` completion enters the SIM connectivity manager first, preserving accepted `SETUP -> MANAGER` routing

## Locked Invariants

- No generic Android setup wizard treatment
- No mascot energy
- No fake assistant chat bubbles in the voice handshake
- No auto-connect from scan results
- No onboarding-owned SIM shell navigation policy
- No account/profile/notification tail inside the active production flow

## Deferred Out of Scope

The following legacy steps are no longer part of the active production onboarding sequence:

- device naming
- account gate
- profile collection
- notification / OEM permission tail

If reintroduced later, they must be scoped as a new approved slice rather than silently appended to this flow.
