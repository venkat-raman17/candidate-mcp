# MCP Server Testing Guide

## Quick Start

### 1. Build and Run the Server

```bash
cd "C:\Users\Kuro Gaming\candidate-ai\candidate-mcp"

# Clean and build
mvn clean package -DskipTests

# Run the server
mvn spring-boot:run
```

**Expected output:**
```
Started McpServerApplication in X.XXX seconds
...
MCP Server running on port 8081
```

### 2. Verify Server Health

```bash
curl http://localhost:8081/actuator/health
```

**Expected response:**
```json
{"status":"UP"}
```

---

## Functional Use Case Tests

### Use Case 1: Draft Multi-Job Application

**Scenario**: Candidate C008 applied to 3 jobs via ApplicationGroup AG002

```bash
# Get the application group
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/call",
    "params": {
      "name": "getApplicationGroup",
      "arguments": {
        "groupId": "AG002"
      }
    }
  }'
```

**Expected**: ApplicationGroup with jobIds: [J002, J004, J005], status: SUBMITTED

```bash
# Get individual applications created from this group
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "getApplicationsByCandidate",
      "arguments": {
        "candidateId": "C008"
      }
    }
  }'
```

**Expected**: 3 applications (A008→J002, A009→J004, A010→J005)

**Validation**:
- ✅ ApplicationGroup correctly linked to 3 job IDs
- ✅ 3 AtsApplications created with correct job mappings
- ✅ No PII fields (internal ratings, recruiter IDs) in response

---

### Use Case 2: SLA Breach Detection

**Scenario**: Application A001 has been in FINAL_INTERVIEW for 28 days (SLA: 5 days)

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "getApplicationStatus",
      "arguments": {
        "applicationId": "A001"
      }
    }
  }'
```

**Expected response fields**:
```json
{
  "applicationId": "A001",
  "currentStage": "FINAL_INTERVIEW",
  "daysInCurrentStage": 28,
  "slaBreached": true,
  // NO assignedRecruiterId
  // NO internalRating
  // NO _cosmosPartitionKey
}
```

**Validation**:
- ✅ SLA calculation: daysInCurrentStage = 28
- ✅ SLA breach flag: slaBreached = true (28 > 5 day threshold)
- ✅ PII stripped: No recruiter IDs, no internal ratings

---

### Use Case 3: Interview Schedule with PII Stripping

**Scenario**: Get upcoming interviews for application A001

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "getScheduledEvents",
      "arguments": {
        "applicationId": "A001"
      }
    }
  }'
```

**Expected**: List of ScheduledEventSummary with:
- ✅ Event type, datetime, location, status
- ✅ Interviewer **names** (e.g., ["Sarah Chen", "Michael Roberts"])
- ❌ NO interviewer IDs

**Validation**:
- ✅ Interviewer names retained for candidate transparency
- ✅ Interviewer IDs stripped (PII protection)

---

### Use Case 4: Offer Negotiation Tracking

**Scenario**: Candidate C004 checking offer status (A004) with 2 negotiation rounds

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 5,
    "method": "tools/call",
    "params": {
      "name": "getApplicationStatus",
      "arguments": {
        "applicationId": "A004"
      }
    }
  }'
```

**Expected response fields**:
```json
{
  "applicationId": "A004",
  "status": "OFFER_EXTENDED",
  "offerSummary": {
    "offerExtendedAt": "...",
    "offerExpiresAt": "...",
    "status": "NEGOTIATING",
    "salaryRangeDisplay": "$160K-$210K"
    // NO exact negotiation internal notes
    // NO offerLetterUrl
  }
}
```

**Validation**:
- ✅ Offer status visible (NEGOTIATING)
- ✅ Expiration date available
- ❌ NO internal negotiation notes
- ❌ NO offer letter document URLs

---

### Use Case 5: Skills Gap with Assessment Code Mapping

**Scenario**: Candidate C003 checking requirements for Job J001

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 6,
    "method": "tools/call",
    "params": {
      "name": "getSkillsGap",
      "arguments": {
        "candidateId": "C003",
        "jobId": "J001"
      }
    }
  }'
```

