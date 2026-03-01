# MCP Primitives Analysis: Agent-Neutral vs Agent-Specific

## Current State Analysis

### ✅ Resources (4) - **KEEP IN MCP (Agent-Neutral)**

These are **pure context/schema** - no persona, no formatting:

1. **`ats://schema/candidate`**
   - Entity schema for Candidate
   - Field types, descriptions, enum values
   - ✅ Agent-neutral ✅ Schema definition

2. **`ats://schema/application`**
   - Entity schema for Application
   - Status enum values, field types
   - ✅ Agent-neutral ✅ Schema definition

3. **`ats://workflow/application-states`**
   - State machine: valid transitions
   - Terminal states list
   - SLA thresholds per stage
   - ✅ Agent-neutral ✅ Business rules

4. **`ats://workflow/assessment-types`**
   - Assessment catalog: types, descriptions, durations
   - ✅ Agent-neutral ✅ Reference data

**Verdict**: ✅ **These belong in MCP** - They provide context and schema, not formatting.

---

### ❌ Prompts (6) - **MOVE TO PYTHON AGENT (Agent-Specific)**

These contain **persona, tone, formatting instructions** - NOT agent-neutral:

1. **`application-status-narrative`**
   ```
   "You are a candidate-facing assistant helping someone understand their application status.
    Be clear, empathetic, and honest."
   ```
   - ❌ Contains persona ("candidate-facing assistant")
   - ❌ Contains tone instructions ("empathetic, honest")
   - ❌ Contains response structure ("Cover: 1. current state, 2. what's happening, 3. next steps")

2. **`next-step-guidance`**
   ```
   "You are a career advisor..."
   ```
   - ❌ Contains persona
   - ❌ Contains formatting ("## Prepare them for the NEXT stage by covering...")

3. **`rejection-debrief`**
   ```
   "You are a career advisor helping a candidate process a rejection..."
   "Be honest, compassionate, and forward-looking."
   ```
   - ❌ Contains persona
   - ❌ Contains tone
   - ❌ Contains response template

4. **`offer-decision-support`**
   ```
   "You are a trusted career advisor..."
   "Be objective, thorough, and personalised..."
   ```
   - ❌ Agent-specific

5. **`profile-gap-coaching`**
   ```
   "You are a technical career coach..."
   ```
   - ❌ Agent-specific

6. **`stuck-candidates-report`**
   ```
   "You are an operational analyst reporting to an HRBP team..."
   ```
   - ❌ Different persona (HRBP, not candidate)
   - ❌ Internal ops persona

**Verdict**: ❌ **These should NOT be in MCP** - They are agent-specific and should live in `post_apply_assistant` Python code.

---

## ✅ Recommended MCP Resources (Agent-Neutral Context)

### What Should Be Resources:

#### 1. **Status/Enum Mappings** (NEW - Add these!)

```
ats://enum/application-status
{
  "enumName": "ApplicationStatus",
  "values": [
    {"code": "RECEIVED", "description": "Application submitted, queued for review"},
    {"code": "SCREENING", "description": "Under initial recruiter review"},
    {"code": "PHONE_INTERVIEW", "description": "Phone screen scheduled or completed"},
    {"code": "TECHNICAL_INTERVIEW", "description": "Technical evaluation phase"},
    {"code": "FINAL_INTERVIEW", "description": "Final round with hiring team"},
    {"code": "OFFER_EXTENDED", "description": "Formal offer made"},
    {"code": "OFFER_ACCEPTED", "description": "Candidate accepted offer"},
    {"code": "OFFER_DECLINED", "description": "Candidate declined offer"},
    {"code": "HIRED", "description": "Offer accepted and position filled"},
    {"code": "REJECTED", "description": "Not selected to move forward"},
    {"code": "WITHDRAWN", "description": "Candidate withdrew application"}
  ]
}
```

