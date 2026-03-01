# Enterprise Mock Data Design for candidate-mcp

## Purpose
This document describes the enterprise-grade mock data structure that simulates real microservice contracts from:
- **job-sync-service**: Job requisition details
- **cx-applications**: ApplicationsGroups (draft) and AtsApplications (active/closed)
- **talent-profile-service**: ProfileV2 with preferences and questionnaires

## Architecture Overview

```
Real Production Services          Mock Data in candidate-mcp
┌─────────────────────────┐      ┌──────────────────────────┐
│  job-sync-service       │      │  JobSyncClient (mock)    │
│  - GET /v1/jobs/{id}    │ ──>  │  - In-memory job store   │
│  - Job requisition data │      │  - Assessment codes      │
│  - Shift details        │      │  - Shift metadata        │
└─────────────────────────┘      └──────────────────────────┘

┌─────────────────────────┐      ┌──────────────────────────┐
│  cx-applications        │      │  CxApplicationsClient    │
│  - ApplicationsGroups   │ ──>  │  - In-memory stores      │
│  - AtsApplications      │      │  - Draft multi-job apps  │
│  - Workflow history     │      │  - Active/closed apps    │
│  - Schedule metadata    │      │  - Full workflow+offers  │
└─────────────────────────┘      └──────────────────────────┘

┌─────────────────────────┐      ┌──────────────────────────┐
│  talent-profile-service │      │  TalentProfileClient     │
│  - GET profilev2        │ ──>  │  - In-memory profiles    │
│  - Base profile         │      │  - Assessment results    │
│  - Preferences          │      │  - Preferences           │
│  - Questionnaires       │      │  - Questionnaire data    │
└─────────────────────────┘      └──────────────────────────┘

                                  ┌──────────────────────────┐
                                  │  AgentContextTransformer │
                                  │  (Layer 1 - PII strip)   │
                                  └──────────────────────────┘
```

## 1. job-sync-service Mock Data

### DTO Structure

```java
// Full Cosmos document shape (what job-sync-service returns)
record JobRequisitionDocument(
    String jobId,
    String requisitionNumber,
    String title,
    String department,
    String location,
    JobType jobType,
    JobStatus status,
    String description,
    RequirementSection requirements,
    CompensationDetails compensation,
    ShiftDetails shift,
    AssessmentCodeMapping assessments,
    String hiringManagerId,
    String hiringManagerName,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    Integer targetHeadcount,
    // Internal/PII fields (stripped in Layer 1)
    String costCenter,
    String budgetCode,
    String internalNotes,
    String _cosmosPartitionKey,
    String _etag
) {}

record RequirementSection(
    List<String> requiredSkills,
    List<String> preferredSkills,
    Integer minYearsExperience,
    EducationLevel minEducation
) {}

record CompensationDetails(
    String currency,
    Integer salaryRangeMin,
    Integer salaryRangeMax,
    BonusStructure bonus,
    List<String> benefits
) {}

record ShiftDetails(
    ShiftType type,              // DAY, NIGHT, SWING, ROTATING, FLEXIBLE
    String timeZone,
    String startTime,            // "09:00"
    String endTime,              // "17:00"
    List<DayOfWeek> workDays,
    Boolean remoteEligible,
    Integer onsiteDaysPerWeek
) {}

record AssessmentCodeMapping(
    List<String> requiredCodes,  // e.g. ["JAVA_01", "SYS_DESIGN_02", "BEHAVIORAL_01"]
    Map<String, String> codeDescriptions,
    Boolean allowExternalCerts
) {}

// Agent-safe projection (what MCP returns after Layer 1)
record JobAgentContext(
    String jobId,
    String title,
    String department,
    String location,
    JobType jobType,
    JobStatus status,
    String description,
    List<String> requiredSkills,
    List<String> preferredSkills,
    Integer minYearsExperience,
    String salaryRangeDisplay,   // "$140K-$180K"
    ShiftDetails shift,          // Retained for candidate transparency
    List<String> requiredAssessmentCodes,
    LocalDateTime openedAt
    // PII/internal fields stripped: costCenter, budgetCode, internalNotes, etag
) {}
```

