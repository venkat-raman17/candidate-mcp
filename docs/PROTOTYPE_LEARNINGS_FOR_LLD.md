# Prototype Learnings for Production LLD

**Document Purpose**: This document extracts key learnings from the candidate-mcp prototype that should inform the production LLD for careers-ai-service integration with the real Java MCP server.

**Target Audience**: Architecture team designing the production careers-ai-service (Python runtime) integration with candidate-mcp (Java MCP server).

**Date**: 2026-03-01

---

## Executive Summary

This prototype validated:
1. ✅ **Three-layer transformation pipeline** (Cosmos → AgentContext → Query Filter → Response) is essential for PII protection
2. ✅ **Client abstraction layer** enables clean separation between MCP tools and downstream service contracts
3. ✅ **SLA tracking** can be computed in the transformer layer without impacting downstream services
4. ✅ **Multi-job applications** (ApplicationGroups) require special handling in both data model and agent logic
5. ✅ **Assessment code mapping** between jobs and profiles is critical for skills gap analysis
6. ✅ **Shift and work mode preferences** are first-class requirements, not afterthoughts
7. ✅ **Interview schedule PII** (names vs IDs) requires careful transformer design

---

## 1. Data Architecture Learnings

### 1.1 Three-Layer Data Transformation Pipeline

**What We Built**:
```
Layer 0: Cosmos Document (raw from cx-applications, talent-profile-service, job-sync-service)
   ↓
Layer 1: candidate-mcp AgentContextTransformer (PII strip + field projection)
   ↓
Layer 2: careers-ai-service post_apply_assistant (query-specific context filter)
   ↓
Layer 3: careers-ai-service post_apply_assistant (candidate-facing response formatter)
```

**Key Learning**: Layer 1 (MCP transformer) MUST be agent-neutral. All PII stripping happens here, not in the Python layer.

**Production Recommendation**:
- Create `AgentContextTransformer<T, R>` interface in candidate-mcp
- Implement 3 concrete transformers: `JobTransformer`, `ApplicationTransformer`, `ProfileTransformer`
- **Never** expose raw Cosmos documents to the Python agent
- **Always** return `*AgentContext` DTOs from MCP tools

**Code Pattern for Production**:
```java
@Component
public class ApplicationTransformer implements AgentContextTransformer<AtsApplication, ApplicationAgentContext> {
    @Override
    public ApplicationAgentContext transform(AtsApplication source) {
        if (source == null) return null;

        // Strip: assignedRecruiterId, internalRating, offerLetterUrl, _etag
        // Compute: currentStage, daysInCurrentStage, slaBreached
        // Transform: workflowHistory → WorkflowStageSummary (remove actor IDs)
        // Transform: schedule → ScheduledEventSummary (remove interviewer IDs, keep names)

        return new ApplicationAgentContext(...);
    }
}
```

---

### 1.2 ApplicationGroups vs AtsApplications Data Model

**What We Built**:
- **ApplicationGroup**: Draft applications where candidates apply to MULTIPLE jobs in one session
  - Fields: `groupId`, `candidateId`, `List<String> jobIds`, `status`, `sourcePlatform`, `draftData`, `completionPercentage`
- **AtsApplication**: Individual job applications (one job per application)
  - Created when ApplicationGroup is submitted → one AtsApplication per jobId

**Key Learning**: Multi-job applications are a real enterprise pattern. Candidates frequently apply to 3-5 similar roles in one session.

**Production Recommendation**:
1. **Add ApplicationGroup entity to cx-applications data model**
   - Cosmos container: `application-groups`
   - Partition key: `candidateId`
2. **Add GET endpoints**:
   - `GET /v1/application-groups/{groupId}`
   - `GET /v1/application-groups?candidateId={id}`
3. **Add MCP tools**:
   - `getApplicationGroup`
   - `getApplicationGroupsByCandidate`
4. **Agent logic**:
   - When user asks "What did I apply for?", check BOTH ApplicationGroups and AtsApplications
   - Draft applications (status=DRAFT) should prompt user to complete profile