```
ats://enum/assessment-type
{
  "enumName": "AssessmentType",
  "values": [
    {"code": "CODING_CHALLENGE", "description": "Algorithm/data structure problems", "typicalDuration": "60-90 min"},
    {"code": "SYSTEM_DESIGN", "description": "Architecture design interview", "typicalDuration": "45-60 min"},
    {"code": "TECHNICAL_SCREENING", "description": "Technical Q&A with engineer", "typicalDuration": "30-45 min"},
    {"code": "BEHAVIORAL", "description": "STAR-format behavioral questions", "typicalDuration": "45-60 min"},
    {"code": "COGNITIVE", "description": "Logic/reasoning assessment", "typicalDuration": "30-45 min"},
    {"code": "TAKE_HOME_PROJECT", "description": "Multi-day project assignment", "typicalDuration": "4-8 hours"}
  ]
}
```

```
ats://enum/shift-type
{
  "enumName": "ShiftType",
  "values": [
    {"code": "DAY", "description": "Standard daytime hours (08:00-17:00)"},
    {"code": "NIGHT", "description": "Overnight hours (22:00-06:00)"},
    {"code": "SWING", "description": "Evening shift (15:00-23:00)"},
    {"code": "ROTATING", "description": "Shifts change weekly or bi-weekly"},
    {"code": "FLEXIBLE", "description": "Flexible hours within business days"},
    {"code": "SPLIT", "description": "Split shifts with break"}
  ]
}
```

```
ats://enum/offer-status
{
  "enumName": "OfferStatus",
  "values": [
    {"code": "PENDING", "description": "Offer extended, awaiting candidate response"},
    {"code": "ACCEPTED", "description": "Candidate accepted the offer"},
    {"code": "DECLINED", "description": "Candidate declined the offer"},
    {"code": "NEGOTIATING", "description": "Active negotiation in progress"},
    {"code": "EXPIRED", "description": "Offer expired before candidate response"},
    {"code": "WITHDRAWN", "description": "Offer withdrawn by company"}
  ]
}
```

**Why**: Pure enum mappings with descriptions - no persona, no formatting.

---

#### 2. **Context Schemas for AgentContext DTOs** (NEW - Add these!)

```
ats://schema/job-agent-context
{
  "entity": "JobAgentContext",
  "description": "Agent-safe job requisition projection (PII stripped)",
  "fields": {
    "jobId": {"type": "string", "required": true},
    "title": {"type": "string", "required": true},
    "department": {"type": "string", "required": true},
    "location": {"type": "string", "required": true},
    "jobType": {"type": "enum", "enum": "JobType", "required": true},
    "status": {"type": "enum", "enum": "JobStatus", "required": true},
    "description": {"type": "string", "required": true},
    "requiredSkills": {"type": "array", "items": "string", "required": true},
    "preferredSkills": {"type": "array", "items": "string"},
    "minYearsExperience": {"type": "integer"},
    "salaryRangeDisplay": {"type": "string", "description": "Formatted salary range (e.g., '$140K-$180K')"},
    "shift": {"type": "object", "ref": "ShiftDetails"},
    "requiredAssessmentCodes": {"type": "array", "items": "string", "description": "Assessment codes required for this role"},
    "openedAt": {"type": "datetime"}
  },
  "piiFieldsStripped": ["costCenter", "budgetCode", "internalNotes", "_cosmosPartitionKey", "_etag"]
}
```