### Mock Data (5 jobs)

| jobId | title | department | location | shift | assessments | status |
|-------|-------|------------|----------|-------|-------------|--------|
| J001 | Senior Software Engineer | Engineering | San Francisco, CA / Remote | FLEXIBLE (2 days onsite) | [JAVA_01, SYS_DESIGN_02] | OPEN |
| J002 | Machine Learning Engineer | Data Science | New York, NY / Hybrid | DAY (3 days onsite) | [PYTHON_ML_01, ML_SYSTEM_01] | OPEN |
| J003 | Platform Engineer | Infrastructure | Remote | FLEXIBLE (all remote) | [GOLANG_01, K8S_INFRA_01] | FILLED |
| J004 | Night Shift SRE | Operations | Austin, TX | NIGHT (22:00-06:00 CST) | [ONCALL_01, INCIDENT_MGMT_01] | OPEN |
| J005 | Rotating Shift Support Engineer | Customer Success | Seattle, WA | ROTATING (week-based) | [SUPPORT_01, COMMS_01] | OPEN |

---

## 2. cx-applications Mock Data

### DTO Structure

```java
// ==================== ApplicationsGroups (Draft Applications) ====================
// Represents draft applications where candidates can apply to multiple jobs in one flow
record ApplicationGroup(
    String groupId,
    String candidateId,
    List<String> jobIds,         // MULTIPLE job IDs
    ApplicationGroupStatus status,  // DRAFT, SUBMITTED, ABANDONED
    String sourcePlatform,       // "careers-portal", "linkedin-apply", "mobile-app"
    LocalDateTime createdAt,
    LocalDateTime submittedAt,
    Map<String, Object> draftData,  // Partial form data, resume, etc.
    Integer completionPercentage
) {}

enum ApplicationGroupStatus { DRAFT, SUBMITTED, ABANDONED }

// ==================== AtsApplications (Active/Closed Applications) ====================
// Represents individual job applications with full workflow
record AtsApplication(
    String applicationId,
    String candidateId,
    String jobId,               // SINGLE job ID
    ApplicationStatus status,
    ApplicationSource source,
    LocalDateTime appliedAt,
    LocalDateTime lastUpdatedAt,

    // Workflow History
    List<WorkflowHistoryEntry> workflowHistory,

    // Schedule Metadata
    ScheduleMetadata schedule,

    // Offer Metadata
    OfferMetadata offer,

    // Recruiter Notes
    List<RecruiterNote> notes,

    // Internal/PII fields (stripped in Layer 1)
    String assignedRecruiterId,
    String internalRating,
    String _cosmosPartitionKey,
    String _etag
) {}

record WorkflowHistoryEntry(
    ApplicationStatus fromStatus,
    ApplicationStatus toStatus,
    LocalDateTime transitionedAt,
    String transitionedBy,      // UserId
    String transitionedByName,
    String reason,
    String notes,
    Map<String, Object> metadata  // Stage-specific data
) {}

record ScheduleMetadata(
    List<ScheduledEvent> events,
    String calendarLink,
    String timezone
) {}

record ScheduledEvent(
    String eventId,
    EventType type,             // PHONE_SCREEN, TECH_INTERVIEW, FINAL_ROUND, OFFER_CALL
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    String location,            // "Zoom", "Office - Building A", etc.
    List<String> interviewerIds,
    List<String> interviewerNames,
    EventStatus status          // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
) {}

enum EventType { PHONE_SCREEN, TECH_INTERVIEW, ONSITE, FINAL_ROUND, OFFER_CALL, ORIENTATION }
enum EventStatus { SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED, NO_SHOW }

record OfferMetadata(
    String offerId,
    LocalDateTime offerExtendedAt,
    LocalDateTime offerExpiresAt,
    LocalDateTime candidateRespondedAt,
    OfferStatus offerStatus,    // PENDING, ACCEPTED, DECLINED, NEGOTIATING, EXPIRED
    CompensationOffer compensation,
    List<NegotiationRound> negotiationHistory,
    String offerLetterUrl       // Signed URL (not returned to agent - PII)
) {}

record CompensationOffer(
    Integer baseSalary,
    String currency,
    Integer signingBonus,
    Integer equityShares,
    String equityValue,
    List<String> benefits,
    LocalDate startDate
) {}

record NegotiationRound(
    Integer roundNumber,
    LocalDateTime requestedAt,
    String requestedBy,         // "Candidate" or recruiter name
    String requestType,         // "SALARY_INCREASE", "EQUITY_ADJUSTMENT", "START_DATE", "BENEFITS"
    String requestDetails,
    String response,
    LocalDateTime respondedAt
) {}

// Agent-safe projection (what MCP returns after Layer 1)
record ApplicationAgentContext(
    String applicationId,
    String candidateId,
    String jobId,
    ApplicationStatus status,
    ApplicationSource source,
    LocalDateTime appliedAt,

    // Workflow summary (PII-stripped)
    String currentStage,
    Integer daysInCurrentStage,
    Boolean slaBreached,
    List<WorkflowStageSummary> workflowSummary,  // Dates + stages only

    // Schedule (interviewer names retained, IDs stripped)
    List<ScheduledEventSummary> upcomingEvents,

    // Offer (base details, no internal negotiation notes)
    OfferSummary offerSummary,

    // Recruiter notes (content retained, author IDs stripped)
    List<PublicRecruiterNote> publicNotes

    // Stripped: assignedRecruiterId, internalRating, etag, offerLetterUrl
) {}

record WorkflowStageSummary(
    String stage,
    LocalDateTime enteredAt,
    Integer daysInStage
    // Stripped: transitionedBy (ID), internal reason/notes
) {}

record ScheduledEventSummary(
    EventType type,
    LocalDateTime scheduledAt,
    Integer durationMinutes,
    String location,
    List<String> interviewerNames,  // Names retained, IDs stripped
    EventStatus status
) {}

record OfferSummary(
    LocalDateTime offerExtendedAt,
    LocalDateTime offerExpiresAt,
    OfferStatus status,
    String salaryRangeDisplay,  // Displayed as range, not exact (unless accepted)
    LocalDate startDate
    // Stripped: exact negotiation history, offerLetterUrl, equity details (until accepted)
) {}

record PublicRecruiterNote(
    LocalDateTime createdAt,
    String note,
    String authorName
    // Stripped: authorId, internal flags
) {}
```