**Data Flow**:
```
User fills careers portal multi-job form
   → ApplicationGroup created (status=DRAFT, jobIds=[J1, J2, J3])
   → User completes questionnaire
   → ApplicationGroup submitted (status=SUBMITTED)
   → Trigger: Create 3 AtsApplications (A1→J1, A2→J2, A3→J3)
   → ApplicationGroup status=SUBMITTED (retained for audit)
```

---

### 1.3 Assessment Code Mapping Schema

**What We Built**:
```java
record AssessmentCodeMapping(
    List<String> requiredCodes,              // ["JAVA_01", "SYS_DESIGN_02"]
    Map<String, String> codeDescriptions,    // {"JAVA_01": "Java Coding Challenge (Medium)", ...}
    Boolean allowExternalCerts
)
```

**Key Learning**: Assessment codes must be standardized across jobs and candidate profiles for skills gap analysis to work.

**Production Recommendation**:
1. **Add assessmentCodeMapping to job-sync-service job requisition schema**
   - Required codes (must complete to be considered)
   - Optional codes (nice to have)
2. **Add assessmentCode field to talent-profile-service assessment results**
   - Map each assessment to a standard code
   - Example: Candidate completes "Java Assessment v2" → code = "JAVA_01"
3. **Maintain assessment code registry**
   - Centralized mapping: code → name → description
   - Versioned (JAVA_01, JAVA_02 for different versions)
4. **MCP getSkillsGap tool**:
   - Compare `job.requiredAssessmentCodes` vs `candidate.completedAssessmentCodes`
   - Return missing assessments prominently

**Example Skills Gap Response**:
```json
{
  "missingSkills": ["Kubernetes", "Terraform"],
  "matchingSkills": ["Java", "Spring Boot"],
  "matchPercentage": 60,
  "requiredAssessmentCodes": ["JAVA_01", "K8S_INFRA_01"],
  "candidateAssessmentCodes": ["JAVA_01"],
  "missingAssessments": ["K8S_INFRA_01"]
}
```

---

### 1.4 Shift Details as First-Class Job Attribute

**What We Built**:
```java
record ShiftDetails(
    ShiftType type,                  // DAY, NIGHT, SWING, ROTATING, FLEXIBLE, SPLIT
    String timeZone,
    String startTime,                // "22:00"
    String endTime,                  // "06:00"
    List<DayOfWeek> workDays,
    Boolean remoteEligible,
    Integer onsiteDaysPerWeek
)
```

**Key Learning**: Shift requirements are a major factor in job matching. Many roles (SRE, Support, Operations) have non-standard shifts.

**Production Recommendation**:
1. **Add shiftDetails to job-sync-service schema**
   - Not optional—every job must specify shift expectations
   - Default: FLEXIBLE with standard business hours
2. **Add acceptableShifts to talent-profile-service WorkStylePreferences**
   - List of acceptable shift types: `[DAY, NIGHT, ROTATING]`
   - Many candidates filter jobs by shift compatibility
3. **Expose in MCP JobAgentContext**
   - `shift` field fully retained (not PII, candidate-facing info)
4. **Agent matching logic**:
   - Filter jobs by shift compatibility before suggesting to candidate
   - Flag mismatches clearly: "This role requires night shift (10pm-6am). Your profile indicates day shift preference."

---

### 1.5 Interview Schedule Metadata

**What We Built**:
```java
record ScheduleMetadata(
    List<ScheduledEvent> events,
    String calendarLink,
    String timezone
)

record ScheduledEvent(
    String eventId,
    EventType type,                  // PHONE_SCREEN, TECH_INTERVIEW, ONSITE, FINAL_ROUND
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    String location,                 // "Zoom", "Office - Building A"
    List<String> interviewerIds,
    List<String> interviewerNames,
    EventStatus status               // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
)
```

**Key Learning**: Interview schedule is the #1 candidate query. "When is my next interview?" and "Who am I meeting with?" are top use cases.

