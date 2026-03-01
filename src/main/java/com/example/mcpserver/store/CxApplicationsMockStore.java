package com.example.mcpserver.store;

import com.example.mcpserver.dto.common.enums.ApplicationGroupStatus;
import com.example.mcpserver.dto.common.enums.EventStatus;
import com.example.mcpserver.dto.common.enums.EventType;
import com.example.mcpserver.dto.common.enums.OfferStatus;
import com.example.mcpserver.dto.cxapplications.*;
import com.example.mcpserver.model.ApplicationSource;
import com.example.mcpserver.model.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mock data store for cx-applications service with comprehensive enterprise application scenarios.
 *
 * Candidates:
 * - C001: Experienced candidate with 3 applications (1 technical interview, 1 offer, 1 rejected)
 * - C002: Recent grad with 2 applications (1 screening, 1 draft group)
 * - C003: Senior candidate with 1 application (offer accepted, hired)
 */
public class CxApplicationsMockStore {

    private static final Map<String, AtsApplication> APPLICATIONS = new HashMap<>();
    private static final Map<String, ApplicationGroup> APPLICATION_GROUPS = new HashMap<>();

    static {
        LocalDateTime now = LocalDateTime.now();

        // ========== CANDIDATE C001: Experienced SRE candidate ==========

        // Application A001: C001 → J001 (Senior SRE) - TECHNICAL_INTERVIEW (in progress, SLA healthy)
        APPLICATIONS.put("A001", new AtsApplication(
                "A001",
                "C001",
                "J001",
                ApplicationStatus.TECHNICAL_INTERVIEW,
                ApplicationSource.LINKEDIN,
                now.minusDays(12),
                now.minusDays(1),
                List.of(
                        new WorkflowHistoryEntry(null, ApplicationStatus.RECEIVED, now.minusDays(12), "SYS", "System", "Application submitted", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.RECEIVED, ApplicationStatus.SCREENING, now.minusDays(11), "REC001", "Emily Martinez", "Resume screening passed", "Strong Kubernetes and Python experience", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.SCREENING, ApplicationStatus.PHONE_INTERVIEW, now.minusDays(9), "REC001", "Emily Martinez", "Phone screen scheduled", "Candidate available next week", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, ApplicationStatus.TECHNICAL_INTERVIEW, now.minusDays(4), "REC001", "Emily Martinez", "Phone screen passed", "Great communication, strong technical background", Map.of())
                ),
                new ScheduleMetadata(
                        List.of(
                                new ScheduledEvent("EVT001", EventType.TECH_INTERVIEW, now.plusDays(2).withHour(14).withMinute(0), 90, "Zoom: https://zoom.us/j/technical-001", List.of("INT001", "INT002"), List.of("Sarah Chen", "David Park"), EventStatus.SCHEDULED),
                                new ScheduledEvent("EVT002", EventType.PANEL_INTERVIEW, now.plusDays(3).withHour(10).withMinute(0), 60, "Zoom: https://zoom.us/j/design-001", List.of("INT003"), List.of("Dr. Lisa Zhang"), EventStatus.SCHEDULED)
                        ),
                        "https://calendar.google.com/event/a001",
                        "America/Los_Angeles"
                ),
                null, // No offer yet
                List.of(
                        new RecruiterNote("NOTE001", "A001", "Strong candidate. Kubernetes experience aligns perfectly with team needs.", "REC001", "Emily Martinez", now.minusDays(10)),
                        new RecruiterNote("NOTE002", "A001", "Phone screen went well. Candidate asked great questions about on-call rotation.", "REC001", "Emily Martinez", now.minusDays(4))
                ),
                "REC001",
                "A", // Internal rating: A/B/C
                "c001-partition",
                "etag-a001"
        ));

        // Application A002: C001 → J002 (Frontend Engineer) - OFFER_EXTENDED (pending response)
        APPLICATIONS.put("A002", new AtsApplication(
                "A002",
                "C001",
                "J002",
                ApplicationStatus.OFFER_EXTENDED,
                ApplicationSource.REFERRAL,
                now.minusDays(20),
                now.minusDays(1),
                List.of(
                        new WorkflowHistoryEntry(null, ApplicationStatus.RECEIVED, now.minusDays(20), "SYS", "System", "Referral from John Doe (EMP-5678)", null, Map.of("referrer", "EMP-5678")),
                        new WorkflowHistoryEntry(ApplicationStatus.RECEIVED, ApplicationStatus.SCREENING, now.minusDays(19), "REC002", "Marcus Johnson", "Referral fast-track", "Strong referral from senior engineer", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.SCREENING, ApplicationStatus.PHONE_INTERVIEW, now.minusDays(17), "REC002", "Marcus Johnson", "Phone screen scheduled", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, ApplicationStatus.TECHNICAL_INTERVIEW, now.minusDays(14), "REC002", "Marcus Johnson", "Phone screen passed", "Excellent React and TypeScript knowledge", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationStatus.FINAL_INTERVIEW, now.minusDays(10), "REC002", "Marcus Johnson", "Technical interview passed", "Coded live whiteboard exercise flawlessly", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.FINAL_INTERVIEW, ApplicationStatus.OFFER_EXTENDED, now.minusDays(3), "MGR002", "Michael Rodriguez", "Team consensus: strong hire", "All interviewers voted to hire", Map.of())
                ),
                new ScheduleMetadata(
                        List.of(),
                        "https://calendar.google.com/event/a002",
                        "America/New_York"
                ),
                new OfferMetadata(
                        "OFFER001",
                        now.minusDays(3),
                        now.plusDays(4), // 7 days to respond
                        null, // Not yet responded
                        OfferStatus.PENDING,
                        new CompensationOffer(155000, 10000, 5000, "USD", "~$75,000", "2026-04-01", List.of("Health insurance", "401k match", "Unlimited PTO", "Remote work stipend")),
                        List.of(), // No negotiation yet
                        "https://offers.company.com/C001/OFFER001.pdf"
                ),
                List.of(
                        new RecruiterNote("NOTE003", "A002", "Referral from John Doe. Candidate has 6 years React experience.", "REC002", "Marcus Johnson", now.minusDays(19)),
                        new RecruiterNote("NOTE004", "A002", "Technical interview feedback: excellent problem-solving, great communication.", "REC002", "Marcus Johnson", now.minusDays(10)),
                        new RecruiterNote("NOTE005", "A002", "Offer extended at $155k base + $10k signing + equity. Competitive package.", "REC002", "Marcus Johnson", now.minusDays(3))
                ),
                "REC002",
                "A+",
                "c001-partition",
                "etag-a002"
        ));

        // Application A003: C001 → J003 (Data Engineer - Night Shift) - REJECTED (shift incompatibility)
        APPLICATIONS.put("A003", new AtsApplication(
                "A003",
                "C001",
                "J003",
                ApplicationStatus.REJECTED,
                ApplicationSource.DIRECT,
                now.minusDays(15),
                now.minusDays(8),
                List.of(
                        new WorkflowHistoryEntry(null, ApplicationStatus.RECEIVED, now.minusDays(15), "SYS", "System", "Application submitted", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.RECEIVED, ApplicationStatus.SCREENING, now.minusDays(14), "REC003", "Amanda Foster", "Initial screening", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.SCREENING, ApplicationStatus.REJECTED, now.minusDays(8), "REC003", "Amanda Foster", "Shift incompatibility", "Candidate cannot commit to night shift schedule", Map.of("rejection_reason", "shift_mismatch"))
                ),
                null, // No schedule
                null, // No offer
                List.of(
                        new RecruiterNote("NOTE006", "A003", "Candidate has strong data engineering background but cannot work night shift.", "REC003", "Amanda Foster", now.minusDays(8))
                ),
                "REC003",
                "N/A",
                "c001-partition",
                "etag-a003"
        ));

        // ========== CANDIDATE C002: Recent graduate ==========

        // Application A004: C002 → J005 (Junior Software Engineer) - SCREENING (early stage)
        APPLICATIONS.put("A004", new AtsApplication(
                "A004",
                "C002",
                "J005",
                ApplicationStatus.SCREENING,
                ApplicationSource.CAMPUS,
                now.minusDays(5),
                now.minusDays(3),
                List.of(
                        new WorkflowHistoryEntry(null, ApplicationStatus.RECEIVED, now.minusDays(5), "SYS", "System", "Campus recruitment application", null, Map.of("university", "MIT")),
                        new WorkflowHistoryEntry(ApplicationStatus.RECEIVED, ApplicationStatus.SCREENING, now.minusDays(3), "REC004", "Jennifer Wu", "Resume review in progress", null, Map.of())
                ),
                null, // No schedule yet
                null, // No offer
                List.of(
                        new RecruiterNote("NOTE007", "A004", "New grad from MIT CS program. GPA 3.8. Strong algorithms coursework.", "REC004", "Jennifer Wu", now.minusDays(3))
                ),
                "REC004",
                "B+",
                "c002-partition",
                "etag-a004"
        ));

        // ApplicationGroup AG001: C002 → [J001, J002, J004] (Multi-job draft - DRAFT, 60% complete)
        APPLICATION_GROUPS.put("AG001", new ApplicationGroup(
                "AG001",
                "C002",
                List.of("J001", "J002", "J004"), // Applying to SRE, Frontend, and DevOps jobs
                ApplicationGroupStatus.DRAFT,
                "careers_portal_v2",
                now.minusDays(2),
                null, // Not submitted yet
                Map.of(
                        "resume_uploaded", true,
                        "cover_letter", "I am passionate about building scalable systems...",
                        "preferred_start_date", "2026-05-01",
                        "willing_to_relocate", true
                ),
                60 // 60% complete
        ));

        // ========== CANDIDATE C003: Senior candidate - hired ==========

        // Application A005: C003 → J008 (Security Engineer) - HIRED (offer accepted)
        APPLICATIONS.put("A005", new AtsApplication(
                "A005",
                "C003",
                "J008",
                ApplicationStatus.HIRED,
                ApplicationSource.AGENCY,
                now.minusDays(30),
                now.minusDays(1),
                List.of(
                        new WorkflowHistoryEntry(null, ApplicationStatus.RECEIVED, now.minusDays(30), "SYS", "System", "Application via TechRecruit agency", null, Map.of("agency", "TechRecruit Inc")),
                        new WorkflowHistoryEntry(ApplicationStatus.RECEIVED, ApplicationStatus.SCREENING, now.minusDays(29), "REC005", "Robert Kim", "Fast-track for senior role", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.SCREENING, ApplicationStatus.PHONE_INTERVIEW, now.minusDays(27), "REC005", "Robert Kim", "Phone screen scheduled", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, ApplicationStatus.TECHNICAL_INTERVIEW, now.minusDays(24), "REC005", "Robert Kim", "Phone screen passed", "Deep AppSec expertise, OSCP certified", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationStatus.FINAL_INTERVIEW, now.minusDays(20), "REC005", "Robert Kim", "Technical interview passed", "Demonstrated threat modeling and pen testing skills", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.FINAL_INTERVIEW, ApplicationStatus.OFFER_EXTENDED, now.minusDays(15), "MGR007", "Robert Kim", "Strong hire", "Team consensus: exceptional candidate", Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.OFFER_EXTENDED, ApplicationStatus.OFFER_ACCEPTED, now.minusDays(10), "SYS", "System", "Offer accepted by candidate", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.OFFER_ACCEPTED, ApplicationStatus.HIRED, now.minusDays(1), "HR001", "HR System", "Background check cleared, start date confirmed", null, Map.of("start_date", "2026-04-01"))
                ),
                new ScheduleMetadata(
                        List.of(),
                        null,
                        "America/Los_Angeles"
                ),
                new OfferMetadata(
                        "OFFER002",
                        now.minusDays(15),
                        now.minusDays(8),
                        now.minusDays(10), // Accepted 5 days before deadline
                        OfferStatus.ACCEPTED,
                        new CompensationOffer(170000, 15000, 8000, "USD", "~$120,000", "2026-04-01", List.of("Health insurance", "401k match", "Unlimited PTO", "Security conference budget")),
                        List.of(
                                new NegotiationRound(1, now.minusDays(13), now.minusDays(12), "Candidate", "Base Salary Increase", "Requested $180k base salary", "Counter-offered $170k + $15k signing bonus"),
                                new NegotiationRound(2, now.minusDays(11), now.minusDays(10), "Candidate", "Acceptance", "Accepted counter-offer", "Deal finalized, offer letter sent")
                        ),
                        "https://offers.company.com/C003/OFFER002.pdf"
                ),
                List.of(
                        new RecruiterNote("NOTE008", "A005", "Exceptional security background. 8 years AppSec experience + OSCP certified.", "REC005", "Robert Kim", now.minusDays(29)),
                        new RecruiterNote("NOTE009", "A005", "Candidate negotiated well. Justified higher comp with market data.", "REC005", "Robert Kim", now.minusDays(13)),
                        new RecruiterNote("NOTE010", "A005", "Offer accepted! Start date April 1. Background check in progress.", "REC005", "Robert Kim", now.minusDays(10))
                ),
                "REC005",
                "A+",
                "c003-partition",
                "etag-a005"
        ));

        // ========== CANDIDATE C004: SLA breach scenario ==========

        // Application A006: C004 → J001 (Senior SRE) - SCREENING (SLA breached, 12 days in screening)
        APPLICATIONS.put("A006", new AtsApplication(
                "A006",
                "C004",
                "J001",
                ApplicationStatus.SCREENING,
                ApplicationSource.JOB_BOARD,
                now.minusDays(14),
                now.minusDays(14),
                List.of(
                        new WorkflowHistoryEntry(null, ApplicationStatus.RECEIVED, now.minusDays(14), "SYS", "System", "Application from Indeed", null, Map.of("source_url", "indeed.com")),
                        new WorkflowHistoryEntry(ApplicationStatus.RECEIVED, ApplicationStatus.SCREENING, now.minusDays(12), "REC001", "Emily Martinez", "Screening assigned", null, Map.of())
                ),
                null, // No schedule (stuck in screening)
                null,
                List.of(
                        new RecruiterNote("NOTE011", "A006", "Resume received. Need to review this week.", "REC001", "Emily Martinez", now.minusDays(12))
                ),
                "REC001",
                "Pending",
                "c004-partition",
                "etag-a006"
        ));

        // ========== CANDIDATE C005: Withdrawn application ==========

        // Application A007: C005 → J002 (Frontend Engineer) - WITHDRAWN (accepted another offer)
        APPLICATIONS.put("A007", new AtsApplication(
                "A007",
                "C005",
                "J002",
                ApplicationStatus.WITHDRAWN,
                ApplicationSource.LINKEDIN,
                now.minusDays(18),
                now.minusDays(7),
                List.of(
                        new WorkflowHistoryEntry(null, ApplicationStatus.RECEIVED, now.minusDays(18), "SYS", "System", "Application submitted", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.RECEIVED, ApplicationStatus.SCREENING, now.minusDays(17), "REC002", "Marcus Johnson", "Resume screening", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.SCREENING, ApplicationStatus.PHONE_INTERVIEW, now.minusDays(14), "REC002", "Marcus Johnson", "Phone screen scheduled", null, Map.of()),
                        new WorkflowHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, ApplicationStatus.WITHDRAWN, now.minusDays(7), "SYS", "System", "Candidate withdrew", "Candidate accepted offer from another company", Map.of("withdrawal_reason", "accepted_other_offer"))
                ),
                null,
                null,
                List.of(
                        new RecruiterNote("NOTE012", "A007", "Candidate withdrew before technical interview. Lost to competitor offer.", "REC002", "Marcus Johnson", now.minusDays(7))
                ),
                "REC002",
                "N/A",
                "c005-partition",
                "etag-a007"
        ));