### Mock Data

#### ApplicationGroups (3 draft applications)

| groupId | candidateId | jobIds | status | completionPercentage |
|---------|-------------|--------|--------|----------------------|
| AG001 | C007 | [J001, J003] | DRAFT | 60% (resume uploaded, profile incomplete) |
| AG002 | C008 | [J002, J004, J005] | SUBMITTED | 100% (submitted to 3 jobs) |
| AG003 | C006 | [J001] | ABANDONED | 30% (started, never completed) |

#### AtsApplications (10 applications with full workflow)

| applicationId | candidateId | jobId | status | workflow entries | scheduled events | offer? |
|---------------|-------------|-------|--------|------------------|------------------|--------|
| A001 | C001 | J001 | FINAL_INTERVIEW | 6 entries | 4 completed, 1 upcoming final round | No |
| A002 | C002 | J002 | PHONE_INTERVIEW | 3 entries | 1 completed phone screen, 1 upcoming tech | No |
| A003 | C003 | J001 | SCREENING | 2 entries | 1 upcoming phone screen | No |
| A004 | C004 | J001 | OFFER_EXTENDED | 7 entries | 5 completed (all interviews done) | Yes (pending, 2 negotiation rounds) |
| A005 | C005 | J003 | HIRED | 8 entries | 6 completed, 1 orientation scheduled | Yes (accepted) |
| A006 | C001 | J003 | REJECTED | 4 entries (rejected after tech interview) | 2 completed, 1 cancelled | No |
| A007 | C006 | J001 | TECHNICAL_INTERVIEW | 5 entries | 2 completed, 1 upcoming onsite | No |
| A008 | C008 | J002 | SCREENING | 2 entries (from ApplicationGroup AG002) | None yet | No |
| A009 | C008 | J004 | PHONE_INTERVIEW | 3 entries (from AG002) | 1 completed | No |
| A010 | C008 | J005 | REJECTED | 3 entries (from AG002 - rejected early) | 1 completed phone, failed | No |