**Production Recommendation**:
1. **Add scheduleMetadata to cx-applications AtsApplication schema**
   - Event list with full interviewer metadata
2. **Transformer rule**: Strip interviewer IDs, retain names
   - Candidate needs to know who they're meeting ("Sarah Chen, Engineering Manager")
   - Candidate does NOT need internal employee IDs
3. **Add MCP tool: getScheduledEvents**
   - Returns only SCHEDULED and upcoming events (not past COMPLETED ones by default)
   - Filter by application ID
4. **Agent response template**:
   ```
   "You have a technical interview scheduled for [date] at [time] ([timezone]).
    You'll be meeting with [interviewer names]. The interview will be conducted via [location].
    Duration: [X] minutes."
   ```

---

## 2. SLA Tracking and Workflow History

### 2.1 SLA Calculation in Transformer Layer

**What We Built**:
```java
private static final Map<ApplicationStatus, Integer> SLA_DAYS = Map.of(
    ApplicationStatus.RECEIVED, 2,
    ApplicationStatus.SCREENING, 5,
    ApplicationStatus.PHONE_INTERVIEW, 3,
    ApplicationStatus.TECHNICAL_INTERVIEW, 7,
    ApplicationStatus.FINAL_INTERVIEW, 5,
    ApplicationStatus.OFFER_EXTENDED, 5
);

private boolean isSlaBreached(ApplicationStatus currentStatus, int daysInStage) {
    Integer slaThreshold = SLA_DAYS.get(currentStatus);
    return slaThreshold != null && daysInStage > slaThreshold;
}
```

**Key Learning**: SLA tracking can be computed on the fly in the transformer without adding database fields.

**Production Recommendation**:
1. **Compute SLA breach in ApplicationTransformer**:
   - Input: `workflowHistory` (last transition timestamp)
   - Compute: `daysInCurrentStage = now - lastTransition.transitionedAt`
   - Compute: `slaBreached = daysInCurrentStage > SLA_DAYS[currentStatus]`
   - Return in `ApplicationAgentContext`
2. **Do NOT add slaBreached as a stored field in Cosmos**
   - It's a derived field, always computed fresh
3. **Agent response pattern**:
   - If `slaBreached = true`: "Your application has been in [stage] for [X] days, which is longer than usual. We'll follow up with the hiring team."
   - If within SLA: "Your application has been in [stage] for [X] days. Typical wait time is [SLA] days."

---

### 2.2 Workflow History Transformation

**What We Built**:
```java
// Raw Cosmos:
record WorkflowHistoryEntry(
    ApplicationStatus fromStatus,
    ApplicationStatus toStatus,
    LocalDateTime transitionedAt,
    String transitionedBy,           // USER ID (PII)
    String transitionedByName,
    String reason,
    String notes,
    Map<String, Object> metadata
)

// Agent Context (PII-stripped):
record WorkflowStageSummary(
    String stage,                    // Just the stage name
    LocalDateTime enteredAt,
    Integer daysInStage
    // NO transitionedBy ID
    // NO internal reason/notes
)
```

**Key Learning**: Workflow history contains sensitive internal data. Agent needs dates and stages only, not actor IDs or internal notes.

**Production Recommendation**:
1. **Store full WorkflowHistoryEntry in Cosmos** (for audit trail)
2. **Transform to WorkflowStageSummary in Layer 1**:
   - Strip: `transitionedBy` (ID), internal `reason`, internal `notes`
   - Retain: `stage`, `enteredAt`, computed `daysInStage`
3. **Agent sees simplified timeline**:
   ```
   RECEIVED → 2 days → SCREENING → 4 days → PHONE_INTERVIEW (current, 12 days)
   ```

---

## 3. Offer Metadata and Negotiation Tracking

### 3.1 Offer Metadata Schema

