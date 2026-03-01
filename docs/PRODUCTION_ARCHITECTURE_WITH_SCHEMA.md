# Production Architecture: careers-data-schema Integration

## Maven Dependency Structure

```
careers-data-schema (Shared Maven Library)
    ↓ (compile dependency)
    ├→ cx-applications
    ├→ talent-profile-service
    ├→ job-sync-service
    └→ candidate-mcp
```

**Key Principle**: `careers-data-schema` is the single source of truth for all domain models.

---

## What Lives Where

### careers-data-schema (Maven Library)

**Package**: `com.careers.schema`

**Contains**: All canonical Cosmos document models (raw domain entities)

```java
// careers-data-schema/src/main/java/com/careers/schema/

// Job Domain
public class JobRequisition {
    private String jobId;
    private String requisitionNumber;
    private String title;
    private String department;
    private String location;
    private JobType jobType;
    private JobStatus status;
    private String description;
    private RequirementSection requirements;
    private CompensationDetails compensation;
    private ShiftDetails shift;
    private AssessmentCodeMapping assessments;
    // ... internal fields
    private String costCenter;        // PII - internal
    private String budgetCode;        // PII - internal
    private String _cosmosPartitionKey;
    private String _etag;
}

// Application Domain
public class Application {
    private String applicationId;
    private String candidateId;
    private String jobId;
    private ApplicationStatus status;
    private ApplicationSource source;
    private LocalDateTime appliedAt;
    private List<WorkflowHistoryEntry> workflowHistory;
    private ScheduleMetadata schedule;
    private OfferMetadata offer;
    private List<RecruiterNote> notes;
    // ... internal fields
    private String assignedRecruiterId;  // PII - internal
    private String internalRating;       // PII - internal
    private String _cosmosPartitionKey;
    private String _etag;
}

public class ApplicationGroup {
    private String groupId;
    private String candidateId;
    private List<String> jobIds;        // MULTIPLE jobs
    private ApplicationGroupStatus status;
    private String sourcePlatform;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private Map<String, Object> draftData;
    private Integer completionPercentage;
}

// Profile Domain
public class CandidateProfile {
    private String candidateId;
    private BaseProfile baseProfile;
    private AssessmentResults assessments;
    private Preferences preferences;
    private QuestionnaireResponses questionnaires;
    // ... PII fields
    private String nationalId;           // PII
    private String ssnLast4;            // PII
    private LocalDate dateOfBirth;      // PII
    private String homeAddress;         // PII
    private String personalEmail;       // PII
    private String personalPhone;       // PII
    private String _cosmosPartitionKey;
    private String _etag;
}

// Supporting Types
public class WorkflowHistoryEntry { ... }
public class ScheduleMetadata { ... }
public class ScheduledEvent { ... }
public class OfferMetadata { ... }
public class CompensationOffer { ... }
public class NegotiationRound { ... }
public class RecruiterNote { ... }
public class ShiftDetails { ... }
public class AssessmentCodeMapping { ... }
public class BaseProfile { ... }
public class AssessmentResults { ... }
public class Preferences { ... }

// Enums
public enum ApplicationStatus { ... }
public enum ApplicationGroupStatus { ... }
public enum ApplicationSource { ... }
public enum JobType { ... }
public enum JobStatus { ... }
public enum AssessmentType { ... }
public enum OfferStatus { ... }
public enum EventType { ... }
public enum EventStatus { ... }
public enum ShiftType { ... }
public enum WorkMode { ... }
```

---

### Downstream Services (Return careers-data-schema models)

#### cx-applications

```java
// Uses careers-data-schema models directly

@GetMapping("/v1/applications/{id}")
public Application getApplication(@PathVariable String id) {
    // Returns careers-data-schema Application model
    return applicationRepository.findById(id);
}

@GetMapping("/v1/applications")
public List<Application> getApplicationsByCandidate(
        @RequestParam String candidateId) {
    return applicationRepository.findByCandidateId(candidateId);
}

@GetMapping("/v1/application-groups/{id}")
public ApplicationGroup getApplicationGroup(@PathVariable String id) {
    // Returns careers-data-schema ApplicationGroup model
    return applicationGroupRepository.findById(id);
}
```

#### talent-profile-service

```java
// Uses careers-data-schema models directly

@GetMapping("/v1/candidates/{id}/profilev2")
public CandidateProfile getProfileV2(@PathVariable String id) {
    // Returns careers-data-schema CandidateProfile model
    return profileRepository.findById(id);
}
```

