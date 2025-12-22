## PostXFyun: hint-only + rewrite-only — mismatches (implementation wins)

- HUD JSON policy:
  - Docs (V5) 明确禁止输出密钥/签名材料与原始音频/HTTP 包体；但未明确禁止“调试 HUD 内联展示 JSON”。
  - Implementation 更严格：HUD 不内联展示任何 raw JSON（包括 LLM 决策 JSON 预览），仅提供 copy-only。
- Suspicious threshold wording:
  - 有些旧描述可能会写成“gapMs >= threshold 才可疑”。
  - Implementation 采用“short gap suspicious”：`gapMs <= suspiciousGapThresholdMs` 才标记为可疑边界。