**What We Built**:
```java
record OfferMetadata(
    String offerId,
    LocalDateTime offerExtendedAt,
    LocalDateTime offerExpiresAt,
    LocalDateTime candidateRespondedAt,
    OfferStatus offerStatus,                    // PENDING, ACCEPTED, DECLINED, NEGOTIATING
    CompensationOffer compensation,
    List<NegotiationRound> negotiationHistory,
    String offerLetterUrl                        // PII - stripped in transformer
)

record NegotiationRound(
    Integer roundNumber,
    LocalDateTime requestedAt,
    String requestedBy,                          // "Candidate" or recruiter name
    String requestType,                          // SALARY_INCREASE, EQUITY_ADJUSTMENT, START_DATE
    String requestDetails,
    String response,
    LocalDateTime respondedAt
)
```

**Key Learning**: Offer negotiation is a multi-round process. Candidates need to track:
- What they requested (salary increase, start date change, etc.)
- What was approved/pending/declined
- Expiration deadline

**Production Recommendation**:
1. **Add offerMetadata to cx-applications AtsApplication**
   - Only populated when status = OFFER_EXTENDED
2. **Transformer rule**: Strip `offerLetterUrl` (document link with PII)
3. **Expose in OfferSummary** (Layer 1):
   - `offerExtendedAt`, `offerExpiresAt`, `status`
   - `salaryRangeDisplay` (not exact until accepted)
   - `startDate`
   - **NO** internal negotiation notes
   - **NO** offer letter document URL
4. **Agent response pattern**:
   ```
   "Your offer was extended on [date] and expires in [X] days.
    Status: Negotiating.
    You requested a salary adjustment (approved) and a later start date (pending response)."
   ```

**Security Note**: Do NOT expose exact compensation details in negotiation history to LLM until offer is accepted. Use ranges.

---

## 4. PII Protection Patterns

### 4.1 Comprehensive PII Stripping Checklist

**From Prototype**:

| Entity | Always Stripped (NEVER in AgentContext) | Retained (Safe for Agent) |
|--------|----------------------------------------|---------------------------|
| **Job** | costCenter, budgetCode, internalNotes, _cosmosPartitionKey, _etag | All public job details, compensation range, shift details, assessment codes |
| **Application** | assignedRecruiterId, internalRating, interviewer IDs, author IDs, offerLetterUrl, internal negotiation notes, _cosmosPartitionKey, _etag | Application status, stage history (dates only), interviewer names, offer summary, recruiter note content |
| **Profile** | nationalId, SSN, DOB, homeAddress, personalEmail, personalPhone, emergencyContact, compensation expectations, raw questionnaire responses, _cosmosPartitionKey, _etag | Display name, location (city/state), professional email, years of experience, skills, education, assessment scores/percentiles, preferences (location, job, work style) |

**Production Recommendation**:
1. **Create PII Audit Checklist** for each new field added to schemas
2. **Default to STRIP** unless there's a clear candidate-facing need
3. **Transformer Unit Tests** should verify PII fields are null in AgentContext
4. **Logging Policy**: Never log AgentContext objects without reviewing for residual PII

---

### 4.2 Interviewer Names vs IDs

**What We Built**:
- Raw: `List<String> interviewerIds`, `List<String> interviewerNames`
- AgentContext: `List<String> interviewerNames` (IDs stripped)

**Key Learning**: Candidate needs to know who they're meeting ("Michael Roberts, Senior Engineer") but does NOT need internal employee IDs.

**Production Recommendation**:
1. **Retain interviewer names in ScheduledEventSummary**
2. **Strip interviewer IDs**
3. **Optional**: Add interviewer titles for context ("Sarah Chen, Hiring Manager")
4. **Do NOT expose**: Employee email, employee ID, internal department codes

---

## 5. Caching Strategy Recommendations

### 5.1 Layer-Specific Caching

**From Prototype Learnings**:

| Layer | Cache? | TTL | Key | Reason |
|-------|--------|-----|-----|--------|
| **Layer 0 (Cosmos docs)** | No | - | - | Always fresh from source |
| **Layer 1 (AgentContext)** | Yes | 5-15 min | `{service}:{entityType}:{entityId}` | Expensive transformation + downstream call |
| **Layer 2 (Query filter)** | No | - | - | Stateless filter, negligible cost |
| **Layer 3 (LLM response)** | No | - | - | Non-deterministic |