#### job-sync-service

```java
// Uses careers-data-schema models directly

@GetMapping("/v1/jobs/{id}")
public JobRequisition getJob(@PathVariable String id) {
    // Returns careers-data-schema JobRequisition model
    return jobRepository.findById(id);
}

@GetMapping("/v1/jobs")
public List<JobRequisition> getActiveJobs() {
    return jobRepository.findByStatus(JobStatus.OPEN);
}
```

---

### candidate-mcp (Consumes careers-data-schema, Adds AgentContext DTOs)

**Maven Dependencies**:
```xml
<dependencies>
    <!-- Shared schema library -->
    <dependency>
        <groupId>com.careers</groupId>
        <artifactId>careers-data-schema</artifactId>
        <version>${careers-data-schema.version}</version>
    </dependency>

    <!-- Spring AI MCP -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-mcp-server-starter</artifactId>
    </dependency>
</dependencies>
```

**Package Structure**:
```
candidate-mcp/src/main/java/com/example/mcpserver/
├── dto/
│   └── agentcontext/              ← ONLY AgentContext DTOs (Layer 1 projections)
│       ├── JobAgentContext.java
│       ├── ApplicationAgentContext.java
│       ├── ProfileAgentContext.java
│       ├── WorkflowStageSummary.java
│       ├── ScheduledEventSummary.java
│       ├── OfferSummary.java
│       └── PublicRecruiterNote.java
│
├── client/
│   ├── JobSyncClient.java
│   ├── CxApplicationsClient.java
│   └── TalentProfileClient.java
│
├── transformer/
│   ├── AgentContextTransformer.java
│   ├── JobTransformer.java            ← JobRequisition → JobAgentContext
│   ├── ApplicationTransformer.java    ← Application → ApplicationAgentContext
│   └── ProfileTransformer.java        ← CandidateProfile → ProfileAgentContext
│
└── config/
    └── CandidateMcpConfiguration.java
```

**Import Structure**:
```java
// candidate-mcp transformer imports

// FROM careers-data-schema (raw models)
import com.careers.schema.JobRequisition;
import com.careers.schema.Application;
import com.careers.schema.ApplicationGroup;
import com.careers.schema.CandidateProfile;
import com.careers.schema.ApplicationStatus;
import com.careers.schema.AssessmentType;
import com.careers.schema.JobType;
import com.careers.schema.JobStatus;
import com.careers.schema.WorkflowHistoryEntry;
import com.careers.schema.ScheduledEvent;
import com.careers.schema.OfferMetadata;
import com.careers.schema.RecruiterNote;

// FROM candidate-mcp (AgentContext DTOs - Layer 1 projections)
import com.example.mcpserver.dto.agentcontext.JobAgentContext;
import com.example.mcpserver.dto.agentcontext.ApplicationAgentContext;
import com.example.mcpserver.dto.agentcontext.ProfileAgentContext;
import com.example.mcpserver.dto.agentcontext.WorkflowStageSummary;
import com.example.mcpserver.dto.agentcontext.ScheduledEventSummary;
import com.example.mcpserver.dto.agentcontext.OfferSummary;
import com.example.mcpserver.dto.agentcontext.PublicRecruiterNote;
```

---

## Transformer Layer (Layer 1: PII Stripping)

### JobTransformer

```java
@Component
public class JobTransformer implements AgentContextTransformer<JobRequisition, JobAgentContext> {

    @Override
    public JobAgentContext transform(JobRequisition source) {
        if (source == null) return null;

        // SOURCE: careers-data-schema JobRequisition
        // OUTPUT: candidate-mcp JobAgentContext

        return new JobAgentContext(
            // Retained fields
            source.getJobId(),
            source.getTitle(),
            source.getDepartment(),
            source.getLocation(),
            source.getJobType(),
            source.getStatus(),
            source.getDescription(),
            source.getRequirements().getRequiredSkills(),
            source.getRequirements().getPreferredSkills(),
            source.getRequirements().getMinYearsExperience(),

            // Computed fields
            formatSalaryRange(source.getCompensation()),

            // Retained (candidate-facing)
            source.getShift(),

            // Extracted
            source.getAssessments().getRequiredCodes(),

            source.getOpenedAt()

            // STRIPPED: costCenter, budgetCode, internalNotes, _cosmosPartitionKey, _etag
        );
    }

    private String formatSalaryRange(CompensationDetails compensation) {
        return String.format("$%,dK-$%,dK",
            compensation.getSalaryRangeMin() / 1000,
            compensation.getSalaryRangeMax() / 1000);
    }
}
```