**Expected response fields**:
```json
{
  "missingSkills": ["Java", "Spring Boot", "AWS", "Kubernetes"],
  "matchingSkills": ["TypeScript"],
  "matchPercentage": 20,
  "fitLevel": "WEAK_FIT",
  "requiredAssessmentCodes": ["JAVA_01", "SYS_DESIGN_02"],
  "candidateAssessmentCodes": [],
  "missingAssessments": ["JAVA_01", "SYS_DESIGN_02"]
}
```

**Validation**:
- ✅ Skill gap calculated correctly
- ✅ Required assessment codes from job (JAVA_01, SYS_DESIGN_02)
- ✅ Candidate's completed assessment codes (none for C003)
- ✅ Missing assessments identified

---

### Use Case 6: Shift Preference Matching

**Scenario**: Candidate C008 (open to night/rotating shifts) viewing Job J004 (night shift)

```bash
# Get candidate preferences
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 7,
    "method": "tools/call",
    "params": {
      "name": "getCandidatePreferences",
      "arguments": {
        "candidateId": "C008"
      }
    }
  }'
```

**Expected**:
```json
{
  "workStyle": {
    "acceptableShifts": ["DAY", "NIGHT", "ROTATING"]
  }
}
```

```bash
# Get job details
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 8,
    "method": "tools/call",
    "params": {
      "name": "getJob",
      "arguments": {
        "jobId": "J004"
      }
    }
  }'
```

**Expected**:
```json
{
  "jobId": "J004",
  "title": "Night Shift SRE",
  "shift": {
    "type": "NIGHT",
    "startTime": "22:00",
    "endTime": "06:00",
    "timeZone": "America/Chicago"
  }
  // NO costCenter
  // NO budgetCode
  // NO internalNotes
}
```

**Validation**:
- ✅ Candidate accepts NIGHT shifts
- ✅ Job requires NIGHT shift (22:00-06:00)
- ✅ Match identified
- ✅ Internal job fields (cost center, budget code) stripped

---

### Use Case 7: Profile PII Stripping

**Scenario**: Get candidate profile C001

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 9,
    "method": "tools/call",
    "params": {
      "name": "getCandidateProfile",
      "arguments": {
        "candidateId": "C001"
      }
    }
  }'
```

**Expected response has**:
- ✅ displayName, location (city/state), yearsOfExperience
- ✅ skills, education summary
- ✅ totalAssessmentsCompleted, averagePercentilesByType

**Expected response does NOT have**:
- ❌ nationalId, ssnLast4, dateOfBirth
- ❌ homeAddress, personalEmail, personalPhone
- ❌ emergencyContact
- ❌ compensation expectations (minBaseSalary, targetBaseSalary)
- ❌ raw questionnaire responses
- ❌ _cosmosPartitionKey, _etag

**Validation**:
- ✅ All PII fields stripped
- ✅ Professional information retained
- ✅ Computed fields present (assessment summaries)

---

### Use Case 8: Incomplete Profile Detection

**Scenario**: Candidate C006 has incomplete questionnaire (30%)

```bash
curl -X POST http://localhost:8081/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 10,
    "method": "tools/call",
    "params": {
      "name": "getCandidateProfile",
      "arguments": {
        "candidateId": "C006"
      }
    }
  }'