---

## 3. talent-profile-service Mock Data

### DTO Structure

```java
// Full ProfileV2 document (what talent-profile-service returns)
record CandidateProfileV2(
    String candidateId,
    BaseProfile baseProfile,
    AssessmentResults assessments,
    Preferences preferences,
    QuestionnaireResponses questionnaires,

    // Internal/PII fields (stripped in Layer 1)
    String nationalId,
    String ssnLast4,
    LocalDate dateOfBirth,
    String homeAddress,
    String personalEmail,
    String personalPhone,
    String emergencyContact,
    BankingDetails bankingInfo,
    String _cosmosPartitionKey,
    String _etag
) {}

record BaseProfile(
    String displayName,
    String professionalEmail,   // Company email (if internal candidate), retained
    String linkedinUrl,
    String location,            // City, State only
    Integer yearsOfExperience,
    String currentRole,
    String currentCompany,
    EducationSummary education,
    List<SkillEndorsement> skills,
    CandidateStatus status
) {}

record EducationSummary(
    EducationLevel highestDegree,
    String major,
    String institution,
    Integer graduationYear
    // PII stripped: transcripts, GPA (unless candidate opts in)
) {}

record SkillEndorsement(
    String skill,
    SkillLevel level,           // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    Integer yearsOfExperience,
    Boolean isCertified,
    String certificationName
) {}

enum SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED, EXPERT }
enum EducationLevel { HIGH_SCHOOL, ASSOCIATE, BACHELOR, MASTER, DOCTORATE }

record AssessmentResults(
    List<AssessmentResult> results,
    Map<String, PercentileScore> percentilesByType
) {}

record AssessmentResult(
    String assessmentId,
    String assessmentCode,      // Maps to job-sync-service assessment requirements
    AssessmentType type,
    Integer score,
    Integer maxScore,
    Integer percentile,
    LocalDateTime completedAt,
    Boolean passed,
    String summary
    // PII stripped: raw answers, scorer notes, proctoring logs
) {}

record PercentileScore(
    AssessmentType type,
    Double averagePercentile,
    Integer sampleSize
) {}

record Preferences(
    LocationPreferences location,
    JobPreferences job,
    CompensationExpectations compensation,
    WorkStylePreferences workStyle
) {}

record LocationPreferences(
    List<String> preferredCities,
    List<String> preferredStates,
    Boolean openToRelocation,
    Boolean requiresVisaSponsorship,
    Integer maxCommuteMinutes
) {}

record JobPreferences(
    List<String> preferredRoles,
    List<String> preferredDepartments,
    List<JobType> preferredJobTypes,
    Boolean openToContract,
    Boolean openToInternship,
    LocalDate earliestStartDate
) {}

record CompensationExpectations(
    String currency,
    Integer minBaseSalary,
    Integer targetBaseSalary,
    Boolean requiresEquity,
    Boolean requiresBonus,
    List<String> mustHaveBenefits
    // PII: current salary (stripped)
) {}

record WorkStylePreferences(
    WorkMode preferredWorkMode,  // REMOTE, HYBRID, ONSITE
    Integer preferredOnsiteDays,
    List<ShiftType> acceptableShifts,
    Boolean willingToWorkWeekends,
    Boolean willingToBeOnCall
) {}

enum WorkMode { REMOTE, HYBRID, ONSITE, FLEXIBLE }

record QuestionnaireResponses(
    String questionnaireVersion,
    LocalDateTime completedAt,
    List<QuestionResponse> responses
) {}

record QuestionResponse(
    String questionId,
    String questionText,
    String responseType,        // SINGLE_CHOICE, MULTI_CHOICE, TEXT, SCALE
    Object response,            // String, List<String>, Integer, etc.
    Map<String, Object> metadata
) {}

// Agent-safe projection (what MCP returns after Layer 1)
record ProfileAgentContext(
    String candidateId,
    String displayName,
    String location,
    Integer yearsOfExperience,
    String currentRole,
    String currentCompany,
    EducationSummary education,
    List<SkillEndorsement> skills,
    CandidateStatus status,

    // Assessment summary (no raw data)
    Integer totalAssessmentsCompleted,
    Map<AssessmentType, Integer> averagePercentilesByType,

    // Preferences (retained for matching)
    LocationPreferences locationPreferences,
    JobPreferences jobPreferences,
    WorkStylePreferences workStylePreferences,

    // Questionnaire summary (not raw responses)
    Boolean questionnaireCompleted,
    LocalDateTime questionnaireCompletedAt

    // Stripped: all PII (nationalId, DOB, addresses, phone, email, compensation expectations)
) {}
```

