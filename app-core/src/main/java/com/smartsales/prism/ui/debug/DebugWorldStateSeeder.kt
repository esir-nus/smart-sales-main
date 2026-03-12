package com.smartsales.prism.ui.debug

import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.memory.MemoryEntry
import com.smartsales.prism.domain.memory.MemoryEntryType
import com.smartsales.prism.domain.memory.MemoryRepository

class DebugWorldStateSeeder(
    private val entityRepository: EntityRepository,
    private val memoryRepository: MemoryRepository
) {
    suspend fun injectChaosSeed() {
        // 1. Entity Registry (The Graph)
        buildEntity(
            id = "ent_101",
            type = EntityType.ACCOUNT,
            name = "字节跳动",
            aliases = """["字节", "ByteDance", "头条厂"]""",
            demeanor = """{"corporateCulture": "Fast-paced", "procurementStyle": "Rigorous"}""",
            attributes = """{"industry": "Technology", "headquarters": "Beijing", "employeeCount": "100k+"}""",
            metrics = """[{"date":"2025-12-01","score":65}, {"date":"2026-03-07","score":92}]""",
            related = """["ent_201"]""",
            decisions = """[{"date":"2026-02-15","decision":"Approved vendor onboarding"}]""",
            updatedAt = 1772841600000,
            createdAt = 1754049600000,
            accountId = "acc_101",
            primaryContactId = "ent_201",
            dealStage = "CONTRACT_NEGOTIATION",
            dealValue = 1200000,
            closeDate = "2026-03-31",
            nextAction = "法务终审"
        )
        
        buildEntity(
            id = "ent_201",
            type = EntityType.PERSON,
            name = "张伟",
            aliases = """["张总", "伟哥"]""",
            demeanor = """{"style": "Direct", "riskTolerance": "Low", "communicationPref": "WeChat"}""",
            attributes = """{"department": "Procurement", "painPoints": "Manual data entry, API latency"}""",
            metrics = """[{"date":"2025-08-15","engagement":20}, {"date":"2026-03-07","engagement":95}]""",
            related = """["ent_101", "ent_102"]""",
            decisions = """[{"date":"2026-03-05","decision":"Selected our AI suite over competitor A"}]""",
            updatedAt = 1772841600000,
            createdAt = 1754049600000,
            accountId = "acc_101",
            jobTitle = "采购总监",
            buyingRole = "ECONOMIC_BUYER",
            nextAction = "周五跟进合同盖章"
        )

        // 2. Memory Center - Phase A: 6-Month Sporadic
        buildMemory(
            id = "mem_001",
            sessionId = "sess_001",
            content = "在深圳行业峰会上认识了张总（字节采购）。交换了名片。他们目前预算全卡在另一个人手里，今年没戏。",
            type = MemoryEntryType.TASK_RECORD,
            createdAt = 1755244800000,
            isArchived = true,
            structuredJson = """{"sentiment": "Neutral", "stage": "Lead Gathering"}""",
            workflow = "conference_leads",
            title = "初次接触",
            outcomeStatus = "NO_BUDGET",
            displayContent = "深圳峰会初次面谈"
        )

        buildMemory(
            id = "mem_002",
            sessionId = "sess_002",
            content = "年底寄了台历并跟进了一次。张伟回复说公司内部在做组织架构大调整，采购部在重组，让我过完春节再联系。",
            type = MemoryEntryType.TASK_RECORD,
            createdAt = 1765872000000,
            isArchived = true,
            structuredJson = """{"sentiment": "Positive", "stage": "Nurture"}""",
            workflow = "holiday_greetings",
            title = "年底关怀",
            outcomeStatus = "DELAYED",
            outcomeJson = """{"nextTouchpoint": "Post-Spring-Festival"}""",
            displayContent = "年底关怀与重组进展反馈"
        )

        // 2. Memory Center - Phase B: 1-Week Dense Incrementation
        buildMemory(
            id = "mem_003",
            sessionId = "sess_101",
            content = "张总主动找我。架构调整完了，他们接到了全年降本增效的KPI，对我们AI提效工具很感兴趣，要求周三前给初步方案。",
            type = MemoryEntryType.TASK_RECORD,
            createdAt = 1772332800000,
            isArchived = false,
            structuredJson = """{"painPoint": "KPI Pressure", "budget": "TBD"}""",
            workflow = "inbound_lead",
            title = "需求确认对接",
            outcomeStatus = "QUALIFIED",
            outcomeJson = """{"actionRequired": "Pitch Deck"}""",
            displayContent = "重新对接，确认 Q3 降本提效需求",
            artifactsJson = """["Requirements.pdf"]"""
        )

        buildMemory(
            id = "mem_004",
            sessionId = "sess_102",
            content = "发送了基础版方案。张伟指出竞品A的价格比我们低20%，且质疑我们的本地化部署周期。",
            type = MemoryEntryType.TASK_RECORD,
            createdAt = 1772505600000,
            isArchived = false,
            structuredJson = """{"objectionType": "Pricing/Deployment", "competitor": "Competitor A"}""",
            workflow = "proposal_sent",
            title = "方案反馈与异议",
            outcomeStatus = "RISK_IDENTIFIED",
            displayContent = "竞争及部署周期异议获取"
        )

        buildMemory(
            id = "mem_005",
            sessionId = "sess_103",
            content = "拉上了技术负责人一起开会。打消了部署疑虑，并以赠送半年代维服务对冲了价格劣势。张总口头同意推进，正在走内部安全法务审批。",
            type = MemoryEntryType.TASK_RECORD,
            createdAt = 1772764800000,
            isArchived = false,
            scheduledAt = 1772764800000,
            structuredJson = """{"concession": "6-mo Maintenance", "status": "Verbal Agreement"}""",
            workflow = "technical_verification",
            title = "破冰与口头达成",
            outcomeStatus = "VERBAL_WIN",
            outcomeJson = """{"nextStep": "Legal Review"}""",
            displayContent = "技术拉通并解决异议，法务走账中",
            artifactsJson = """["Security_Audit.pdf"]"""
        )
    }

    private suspend fun buildEntity(
        id: String, type: EntityType, name: String, aliases: String = "[]",
        demeanor: String = "{}", attributes: String = "{}", metrics: String = "{}",
        related: String = "[]", decisions: String = "[]", updatedAt: Long, createdAt: Long,
        accountId: String? = null, primaryContactId: String? = null, jobTitle: String? = null,
        buyingRole: String? = null, dealStage: String? = null, dealValue: Long? = null,
        closeDate: String? = null, nextAction: String? = null
    ) {
        val entry = EntityEntry(
            entityId = id, entityType = type, displayName = name,
            aliasesJson = aliases, demeanorJson = demeanor, attributesJson = attributes,
            metricsHistoryJson = metrics, relatedEntitiesJson = related, decisionLogJson = decisions,
            lastUpdatedAt = updatedAt, createdAt = createdAt,
            accountId = accountId, primaryContactId = primaryContactId, jobTitle = jobTitle,
            buyingRole = buyingRole, dealStage = dealStage, dealValue = dealValue,
            closeDate = closeDate, nextAction = nextAction
        )
        entityRepository.save(entry)
    }

    private suspend fun buildMemory(
        id: String, sessionId: String, content: String, type: MemoryEntryType,
        createdAt: Long, isArchived: Boolean, scheduledAt: Long? = null,
        structuredJson: String? = null, workflow: String? = null, title: String? = null,
        outcomeStatus: String? = null, outcomeJson: String? = null, displayContent: String? = null,
        artifactsJson: String? = null
    ) {
        val entry = MemoryEntry(
            entryId = id, sessionId = sessionId, content = content, entryType = type,
            createdAt = createdAt, updatedAt = createdAt, isArchived = isArchived,
            scheduledAt = scheduledAt, structuredJson = structuredJson, workflow = workflow,
            title = title, completedAt = createdAt, outcomeStatus = outcomeStatus,
            outcomeJson = outcomeJson, payloadJson = "{}", displayContent = displayContent,
            artifactsJson = artifactsJson ?: "[]"
        )
        memoryRepository.save(entry)
    }
}