### ApplicationTransformer

```java
@Component
public class ApplicationTransformer implements AgentContextTransformer<Application, ApplicationAgentContext> {

    private static final Map<ApplicationStatus, Integer> SLA_DAYS = Map.of(
        ApplicationStatus.RECEIVED, 2,
        ApplicationStatus.SCREENING, 5,
        ApplicationStatus.PHONE_INTERVIEW, 3,
        ApplicationStatus.TECHNICAL_INTERVIEW, 7,
        ApplicationStatus.FINAL_INTERVIEW, 5,
        ApplicationStatus.OFFER_EXTENDED, 5
    );

    @Override
    public ApplicationAgentContext transform(Application source) {
        if (source == null) return null;

        // SOURCE: careers-data-schema Application
        // OUTPUT: candidate-mcp ApplicationAgentContext

        ApplicationStatus currentStage = source.getStatus();
        int daysInStage = calculateDaysInCurrentStage(source);
        boolean slaBreached = isSlaBreached(currentStage, daysInStage);

        return new ApplicationAgentContext(
            source.getApplicationId(),
            source.getCandidateId(),
            source.getJobId(),
            source.getStatus(),
            source.getSource(),
            source.getAppliedAt(),

            // Computed
            currentStage.name(),
            daysInStage,
            slaBreached,

            // Transformed (PII stripped)
            transformWorkflowHistory(source.getWorkflowHistory()),
            transformScheduledEvents(source.getSchedule()),
            transformOfferMetadata(source.getOffer()),
            transformRecruiterNotes(source.getNotes())

            // STRIPPED: assignedRecruiterId, internalRating, interviewer IDs,
            //           author IDs, offerLetterUrl, internal negotiation notes,
            //           _cosmosPartitionKey, _etag
        );
    }

    private List<WorkflowStageSummary> transformWorkflowHistory(List<WorkflowHistoryEntry> history) {
        return history.stream()
            .map(entry -> new WorkflowStageSummary(
                entry.getToStatus().name(),
                entry.getTransitionedAt(),
                // Compute days in this stage
                (int) Duration.between(entry.getTransitionedAt(), Instant.now()).toDays()
                // STRIPPED: transitionedBy (ID), internal reason/notes
            ))
            .toList();
    }

    private List<ScheduledEventSummary> transformScheduledEvents(ScheduleMetadata schedule) {
        if (schedule == null || schedule.getEvents() == null) return List.of();

        return schedule.getEvents().stream()
            .filter(event -> event.getStatus() == EventStatus.SCHEDULED)
            .map(event -> new ScheduledEventSummary(
                event.getType(),
                event.getScheduledAt(),
                event.getDurationMinutes(),
                event.getLocation(),
                event.getInterviewerNames(),  // Names retained
                event.getStatus()
                // STRIPPED: interviewer IDs
            ))
            .toList();
    }

    private OfferSummary transformOfferMetadata(OfferMetadata offer) {
        if (offer == null) return null;

        return new OfferSummary(
            offer.getOfferExtendedAt(),
            offer.getOfferExpiresAt(),
            offer.getOfferStatus(),
            formatSalaryRange(offer.getCompensation()),
            offer.getCompensation().getStartDate()
            // STRIPPED: offerLetterUrl, exact negotiation history, internal notes
        );
    }
}
```

### ProfileTransformer