### Mock Data (8 candidates with full profiles)

| candidateId | name | experience | skills | assessments | preferences | questionnaire |
|-------------|------|------------|--------|-------------|-------------|---------------|
| C001 | Alice Johnson | 8 yrs | Java, Spring Boot, AWS, K8s (EXPERT) | 3 completed (avg 81st percentile) | Remote/Hybrid, San Francisco area | Complete |
| C002 | Bob Smith | 5 yrs | Python, ML, TensorFlow (ADVANCED) | 2 completed (94th percentile) | Hybrid, New York area | Complete |
| C003 | Carol Williams | 6 yrs | React, TypeScript, Node.js (ADVANCED) | 1 completed (65th percentile) | Remote only, open to relocate | Complete |
| C004 | David Brown | 12 yrs | Java, Microservices, Kafka (EXPERT) | 4 completed (avg 97th percentile) | Flexible, Austin area | Complete |
| C005 | Emma Davis | 7 yrs | Go, Rust, K8s, Terraform (EXPERT) | 3 completed (avg 95th percentile) | Remote only, Seattle area | Complete |
| C006 | Frank Lee | 4 yrs | Java, Spring Boot, Kafka (INTERMEDIATE) | 2 completed (72nd percentile) | Onsite preferred, San Francisco | Incomplete (30%) |
| C007 | Grace Chen | 3 yrs | React, Node.js, AWS (INTERMEDIATE) | None yet | Remote/Hybrid, open to all | Draft in progress |
| C008 | Henry Kim | 6 yrs | Python, Django, PostgreSQL (ADVANCED) | 1 completed (88th percentile) | Open to night/rotating shifts, Austin | Complete |

---

## 4. Functional Use Cases Covered

### Use Case 1: Draft Multi-Job Application
**Scenario**: Candidate C008 applies to 3 jobs (J002, J004, J005) in one session via ApplicationGroup AG002.
- **ApplicationGroup AG002**: status SUBMITTED, jobIds [J002, J004, J005]
- **Result**: 3 AtsApplications created (A008, A009, A010) linking to each job
- **Agent Query**: "What jobs did I apply to?" → Returns all 3 applications with job details

### Use Case 2: Workflow History with Schedule
**Scenario**: Candidate C001 asks "What's next in my application A001?"
- **Workflow**: 6 stages completed, currently in FINAL_INTERVIEW
- **Schedule**: 4 past events (phone, 2× tech interviews, panel), 1 upcoming final round
- **Agent Response**: "You have a final round interview scheduled for [date] with [names]. You've completed 4 interviews so far."

### Use Case 3: Offer with Negotiation History
**Scenario**: Candidate C004 asks about offer status (A004)
- **Offer**: Extended 5 days ago, expires in 9 days, status NEGOTIATING
- **Negotiation**: 2 rounds (salary increase request → approved; start date shift → pending)
- **Agent Response**: "Your offer was extended on [date]. You've requested a salary adjustment (approved) and a later start date (pending response). Offer expires in 9 days."