```
ats://schema/application-agent-context
{
  "entity": "ApplicationAgentContext",
  "description": "Agent-safe application projection (PII stripped)",
  "fields": {
    "applicationId": {"type": "string", "required": true},
    "candidateId": {"type": "string", "required": true},
    "jobId": {"type": "string", "required": true},
    "status": {"type": "enum", "enum": "ApplicationStatus", "required": true},
    "source": {"type": "enum", "enum": "ApplicationSource"},
    "appliedAt": {"type": "datetime", "required": true},
    "currentStage": {"type": "string", "description": "Human-readable stage name"},
    "daysInCurrentStage": {"type": "integer", "description": "Computed: days since last transition"},
    "slaBreached": {"type": "boolean", "description": "Computed: true if daysInCurrentStage > SLA threshold"},
    "workflowSummary": {"type": "array", "items": {"$ref": "WorkflowStageSummary"}},
    "upcomingEvents": {"type": "array", "items": {"$ref": "ScheduledEventSummary"}},
    "offerSummary": {"type": "object", "ref": "OfferSummary", "nullable": true},
    "publicNotes": {"type": "array", "items": {"$ref": "PublicRecruiterNote"}}
  },
  "piiFieldsStripped": ["assignedRecruiterId", "internalRating", "interviewer IDs", "author IDs", "offerLetterUrl", "internal negotiation notes", "_cosmosPartitionKey", "_etag"]
}
```

```
ats://schema/profile-agent-context
{
  "entity": "ProfileAgentContext",
  "description": "Agent-safe candidate profile projection (PII stripped)",
  "fields": {
    "candidateId": {"type": "string", "required": true},
    "displayName": {"type": "string", "required": true},
    "location": {"type": "string", "description": "City, state only (no street address)"},
    "yearsOfExperience": {"type": "integer"},
    "currentRole": {"type": "string"},
    "currentCompany": {"type": "string"},
    "education": {"type": "object", "ref": "EducationSummary"},
    "skills": {"type": "array", "items": {"$ref": "SkillEndorsement"}},
    "status": {"type": "enum", "enum": "CandidateStatus"},
    "totalAssessmentsCompleted": {"type": "integer", "description": "Computed count"},
    "averagePercentilesByType": {"type": "object", "description": "Map<AssessmentType, Integer> - computed average"},
    "locationPreferences": {"type": "object", "ref": "LocationPreferences"},
    "jobPreferences": {"type": "object", "ref": "JobPreferences"},
    "workStylePreferences": {"type": "object", "ref": "WorkStylePreferences"},
    "questionnaireCompleted": {"type": "boolean"},
    "questionnaireCompletedAt": {"type": "datetime", "nullable": true}
  },
  "piiFieldsStripped": ["nationalId", "ssnLast4", "dateOfBirth", "homeAddress", "personalEmail", "personalPhone", "emergencyContact", "compensation expectations", "raw questionnaire responses", "_cosmosPartitionKey", "_etag"]
}
```

**Why**: These tell the agent what fields exist in the transformed data and what was stripped - pure schema, no persona.

---

#### 3. **SLA Configuration** (ENHANCE existing workflow resource)

```
ats://workflow/sla-thresholds
{
  "description": "Expected SLA in business days per application stage",
  "thresholds": {
    "RECEIVED": {"days": 2, "description": "Time for initial recruiter assignment"},
    "SCREENING": {"days": 5, "description": "Time for initial profile review completion"},
    "PHONE_INTERVIEW": {"days": 3, "description": "Time to schedule and complete phone screen"},
    "TECHNICAL_INTERVIEW": {"days": 7, "description": "Time for technical evaluation completion"},
    "FINAL_INTERVIEW": {"days": 5, "description": "Time for final round completion"},
    "OFFER_EXTENDED": {"days": 5, "description": "Time for candidate to respond to offer"}
  }
}
```

**Why**: Business rules - no persona, no formatting.

---

#### 4. **Stage Context Guidance** (NEW - Agent-neutral facts)

