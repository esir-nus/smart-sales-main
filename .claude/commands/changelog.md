Update the changelog to the latest state:

1. Run `bash scripts/changelog.sh` from the repo root to regenerate `CHANGELOG.html` from the current `CHANGELOG.md`.

2. Then look at git commits since the most recent date already in `CHANGELOG.md` (check the latest `### YYYY-MM-DD` header). For any `feat:`, `fix:`, `ship:`, or significant `chore:`/`refactor:` commits not yet represented, append new dated sections to the top of the `## 最近更新` section in `CHANGELOG.md` — written in Chinese, with tech jargon kept in English (BLE, SIM, HarmonyOS, Tingwu, DashScope, FunASR, etc.), and 徽章 instead of Badge. Use the format:

```
### YYYY-MM-DD

- **[新增] Title** — Description in Chinese.
- **[修复] Title** — Description in Chinese.
- **[发布] Title** — Description in Chinese.
```

3. After updating `CHANGELOG.md`, run `bash scripts/changelog.sh` again to regenerate `CHANGELOG.html` with the new content.

4. Report how many new entries were added and the date range covered.
