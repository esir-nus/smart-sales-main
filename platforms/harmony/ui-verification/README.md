# Harmony UI Verification Root

This is the dedicated Harmony-owned root for the internal `ui-verification` app.

Current role:

- host Harmony-native packaging, lifecycle, and ArkUI page-native verification files
- keep UI page rewrite work isolated from the backend mini-lab root
- give later device UI checks a separate bundle identity from the mini-lab app

Current capability boundary:

- supported: internal shell/route scaffold, Tingwu page rewrite with mock data, hidden scheduler preview rewrite with mock data
- hidden and unsupported: public scheduler parity, reminder/alarm parity, onboarding scheduler handoff parity, any claim that mock-backed pages are production-complete

Guardrail:

- do not place Harmony-native files for this app under `app/**`, `app-core/**`, `core/**`, `data/**`, or `domain/**`

Signing helper:

- `./scripts/assemble_device_debug.sh --mode local` keeps the repeatable local signed-HAP lane
- `./scripts/assemble_device_debug.sh --mode agc` expects a dedicated UI asset set under `~/.ohos/ui-verification-signing/agc-debug/` or explicit `HARMONY_UI_AGC_*` overrides
- `--install` and `--start` default to AGC mode and refuse the known-bad local pkcs7 lane for device proof
