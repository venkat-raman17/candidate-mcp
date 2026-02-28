package com.example.mcpserver.config;

import com.example.mcpserver.model.*;
import com.example.mcpserver.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

/**
 * ATS Candidate Domain — MCP Subject Matter Expert
 *
 * <p>Designed for three consumer types:
 * <ul>
 *   <li><b>Candidate Agents</b> — AI acting on behalf of a specific candidate:
 *       profile lookup, application status, next steps, feedback, job matching.</li>
 *   <li><b>Developer Assistants</b> — building on or integrating with the ATS:
 *       live entity schemas, workflow state machine, assessment catalog.</li>
 *   <li><b>Operational Chatbots</b> — HR ops / HRBP internal tooling:
 *       candidate journeys, stuck applications, pipeline narratives.</li>
 * </ul>
 *
 * <p>Transport context headers consumed:
 * <ul>
 *   <li>{@code X-Candidate-ID}  — candidate agent: who the agent is acting for</li>
 *   <li>{@code X-Environment}   — dev assistant: dev / staging / prod</li>
 *   <li>{@code X-Team}          — ops chatbot: requesting team name</li>
 *   <li>{@code X-Correlation-ID}— distributed trace propagation</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class CandidateMcpConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CandidateMcpConfiguration.class);

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // =========================================================================
    // TOOL ANNOTATION PRESETS
    // =========================================================================

    private static final McpSchema.ToolAnnotations READ_ONLY =
            new McpSchema.ToolAnnotations(null, true, false, true, false, false);

    private static final McpSchema.ToolAnnotations READ_OPEN =
            new McpSchema.ToolAnnotations(null, true, false, false, true, false);

    // =========================================================================
    // DOMAIN KNOWLEDGE CONSTANTS
    // =========================================================================

    private static final Map<String, Object> WORKFLOW_TRANSITIONS = buildWorkflow();
    private static final Map<String, Object> ENTITY_SCHEMAS       = buildEntitySchemas();

    /** Expected SLA in business days per pipeline stage. */
    private static final Map<ApplicationStatus, Integer> STAGE_SLA_DAYS = Map.of(
            ApplicationStatus.RECEIVED,             2,
            ApplicationStatus.SCREENING,            5,
            ApplicationStatus.PHONE_INTERVIEW,      3,
            ApplicationStatus.TECHNICAL_INTERVIEW,  7,
            ApplicationStatus.FINAL_INTERVIEW,      5,
            ApplicationStatus.OFFER_EXTENDED,       5
    );

    /** Candidate-facing guidance for each pipeline stage. */
    private static final Map<ApplicationStatus, Map<String, Object>> STAGE_GUIDANCE = Map.of(
            ApplicationStatus.RECEIVED, Map.of(
                    "what_is_happening", "Your application has been submitted and is queued for recruiter review.",
                    "candidate_action",  "No action needed — sit tight.",
                    "typical_wait",      "3–5 business days",
                    "possible_next",     List.of("Move to Screening", "Rejection if not a profile fit")),
            ApplicationStatus.SCREENING, Map.of(
                    "what_is_happening", "A recruiter is reviewing your profile against the job requirements.",
                    "candidate_action",  "Ensure your LinkedIn and profile are current. You may receive an email to schedule a call.",
                    "typical_wait",      "5–7 business days",
                    "possible_next",     List.of("Phone Interview invitation", "Rejection with feedback")),
            ApplicationStatus.PHONE_INTERVIEW, Map.of(
                    "what_is_happening", "You are in or have completed a phone screen with the recruiter.",
                    "candidate_action",  "Prepare a concise pitch: role motivation, key highlights, availability.",
                    "typical_wait",      "3–5 business days after call",
                    "possible_next",     List.of("Technical Interview / Assessment", "Rejection")),
            ApplicationStatus.TECHNICAL_INTERVIEW, Map.of(
                    "what_is_happening", "You are in the technical evaluation phase (coding challenge, system design, or take-home).",
                    "candidate_action",  "Review fundamentals relevant to the job's required skills. Practice on the assessment platform if a link was sent.",
                    "typical_wait",      "7–10 business days",
                    "possible_next",     List.of("Final Interview with hiring team", "Rejection with technical feedback")),
            ApplicationStatus.FINAL_INTERVIEW, Map.of(
                    "what_is_happening", "You are meeting the hiring team, engineering leadership, or executive stakeholders.",
                    "candidate_action",  "Prepare STAR stories, questions about the team/roadmap, and salary expectations.",
                    "typical_wait",      "5–7 business days",
                    "possible_next",     List.of("Offer Extended", "Rejection")),
            ApplicationStatus.OFFER_EXTENDED, Map.of(
                    "what_is_happening", "A formal offer has been made. The ball is in your court.",
                    "candidate_action",  "Review compensation, equity, benefits. Respond within the deadline (usually 5 business days).",
                    "typical_wait",      "Your decision",
                    "possible_next",     List.of("Accept → Hired", "Decline → Process closes")),
            ApplicationStatus.HIRED, Map.of(
                    "what_is_happening", "Congratulations — offer accepted and position filled.",
                    "candidate_action",  "Complete onboarding paperwork and prepare for your start date.",
                    "typical_wait",      "N/A",
                    "possible_next",     List.of()),
            ApplicationStatus.REJECTED, Map.of(
                    "what_is_happening", "Your application was not selected to move forward at this time.",
                    "candidate_action",  "Request feedback if not provided. Continue building skills. You may reapply after 6 months.",
                    "typical_wait",      "N/A",
                    "possible_next",     List.of("Reapply after 6 months", "Apply to a different role"))
    );

    // =========================================================================
    // TOOLS — 17 tools across 4 domains
    // =========================================================================

    @Bean
    public List<McpStatelessServerFeatures.SyncToolSpecification> allTools(
            CandidateService candidates,
            ApplicationService applications,
            AssessmentService assessments,
            JobService jobs) {
        return Stream.of(
                candidateProfileTools(candidates, jobs),
                applicationIntelligenceTools(applications, candidates, jobs, assessments),
                assessmentIntelligenceTools(assessments),
                jobIntelligenceTools(jobs, candidates),
                atsKnowledgeTools()
        ).flatMap(List::stream).toList();
    }

    // ------------------------------------------------------------------ candidate profile & discovery

    private List<McpStatelessServerFeatures.SyncToolSpecification> candidateProfileTools(
            CandidateService candidates, JobService jobs) {
        return List.of(

            tool("getCandidateProfile",
                "Get Candidate Profile",
                "Retrieve a candidate's complete ATS profile: contact details, skills, experience, " +
                "current role, status, and LinkedIn. Use when an agent needs to describe who this person is.",
                READ_ONLY,
                schema(Map.of("candidateId", prop("string", "Candidate ID, e.g. C001")),
                       List.of("candidateId")),
                (ctx, req) -> {
                    String id = str(req, "candidateId");
                    logCtx(ctx, "getCandidateProfile", id);
                    return candidates.findById(id)
                            .map(c -> ok(toJson(c), c))
                            .orElse(err("Candidate not found: " + id));
                }),

            tool("getJobsMatchingCandidate",
                "Get Jobs Matching Candidate",
                "Inverted match: given a candidate, find all open jobs that fit their skills and experience. " +
                "Returns jobs ranked by match score. Ideal for 'what roles should I apply to?' queries.",
                READ_OPEN,
                schema(Map.of(
                    "candidateId", prop("string", "Candidate ID"),
                    "minScore",    prop("integer", "Minimum match score 0-100, default 40")),
                    List.of("candidateId")),
                (ctx, req) -> {
                    String cid = str(req, "candidateId");
                    int minScore = intArg(req, "minScore", 40);
                    logCtx(ctx, "getJobsMatchingCandidate", cid);
                    return candidates.findById(cid).map(c -> {
                        List<Map<String, Object>> matches = jobs.findMatchingJobs(c, minScore);
                        if (matches.isEmpty()) return ok("No open jobs meet the minimum match score of " + minScore + " for candidate " + cid);
                        return ok(toJson(matches), matches);
                    }).orElse(err("Candidate not found: " + cid));
                }),

            tool("searchOpenJobs",
                "Search Open Jobs",
                "Search currently open job requisitions. Useful for a candidate agent discovering " +
                "opportunities based on skills or location preference.",
                READ_OPEN,
                schema(Map.of(
                    "skills",     Map.of("type", "array", "items", Map.of("type", "string"),
                                        "description", "Required skills to filter by"),
                    "location",   prop("string", "Location keyword, e.g. 'Remote', 'New York'"),
                    "department", prop("string", "Department name filter")),
                    List.of()),
                (ctx, req) -> {
                    logCtx(ctx, "searchOpenJobs", null);
                    @SuppressWarnings("unchecked")
                    List<String> skills = req.arguments().get("skills") instanceof List<?> l
                            ? (List<String>) l : List.of();
                    String location   = str(req, "location");
                    String department = str(req, "department");
                    List<JobRequisition> result = jobs.findActive().stream()
                            .filter(j -> skills.isEmpty() || skills.stream().anyMatch(s ->
                                    j.requiredSkills().stream().anyMatch(rs -> rs.equalsIgnoreCase(s))
                                    || j.preferredSkills().stream().anyMatch(ps -> ps.equalsIgnoreCase(s))))
                            .filter(j -> location == null || j.location().toLowerCase().contains(location.toLowerCase()))
                            .filter(j -> department == null || j.department().equalsIgnoreCase(department))
                            .toList();
                    return ok(toJson(result), result);
                })
        );
    }

    // ------------------------------------------------------------------ application intelligence

    private List<McpStatelessServerFeatures.SyncToolSpecification> applicationIntelligenceTools(
            ApplicationService applications, CandidateService candidates,
            JobService jobs, AssessmentService assessments) {
        return List.of(

            tool("getApplicationStatus",
                "Get Application Status",
                "Get the current status of a specific application with context: stage name, " +
                "days in current stage, SLA status, interview round, and latest recruiter note. " +
                "Core tool for 'where do I stand?' candidate queries.",
                READ_ONLY,
                schema(Map.of("applicationId", prop("string", "Application ID, e.g. A001")),
                       List.of("applicationId")),
                (ctx, req) -> {
                    String id = str(req, "applicationId");
                    logCtx(ctx, "getApplicationStatus", id);
                    return applications.findById(id).map(a -> {
                        long daysInStage = applications.daysInCurrentStage(a);
                        int  sla         = STAGE_SLA_DAYS.getOrDefault(a.status(), 999);
                        String slaStatus = daysInStage > sla ? "OVERDUE by " + (daysInStage - sla) + " days"
                                         : daysInStage == sla ? "AT_LIMIT"
                                         : "ON_TRACK (" + (sla - daysInStage) + " days remaining)";
                        String latestNote = a.notes().isEmpty() ? null
                                : a.notes().get(a.notes().size() - 1).note();
                        Map<String, Object> view = new LinkedHashMap<>();
                        view.put("applicationId",       a.id());
                        view.put("candidateId",         a.candidateId());
                        view.put("jobId",               a.jobId());
                        view.put("currentStatus",       a.status());
                        view.put("currentInterviewRound", a.currentInterviewRound());
                        view.put("source",              a.source());
                        view.put("appliedAt",           a.appliedAt());
                        view.put("daysInCurrentStage",  daysInStage);
                        view.put("slaStatus",           slaStatus);
                        view.put("totalStages",         a.statusHistory().size());
                        view.put("latestNote",          latestNote);
                        return ok(toJson(view), view);
                    }).orElse(err("Application not found: " + id));
                }),

            tool("getApplicationsByCandidate",
                "Get Applications by Candidate",
                "List all job applications for a candidate, showing job title, current status, " +
                "applied date, and days in pipeline. Good for an agent summarising a candidate's full job search activity.",
                READ_ONLY,
                schema(Map.of("candidateId", prop("string", "Candidate ID")),
                       List.of("candidateId")),
                (ctx, req) -> {
                    String cid = str(req, "candidateId");
                    logCtx(ctx, "getApplicationsByCandidate", cid);
                    if (candidates.findById(cid).isEmpty()) return err("Candidate not found: " + cid);
                    List<Application> apps = applications.findByCandidate(cid);
                    List<Map<String, Object>> summary = apps.stream().map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("applicationId",  a.id());
                        m.put("jobId",          a.jobId());
                        m.put("jobTitle",       jobs.findById(a.jobId()).map(JobRequisition::title).orElse("Unknown"));
                        m.put("status",         a.status());
                        m.put("appliedAt",      a.appliedAt());
                        m.put("daysInPipeline", ChronoUnit.DAYS.between(a.appliedAt(), java.time.LocalDateTime.now()));
                        return m;
                    }).toList();
                    return ok(toJson(summary), summary);
                }),

            tool("getCandidateJourney",
                "Get Candidate Journey",
                "Full end-to-end narrative of a candidate's entire history in the ATS: every application, " +
                "every stage transition, assessments taken, and current open actions. " +
                "Best tool for ops chatbots generating a candidate briefing.",
                READ_ONLY,
                schema(Map.of("candidateId", prop("string", "Candidate ID")),
                       List.of("candidateId")),
                (ctx, req) -> {
                    String cid = str(req, "candidateId");
                    logCtx(ctx, "getCandidateJourney", cid);
                    return candidates.findById(cid).map(c -> {
                        List<Application> apps = applications.findByCandidate(cid);
                        List<Map<String, Object>> appViews = apps.stream().map(a -> {
                            Map<String, Object> av = new LinkedHashMap<>();
                            av.put("applicationId",      a.id());
                            av.put("jobId",              a.jobId());
                            av.put("jobTitle",           jobs.findById(a.jobId()).map(JobRequisition::title).orElse("Unknown"));
                            av.put("status",             a.status());
                            av.put("source",             a.source());
                            av.put("appliedAt",          a.appliedAt());
                            av.put("daysInPipeline",     ChronoUnit.DAYS.between(a.appliedAt(), java.time.LocalDateTime.now()));
                            av.put("daysInCurrentStage", applications.daysInCurrentStage(a));
                            av.put("currentInterviewRound", a.currentInterviewRound());
                            av.put("statusHistory",      a.statusHistory());
                            av.put("assessments",        assessments.findByApplication(a.id())
                                    .stream().map(ar -> Map.of(
                                            "type",        ar.type(),
                                            "score",       ar.score(),
                                            "maxScore",    ar.maxScore(),
                                            "percentile",  ar.percentile(),
                                            "completedAt", ar.completedAt())).toList());
                            av.put("recruiterNotes",     a.notes().size());
                            return av;
                        }).toList();
                        Map<String, Object> journey = new LinkedHashMap<>();
                        journey.put("candidateId",       c.id());
                        journey.put("name",              c.name());
                        journey.put("status",            c.status());
                        journey.put("totalApplications", apps.size());
                        journey.put("activeApplications",apps.stream().filter(a -> a.status() != ApplicationStatus.REJECTED
                                && a.status() != ApplicationStatus.WITHDRAWN
                                && a.status() != ApplicationStatus.HIRED).count());
                        journey.put("applications",      appViews);
                        return ok(toJson(journey), journey);
                    }).orElse(err("Candidate not found: " + cid));
                }),

            tool("getNextSteps",
                "Get Next Steps for Application",
                "Returns candidate-facing guidance for the current pipeline stage: what is happening, " +
                "what the candidate should do now, typical wait time, and possible outcomes. " +
                "Core tool for 'what should I do next?' or 'what should I expect?' queries.",
                READ_ONLY,
                schema(Map.of("applicationId", prop("string", "Application ID")),
                       List.of("applicationId")),
                (ctx, req) -> {
                    String id = str(req, "applicationId");
                    logCtx(ctx, "getNextSteps", id);
                    return applications.findById(id).map(a -> {
                        Map<String, Object> guidance = STAGE_GUIDANCE.getOrDefault(a.status(),
                                Map.of("what_is_happening", "Status: " + a.status(),
                                       "candidate_action",  "Contact your recruiter for details.",
                                       "typical_wait",      "Varies", "possible_next", List.of()));
                        long daysInStage = applications.daysInCurrentStage(a);
                        Map<String, Object> result = new LinkedHashMap<>(guidance);
                        result.put("applicationId",    a.id());
                        result.put("currentStatus",    a.status());
                        result.put("daysInStage",      daysInStage);
                        result.put("sla_days",         STAGE_SLA_DAYS.getOrDefault(a.status(), 0));
                        return ok(toJson(result), result);
                    }).orElse(err("Application not found: " + id));
                }),

            tool("getInterviewFeedback",
                "Get Interview Feedback",
                "Returns recruiter observations and panel notes attached to an application. " +
                "Surfaces the feedback a candidate would want to know about their performance. " +
                "Use to answer 'what did the interviewers think of me?' queries.",
                READ_ONLY,
                schema(Map.of("applicationId", prop("string", "Application ID")),
                       List.of("applicationId")),
                (ctx, req) -> {
                    String id = str(req, "applicationId");
                    logCtx(ctx, "getInterviewFeedback", id);
                    return applications.findById(id).map(a -> {
                        List<Map<String, Object>> feedback = a.notes().stream()
                                .map(n -> Map.<String, Object>of(
                                        "from",       n.authorName(),
                                        "date",       n.createdAt(),
                                        "observation", n.note()))
                                .toList();
                        Map<String, Object> result = Map.of(
                                "applicationId", a.id(),
                                "stage",         a.status(),
                                "feedbackCount", feedback.size(),
                                "feedback",      feedback);
                        return ok(toJson(result), result);
                    }).orElse(err("Application not found: " + id));
                }),

            tool("getStageDuration",
                "Get Stage Duration",
                "Returns how long the application has been in its current stage compared to expected SLA. " +
                "Use to answer 'is my application taking too long?' or 'is there a delay?' queries.",
                READ_ONLY,
                schema(Map.of("applicationId", prop("string", "Application ID")),
                       List.of("applicationId")),
                (ctx, req) -> {
                    String id = str(req, "applicationId");
                    logCtx(ctx, "getStageDuration", id);
                    return applications.findById(id).map(a -> {
                        long days = applications.daysInCurrentStage(a);
                        int  sla  = STAGE_SLA_DAYS.getOrDefault(a.status(), 0);
                        Map<String, Object> result = new LinkedHashMap<>();
                        result.put("applicationId",      a.id());
                        result.put("currentStatus",      a.status());
                        result.put("daysInCurrentStage", days);
                        result.put("expectedSLADays",    sla);
                        result.put("slaBreached",        sla > 0 && days > sla);
                        result.put("breachByDays",       sla > 0 ? Math.max(0, days - sla) : null);
                        result.put("lastChangedAt",      applications.lastStatusChange(a));
                        return ok(toJson(result), result);
                    }).orElse(err("Application not found: " + id));
                })
        );
    }

    // ------------------------------------------------------------------ assessment intelligence

    private List<McpStatelessServerFeatures.SyncToolSpecification> assessmentIntelligenceTools(
            AssessmentService assessments) {
        return List.of(

            tool("getAssessmentResults",
                "Get Assessment Results",
                "All assessment results for a candidate across every type and application. " +
                "Includes score, percentile, completion date, and breakdown. " +
                "Use to answer 'how did I do on my assessments?' queries.",
                READ_ONLY,
                schema(Map.of("candidateId", prop("string", "Candidate ID")),
                       List.of("candidateId")),
                (ctx, req) -> {
                    String cid = str(req, "candidateId");
                    logCtx(ctx, "getAssessmentResults", cid);
                    List<AssessmentResult> results = assessments.findByCandidate(cid);
                    if (results.isEmpty()) return ok("No assessments found for candidate " + cid);
                    OptionalDouble avg = assessments.averageScorePercent(cid);
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("candidateId",         cid);
                    view.put("totalAssessments",    results.size());
                    view.put("averageScorePercent", avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : null);
                    view.put("assessments",         results);
                    return ok(toJson(view), view);
                }),

            tool("getAssessmentByType",
                "Get Assessment by Type",
                "Most recent assessment result of a specific type for a candidate.",
                READ_ONLY,
                schema(Map.of(
                    "candidateId",   prop("string", "Candidate ID"),
                    "assessmentType",propEnum("Assessment type to query",
                            "CODING_CHALLENGE","SYSTEM_DESIGN","TECHNICAL_SCREENING",
                            "BEHAVIORAL","COGNITIVE","TAKE_HOME_PROJECT")),
                    List.of("candidateId", "assessmentType")),
                (ctx, req) -> {
                    String cid = str(req, "candidateId");
                    String raw = str(req, "assessmentType");
                    logCtx(ctx, "getAssessmentByType", cid + "/" + raw);
                    try {
                        AssessmentType type = AssessmentType.valueOf(raw);
                        return assessments.findByCandidateAndType(cid, type)
                                .map(a -> ok(toJson(a), a))
                                .orElse(ok("No " + raw + " assessment on record for candidate " + cid));
                    } catch (IllegalArgumentException e) {
                        return err("Invalid assessment type: " + raw);
                    }
                }),

            tool("compareToPercentile",
                "Compare Score to Percentile",
                "Contextualises a candidate's assessment score relative to all other candidates. " +
                "Returns what the percentile rank means in plain language. " +
                "Use to answer 'how do I compare to other applicants?'",
                READ_ONLY,
                schema(Map.of(
                    "candidateId",  prop("string", "Candidate ID"),
                    "assessmentId", prop("string", "Assessment result ID, e.g. AS001")),
                    List.of("candidateId", "assessmentId")),
                (ctx, req) -> {
                    String cid = str(req, "candidateId");
                    String aid = str(req, "assessmentId");
                    logCtx(ctx, "compareToPercentile", aid);
                    return assessments.findByCandidate(cid).stream()
                            .filter(a -> a.id().equals(aid))
                            .findFirst()
                            .map(a -> {
                                int pct = a.percentile();
                                String label = pct >= 90 ? "Top 10% — exceptional"
                                             : pct >= 75 ? "Top 25% — strong"
                                             : pct >= 50 ? "Above average"
                                             : pct >= 25 ? "Below average — room to grow"
                                             :             "Bottom 25% — significant improvement needed";
                                Map<String, Object> view = new LinkedHashMap<>();
                                view.put("assessmentId",   a.id());
                                view.put("type",           a.type());
                                view.put("score",          a.score());
                                view.put("maxScore",       a.maxScore());
                                view.put("scorePercent",   Math.round(a.scorePercent() * 10.0) / 10.0);
                                view.put("percentile",     pct);
                                view.put("interpretation", label);
                                view.put("beatCandidates", pct + "% of all candidates who attempted this assessment");
                                view.put("summary",        a.summary());
                                return ok(toJson(view), view);
                            }).orElse(err("Assessment " + aid + " not found for candidate " + cid));
                })
        );
    }

    // ------------------------------------------------------------------ job intelligence

    private List<McpStatelessServerFeatures.SyncToolSpecification> jobIntelligenceTools(
            JobService jobs, CandidateService candidates) {
        return List.of(

            tool("getJob",
                "Get Job Details",
                "Full job requisition: title, department, description, required skills, preferred skills, " +
                "salary range, and hiring manager. Use when a candidate wants to understand a specific role.",
                READ_ONLY,
                schema(Map.of("jobId", prop("string", "Job ID, e.g. J001")),
                       List.of("jobId")),
                (ctx, req) -> {
                    String jid = str(req, "jobId");
                    logCtx(ctx, "getJob", jid);
                    return jobs.findById(jid)
                            .map(j -> ok(toJson(j), j))
                            .orElse(err("Job not found: " + jid));
                }),

            tool("listOpenJobs",
                "List Open Jobs",
                "All currently open job requisitions. Optionally filter by department.",
                READ_OPEN,
                schema(Map.of("department", prop("string", "Department filter (optional)")),
                       List.of()),
                (ctx, req) -> {
                    logCtx(ctx, "listOpenJobs", str(req, "department"));
                    String dept = str(req, "department");
                    List<JobRequisition> result = dept != null
                            ? jobs.findByDepartment(dept) : jobs.findActive();
                    return ok(toJson(result), result);
                }),

            tool("getSkillsGap",
                "Get Skills Gap",
                "What skills is a candidate missing for a specific job? " +
                "Returns required skills they have, required skills they lack, preferred skill coverage, " +
                "and an overall fit estimate. Use to answer 'what do I need to learn to qualify for this role?'",
                READ_ONLY,
                schema(Map.of(
                    "candidateId", prop("string", "Candidate ID"),
                    "jobId",       prop("string", "Job ID to evaluate against")),
                    List.of("candidateId", "jobId")),
                (ctx, req) -> {
                    String cid = str(req, "candidateId");
                    String jid = str(req, "jobId");
                    logCtx(ctx, "getSkillsGap", cid + " → " + jid);
                    Optional<Candidate>      c = candidates.findById(cid);
                    Optional<JobRequisition> j = jobs.findById(jid);
                    if (c.isEmpty()) return err("Candidate not found: " + cid);
                    if (j.isEmpty()) return err("Job not found: " + jid);
                    Map<String, Object> gap = jobs.matchScore(c.get(), j.get());
                    return ok(toJson(gap), gap);
                })
        );
    }

    // ------------------------------------------------------------------ ATS knowledge (dev assistants)

    private List<McpStatelessServerFeatures.SyncToolSpecification> atsKnowledgeTools() {
        return List.of(

            tool("getEntitySchema",
                "Get ATS Entity Schema",
                "Returns the live documented field schema for a given ATS entity type. " +
                "Designed for developer assistants and integration builders who need to understand the data model.",
                READ_ONLY,
                schema(Map.of("entity", propEnum("ATS entity to describe",
                        "Candidate", "Application", "AssessmentResult", "JobRequisition",
                        "StatusHistoryEntry", "RecruiterNote")),
                    List.of("entity")),
                (ctx, req) -> {
                    String entity = str(req, "entity");
                    logCtx(ctx, "getEntitySchema", entity);
                    Object schema = ENTITY_SCHEMAS.get(entity);
                    if (schema == null) return err("Unknown entity: " + entity +
                            ". Valid: Candidate, Application, AssessmentResult, JobRequisition, StatusHistoryEntry, RecruiterNote");
                    return ok(toJson(schema), schema);
                }),

            tool("getWorkflowTransitions",
                "Get Application Workflow Transitions",
                "Returns the valid state machine for application status. " +
                "Given a current status, shows which statuses it can transition to. " +
                "Also returns the full transition graph, terminal states, and stage SLAs. " +
                "Designed for developer assistants building status-change logic.",
                READ_ONLY,
                schema(Map.of("fromStatus", propEnum("Current application status (optional — returns full graph if omitted)",
                        "RECEIVED","SCREENING","PHONE_INTERVIEW","TECHNICAL_INTERVIEW",
                        "FINAL_INTERVIEW","OFFER_EXTENDED","OFFER_ACCEPTED","OFFER_DECLINED",
                        "HIRED","REJECTED","WITHDRAWN")),
                    List.of()),
                (ctx, req) -> {
                    logCtx(ctx, "getWorkflowTransitions", str(req, "fromStatus"));
                    String fromStatus = str(req, "fromStatus");
                    if (fromStatus != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> transitions =
                                (Map<String, List<String>>) WORKFLOW_TRANSITIONS.get("transitions");
                        List<String> next = transitions.getOrDefault(fromStatus, List.of());
                        Map<String, Object> view = Map.of(
                                "from",          fromStatus,
                                "validNextStates", next,
                                "isTerminal",    ((List<?>) WORKFLOW_TRANSITIONS.get("terminalStates")).contains(fromStatus),
                                "expectedSLADays", STAGE_SLA_DAYS.entrySet().stream()
                                        .filter(e -> e.getKey().name().equals(fromStatus))
                                        .map(Map.Entry::getValue).findFirst().orElse(0));
                        return ok(toJson(view), view);
                    }
                    return ok(toJson(WORKFLOW_TRANSITIONS), WORKFLOW_TRANSITIONS);
                })
        );
    }

    // =========================================================================
    // STATIC RESOURCES — ATS knowledge base
    // =========================================================================

    @Bean
    public List<McpStatelessServerFeatures.SyncResourceSpecification> staticResources() {
        return List.of(

            resource("ats://schema/candidate",
                "Candidate Schema",
                "Documented field schema for the Candidate entity.",
                "application/json",
                (ctx, req) -> jsonResource(req.uri(), toJson(ENTITY_SCHEMAS.get("Candidate")))),

            resource("ats://schema/application",
                "Application Schema",
                "Documented field schema for the Application entity including status enum values.",
                "application/json",
                (ctx, req) -> jsonResource(req.uri(), toJson(ENTITY_SCHEMAS.get("Application")))),

            resource("ats://workflow/application-states",
                "Application State Machine",
                "Complete state transition graph for application status, with terminal states and SLA expectations.",
                "application/json",
                (ctx, req) -> jsonResource(req.uri(), toJson(WORKFLOW_TRANSITIONS))),

            resource("ats://workflow/assessment-types",
                "Assessment Types Catalog",
                "All assessment types used in the ATS with descriptions and typical scoring context.",
                "application/json",
                (ctx, req) -> {
                    Map<String, Object> catalog = Map.of(
                        "assessmentTypes", List.of(
                            Map.of("type","CODING_CHALLENGE",     "description","Algorithm and data-structure problems (LeetCode-style). Timed, auto-graded.","typicalDuration","60–90 minutes"),
                            Map.of("type","SYSTEM_DESIGN",        "description","Open-ended architectural design interview. Scored by engineer interviewers.","typicalDuration","45–60 minutes"),
                            Map.of("type","TECHNICAL_SCREENING",  "description","Phone/video screen with engineer covering fundamentals and past experience.","typicalDuration","30–45 minutes"),
                            Map.of("type","BEHAVIORAL",           "description","STAR-format questions assessing leadership, communication, and culture fit.","typicalDuration","45–60 minutes"),
                            Map.of("type","COGNITIVE",            "description","Aptitude and logical reasoning test.","typicalDuration","30 minutes"),
                            Map.of("type","TAKE_HOME_PROJECT",    "description","Real-world project completed independently. Reviewed by engineering team.","typicalDuration","4–8 hours")));
                    return jsonResource(req.uri(), toJson(catalog));
                })
        );
    }

    // =========================================================================
    // RESOURCE TEMPLATES — candidate-scoped sub-resources
    // =========================================================================

    @Bean
    public List<McpStatelessServerFeatures.SyncResourceTemplateSpecification> resourceTemplates(
            CandidateService candidates,
            ApplicationService applications,
            AssessmentService assessments,
            JobService jobs) {
        return List.of(

            template("candidate://{candidateId}/profile",
                "Candidate Profile",
                "Full profile record for a candidate.",
                "application/json",
                (ctx, req) -> {
                    String id = seg(req.uri(), "candidate://", "/profile");
                    return candidates.findById(id)
                            .map(c -> jsonResource(req.uri(), toJson(c)))
                            .orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + id));
                }),

            template("candidate://{candidateId}/applications",
                "Candidate Applications",
                "All job applications submitted by the candidate.",
                "application/json",
                (ctx, req) -> {
                    String id = seg(req.uri(), "candidate://", "/applications");
                    return jsonResource(req.uri(), toJson(applications.findByCandidate(id)));
                }),

            template("candidate://{candidateId}/assessments",
                "Candidate Assessments",
                "All assessment results for the candidate with scores and breakdowns.",
                "application/json",
                (ctx, req) -> {
                    String id = seg(req.uri(), "candidate://", "/assessments");
                    return jsonResource(req.uri(), toJson(assessments.findByCandidate(id)));
                }),

            template("candidate://{candidateId}/open-actions",
                "Candidate Open Actions",
                "Applications that are currently active and awaiting next steps.",
                "application/json",
                (ctx, req) -> {
                    String id = seg(req.uri(), "candidate://", "/open-actions");
                    List<Application> open = applications.findByCandidate(id).stream()
                            .filter(a -> a.status() != ApplicationStatus.REJECTED
                                    && a.status() != ApplicationStatus.WITHDRAWN
                                    && a.status() != ApplicationStatus.HIRED
                                    && a.status() != ApplicationStatus.OFFER_DECLINED)
                            .toList();
                    List<Map<String, Object>> actions = open.stream().map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("applicationId", a.id());
                        m.put("jobId",         a.jobId());
                        m.put("jobTitle",      jobs.findById(a.jobId()).map(JobRequisition::title).orElse("Unknown"));
                        m.put("currentStatus", a.status());
                        m.put("daysInStage",   applications.daysInCurrentStage(a));
                        m.put("candidateAction", STAGE_GUIDANCE.getOrDefault(a.status(), Map.of())
                                .getOrDefault("candidate_action", "Contact recruiter for details"));
                        return m;
                    }).toList();
                    return jsonResource(req.uri(), toJson(actions));
                }),

            template("application://{applicationId}/timeline",
                "Application Timeline",
                "Chronological status change history for an application.",
                "application/json",
                (ctx, req) -> {
                    String id = seg(req.uri(), "application://", "/timeline");
                    return applications.findById(id)
                            .map(a -> jsonResource(req.uri(), toJson(a.statusHistory())))
                            .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
                }),

            template("candidate://{candidateId}/journey",
                "Candidate Journey",
                "Complete cross-application journey: all applications with timelines, assessments, and current state.",
                "application/json",
                (ctx, req) -> {
                    String id = seg(req.uri(), "candidate://", "/journey");
                    return candidates.findById(id).map(c -> {
                        List<Application> apps = applications.findByCandidate(id);
                        Map<String, Object> journey = new LinkedHashMap<>();
                        journey.put("candidateId",       c.id());
                        journey.put("name",              c.name());
                        journey.put("totalApplications", apps.size());
                        journey.put("applications",      apps.stream().map(a -> {
                            Map<String, Object> av = new LinkedHashMap<>();
                            av.put("applicationId", a.id());
                            av.put("jobId",         a.jobId());
                            av.put("status",        a.status());
                            av.put("appliedAt",     a.appliedAt());
                            av.put("statusHistory", a.statusHistory());
                            av.put("assessments",   assessments.findByApplication(a.id()));
                            return av;
                        }).toList());
                        return jsonResource(req.uri(), toJson(journey));
                    }).orElseThrow(() -> new IllegalArgumentException("Candidate not found: " + id));
                }),

            template("job://{jobId}/match/{candidateId}",
                "Job-Candidate Match",
                "Skill match score and gap breakdown for a candidate against a specific job.",
                "application/json",
                (ctx, req) -> {
                    // URI: job://J001/match/C001
                    String uri       = req.uri();
                    String afterJob  = uri.substring("job://".length());
                    String jobId     = afterJob.substring(0, afterJob.indexOf("/match/"));
                    String candId    = afterJob.substring(afterJob.indexOf("/match/") + "/match/".length());
                    Optional<Candidate>      c = candidates.findById(candId);
                    Optional<JobRequisition> j = jobs.findById(jobId);
                    if (c.isEmpty() || j.isEmpty()) throw new IllegalArgumentException("Candidate or job not found");
                    return jsonResource(req.uri(), toJson(jobs.matchScore(c.get(), j.get())));
                })
        );
    }

    // =========================================================================
    // PROMPTS — candidate / ops facing LLM instruction templates
    // =========================================================================

    @Bean
    public List<McpStatelessServerFeatures.SyncPromptSpecification> prompts(
            CandidateService candidates,
            ApplicationService applications,
            AssessmentService assessments,
            JobService jobs) {
        return List.of(

            prompt("application-status-narrative",
                "Generate a plain-language status update a candidate can read about their own application.",
                List.of(
                    arg("candidateId",    "Candidate ID",    true),
                    arg("applicationId",  "Application ID",  true)),
                (ctx, req) -> {
                    String cid = str(req.arguments(), "candidateId");
                    String aid = str(req.arguments(), "applicationId");
                    String cJson = candidates.findById(cid).map(this::toJson).orElse("Not found");
                    String aJson = applications.findById(aid).map(this::toJson).orElse("Not found");
                    String guidanceJson = applications.findById(aid)
                            .map(a -> toJson(STAGE_GUIDANCE.getOrDefault(a.status(), Map.of())))
                            .orElse("{}");
                    return promptResult("Application status narrative for " + cid,
                        """
                        You are a helpful career assistant communicating with a job candidate.
                        Write a clear, warm, and honest status update about their application.

                        ## Candidate
                        %s

                        ## Application Record
                        %s

                        ## Stage Guidance Context
                        %s

                        ## Your Response Should:
                        - Be written directly to the candidate (use "your application", "you")
                        - Explain the current stage in plain language (no jargon)
                        - State what is happening right now and what they should expect next
                        - Give a realistic timeline estimate
                        - End with one concrete action they can take today
                        - Tone: professional but warm; honest without being discouraging
                        """.formatted(cJson, aJson, guidanceJson));
                }),

            prompt("next-step-guidance",
                "Prepare a candidate for what is coming next in their pipeline stage.",
                List.of(
                    arg("candidateId",   "Candidate ID",   true),
                    arg("applicationId", "Application ID", true)),
                (ctx, req) -> {
                    String cid = str(req.arguments(), "candidateId");
                    String aid = str(req.arguments(), "applicationId");
                    String cJson = candidates.findById(cid).map(this::toJson).orElse("Not found");
                    String aJson = applications.findById(aid).map(this::toJson).orElse("Not found");
                    String assJson = toJson(assessments.findByCandidate(cid));
                    String jid   = applications.findById(aid).map(Application::jobId).orElse(null);
                    String jJson = jid != null ? jobs.findById(jid).map(this::toJson).orElse("Not found") : "Not found";
                    return promptResult("Next-step guidance for " + cid,
                        """
                        You are a career coach helping a candidate prepare for the next stage of their interview process.

                        ## Candidate
                        %s

                        ## Their Application & Current Status
                        %s

                        ## Target Job
                        %s

                        ## Previous Assessments
                        %s

                        ## Prepare them for the NEXT stage by covering:
                        1. **What to expect** — format, duration, who they'll meet, evaluation criteria
                        2. **Preparation checklist** — specific topics to review based on the job's required skills and their current assessment profile
                        3. **Strengths to lean on** — skills or experiences from their profile that will shine in this stage
                        4. **Gaps to address** — areas where their profile is weaker; practical steps to close in the time available
                        5. **Day-of tips** — 3 actionable tips for performing well in this specific stage type
                        """.formatted(cJson, aJson, jJson, assJson));
                }),

            prompt("rejection-debrief",
                "Explain a rejection to a candidate honestly and map a growth path forward.",
                List.of(arg("applicationId", "Application ID for the rejected application", true)),
                (ctx, req) -> {
                    String aid   = str(req.arguments(), "applicationId");
                    String aJson = applications.findById(aid).map(this::toJson).orElse("Not found");
                    String cid   = applications.findById(aid).map(Application::candidateId).orElse(null);
                    String cJson = cid != null ? candidates.findById(cid).map(this::toJson).orElse("Not found") : "Not found";
                    String jid   = applications.findById(aid).map(Application::jobId).orElse(null);
                    String jJson = jid != null ? jobs.findById(jid).map(this::toJson).orElse("Not found") : "Not found";
                    String assJson = cid != null ? toJson(assessments.findByCandidate(cid)) : "[]";
                    return promptResult("Rejection debrief for application " + aid,
                        """
                        You are a career advisor helping a candidate process a rejection and plan their next move.
                        Be honest, compassionate, and forward-looking.

                        ## Candidate Profile
                        %s

                        ## Application & Status History (to understand where it fell apart)
                        %s

                        ## Role They Applied For
                        %s

                        ## Assessment Results
                        %s

                        ## Your Debrief Should Cover:
                        1. **Likely rejection reasons** — infer from the data: where in the process did it stop, what skills were missing, what assessment scores suggest
                        2. **What they did well** — genuine strengths demonstrated during the process
                        3. **The actual gap** — be specific and constructive, not vague
                        4. **3-Month growth plan** — concrete steps to address the top 2 gaps (courses, projects, practice)
                        5. **Better-fit roles** — suggest 1-2 role types that better match their current profile
                        6. **Re-application advice** — when and how they could apply again if appropriate
                        """.formatted(cJson, aJson, jJson, assJson));
                }),

            prompt("offer-decision-support",
                "Help a candidate evaluate a job offer they have received.",
                List.of(
                    arg("applicationId",    "Application ID at OFFER_EXTENDED stage", true),
                    arg("candidateContext", "Optional: candidate's personal priorities, competing offers, concerns", false)),
                (ctx, req) -> {
                    String aid     = str(req.arguments(), "applicationId");
                    String context = strOr(req.arguments(), "candidateContext", "None provided");
                    String aJson   = applications.findById(aid).map(this::toJson).orElse("Not found");
                    String cid     = applications.findById(aid).map(Application::candidateId).orElse(null);
                    String cJson   = cid != null ? candidates.findById(cid).map(this::toJson).orElse("Not found") : "Not found";
                    String jid     = applications.findById(aid).map(Application::jobId).orElse(null);
                    String jJson   = jid != null ? jobs.findById(jid).map(this::toJson).orElse("Not found") : "Not found";
                    return promptResult("Offer decision support for application " + aid,
                        """
                        You are a trusted career advisor helping a candidate make a high-stakes offer decision.
                        Be objective, thorough, and personalised to their situation.

                        ## Candidate Background
                        %s

                        ## Application & Offer Details
                        %s

                        ## Job Details (salary range, role, company)
                        %s

                        ## Candidate's Personal Context & Concerns
                        %s

                        ## Your Analysis Should Cover:
                        1. **Offer summary** — role, compensation band, key terms
                        2. **Alignment with profile** — does this role fit their skills and trajectory?
                        3. **Growth potential** — what career capital does this role build?
                        4. **Compensation analysis** — is the salary competitive for the level and location?
                        5. **Questions to ask before deciding** — 3-5 questions they should clarify with the recruiter
                        6. **Negotiation levers** — what they could reasonably negotiate based on their profile
                        7. **Recommendation** — ACCEPT / NEGOTIATE / DECLINE with honest rationale
                        """.formatted(cJson, aJson, jJson, context));
                }),

            prompt("profile-gap-coaching",
                "Coach a candidate on how to strengthen their profile for a target role.",
                List.of(
                    arg("candidateId", "Candidate ID",                      true),
                    arg("jobId",       "Target job ID to close the gap for", true)),
                (ctx, req) -> {
                    String cid   = str(req.arguments(), "candidateId");
                    String jid   = str(req.arguments(), "jobId");
                    String cJson = candidates.findById(cid).map(this::toJson).orElse("Not found");
                    String jJson = jobs.findById(jid).map(this::toJson).orElse("Not found");
                    String gapJson = candidates.findById(cid).flatMap(c -> jobs.findById(jid).map(j -> toJson(jobs.matchScore(c, j)))).orElse("{}");
                    return promptResult("Profile gap coaching: " + cid + " → " + jid,
                        """
                        You are a technical career coach helping a candidate close the gap between their current profile and a target role.

                        ## Current Profile
                        %s

                        ## Target Role
                        %s

                        ## Skills Gap Analysis
                        %s

                        ## Coaching Plan (be specific and actionable):
                        1. **Quick wins** (0-4 weeks) — skills or areas where they're close and just need to refresh/demonstrate
                        2. **Medium-term investments** (1-3 months) — skills that require dedicated learning; recommend specific resources (courses, books, projects)
                        3. **Project recommendations** — 1-2 portfolio projects that would directly address the top missing skills
                        4. **Profile optimisation** — how to better present existing skills on LinkedIn/resume for this role type
                        5. **Readiness timeline** — realistic estimate of when they'd be a competitive applicant for this role
                        """.formatted(cJson, jJson, gapJson));
                }),

            prompt("stuck-candidates-report",
                "Operational report: candidates stuck in pipeline stages beyond SLA thresholds.",
                List.of(arg("thresholdDays", "Minimum days in stage to flag as stuck, default 7", false)),
                (ctx, req) -> {
                    int days = intArg2(req.arguments(), "thresholdDays", 7);
                    List<Application> stuck = applications.findStuck(days);
                    List<Map<String, Object>> stuckView = stuck.stream().map(a -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("applicationId",      a.id());
                        m.put("candidateId",        a.candidateId());
                        m.put("jobId",              a.jobId());
                        m.put("currentStatus",      a.status());
                        m.put("daysInStage",        applications.daysInCurrentStage(a));
                        m.put("slaThreshold",       STAGE_SLA_DAYS.getOrDefault(a.status(), 0));
                        m.put("lastChangedAt",      applications.lastStatusChange(a));
                        return m;
                    }).toList();
                    return promptResult("Stuck candidates operational report",
                        """
                        You are an HR operations analyst generating an actionable pipeline health report.

                        ## Stuck Applications (in stage > %d days)
                        %s

                        ## Report Requirements:
                        1. **Summary** — total stuck, breakdown by stage, most critical cases
                        2. **Stage Analysis** — which stages are the biggest bottlenecks right now
                        3. **Individual Actions** — for each stuck application: recommended next action, who should take it, urgency (HIGH/MEDIUM/LOW)
                        4. **Root Cause Hypotheses** — why might these stages be delayed?
                        5. **Process Recommendations** — 2-3 systemic improvements to reduce future SLA breaches
                        """.formatted(days, toJson(stuckView)));
                })
        );
    }

    // =========================================================================
    // COMPLETIONS
    // =========================================================================

    @Bean
    public List<McpStatelessServerFeatures.SyncCompletionSpecification> completions(
            CandidateService candidates, JobService jobs) {
        return List.of(

            // Candidate ID across multiple prompts
            new McpStatelessServerFeatures.SyncCompletionSpecification(
                new McpSchema.PromptReference("application-status-narrative"),
                (McpTransportContext ctx, McpSchema.CompleteRequest req) -> {
                    if (!"candidateId".equals(req.argument().name())) return emptyCompletion();
                    return candidateIdCompletion(candidates, req.argument().value());
                }),

            // candidateId and jobId for profile-gap-coaching
            new McpStatelessServerFeatures.SyncCompletionSpecification(
                new McpSchema.PromptReference("profile-gap-coaching"),
                (McpTransportContext ctx, McpSchema.CompleteRequest req) -> {
                    String partial = req.argument().value().toLowerCase();
                    return switch (req.argument().name()) {
                        case "candidateId" -> candidateIdCompletion(candidates, partial);
                        case "jobId"       -> jobIdCompletion(jobs, partial);
                        default            -> emptyCompletion();
                    };
                }),

            // entity name for getEntitySchema tool (via resource ref)
            new McpStatelessServerFeatures.SyncCompletionSpecification(
                new McpSchema.PromptReference("stuck-candidates-report"),
                (McpTransportContext ctx, McpSchema.CompleteRequest req) -> emptyCompletion())
        );
    }

    // =========================================================================
    // BUILDER HELPERS
    // =========================================================================

    @FunctionalInterface
    interface ToolHandler {
        McpSchema.CallToolResult handle(McpTransportContext ctx, McpSchema.CallToolRequest req);
    }

    private McpStatelessServerFeatures.SyncToolSpecification tool(
            String name, String title, String description,
            McpSchema.ToolAnnotations annotations, McpSchema.JsonSchema inputSchema,
            ToolHandler handler) {
        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(McpSchema.Tool.builder()
                        .name(name).title(title).description(description)
                        .inputSchema(inputSchema).annotations(annotations).build())
                .callHandler(handler::handle)
                .build();
    }

    private McpStatelessServerFeatures.SyncResourceSpecification resource(
            String uri, String name, String description, String mimeType,
            java.util.function.BiFunction<McpTransportContext, McpSchema.ReadResourceRequest,
                    McpSchema.ReadResourceResult> handler) {
        return new McpStatelessServerFeatures.SyncResourceSpecification(
                McpSchema.Resource.builder().uri(uri).name(name).description(description).mimeType(mimeType).build(),
                handler);
    }

    private McpStatelessServerFeatures.SyncResourceTemplateSpecification template(
            String uriTemplate, String name, String description, String mimeType,
            java.util.function.BiFunction<McpTransportContext, McpSchema.ReadResourceRequest,
                    McpSchema.ReadResourceResult> handler) {
        return new McpStatelessServerFeatures.SyncResourceTemplateSpecification(
                McpSchema.ResourceTemplate.builder()
                        .uriTemplate(uriTemplate).name(name).description(description).mimeType(mimeType).build(),
                handler);
    }

    private McpStatelessServerFeatures.SyncPromptSpecification prompt(
            String name, String description, List<McpSchema.PromptArgument> args,
            java.util.function.BiFunction<McpTransportContext, McpSchema.GetPromptRequest,
                    McpSchema.GetPromptResult> handler) {
        return new McpStatelessServerFeatures.SyncPromptSpecification(
                new McpSchema.Prompt(name, description, args), handler);
    }

    private static McpSchema.PromptArgument arg(String name, String description, boolean required) {
        return new McpSchema.PromptArgument(name, description, required);
    }

    private static McpSchema.GetPromptResult promptResult(String description, String text) {
        return new McpSchema.GetPromptResult(description,
                List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(text))));
    }

    // =========================================================================
    // CALL RESULT HELPERS
    // =========================================================================

    private McpSchema.CallToolResult ok(String text) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
    }

    private McpSchema.CallToolResult ok(String text, Object structured) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false, structured, null);
    }

    private static McpSchema.CallToolResult err(String message) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(message)), true);
    }

    // =========================================================================
    // RESOURCE HELPERS
    // =========================================================================

    private McpSchema.ReadResourceResult jsonResource(String uri, String json) {
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(uri, "application/json", json)));
    }

    // =========================================================================
    // COMPLETION HELPERS
    // =========================================================================

    private McpSchema.CompleteResult candidateIdCompletion(CandidateService candidates, String partial) {
        List<String> s = candidates.findAll().stream()
                .filter(c -> c.id().toLowerCase().startsWith(partial)
                        || c.name().toLowerCase().contains(partial))
                .map(c -> c.id() + " — " + c.name())
                .limit(5).toList();
        return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(s, s.size(), false));
    }

    private McpSchema.CompleteResult jobIdCompletion(JobService jobs, String partial) {
        List<String> s = jobs.findAll().stream()
                .filter(j -> j.id().toLowerCase().startsWith(partial)
                        || j.title().toLowerCase().contains(partial))
                .map(j -> j.id() + " — " + j.title())
                .limit(5).toList();
        return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(s, s.size(), false));
    }

    private static McpSchema.CompleteResult emptyCompletion() {
        return new McpSchema.CompleteResult(
                new McpSchema.CompleteResult.CompleteCompletion(List.of(), 0, false));
    }

    // =========================================================================
    // SCHEMA HELPERS
    // =========================================================================

    private static McpSchema.JsonSchema schema(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema("object", properties, required, null, null, null);
    }

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static Map<String, Object> propEnum(String description, String... values) {
        return Map.of("type", "string", "description", description, "enum", List.of(values));
    }

    // =========================================================================
    // ARGUMENT EXTRACTION HELPERS
    // =========================================================================

    private static String str(McpSchema.CallToolRequest req, String key) {
        Object v = req.arguments().get(key);
        return v instanceof String s ? s : null;
    }

    private static String str(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v instanceof String s ? s : null;
    }

    private static String strOr(Map<String, Object> args, String key, String def) {
        String v = str(args, key);
        return v != null ? v : def;
    }

    private static int intArg(McpSchema.CallToolRequest req, String key, int def) {
        Object v = req.arguments().get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private static int intArg2(Map<String, Object> args, String key, int def) {
        Object v = args.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    /** Extracts the variable segment from a resolved URI template. */
    private static String seg(String uri, String prefix, String suffix) {
        String after = uri.startsWith(prefix) ? uri.substring(prefix.length()) : uri;
        return !suffix.isEmpty() && after.contains(suffix)
                ? after.substring(0, after.indexOf(suffix)) : after;
    }

    private void logCtx(McpTransportContext ctx, String tool, String subject) {
        String tenant  = ctx.get("X-Candidate-ID") instanceof String c ? "candidate=" + c
                       : ctx.get("X-Team")         instanceof String t ? "team=" + t : "env=unknown";
        String corr    = ctx.get("X-Correlation-ID") instanceof String c ? c : "-";
        log.info("[{}] [corr={}] tool={} subject={}", tenant, corr, tool, subject);
    }

    private String toJson(Object obj) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    // =========================================================================
    // STATIC KNOWLEDGE BUILDERS
    // =========================================================================

    private static Map<String, Object> buildWorkflow() {
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("states", Arrays.stream(ApplicationStatus.values()).map(Enum::name).toList());
        w.put("transitions", Map.ofEntries(
                Map.entry("RECEIVED",             List.of("SCREENING", "REJECTED", "WITHDRAWN")),
                Map.entry("SCREENING",            List.of("PHONE_INTERVIEW", "REJECTED", "WITHDRAWN")),
                Map.entry("PHONE_INTERVIEW",      List.of("TECHNICAL_INTERVIEW", "REJECTED", "WITHDRAWN")),
                Map.entry("TECHNICAL_INTERVIEW",  List.of("FINAL_INTERVIEW", "REJECTED", "WITHDRAWN")),
                Map.entry("FINAL_INTERVIEW",      List.of("OFFER_EXTENDED", "REJECTED", "WITHDRAWN")),
                Map.entry("OFFER_EXTENDED",       List.of("OFFER_ACCEPTED", "OFFER_DECLINED", "WITHDRAWN")),
                Map.entry("OFFER_ACCEPTED",       List.of("HIRED")),
                Map.entry("HIRED",                List.of()),
                Map.entry("REJECTED",             List.of()),
                Map.entry("WITHDRAWN",            List.of()),
                Map.entry("OFFER_DECLINED",       List.of())
        ));
        w.put("terminalStates", List.of("HIRED", "REJECTED", "WITHDRAWN", "OFFER_DECLINED"));
        w.put("stageSLADays", Map.of(
                "RECEIVED", 2, "SCREENING", 5, "PHONE_INTERVIEW", 3,
                "TECHNICAL_INTERVIEW", 7, "FINAL_INTERVIEW", 5, "OFFER_EXTENDED", 5));
        return Collections.unmodifiableMap(w);
    }

    // ── Entity schema helpers ────────────────────────────────────────────────
    private static Map<String, Object> fld(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static Map<String, Object> enumFld(List<String> values) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "enum");
        m.put("values", values);
        return m;
    }

    private static Map<String, Object> entitySchema(String entity, String description,
                                                     Map<String, Object> fields) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("entity", entity);
        schema.put("description", description);
        schema.put("fields", fields);
        return schema;
    }

    private static Map<String, Object> buildEntitySchemas() {
        Map<String, Object> schemas = new LinkedHashMap<>();

        // Candidate — 13 fields (exceeds Map.of() limit, use LinkedHashMap)
        Map<String, Object> candidateFields = new LinkedHashMap<>();
        candidateFields.put("id",                fld("string",   "Unique ID, e.g. C001"));
        candidateFields.put("name",              fld("string",   "Full legal name"));
        candidateFields.put("email",             fld("string",   "Primary contact email"));
        candidateFields.put("phone",             fld("string",   "Phone with country code"));
        candidateFields.put("location",          fld("string",   "Current city or Remote"));
        candidateFields.put("skills",            fld("string[]", "Technical skills list"));
        candidateFields.put("yearsOfExperience", fld("integer",  "Total professional years"));
        candidateFields.put("currentRole",       fld("string",   "Most recent job title"));
        candidateFields.put("currentCompany",    fld("string",   "Most recent employer"));
        candidateFields.put("status",            enumFld(List.of("ACTIVE","INACTIVE","HIRED","BLACKLISTED")));
        candidateFields.put("summary",           fld("string",   "Professional bio"));
        candidateFields.put("linkedinUrl",       fld("string",   "LinkedIn profile URL"));
        candidateFields.put("createdAt",         fld("datetime", "ATS entry timestamp"));
        schemas.put("Candidate", entitySchema("Candidate",
                "A person in the ATS who has or may apply for positions.", candidateFields));

        // Application
        Map<String, Object> appFields = new LinkedHashMap<>();
        appFields.put("id",                    fld("string",   "Unique ID, e.g. A001"));
        appFields.put("candidateId",           fld("string",   "FK to Candidate"));
        appFields.put("jobId",                 fld("string",   "FK to JobRequisition"));
        appFields.put("status",                enumFld(Arrays.stream(ApplicationStatus.values()).map(Enum::name).toList()));
        appFields.put("source",                enumFld(Arrays.stream(ApplicationSource.values()).map(Enum::name).toList()));
        appFields.put("appliedAt",             fld("datetime",              "Submission timestamp"));
        appFields.put("currentInterviewRound", fld("integer",               "Interview round count"));
        appFields.put("statusHistory",         fld("StatusHistoryEntry[]",  "Ordered stage change log"));
        appFields.put("notes",                 fld("RecruiterNote[]",       "Recruiter observations"));
        schemas.put("Application", entitySchema("Application",
                "One candidate's application to one job requisition.", appFields));

        // AssessmentResult
        Map<String, Object> assessFields = new LinkedHashMap<>();
        assessFields.put("id",            fld("string",   "Unique ID, e.g. AS001"));
        assessFields.put("candidateId",   fld("string",   "FK to Candidate"));
        assessFields.put("applicationId", fld("string",   "FK to Application"));
        assessFields.put("type",          enumFld(Arrays.stream(AssessmentType.values()).map(Enum::name).toList()));
        assessFields.put("score",         fld("double",   "Raw score achieved"));
        assessFields.put("maxScore",      fld("double",   "Maximum possible score"));
        assessFields.put("percentile",    fld("integer",  "Percentile rank 0-100 vs all candidates"));
        assessFields.put("completedAt",   fld("datetime", "Completion timestamp"));
        assessFields.put("summary",       fld("string",   "Evaluator summary notes"));
        assessFields.put("breakdown",     fld("object",   "Type-specific score sub-categories"));
        schemas.put("AssessmentResult", entitySchema("AssessmentResult",
                "Outcome of one assessment taken by a candidate for an application.", assessFields));

        // JobRequisition
        Map<String, Object> jobFields = new LinkedHashMap<>();
        jobFields.put("id",                fld("string",   "Unique ID, e.g. J001"));
        jobFields.put("title",             fld("string",   "Job title"));
        jobFields.put("department",        fld("string",   "Hiring department"));
        jobFields.put("location",          fld("string",   "Office location or Remote"));
        jobFields.put("type",              enumFld(Arrays.stream(JobType.values()).map(Enum::name).toList()));
        jobFields.put("status",            enumFld(Arrays.stream(JobStatus.values()).map(Enum::name).toList()));
        jobFields.put("requiredSkills",    fld("string[]", "Must-have skills"));
        jobFields.put("preferredSkills",   fld("string[]", "Nice-to-have skills"));
        jobFields.put("salaryRange",       fld("string",   "Compensation band"));
        jobFields.put("hiringManagerName", fld("string",   "Hiring manager full name"));
        schemas.put("JobRequisition", entitySchema("JobRequisition",
                "An open position in the company.", jobFields));

        // StatusHistoryEntry
        Map<String, Object> histFields = new LinkedHashMap<>();
        histFields.put("status",    enumFld(Arrays.stream(ApplicationStatus.values()).map(Enum::name).toList()));
        histFields.put("changedAt", fld("datetime", "When the change occurred"));
        histFields.put("changedBy", fld("string",   "User ID or 'system'"));
        histFields.put("reason",    fld("string",   "Free-text reason for the change"));
        schemas.put("StatusHistoryEntry", entitySchema("StatusHistoryEntry",
                "One status transition record in an application's history.", histFields));

        // RecruiterNote
        Map<String, Object> noteFields = new LinkedHashMap<>();
        noteFields.put("id",            fld("string",   "Unique note ID"));
        noteFields.put("applicationId", fld("string",   "FK to Application"));
        noteFields.put("note",          fld("string",   "Note content"));
        noteFields.put("authorId",      fld("string",   "Recruiter user ID"));
        noteFields.put("authorName",    fld("string",   "Recruiter display name"));
        noteFields.put("createdAt",     fld("datetime", "Note creation timestamp"));
        schemas.put("RecruiterNote", entitySchema("RecruiterNote",
                "A recruiter observation attached to an application.", noteFields));

        return Collections.unmodifiableMap(schemas);
    }
}