        // ========== Additional Application Groups ==========

        // ApplicationGroup AG002: C001 → [J004, J008] (Multi-job SUBMITTED)
        APPLICATION_GROUPS.put("AG002", new ApplicationGroup(
                "AG002",
                "C001",
                List.of("J004", "J008"), // DevOps and Security roles
                ApplicationGroupStatus.SUBMITTED,
                "careers_portal_v2",
                now.minusDays(25),
                now.minusDays(23), // Submitted 2 days after creation
                Map.of(
                        "resume_uploaded", true,
                        "cover_letter", "With 7 years of experience in DevOps and Security...",
                        "preferred_start_date", "2026-04-15"
                ),
                100 // 100% complete
        ));

        // ApplicationGroup AG003: C003 → [J001, J004] (ABANDONED - 35 days idle)
        APPLICATION_GROUPS.put("AG003", new ApplicationGroup(
                "AG003",
                "C003",
                List.of("J001", "J004"),
                ApplicationGroupStatus.ABANDONED,
                "mobile_app_v1",
                now.minusDays(35),
                null, // Never submitted
                Map.of(
                        "resume_uploaded", true,
                        "preferred_start_date", "2026-03-01"
                ),
                25 // Only 25% complete, abandoned
        ));
    }

    public static Optional<ApplicationGroup> getApplicationGroup(String groupId) {
        return Optional.ofNullable(APPLICATION_GROUPS.get(groupId));
    }

    public static List<ApplicationGroup> getApplicationGroupsByCandidate(String candidateId) {
        return APPLICATION_GROUPS.values().stream()
                .filter(group -> group.candidateId().equals(candidateId))
                .toList();
    }

    public static Optional<AtsApplication> getApplication(String applicationId) {
        return Optional.ofNullable(APPLICATIONS.get(applicationId));
    }

    public static List<AtsApplication> getApplicationsByCandidate(String candidateId) {
        return APPLICATIONS.values().stream()
                .filter(app -> app.candidateId().equals(candidateId))
                .toList();
    }

    public static List<AtsApplication> getApplicationsByJob(String jobId) {
        return APPLICATIONS.values().stream()
                .filter(app -> app.jobId().equals(jobId))
                .toList();
    }

    public static Optional<AtsApplication> getApplicationByCandidateAndJob(String candidateId, String jobId) {
        return APPLICATIONS.values().stream()
                .filter(app -> app.candidateId().equals(candidateId) && app.jobId().equals(jobId))
                .findFirst();
    }

    public static List<AtsApplication> getAllApplications() {
        return List.copyOf(APPLICATIONS.values());
    }

    public static List<ApplicationGroup> getAllApplicationGroups() {
        return List.copyOf(APPLICATION_GROUPS.values());
    }
}