### Use Case 4: Skills Gap with Assessment Mapping
**Scenario**: Candidate C003 asks "What do I need for job J001?"
- **Job J001 Requirements**: requiredAssessmentCodes = [JAVA_01, SYS_DESIGN_02]
- **Candidate C003 Profile**: Only has [REACT_FE_01] completed
- **Agent Response**: "Job J001 requires Java and System Design assessments. You've completed a frontend assessment. Consider taking the Java coding challenge and system design interview."

### Use Case 5: Shift Preference Matching
**Scenario**: Candidate C008 (willing to work night/rotating shifts) asks about J004
- **Job J004**: Night shift (22:00-06:00 CST)
- **Candidate Preferences**: acceptableShifts = [DAY, NIGHT, ROTATING]
- **Agent Response**: "This role requires night shift (10pm-6am CST). Your profile indicates you're open to night shifts. This is a good match."

### Use Case 6: Location Preference Matching
**Scenario**: Candidate C005 (remote only, Seattle area) views J003
- **Job J003**: Remote, full flexibility
- **Candidate Preferences**: preferredWorkMode = REMOTE, preferredCities = ["Seattle"]
- **Agent Response**: "This is a fully remote role, matching your preference. No onsite requirements."

### Use Case 7: Questionnaire-Based Routing
**Scenario**: Candidate C007 has incomplete questionnaire (30%)
- **Post-Apply Assistant**: Checks questionnaireCompleted = false
- **Agent Response**: "Your application is in draft. Complete your profile questionnaire to submit."

### Use Case 8: SLA Breach Detection
**Scenario**: Application A001 has been in FINAL_INTERVIEW for 15 days (SLA: 5 days)
- **Workflow**: daysInCurrentStage = 15, slaBreached = true
- **Agent Response**: "Your application is in final interview stage (15 days). This is taking longer than usual. We'll follow up with the hiring team."

---

## 5. Layer 1 Transformation Rules (PII Stripping)

### Always Stripped (Never in AgentContext)
- National ID / SSN / Tax IDs
- Exact date of birth (age range retained if needed)
- Home address (city/state retained)
- Personal phone number
- Personal email (professional email retained if relevant)
- Emergency contact details
- Banking/financial information
- Internal recruiter IDs (names retained)
- Cosmos metadata (_etag, _ts, partition keys)
- Cost center, budget codes, internal ratings
- Offer letter URLs (document links)
- Raw assessment answers, scorer notes, proctoring logs
- Exact current salary (expectations/ranges stripped)
- Internal notes with sensitive content

### Retained (Safe for Agent)
- Candidate display name
- Professional email (if work-related)
- Location (city, state)
- Years of experience
- Skills and certifications
- Education summary (degree, major, institution, year)
- Job preferences (roles, departments, work mode)
- Assessment scores and percentiles
- Workflow stage history (dates + stages)
- Scheduled event details (interviewer names, dates, location type)
- Offer status and expiration (not internal negotiation notes)
- Recruiter notes (content, author name - not internal flags)

---

## 6. Directory Structure

```
candidate-mcp/src/main/java/com/example/mcpserver/
├── dto/
│   ├── jobsync/
│   │   ├── JobRequisitionDocument.java
│   │   ├── ShiftDetails.java
│   │   ├── AssessmentCodeMapping.java
│   │   ├── CompensationDetails.java
│   │   └── JobAgentContext.java            (Layer 1 output)
│   │
│   ├── cxapplications/
│   │   ├── ApplicationGroup.java
│   │   ├── AtsApplication.java
│   │   ├── WorkflowHistoryEntry.java
│   │   ├── ScheduleMetadata.java
│   │   ├── OfferMetadata.java
│   │   ├── NegotiationRound.java
│   │   └── ApplicationAgentContext.java    (Layer 1 output)
│   │
│   ├── talentprofile/
│   │   ├── CandidateProfileV2.java
│   │   ├── BaseProfile.java
│   │   ├── AssessmentResults.java
│   │   ├── Preferences.java
│   │   ├── QuestionnaireResponses.java
│   │   └── ProfileAgentContext.java        (Layer 1 output)
│   │
│   └── common/
│       ├── SkillEndorsement.java
│       ├── EducationSummary.java
│       └── enums/
│           ├── ShiftType.java
│           ├── WorkMode.java
│           ├── OfferStatus.java
│           ├── EventType.java
│           └── EventStatus.java
│
├── client/
│   ├── JobSyncClient.java                  (Interface + mock impl)
│   ├── CxApplicationsClient.java           (Interface + mock impl)
│   └── TalentProfileClient.java            (Interface + mock impl)
│
├── transformer/
│   ├── AgentContextTransformer.java        (Layer 1 - PII stripping)
│   ├── JobTransformer.java
│   ├── ApplicationTransformer.java
│   └── ProfileTransformer.java
│
├── service/
│   ├── JobSyncService.java                 (In-memory mock data store)
│   ├── CxApplicationsService.java          (In-memory mock data store)
│   └── TalentProfileService.java           (In-memory mock data store)
│
└── tool/
    ├── ProfileTools.java                   (Uses TalentProfileClient + transformer)
    ├── ApplicationTools.java               (Uses CxApplicationsClient + transformer)
    ├── JobTools.java                       (Uses JobSyncClient + transformer)
    └── AssessmentTools.java                (Uses TalentProfileClient + transformer)
```