**Production Recommendation for candidate-mcp**:
1. **Tool Response Cache** (Redis):
   ```java
   @Cacheable(value = "mcp:tool:profile", key = "#candidateId", unless = "#result == null")
   public ProfileAgentContext getCandidateProfile(String candidateId) {
       CandidateProfileV2 raw = talentProfileClient.getProfileV2(candidateId);
       return profileTransformer.transform(raw);
   }
   ```
2. **TTL by data volatility**:
   - Profile: 10 min (changes infrequently)
   - Job: 15 min (stable after posting)
   - Application status: **NO CACHE** (live status)
   - Assessment results: 5 min (stable once completed)
3. **Cache invalidation**: Event-driven on profile/job update

---

## 6. Observability and Debugging

### 6.1 Correlation ID Propagation

**Production Recommendation**:
```
HTTP Request → careers-ai-service (Python)
   → generates correlation_id
   → adds to LangGraph AgentState
   → passes in X-Correlation-ID header to candidate-mcp
   → candidate-mcp adds to MDC (Mapped Diagnostic Context)
   → all logs tagged with correlation_id
   → propagates to downstream service calls (cx-applications, talent-profile-service)
```

**Log Pattern**:
```
[correlation_id=abc123] [tool=getCandidateProfile] [candidateId=C001] Fetching profile from talent-profile-service
[correlation_id=abc123] [tool=getCandidateProfile] [candidateId=C001] Transformer stripped 8 PII fields
[correlation_id=abc123] [tool=getCandidateProfile] [candidateId=C001] Returned ProfileAgentContext (142 KB)
```

---

### 6.2 Key Metrics

**Production Metrics to Collect**:

| Metric | Type | Labels | Purpose |
|--------|------|--------|---------|
| `mcp.tool.calls.total` | Counter | `tool`, `status` | Tool usage distribution |
| `mcp.tool.duration.seconds` | Histogram | `tool` | Tool latency (includes downstream + transformation) |
| `mcp.transformer.duration.seconds` | Histogram | `transformer` | Transformation overhead |
| `mcp.pii.fields.stripped` | Counter | `entity_type`, `field_name` | PII protection audit |
| `mcp.sla.breached.total` | Counter | `application_status` | SLA breach rate |
| `downstream.calls.total` | Counter | `service`, `endpoint`, `status` | Downstream health |
| `circuit_breaker.state` | Gauge | `service` | Resilience status |

**Alerting**:
- Alert if `mcp.sla.breached.total` > 20% of applications in FINAL_INTERVIEW
- Alert if `circuit_breaker.state{service=cx-applications}` = OPEN
- Alert if `mcp.tool.duration.seconds{quantile=0.95}` > 2s

---

## 7. Testing Strategy

### 7.1 Contract Tests (Pact)

**Production Recommendation**:
1. **candidate-mcp as Consumer** of cx-applications, talent-profile-service, job-sync-service
2. **Publish consumer contracts** to Pact Broker
3. **Provider verification** runs in each downstream service CI
4. **Breaking changes** caught before deployment

**Example Contract**:
```javascript
// candidate-mcp → cx-applications contract
{
  "consumer": "candidate-mcp",
  "provider": "cx-applications",
  "interactions": [
    {
      "description": "Get application by ID",
      "request": {
        "method": "GET",
        "path": "/v1/applications/A001"
      },
      "response": {
        "status": 200,
        "body": {
          "applicationId": "A001",
          "candidateId": "C001",
          "jobId": "J001",
          "status": "FINAL_INTERVIEW",
          // ... all required fields
        }
      }
    }
  ]
}
```

---

### 7.2 Transformer Tests