```
ats://workflow/stage-context
{
  "description": "Factual context about each application stage",
  "stages": {
    "RECEIVED": {
      "what_happens": "Application queued for recruiter review",
      "typical_duration_days": "3-5",
      "possible_next_stages": ["SCREENING", "REJECTED"]
    },
    "SCREENING": {
      "what_happens": "Recruiter reviews profile against requirements",
      "typical_duration_days": "5-7",
      "possible_next_stages": ["PHONE_INTERVIEW", "REJECTED"]
    },
    "PHONE_INTERVIEW": {
      "what_happens": "Phone screen with recruiter or hiring manager",
      "typical_duration_days": "3-5",
      "possible_next_stages": ["TECHNICAL_INTERVIEW", "REJECTED"]
    },
    "TECHNICAL_INTERVIEW": {
      "what_happens": "Technical evaluation (coding, system design, or take-home)",
      "typical_duration_days": "7-10",
      "possible_next_stages": ["FINAL_INTERVIEW", "REJECTED"]
    },
    "FINAL_INTERVIEW": {
      "what_happens": "Meetings with hiring team, leadership, or stakeholders",
      "typical_duration_days": "5-7",
      "possible_next_stages": ["OFFER_EXTENDED", "REJECTED"]
    },
    "OFFER_EXTENDED": {
      "what_happens": "Formal offer made, candidate decides",
      "typical_duration_days": "Candidate's decision window (usually 5 days)",
      "possible_next_stages": ["OFFER_ACCEPTED", "OFFER_DECLINED"]
    }
  }
}
```

**Why**: These are FACTS about the process (what happens, typical duration, possible next steps) - NO persona, NO "you should..." instructions.

---

## ❌ What Should MOVE to Python post_apply_assistant

### Response Templates (Agent-Specific)

These live in `candidate-agent/src/agent/post_apply_assistant.py`:

```python
RESPONSE_TEMPLATES = {
    "status_update": {
        "persona": "empathetic career guide",
        "tone": "warm, professional, first-person plural",
        "structure": [
            "Current stage clearly stated",
            "What's happening now",
            "Expected next steps",
            "Concrete action if any"
        ],
        "example": """
        You're currently in the {stage} stage of your application for {job_title}.
        We're {what_is_happening}. Typically this takes {typical_duration}, and you've been
        in this stage for {days_in_stage} days.

        Next up: {what_happens_next}

        {action_if_any}
        """
    },

    "rejection_debrief": {
        "persona": "honest, compassionate career advisor",
        "tone": "direct but supportive, forward-looking",
        "structure": [
            "Acknowledge the outcome honestly",
            "Infer likely reasons from data",
            "Highlight strengths demonstrated",
            "Specific gaps identified",
            "3-month growth plan",
            "Better-fit roles suggestion",
            "Re-application advice"
        ]
    },

    "next_steps_guidance": {
        "persona": "coach preparing candidate for next stage",
        "tone": "encouraging, practical, specific",
        "structure": [
            "What to expect in next stage",
            "Preparation checklist",
            "Strengths to lean on",
            "Gaps to address",
            "Day-of tips"
        ]
    },

    "offer_decision_support": {
        "persona": "objective career advisor",
        "tone": "analytical, balanced, no pressure",
        "structure": [
            "Offer summary",
            "Alignment analysis",
            "Growth potential assessment",
            "Compensation analysis",
            "Questions to ask",
            "Negotiation levers",
            "Recommendation with rationale"
        ]
    }
}
```

### System Prompt (Agent-Specific)

```python
POST_APPLY_ASSISTANT_SYSTEM_PROMPT = """
You are a candidate-facing AI assistant helping individuals navigate their job applications.

## Your Role
- Act as a warm, empathetic career guide
- Use first-person plural when referring to the hiring process ("we're reviewing...")
- Never expose internal tool names, field keys, or system identifiers
- Be honest about outcomes, including rejections
- Always provide actionable next steps when possible

## Tone Guidelines
- **For status queries**: Clear, factual, reassuring
- **For rejections**: Honest, compassionate, forward-looking
- **For offers**: Analytical, balanced, supportive
- **For next steps**: Encouraging, practical, specific

## ATS Code Mapping (Internal → Candidate-Facing)
- TECHNICAL_SCREEN → "technical interview stage"
- OFFER_EXTENDED → "an offer has been made"
- REJECTED → "not moved forward at this time"
- SLA breached → "taking longer than usual"

## Response Structure
Always include:
1. Current state (what's happening now)
2. Context (how we got here, what's typical)
3. What happens next
4. Concrete action if any

## Sensitive Topics
- **Rejection**: Constructive, reference strengths where data supports, suggest growth path
- **Offer**: Factual summary, don't advise on accept/decline unless explicitly asked
- **Delays**: Honest about longer-than-usual, no false reassurance

## Tools Available
{mcp_tools_list}

## Schemas
{mcp_schemas}

## Active Request Context
{candidate_id_injection}
{application_id_injection}
"""
```

