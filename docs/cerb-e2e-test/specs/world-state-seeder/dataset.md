# World State Seeder Dataset

> **Senior Engineer Note**: A valid "Chaos Seed" doesn't just inject random strings into random fields. It must prove that the routing engine handles *temporal distance* (sporadic vs dense) and *semantic evolution* (objections turning into agreements). If we just dump 50 random rows, any test failure becomes impossible to debug. By structuring this as a temporal narrative (6-month relationship building + 1-week hot deal), we can assert exactly which memory should win when the LLM is asked.

This document contains the canonical injection data used for Warm Start L2 Testing scenarios.

## 1. Entity Registry (The Graph)
Fully occupied `EntityEntryEntity` models.

```json
[
  {
    "entityId": "ent_101",
    "entityType": "COMPANY",
    "displayName": "字节跳动",
    "aliasesJson": "[\"字节\", \"ByteDance\", \"头条厂\"]",
    "demeanorJson": "{\"corporateCulture\": \"Fast-paced\", \"procurementStyle\": \"Rigorous\"}",
    "attributesJson": "{\"industry\": \"Technology\", \"headquarters\": \"Beijing\", \"employeeCount\": \"100k+\"}",
    "metricsHistoryJson": "[{\"date\":\"2025-12-01\",\"score\":65}, {\"date\":\"2026-03-07\",\"score\":92}]",
    "relatedEntitiesJson": "[\"ent_201\"]",
    "decisionLogJson": "[{\"date\":\"2026-02-15\",\"decision\":\"Approved vendor onboarding\"}]",
    "lastUpdatedAt": 1772841600000,
    "createdAt": 1754049600000,
    "accountId": "acc_101",
    "primaryContactId": "ent_201",
    "jobTitle": null,
    "buyingRole": null,
    "dealStage": "CONTRACT_NEGOTIATION",
    "dealValue": 1200000,
    "closeDate": "2026-03-31",
    "nextAction": "法务终审"
  },
  {
    "entityId": "ent_201",
    "entityType": "PERSON",
    "displayName": "张伟",
    "aliasesJson": "[\"张总\", \"伟哥\"]",
    "demeanorJson": "{\"style\": \"Direct\", \"riskTolerance\": \"Low\", \"communicationPref\": \"WeChat\"}",
    "attributesJson": "{\"department\": \"Procurement\", \"painPoints\": \"Manual data entry, API latency\"}",
    "metricsHistoryJson": "[{\"date\":\"2025-08-15\",\"engagement\":20}, {\"date\":\"2026-03-07\",\"engagement\":95}]",
    "relatedEntitiesJson": "[\"ent_101\", \"ent_102\"]",
    "decisionLogJson": "[{\"date\":\"2026-03-05\",\"decision\":\"Selected our AI suite over competitor A\"}]",
    "lastUpdatedAt": 1772841600000,
    "createdAt": 1754049600000,
    "accountId": "acc_101",
    "primaryContactId": null,
    "jobTitle": "采购总监",
    "buyingRole": "ECONOMIC_BUYER",
    "dealStage": null,
    "dealValue": null,
    "closeDate": null,
    "nextAction": "周五跟进合同盖章"
  }
]
```

## 2. Memory Center (`MemoryEntryEntity`)

### Phase A: 6-Month Sporadic (Relationship Building)
*Proves the engine doesn't overweight old, irrelevant context.*

```json
[
  {
    "entryId": "mem_001",
    "sessionId": "sess_001",
    "content": "在深圳行业峰会上认识了张总（字节采购）。交换了名片。他们目前预算全卡在另一个人手里，今年没戏。",
    "entryType": "NOTE",
    "createdAt": 1755244800000, 
    "updatedAt": 1755244800000,
    "isArchived": true,
    "scheduledAt": null,
    "structuredJson": "{\"sentiment\": \"Neutral\", \"stage\": \"Lead Gathering\"}",
    "workflow": "conference_leads",
    "title": "初次接触",
    "completedAt": 1755244800000,
    "outcomeStatus": "NO_BUDGET",
    "outcomeJson": "{}",
    "payloadJson": "{}",
    "displayContent": "深圳峰会初次面谈",
    "artifactsJson": "[]"
  },
  {
    "entryId": "mem_002",
    "sessionId": "sess_002",
    "content": "年底寄了台历并跟进了一次。张伟回复说公司内部在做组织架构大调整，采购部在重组，让我过完春节再联系。",
    "entryType": "FOLLOW_UP",
    "createdAt": 1765872000000, 
    "updatedAt": 1765872000000,
    "isArchived": true,
    "scheduledAt": null,
    "structuredJson": "{\"sentiment\": \"Positive\", \"stage\": \"Nurture\"}",
    "workflow": "holiday_greetings",
    "title": "年底关怀",
    "completedAt": 1765872000000,
    "outcomeStatus": "DELAYED",
    "outcomeJson": "{\"nextTouchpoint\": \"Post-Spring-Festival\"}",
    "payloadJson": "{}",
    "displayContent": "年底关怀与重组进展反馈",
    "artifactsJson": "[]"
  }
]
```