---

## 7. Implementation Phases

### Phase 1: DTO Creation (Current)
- Create all Java record DTOs for 3 services
- Define enums for shift types, work modes, offer statuses, event types
- Define AgentContext projections

### Phase 2: Mock Data Population
- Build comprehensive in-memory data stores in service layer
- 8 candidates with full ProfileV2
- 5 jobs with assessment codes and shift details
- 3 ApplicationGroups (draft multi-job applications)
- 10 AtsApplications with workflow, schedule, offer metadata

### Phase 3: Transformer Implementation
- Implement AgentContextTransformer interface
- Job transformer: strip cost center, budget code
- Application transformer: strip recruiter IDs, internal ratings, offer letter URLs
- Profile transformer: strip all PII (nationalId, DOB, addresses, phone, banking)

### Phase 4: Client Layer
- Define client interfaces (JobSyncClient, CxApplicationsClient, TalentProfileClient)
- Implement mock clients that read from in-memory services
- Production evolution: replace mock impl with WebClient + REST calls

### Phase 5: Tool Updates
- Update all MCP tool handlers to use new DTOs
- Integrate transformer layer in every tool response
- Update MCP static resources to reflect new schemas

### Phase 6: LLD Update
- Document prototype learnings
- Add flow diagrams with real data shapes
- Update caching strategy based on new data structure
- Add observability recommendations based on prototype testing

---

## 8. Key Design Decisions

### Why ApplicationGroups + AtsApplications?
- **Real-world pattern**: Candidates often apply to multiple jobs in one session via a careers portal
- **Draft state**: ApplicationGroups capture partial applications before submission
- **Atomicity**: Each AtsApplication is atomic with its own workflow, but grouped by source

### Why Full Workflow History?
- **Transparency**: Candidates need to see their full journey
- **SLA Tracking**: daysInStage calculated from workflow entries
- **Audit Trail**: Every status transition has timestamp, actor, reason

### Why Schedule Metadata?
- **Candidate Experience**: "What's my next interview?" is a top query
- **Interviewer Context**: Candidate sees who they're meeting with (names, not IDs)
- **Rescheduling**: EventStatus tracks cancellations and reschedules

### Why Offer Metadata?
- **Negotiation Transparency**: Candidates track offer status and expiration
- **Compensation Clarity**: Offer details visible once extended
- **Decision Support**: Agent can help with "Should I accept?" queries using offer data

### Why Questionnaire Responses?
- **Profile Completion**: Many companies gate application submission on questionnaire completion
- **Custom Questions**: Diversity, veteran status, referral source, etc.
- **Routing Logic**: Some questions drive automatic routing (e.g., visa sponsorship)

---

## Next Steps
1. Implement all DTOs in `candidate-mcp/src/main/java/com/example/mcpserver/dto/`
2. Create mock data stores in service layer with 20+ realistic records
3. Build transformer layer with comprehensive PII stripping
4. Update tool handlers to use new architecture
5. Test all functional use cases
6. Document learnings in LLD