---

## 🎯 Clear Separation: MCP vs Python Agent

| Primitive | MCP (Agent-Neutral) | Python Agent (Agent-Specific) |
|-----------|---------------------|-------------------------------|
| **Resources** | ✅ Enum mappings<br>✅ Entity schemas<br>✅ Workflow state machine<br>✅ SLA thresholds<br>✅ Stage facts | ❌ (none) |
| **Tools** | ✅ Data access only<br>✅ Return AgentContext<br>✅ No formatting | ✅ Tool selection logic<br>✅ Layer 2 filtering<br>✅ Context assembly |
| **Prompts** | ❌ Remove all 6 prompts | ✅ System prompt<br>✅ Response templates<br>✅ Persona definitions<br>✅ Tone guidelines<br>✅ ATS code mapping |
| **Schemas** | ✅ AgentContext schemas<br>✅ PII stripping docs | ✅ Query-specific context filters |
| **Logic** | ✅ SLA calculation<br>✅ Transformer layer | ✅ Response formatting<br>✅ Multi-turn conversation<br>✅ Candidate vs HRBP routing |

---

## 📋 Action Items for Production

### MCP Server (Java - candidate-mcp)

**Add Resources**:
- [ ] `ats://enum/application-status`
- [ ] `ats://enum/assessment-type`
- [ ] `ats://enum/shift-type`
- [ ] `ats://enum/offer-status`
- [ ] `ats://schema/job-agent-context`
- [ ] `ats://schema/application-agent-context`
- [ ] `ats://schema/profile-agent-context`
- [ ] `ats://workflow/sla-thresholds`
- [ ] `ats://workflow/stage-context` (factual only)

**Remove Prompts**:
- [ ] Delete `application-status-narrative`
- [ ] Delete `next-step-guidance`
- [ ] Delete `rejection-debrief`
- [ ] Delete `offer-decision-support`
- [ ] Delete `profile-gap-coaching`
- [ ] Delete `stuck-candidates-report`

**Keep**:
- ✅ All 21 tools (data access only)
- ✅ Existing 4 resources (schemas + workflow)
- ✅ Resource templates (entity lookups)

### Python Agent (careers-ai-service)

**Add**:
- [ ] `post_apply_assistant.py` with system prompt
- [ ] Response templates (6 templates for different scenarios)
- [ ] ATS code → human language mapping
- [ ] Layer 2 context filtering logic
- [ ] Persona/tone configuration per consumer type

---

## 🔑 Key Principle

**MCP = DATA CONTEXT, not RESPONSE FORMAT**

✅ **MCP should provide**:
- "Here's the application. Status: FINAL_INTERVIEW. Days in stage: 28. SLA: 5 days. SLA breached: true."
- "Here's the schema. Here are the enums. Here are the state transitions."

❌ **MCP should NOT provide**:
- "You are a career advisor helping a candidate..."
- "Be empathetic and warm..."
- "Structure your response with: 1. Current state, 2. Next steps..."

✅ **Python Agent should provide**:
- All persona, tone, formatting, response structure
- "You've been in the final interview stage for 28 days, which is longer than usual. We'll follow up with the hiring team."

This separation enables:
1. **Multiple agent types** consuming same MCP (candidate agent, HRBP agent, developer agent)
2. **Different personas** per agent without changing MCP
3. **Clean testing** - MCP returns data, agent tests response formatting separately