```java
@Component
public class ProfileTransformer implements AgentContextTransformer<CandidateProfile, ProfileAgentContext> {

    @Override
    public ProfileAgentContext transform(CandidateProfile source) {
        if (source == null) return null;

        // SOURCE: careers-data-schema CandidateProfile
        // OUTPUT: candidate-mcp ProfileAgentContext

        return new ProfileAgentContext(
            source.getCandidateId(),
            source.getBaseProfile().getDisplayName(),
            formatLocation(source.getBaseProfile().getLocation()),  // City, state only
            source.getBaseProfile().getYearsOfExperience(),
            source.getBaseProfile().getCurrentRole(),
            source.getBaseProfile().getCurrentCompany(),
            source.getBaseProfile().getEducation(),
            source.getBaseProfile().getSkills(),
            source.getBaseProfile().getStatus(),

            // Computed
            source.getAssessments().getResults().size(),
            calculateAveragePercentilesByType(source.getAssessments()),

            // Preferences (excluding compensation)
            source.getPreferences().getLocation(),
            source.getPreferences().getJob(),
            source.getPreferences().getWorkStyle(),

            // Questionnaire (completion flag only)
            source.getQuestionnaires() != null && source.getQuestionnaires().getCompletedAt() != null,
            source.getQuestionnaires() != null ? source.getQuestionnaires().getCompletedAt() : null

            // STRIPPED: nationalId, ssnLast4, dateOfBirth, homeAddress,
            //           personalEmail, personalPhone, emergencyContact,
            //           compensation expectations, raw questionnaire responses,
            //           _cosmosPartitionKey, _etag
        );
    }

    private String formatLocation(String fullAddress) {
        // Extract city, state from full address
        // Example: "123 Main St, San Francisco, CA 94102" → "San Francisco, CA"
        // STRIP street address
        return extractCityState(fullAddress);
    }

    private Map<AssessmentType, Integer> calculateAveragePercentilesByType(AssessmentResults results) {
        return results.getResults().stream()
            .collect(Collectors.groupingBy(
                AssessmentResult::getType,
                Collectors.averagingInt(AssessmentResult::getPercentile)
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().intValue()
            ));
    }
}
```

---

## MCP Static Resources (Schema Bridge)

### Resource: AgentContext Schemas

These resources describe the **Layer 1 output** (what AgentContext looks like after transformation), not the raw careers-data-schema models.

```java
@Bean
public List<McpStatelessServerFeatures.SyncResourceSpecification> staticResources() {
    return List.of(

        // Schema for JobAgentContext (Layer 1 projection)
        resource("ats://schema/job-agent-context",
            "Job AgentContext Schema",
            "Schema for the agent-safe job requisition projection (PII stripped). " +
            "This is what tools return after transformation, not the raw JobRequisition from careers-data-schema.",
            "application/json",
            (ctx, req) -> jsonResource(req.uri(), generateJobAgentContextSchema())),

        // Schema for ApplicationAgentContext (Layer 1 projection)
        resource("ats://schema/application-agent-context",
            "Application AgentContext Schema",
            "Schema for the agent-safe application projection (PII stripped). " +
            "Lists fields present in ApplicationAgentContext and explicitly documents stripped PII fields.",
            "application/json",
            (ctx, req) -> jsonResource(req.uri(), generateApplicationAgentContextSchema())),

        // Schema for ProfileAgentContext (Layer 1 projection)
        resource("ats://schema/profile-agent-context",
            "Profile AgentContext Schema",
            "Schema for the agent-safe candidate profile projection (PII stripped). " +
            "Documents ALL PII fields that are stripped from CandidateProfile before reaching the agent.",
            "application/json",
            (ctx, req) -> jsonResource(req.uri(), generateProfileAgentContextSchema())),

        // Existing resources (still relevant)
        resource("ats://workflow/application-states", ...),
        resource("ats://workflow/assessment-types", ...),
        resource("ats://enum/application-status", ...),
        resource("ats://enum/offer-status", ...)
    );
}

private Map<String, Object> generateJobAgentContextSchema() {
    return Map.of(
        "entity", "JobAgentContext",
        "source", "careers-data-schema JobRequisition (transformed)",
        "description", "Agent-safe job requisition projection",
        "fields", Map.of(
            "jobId", Map.of("type", "string", "required", true),
            "title", Map.of("type", "string", "required", true),
            // ... all AgentContext fields
            "requiredAssessmentCodes", Map.of("type", "array", "items", "string")
        ),
        "piiFieldsStripped", List.of(
            "costCenter (internal budget tracking)",
            "budgetCode (internal budget tracking)",
            "internalNotes (recruiter private notes)",
            "_cosmosPartitionKey (database metadata)",
            "_etag (concurrency control)"
        ),
        "computedFields", List.of(
            "salaryRangeDisplay - formatted from compensation.salaryRangeMin/Max",
            "requiredAssessmentCodes - extracted from assessments.requiredCodes"
        )
    );
}
```

---

## Build Process

### careers-data-schema Release