### Phase B: 1-Week Dense Incrementation (The Hot Deal)
*Dates: 2026-03-01 to 2026-03-07. Proves the engine tracks rapidly evolving deal stages.*

```json
[
  {
    "entryId": "mem_003",
    "sessionId": "sess_101",
    "content": "张总主动找我。架构调整完了，他们接到了全年降本增效的KPI，对我们AI提效工具很感兴趣，要求周三前给初步方案。",
    "entryType": "MEETING_NOTE",
    "createdAt": 1772332800000,
    "updatedAt": 1772332800000,
    "isArchived": false,
    "scheduledAt": null,
    "structuredJson": "{\"painPoint\": \"KPI Pressure\", \"budget\": \"TBD\"}",
    "workflow": "inbound_lead",
    "title": "需求确认对接",
    "completedAt": 1772332800000,
    "outcomeStatus": "QUALIFIED",
    "outcomeJson": "{\"actionRequired\": \"Pitch Deck\"}",
    "payloadJson": "{}",
    "displayContent": "重新对接，确认 Q3 降本提效需求",
    "artifactsJson": "[\"Requirements.pdf\"]"
  },
  {
    "entryId": "mem_004",
    "sessionId": "sess_102",
    "content": "发送了基础版方案。张伟指出竞品A的价格比我们低20%，且质疑我们的本地化部署周期。",
    "entryType": "OBJECTION",
    "createdAt": 1772505600000,
    "updatedAt": 1772505600000,
    "isArchived": false,
    "scheduledAt": null,
    "structuredJson": "{\"objectionType\": \"Pricing/Deployment\", \"competitor\": \"Competitor A\"}",
    "workflow": "proposal_sent",
    "title": "方案反馈与异议",
    "completedAt": 1772505600000,
    "outcomeStatus": "RISK_IDENTIFIED",
    "outcomeJson": "{}",
    "payloadJson": "{}",
    "displayContent": "竞争及部署周期异议获取",
    "artifactsJson": "[]"
  },
  {
    "entryId": "mem_005",
    "sessionId": "sess_103",
    "content": "拉上了技术负责人一起开会。打消了部署疑虑，并以赠送半年代维服务对冲了价格劣势。张总口头同意推进，正在走内部安全法务审批。",
    "entryType": "NEGOTIATION",
    "createdAt": 1772764800000,
    "updatedAt": 1772764800000,
    "isArchived": false,
    "scheduledAt": 1772764800000,
    "structuredJson": "{\"concession\": \"6-mo Maintenance\", \"status\": \"Verbal Agreement\"}",
    "workflow": "technical_verification",
    "title": "破冰与口头达成",
    "completedAt": 1772764800000,
    "outcomeStatus": "VERBAL_WIN",
    "outcomeJson": "{\"nextStep\": \"Legal Review\"}",
    "payloadJson": "{}",
    "displayContent": "技术拉通并解决异议，法务走账中",
    "artifactsJson": "[\"Security_Audit.pdf\"]"
  }
]
```

## Usage Notes (For L2 Tests)
- **Time Window Verification**: If asked "what was their original concern?", the pipeline must correctly skip the old `NO_BUDGET` (mem_001) and identify the pricing/deployment objection (mem_004).
- **Outcome Status Tracking**: If asked "deal status?", the system must read the `VERBAL_WIN` flag in `mem_005` combined with the Entity Registry's `dealStage: CONTRACT_NEGOTIATION` to definitively answer without hallucinating.
