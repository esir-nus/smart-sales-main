    private fun buildPromptWithHistory(request: ChatRequest): String {
        val builder = StringBuilder()
        if (request.history.isNotEmpty()) {
            val historyText = request.history.joinToString(separator = "\n") { item ->
                val role = when (item.role) {
                    ChatRole.USER -> "用户"
                    ChatRole.ASSISTANT -> "助手"
                }
                "$role：${item.content}"
            }
            builder.appendLine("历史对话：")
            builder.appendLine(historyText)
            builder.appendLine()
        }
        builder.append("最新问题：${request.userMessage}")
        // 根据模式追加提示：GENERAL_CHAT 与 SMART_ANALYSIS 各自定制
        if (request.quickSkillId == null) {
            builder.appendLine()
            builder.appendLine()
            if (request.isFirstAssistantReply) {
                // GENERAL：首条回复使用规则 + 输出分离结构
                builder.appendLine("<RULES>")
                builder.appendLine("1) 角色与对象")
                builder.appendLine("   - 你是销售助手，服务对象是【销售人员本人】，不是终端客户。")
                builder.appendLine("   - 永远用“你”来称呼销售，不要假装自己是销售在给客户发消息。")
                builder.appendLine()
                builder.appendLine("2) 先判断输入类型")
                builder.appendLine("   - 纯打招呼/无意义内容（hello/表情/纯数字/乱码）：只做友好回应 + 引导销售粘贴真实对话或会议纪要；不要做分析，也不要输出 JSON。")
                builder.appendLine("   - 只有短语/关键词，能看出销售场景但信息明显不足（如“李总 奥迪产线扩张 技术需求沟通”）：")
                builder.appendLine("     · 明确说明当前信息不足，不适合直接给出完整方案；")
                builder.appendLine("     · 列出 2–4 个必须补充的关键信息点（例如项目背景、数量/预算、时间规划、决策链等）；")
                builder.appendLine("     · 给 1 句澄清用示例话术，帮助销售向客户把情况问清楚。")
                builder.appendLine("   - 已经提供较完整的对话/纪要/需求描述：")
                builder.appendLine("     · 先用 1 段话总结本次机会；")
                builder.appendLine("     · 再用 2–4 条要点写客户关注点、机会/风险、建议的下一步行动；")
                builder.appendLine("     · 最后可给 1–2 句示例话术，帮助销售落地沟通。")
                builder.appendLine()
                builder.appendLine("3) 长度与重复约束（硬性规则）")
                builder.appendLine("   - 中文自然语言部分总长度不超过 160 个汉字。")
                builder.appendLine("   - 不允许出现完全相同的句子两次以上；如果你发现自己在重复同一句话，应立即结束输出。")
                builder.appendLine("   - 客户称呼（如“李总”“罗总”）在整条回复中出现不超过 2 次。")
                builder.appendLine()
                builder.appendLine("4) 可选 JSON 元数据（仅在信息足够清晰时）")
                builder.appendLine("   - 只有当你已经从文本中提炼出较清晰的客户与场景信息时，才在回复最后一行额外输出一个 JSON 对象；不确定就完全不要输出 JSON。")
                builder.appendLine("   - JSON 最多一个对象，必须放在最后一行，该行之后不能再有任何文字。")
                builder.appendLine("   - 推荐字段：main_person, short_summary, summary_title_6chars, location, highlights, actionable_tips, core_insight, sharp_line。")
                builder.appendLine("   - 无法确定的字段留空或使用占位（例如 main_person 用“未知客户”，location 用“未知”），不要凭空编造具体数量、预算金额、地点或内部姓名。")
                builder.appendLine()
                builder.appendLine("5) 元信息约束")
                builder.appendLine("   - 上述内容全部是系统规则，不是示例回答。")
                builder.appendLine("   - 你生成的回复中，禁止原文复述这些规则，也不要解释规则内容。")
                builder.appendLine("</RULES>")
                builder.appendLine()
                builder.appendLine("<OUTPUT>")
                builder.appendLine("现在根据上面的 <RULES> 生成一条给销售人员看的中文回复：")
                builder.appendLine(" - 只输出最终给销售看的内容，不要输出任何 <RULES> 或 <OUTPUT> 标签。")
                builder.appendLine(" - 不要复述“上述规则”“根据你的描述我将……”之类的元话语，直接进入具体内容。")
                builder.appendLine(" - 严格遵守长度与重复约束；如要列要点，只用“1、2、3、4”这种简单编号。")
                builder.appendLine("</OUTPUT>")
                builder.appendLine("【开始输出给销售看的回复】")
            } else {
                // GENERAL：后续回复保持简单提示，避免长 scaffold
                builder.appendLine("你是销售助手，继续面向【销售人员】回答。")
                builder.appendLine("用简洁中文回应当前问题，控制在 80–160 个汉字：")
                builder.appendLine("1、先简要回应或澄清用户问题；")
                builder.appendLine("2、给出 2–4 条关键要点或下一步建议；")
                builder.appendLine("3、如信息不足，直接说明需要补充哪些关键信息。")
                builder.appendLine("避免重复同一句话，不要输出 JSON。")
            }
        } else if (request.quickSkillId == "SMART_ANALYSIS") {
            // SMART_ANALYSIS 专用：LLM 只负责产出结构化 JSON 元数据，不负责排版
            builder.appendLine()
            builder.appendLine()
            builder.appendLine("你是 SmartSales 的销售分析助手。只能输出一个 JSON 对象，不要输出 Markdown、解释或多版本草稿。")
            builder.appendLine("JSON 字段：")
            builder.appendLine("- main_person: string，本次会话最重要的客户/外部联系人称呼，未知用 \"未知客户\"，不要写你自己或内部同事/老板。")
            builder.appendLine("- short_summary: string，2–3 句中文总结。")
            builder.appendLine("- summary_title_6chars: string，≤6 个汉字的标题。")
            builder.appendLine("- location: string，可为空。")
            builder.appendLine("- highlights: string[]，2–4 条关键要点。")
            builder.appendLine("- actionable_tips: string[]，2–4 条可执行的后续行动建议。")
            builder.appendLine("- core_insight: string，一句核心洞察。")
            builder.appendLine("- sharp_line: string，一句精炼话术（≤20 汉字）。")
            builder.appendLine("- stage: string，可选，DISCOVERY/NEGOTIATION/PROPOSAL/CLOSING/POST_SALE/UNKNOWN。")
            builder.appendLine("- risk_level: string，可选，LOW/MEDIUM/HIGH/UNKNOWN。")
            builder.appendLine("示例（仅用于格式，按需增删字段）：")
            builder.appendLine("{")
            builder.appendLine("  \"main_person\": \"王总\",")
            builder.appendLine("  \"short_summary\": \"客户关注报价与交付周期\",")
            builder.appendLine("  \"summary_title_6chars\": \"报价跟进\",")
            builder.appendLine("  \"location\": \"上海\",")
            builder.appendLine("  \"highlights\": [\"预算紧张\", \"交付周期要求短\"],")
            builder.appendLine("  \"actionable_tips\": [\"提供分期方案\", \"明确交付里程碑\"],")
            builder.appendLine("  \"core_insight\": \"客户看重价格确定性\",")
            builder.appendLine("  \"sharp_line\": \"价格透明、交付准时\"")
            builder.appendLine("}")
            builder.appendLine("不要输出示例外的任何文字或格式。")
        }
        return builder.toString()
    }
