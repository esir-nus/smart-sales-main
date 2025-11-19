# main_app

A self-contained multi-module Android project that merges the proven pieces from:

1. `aiFeatureTestApp` – Compose shell that already wires DashScope chat, media preview, Tingwu diagnostics, and the Wi-Fi/BLE tester UI.
2. `tingwuTestApp` – The standalone Tingwu + OSS coordinator used by the tester card.
3. `feature/connectivity` + shared `core`/`data` libraries – BLE scanning/provisioning stack, DashScope chat pipeline, media state, and platform utilities.

All of the modules live under this new root, so you can archive or iterate on them independently before running `git pull` on the original repo.

> Note: due to workspace permissions the project lives at `smart-sales-app/main_app` rather than `/main_app`. Feel free to move it elsewhere if needed.

## Module map

| Module | Purpose |
| --- | --- |
| `:app` | Copy of the proven `aiFeatureTestApp`, now serving as the main entry point. It renders chat/media panels, Tingwu upload diagnostics, and the WiFi/BLE/HTTP tester cards in one Compose screen. Hilt config + overrides are already in `AiFeatureTestApplication`. |
| `:tingwuTestApp` | Tingwu playground that exposes `TingwuTestViewModel`, OSS upload helpers, and config overrides reused by `:app`. |
| `:feature:connectivity` | BLE profile/scanner abstractions, `DeviceConnectionManager`, Wi-Fi provisioning state machine, media server HTTP helper, etc. |
| `:feature:chat` / `:feature:media` | DashScope chat state + Media sync coordinators. |
| `:data:ai-core` | DashScope + Tingwu + OSS clients (reads secrets from `local.properties`). |
| `:core:util` / `:core:test` | Shared dispatcher/result utilities and JVM test helpers. |

The Gradle settings include only these modules, so the dependency graph stays lean.

## Building & running

```bash
cd smart-sales-app/main_app
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start com.smartsales.aitest/.AiFeatureTestActivity
```

Requirements:

- JDK 17 is pinned in `gradle.properties`.
- `local.properties` already mirrors the original project (SDK path plus DashScope/Tingwu/OSS credentials). Update it if you rotate keys.
- If Gradle cannot download dependencies in this environment (sandbox), copy the project to a writable location and rerun the commands.

### Tingwu base URL

`TINGWU_BASE_URL` now points directly at whatever host/path you configure. When you enter an Aliyun host such as `https://tingwu.cn-beijing.aliyuncs.com/`, we automatically append `/openapi/tingwu/v2/` so it matches the official API in `tingwu-doc.md`. If you explicitly set a proxy URL (for example `https://tingwu.cn/api/v1/`), that value is used verbatim. Retrofit paths stay relative (`tasks`, `tasks/{taskId}`, …), so the base you provide controls exactly which service receives the requests.

## Next steps

1. Confirm Gradle has permission to write to `~/.gradle` (the CLI sandbox blocked me when I tried running `./gradlew :app:help`).
2. After verifying builds/tests locally, optionally prune modules you no longer need or move the entire `main_app` directory outside the repo to keep it safe from upcoming `git pull` operations.
3. When you are ready, integrate this project (or cherry-pick modules) back into the refreshed repo.