```

**Expected response fields**:
```json
{
  "candidateId": "C006",
  "questionnaireCompleted": false,
  "questionnaireCompletedAt": null
  // NO raw questionnaire responses
}
```

**Validation**:
- ✅ Questionnaire completion flag available
- ❌ NO raw responses (PII protection)
- ✅ Agent can detect incomplete profile

---

## PII Stripping Verification Checklist

### Job Data
- [x] costCenter removed
- [x] budgetCode removed
- [x] internalNotes removed
- [x] _cosmosPartitionKey removed
- [x] _etag removed

### Application Data
- [x] assignedRecruiterId removed
- [x] internalRating removed
- [x] interviewer IDs removed (names retained)
- [x] author IDs removed (names retained)
- [x] offerLetterUrl removed
- [x] internal negotiation notes removed
- [x] _cosmosPartitionKey removed
- [x] _etag removed

### Profile Data
- [x] nationalId removed
- [x] ssnLast4 removed
- [x] dateOfBirth removed
- [x] homeAddress removed (city/state retained)
- [x] personalEmail removed
- [x] personalPhone removed
- [x] emergencyContact removed
- [x] compensation expectations removed
- [x] raw questionnaire responses removed
- [x] _cosmosPartitionKey removed
- [x] _etag removed

---

## Tools Inventory (21 Total)

### Candidate Profile Tools (3)
1. ✅ `getCandidateProfile` - Returns ProfileAgentContext
2. ✅ `getJobsMatchingCandidate` - Uses transformed contexts
3. ✅ `searchOpenJobs` - Returns JobAgentContext list

### Application Intelligence Tools (10)
4. ✅ `getApplicationStatus` - Returns ApplicationAgentContext
5. ✅ `getApplicationsByCandidate` - Transformed list
6. ✅ `getCandidateJourney` - Enriched narrative
7. ✅ `getNextSteps` - Stage guidance
8. ✅ `getStageDuration` - Uses daysInCurrentStage
9. ✅ `getInterviewFeedback` - Uses publicNotes
10. ✅ `getApplicationGroup` - NEW: Draft multi-job applications
11. ✅ `getApplicationGroupsByCandidate` - NEW: All draft applications
12. ✅ `getCandidatePreferences` - NEW: Location, job, work style preferences
13. ✅ `getScheduledEvents` - NEW: Upcoming interview schedule

### Assessment Intelligence Tools (3)
14. ✅ `getAssessmentResults` - From profile
15. ✅ `getAssessmentByType` - Filtered from profile
16. ✅ `compareToPercentile` - Uses averagePercentilesByType

### Job Intelligence Tools (3)
17. ✅ `getJob` - Returns JobAgentContext
18. ✅ `listOpenJobs` - Transformed list
19. ✅ `getSkillsGap` - Enhanced with assessment codes

### ATS Knowledge Tools (2)
20. ✅ `getEntitySchema` - Static schemas
21. ✅ `getWorkflowTransitions` - State machine

---

## Performance Considerations

### Expected Response Times
- **Profile queries**: < 100ms (in-memory mock)
- **Application queries**: < 100ms (in-memory mock)
- **Job queries**: < 50ms (in-memory mock)
- **Journey/complex queries**: < 200ms (multiple lookups + transformation)

### Memory Usage
- **Mock data**: ~5MB in memory
- **JVM heap**: 512MB recommended minimum
- **Transformers**: Stateless, no memory overhead

---

## Next Steps After Testing

1. **Verify all 8 use cases** pass
2. **Check PII stripping** in all responses
3. **Measure response times** for performance baseline
4. **Document any issues** encountered
5. **Extract learnings** for production LLD

---

## Troubleshooting

### Server won't start
```bash
# Check Java version (need Java 21+)
java -version

# Check port 8081 availability
netstat -ano | findstr :8081

# View detailed startup logs
mvn spring-boot:run -X
```

### Tool call fails
- Verify request JSON format (jsonrpc 2.0)
- Check tool name spelling (case-sensitive)
- Validate required parameters provided
- Check server logs for exceptions

### Empty/null responses
- Verify entity IDs exist in mock data (C001-C008, J001-J005, A001-A010, AG001-AG003)
- Check transformer is returning non-null
- Verify client found the entity (check Optional handling)

---

## MCP Protocol Reference

### Tool Call Format
```json
{
  "jsonrpc": "2.0",
  "id": <request_id>,
  "method": "tools/call",
  "params": {
    "name": "<tool_name>",
    "arguments": {
      "<param1>": "<value1>",
      "<param2>": "<value2>"
    }
  }
}
```

### Success Response Format
```json
{
  "jsonrpc": "2.0",
  "id": <request_id>,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "<JSON response>"
      }
    ]
  }
}
```

### Error Response Format
```json
{
  "jsonrpc": "2.0",
  "id": <request_id>,
  "error": {
    "code": <error_code>,
    "message": "<error_message>"
  }
}
```