**Critical Test Cases**:
```java
@Test
void applicationTransformer_stripsPII() {
    AtsApplication raw = buildApplicationWithPII();
    ApplicationAgentContext transformed = transformer.transform(raw);

    // Verify PII stripped
    assertNull(transformed.assignedRecruiterId());
    assertNull(transformed.internalRating());
    assertNull(transformed.offerSummary().offerLetterUrl());

    // Verify interviewer IDs stripped but names retained
    assertTrue(transformed.upcomingEvents().get(0).interviewerNames().contains("Sarah Chen"));
}

@Test
void applicationTransformer_computesSLA() {
    AtsApplication app = buildApplicationInFinalInterview(daysInStage = 28);
    ApplicationAgentContext transformed = transformer.transform(app);

    assertEquals(28, transformed.daysInCurrentStage());
    assertTrue(transformed.slaBreached());  // 28 > 5 day SLA
}
```

---

## 8. Production Deployment Checklist

### 8.1 careers-data-schema Updates

**Required Schema Additions**:
- [ ] Add `ApplicationGroup` entity
- [ ] Add `shiftDetails` to JobRequisition
- [ ] Add `assessmentCodeMapping` to JobRequisition
- [ ] Add `scheduleMetadata` to AtsApplication
- [ ] Add `offerMetadata` to AtsApplication
- [ ] Add `acceptableShifts` to WorkStylePreferences (in CandidateProfile)

### 8.2 Downstream Service Updates

**cx-applications**:
- [ ] Add GET /v1/application-groups/{groupId}
- [ ] Add GET /v1/application-groups?candidateId={id}
- [ ] Add `scheduleMetadata` field to AtsApplication
- [ ] Add `offerMetadata` field to AtsApplication

**talent-profile-service**:
- [ ] Add `assessmentCode` field to AssessmentResult
- [ ] Add `acceptableShifts` to WorkStylePreferences
- [ ] Ensure ProfileV2 includes all preference sub-objects

**job-sync-service**:
- [ ] Add `shiftDetails` to job requisition
- [ ] Add `assessmentCodeMapping` to job requisition

### 8.3 candidate-mcp (Java)

- [ ] Create `dto` package structure (jobsync, cxapplications, talentprofile, agentcontext)
- [ ] Create client interfaces (JobSyncClient, CxApplicationsClient, TalentProfileClient)
- [ ] Implement WebClient-based clients with circuit breakers
- [ ] Create transformer layer (JobTransformer, ApplicationTransformer, ProfileTransformer)
- [ ] Update MCP configuration to use clients + transformers
- [ ] Add 4 new tools (ApplicationGroups, preferences, scheduled events, enhanced skills gap)
- [ ] Unit tests for transformers (PII stripping, SLA calculation)
- [ ] Integration tests with WireMock
- [ ] Pact contract tests

### 8.4 careers-ai-service (Python)

- [ ] Update to latest langchain-mcp-adapters (with signature provider support)
- [ ] Implement App2App signature provider (HMAC-SHA256)
- [ ] Configure httpx connection pool (HTTP/2, TLS session resumption)
- [ ] Build post_apply_assistant with Layer 2 context filtering
- [ ] Add response templates for 6 scenarios (status, next steps, rejection, offer, gap, journey)
- [ ] Redis checkpointer for multi-turn conversations
- [ ] Distributed schema cache with lock
- [ ] Integration tests against candidate-mcp

---

## 9. Risk Mitigation

### 9.1 Identified Risks from Prototype

| Risk | Severity | Mitigation |
|------|----------|------------|
| **PII leakage in logs** | High | Structured logging with PII field blacklist; never log AgentContext |
| **Schema drift between Python and Java** | High | Use MCP static resources for schema propagation; monitor version mismatches |
| **SLA calculation inconsistency** | Medium | Centralize SLA thresholds in configuration; unit test transformer logic |
| **Interviewer ID exposure** | High | Transformer tests verify IDs are stripped |
| **Compensation exposure in negotiation** | High | Strip exact amounts until offer accepted |
| **Clock drift causing signature failures** | Medium | NTP synchronization across all pods; signature TTL tolerance |

