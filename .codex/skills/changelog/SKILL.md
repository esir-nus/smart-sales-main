---
name: changelog
description: Update CHANGELOG.md with new entries from git history, regenerate CHANGELOG.html, commit and push. Use when the user says /changelog or asks to update the changelog.
---

# Update Changelog

Update the changelog to the latest state, then commit and push.

## Step 1: Regenerate HTML

```bash
bash scripts/changelog.sh
```

## Step 2: Find new commits

Check the latest `### YYYY-MM-DD` header in `CHANGELOG.md` to find the cutoff date. Then get commits since that date:

```bash
git log --oneline --after="YYYY-MM-DD" --no-merges
```

## Step 3: Append new entries

For any `feat:`, `fix:`, `ship:`, or significant `chore:`/`refactor:` commits not yet represented, append new dated sections to the top of the `## 最近更新` section in `CHANGELOG.md`.

**Format rules:**
- Written in Chinese
- In changelog prose, write `鸿蒙OS`, never `HarmonyOS`
- Tech jargon kept in English: BLE, SIM, Tingwu, DashScope, FunASR, ESP32, Compose, Kotlin, Gradle, CI, PR
- Use 徽章 instead of Badge

**Entry format:**
```markdown
### YYYY-MM-DD

- **[新增] Title** — Description in Chinese.
- **[修复] Title** — Description in Chinese.
- **[发布] Title** — Description in Chinese.
- **[重构] Title** — Description in Chinese.
```

**Tag mapping:**
- `feat:` -> `[新增]`
- `fix:` -> `[修复]`
- `ship:` -> `[发布]`
- `refactor:` -> `[重构]`
- significant `chore:` -> `[维护]`
- `docs:` -> skip unless user-facing

## Step 4: Regenerate HTML again

```bash
bash scripts/changelog.sh
```

## Step 5: Commit and push

```bash
git add CHANGELOG.md CHANGELOG.html
git commit -m "$(cat <<'EOF'
docs(changelog): update changelog to YYYY-MM-DD

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>
EOF
)"
git push
```

## Step 6: Report

Print how many new entries were added and the date range covered.

## Rules

- Never commit directly to master. This skill runs on develop.
- Group commits by date, not by individual commit.
- Skip merge commits and trivial CI/config changes unless they're user-visible.
- Keep entries concise -- one line per feature/fix.