```bash
# careers-data-schema repository
mvn clean deploy

# Publishes to internal Maven repository
# Artifact: com.careers:careers-data-schema:1.5.0
```

### candidate-mcp Build

```bash
# candidate-mcp pom.xml specifies careers-data-schema version
<properties>
    <careers-data-schema.version>1.5.0</careers-data-schema.version>
</properties>

# Build pulls careers-data-schema from Maven repo
mvn clean package

# Transformers compile against careers-data-schema models
```

**Version Alignment**: When careers-data-schema releases a new version with breaking changes:
1. careers-data-schema bumps to 1.6.0
2. cx-applications, talent-profile-service, job-sync-service update to 1.6.0
3. candidate-mcp updates dependency to 1.6.0, updates transformers if needed
4. careers-ai-service fetches new schemas from MCP resources (automatic)

---

## Key Architectural Benefits

### 1. Single Source of Truth
- careers-data-schema defines models **once**
- All services (downstream + MCP) use same canonical definitions
- No schema drift between services

### 2. Versioned Schema Evolution
- careers-data-schema version bumps signal breaking changes
- All consumers (including candidate-mcp) update together
- MCP static resources reflect current schema version

### 3. Clean Separation
- **careers-data-schema**: Raw Cosmos models (PII included)
- **candidate-mcp AgentContext DTOs**: Projected models (PII stripped)
- **Python agent**: Consumes AgentContext, never sees raw models

### 4. Compile-Time Safety
- candidate-mcp transformers fail to compile if careers-data-schema changes fields
- Catches breaking changes at build time, not runtime

### 5. Schema Propagation
- careers-data-schema models → MCP static resources → Python agent system prompt
- Agent knows exact field names, types, enums from source of truth

---

## Prototype vs Production Mapping

| Prototype (What We Built) | Production (Real System) |
|---------------------------|--------------------------|
| Created all DTOs from scratch in candidate-mcp | Import raw models from careers-data-schema |
| `dto/jobsync/JobRequisitionDocument.java` | `careers-data-schema: JobRequisition.java` |
| `dto/cxapplications/AtsApplication.java` | `careers-data-schema: Application.java` |
| `dto/talentprofile/CandidateProfileV2.java` | `careers-data-schema: CandidateProfile.java` |
| `dto/agentcontext/*` (created by us) | **SAME** - candidate-mcp creates AgentContext DTOs |
| Transformer: DTO → AgentContext | Transformer: careers-data-schema model → AgentContext |
| Mock clients return our DTOs | Real clients return careers-data-schema models |

**What Stays the Same**:
- ✅ AgentContext DTOs (JobAgentContext, ApplicationAgentContext, ProfileAgentContext)
- ✅ Transformer layer architecture
- ✅ PII stripping logic
- ✅ SLA calculation
- ✅ MCP tool structure

**What Changes**:
- ❌ No need to create raw DTO layer (jobsync, cxapplications, talentprofile packages)
- ✅ Import from careers-data-schema instead
- ✅ Update transformer imports to use `com.careers.schema.*`

---

## Action Items for Production

### careers-data-schema Updates

- [ ] Add `ApplicationGroup` entity
- [ ] Add `shiftDetails` to `JobRequisition`
- [ ] Add `assessmentCodeMapping` to `JobRequisition`
- [ ] Add `scheduleMetadata` to `Application`
- [ ] Add `offerMetadata` to `Application`
- [ ] Add `acceptableShifts` to `WorkStylePreferences` (in `CandidateProfile`)
- [ ] Version bump to 1.6.0 (breaking changes)

### candidate-mcp Updates

- [ ] Add careers-data-schema dependency (version 1.6.0)
- [ ] Create `dto/agentcontext/` package (AgentContext DTOs only)
- [ ] **Delete** `dto/jobsync/`, `dto/cxapplications/`, `dto/talentprofile/` (use careers-data-schema instead)
- [ ] Update transformer imports to `com.careers.schema.*`
- [ ] Implement transformers (JobRequisition → JobAgentContext, etc.)
- [ ] Update MCP static resources to document AgentContext schemas
- [ ] Add PII stripping documentation to each resource

### Downstream Services

- [ ] cx-applications: Update to careers-data-schema 1.6.0
- [ ] talent-profile-service: Update to careers-data-schema 1.6.0
- [ ] job-sync-service: Update to careers-data-schema 1.6.0

---

**End of Document**