---

## 10. Key Takeaways for Monday LLD Submission

### For Architecture Review

1. **Three-layer transformation is non-negotiable**: Cosmos → AgentContext → Query Filter → Response
2. **ApplicationGroups are essential**: Draft multi-job applications are real enterprise use cases
3. **Assessment code mapping must be standardized**: Skills gap analysis depends on it
4. **Shift details are first-class job attributes**: Not optional for operations/support roles
5. **Interview schedule PII handling is nuanced**: Strip IDs, retain names
6. **SLA tracking is a derived field**: Compute in transformer, don't store in Cosmos
7. **Offer negotiation tracking requires special care**: Hide internal notes, expose status only

### For Security Review

1. **PII stripping happens in Layer 1 (MCP)**: Never expose raw Cosmos docs to Python/LLM
2. **Comprehensive PII checklist validated**: SSN, DOB, addresses, emails, phones, compensation expectations, internal IDs
3. **Interviewer names safe, IDs are PII**: Transformer pattern verified
4. **Offer letter URLs are PII**: Document links stripped
5. **Questionnaire responses are PII**: Expose completion flag only, not raw answers

### For Platform Team

1. **Client abstraction layer works**: Easy to swap mock → WebClient in production
2. **Transformer layer is stateless**: No memory overhead, Spring @Component autowiring
3. **MCP configuration scales**: 21 tools with consistent patterns
4. **Contract testing (Pact) is critical**: Catch breaking changes before deployment
5. **Observability designed in**: Correlation ID propagation, structured logging, key metrics

---

## Appendix: File Structure for Production

```
careers-ai-service/                         (Python repository)
├── src/
│   ├── agent/
│   │   ├── v2_primary_assistant.py
│   │   ├── post_apply_assistant.py
│   │   └── tools/
│   │       └── mcp_tool_registry.py        (Schema cache + connection pool)
│   ├── api/
│   │   └── v2_routes.py
│   └── config/
│       └── settings.py

candidate-mcp/                              (Java repository)
├── src/main/java/com/example/mcpserver/
│   ├── dto/
│   │   ├── jobsync/                       (JobRequisitionDocument, ShiftDetails, etc.)
│   │   ├── cxapplications/                (ApplicationGroup, AtsApplication, etc.)
│   │   ├── talentprofile/                 (CandidateProfileV2, Preferences, etc.)
│   │   └── agentcontext/                  (JobAgentContext, ApplicationAgentContext, etc.)
│   ├── client/
│   │   ├── JobSyncClient.java
│   │   ├── CxApplicationsClient.java
│   │   └── TalentProfileClient.java
│   ├── transformer/
│   │   ├── AgentContextTransformer.java   (interface)
│   │   ├── JobTransformer.java
│   │   ├── ApplicationTransformer.java
│   │   └── ProfileTransformer.java
│   └── config/
│       └── CandidateMcpConfiguration.java (21 tools)

careers-data-schema/                        (Shared Maven library)
├── src/main/java/com/careers/schema/
│   ├── ApplicationGroup.java
│   ├── AtsApplication.java
│   ├── JobRequisition.java
│   └── CandidateProfile.java

cx-applications/                            (Downstream service)
├── GET /v1/applications/{id}
├── GET /v1/applications?candidateId={id}
├── GET /v1/application-groups/{id}
└── GET /v1/application-groups?candidateId={id}

talent-profile-service/                     (Downstream service)
└── GET /v1/candidates/{id}/profilev2

job-sync-service/                           (Downstream service)
├── GET /v1/jobs/{id}
└── GET /v1/jobs?status=OPEN
```

---

**End of Document**

**Next Steps**:
1. Incorporate these learnings into production LLD sections 5, 6, 7, 8, 12
2. Use prototype code as reference implementation for Java transformer layer
3. Plan schema migration timeline for careers-data-schema updates
4. Schedule architecture review with security team (PII handling)
5. Plan Pact contract test adoption across downstream services
