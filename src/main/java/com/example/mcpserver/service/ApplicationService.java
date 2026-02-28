package com.example.mcpserver.service;

import com.example.mcpserver.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final Map<String, Application> store = new ConcurrentHashMap<>();
    private final AtomicInteger noteIdSeq = new AtomicInteger(100);

    public ApplicationService() {
        LocalDateTime base = LocalDateTime.now();

        Application a001 = new Application("A001", "C001", "J001",
                ApplicationStatus.FINAL_INTERVIEW, ApplicationSource.LINKEDIN,
                base.minusDays(28), 3,
                List.of(
                    new StatusHistoryEntry(ApplicationStatus.RECEIVED,     base.minusDays(28), "system",     "Application received"),
                    new StatusHistoryEntry(ApplicationStatus.SCREENING,    base.minusDays(25), "recruiter-1","Passed resume screening"),
                    new StatusHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, base.minusDays(20), "recruiter-1","Phone screen completed"),
                    new StatusHistoryEntry(ApplicationStatus.TECHNICAL_INTERVIEW, base.minusDays(14), "recruiter-1","Technical round passed"),
                    new StatusHistoryEntry(ApplicationStatus.FINAL_INTERVIEW, base.minusDays(3), "recruiter-1","Scheduled final interview")
                ),
                List.of(
                    new RecruiterNote("N001", "A001", "Strong Java background. Answered system design questions confidently.", "recruiter-1", "Jane Smith", base.minusDays(20)),
                    new RecruiterNote("N002", "A001", "Technical round: solved 2 medium LeetCode problems optimally. Good communication.", "recruiter-1", "Jane Smith", base.minusDays(14))
                ));

        Application a002 = new Application("A002", "C002", "J002",
                ApplicationStatus.PHONE_INTERVIEW, ApplicationSource.REFERRAL,
                base.minusDays(18), 1,
                List.of(
                    new StatusHistoryEntry(ApplicationStatus.RECEIVED,  base.minusDays(18), "system",     "Application received"),
                    new StatusHistoryEntry(ApplicationStatus.SCREENING, base.minusDays(15), "recruiter-2","Excellent ML background"),
                    new StatusHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, base.minusDays(7), "recruiter-2","Phone screen scheduled")
                ),
                List.of(
                    new RecruiterNote("N003", "A002", "Referred by Dr. Emily Chen (Head of Research). Strong ML profile, 3 patents.", "recruiter-2", "Mark Johnson", base.minusDays(18))
                ));

        Application a003 = new Application("A003", "C003", "J001",
                ApplicationStatus.SCREENING, ApplicationSource.DIRECT,
                base.minusDays(10), 0,
                List.of(
                    new StatusHistoryEntry(ApplicationStatus.RECEIVED,  base.minusDays(10), "system",     "Application received"),
                    new StatusHistoryEntry(ApplicationStatus.SCREENING, base.minusDays(7),  "recruiter-1","Under resume review")
                ),
                List.of());

        Application a004 = new Application("A004", "C004", "J001",
                ApplicationStatus.OFFER_EXTENDED, ApplicationSource.AGENCY,
                base.minusDays(40), 4,
                List.of(
                    new StatusHistoryEntry(ApplicationStatus.RECEIVED,      base.minusDays(40), "system",     "Agency submission"),
                    new StatusHistoryEntry(ApplicationStatus.SCREENING,     base.minusDays(38), "recruiter-1","Top-tier candidate"),
                    new StatusHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, base.minusDays(33), "recruiter-1","Excellent culture fit"),
                    new StatusHistoryEntry(ApplicationStatus.TECHNICAL_INTERVIEW, base.minusDays(25), "recruiter-1","Passed all technical rounds"),
                    new StatusHistoryEntry(ApplicationStatus.FINAL_INTERVIEW, base.minusDays(15), "recruiter-1","Final exec interview done"),
                    new StatusHistoryEntry(ApplicationStatus.OFFER_EXTENDED, base.minusDays(5), "recruiter-1","Offer sent: $195K + equity")
                ),
                List.of(
                    new RecruiterNote("N004", "A004", "Exceptional system design skills. 12 years of relevant experience. HIGHLY RECOMMENDED.", "recruiter-1", "Jane Smith", base.minusDays(25)),
                    new RecruiterNote("N005", "A004", "Offer extended. Candidate is comparing with two other offers. Decision expected by EOW.", "recruiter-1", "Jane Smith", base.minusDays(5))
                ));

        Application a005 = new Application("A005", "C005", "J003",
                ApplicationStatus.HIRED, ApplicationSource.LINKEDIN,
                base.minusDays(80), 3,
                List.of(
                    new StatusHistoryEntry(ApplicationStatus.RECEIVED,    base.minusDays(80), "system",     "Application received"),
                    new StatusHistoryEntry(ApplicationStatus.SCREENING,   base.minusDays(76), "recruiter-3","Strong platform background"),
                    new StatusHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, base.minusDays(70), "recruiter-3","Excellent fit"),
                    new StatusHistoryEntry(ApplicationStatus.TECHNICAL_INTERVIEW, base.minusDays(63), "recruiter-3","Top-notch"),
                    new StatusHistoryEntry(ApplicationStatus.FINAL_INTERVIEW, base.minusDays(55), "recruiter-3","Passed final"),
                    new StatusHistoryEntry(ApplicationStatus.OFFER_EXTENDED, base.minusDays(50), "recruiter-3","Offer accepted"),
                    new StatusHistoryEntry(ApplicationStatus.HIRED, base.minusDays(30), "system", "Started on " + base.minusDays(30).toLocalDate())
                ),
                List.of(
                    new RecruiterNote("N006", "A005", "Best platform candidate we've seen this year. Offer accepted immediately.", "recruiter-3", "Tom Wilson", base.minusDays(50))
                ));

        Application a006 = new Application("A006", "C001", "J003",
                ApplicationStatus.REJECTED, ApplicationSource.DIRECT,
                base.minusDays(50), 1,
                List.of(
                    new StatusHistoryEntry(ApplicationStatus.RECEIVED,  base.minusDays(50), "system",     "Application received"),
                    new StatusHistoryEntry(ApplicationStatus.SCREENING, base.minusDays(48), "recruiter-3","Go/Rust skills missing"),
                    new StatusHistoryEntry(ApplicationStatus.REJECTED,  base.minusDays(46), "recruiter-3","Skills mismatch for platform role")
                ),
                List.of(
                    new RecruiterNote("N007", "A006", "Strong Java background but no Go/Rust/Terraform experience required for this role. Rejected. Consider for J001.", "recruiter-3", "Tom Wilson", base.minusDays(46))
                ));

        Application a007 = new Application("A007", "C006", "J001",
                ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationSource.JOB_BOARD,
                base.minusDays(8), 2,
                List.of(
                    new StatusHistoryEntry(ApplicationStatus.RECEIVED,   base.minusDays(8), "system",     "Application received"),
                    new StatusHistoryEntry(ApplicationStatus.SCREENING,  base.minusDays(6), "recruiter-1","Good fintech background"),
                    new StatusHistoryEntry(ApplicationStatus.PHONE_INTERVIEW, base.minusDays(3), "recruiter-1","Phone screen passed"),
                    new StatusHistoryEntry(ApplicationStatus.TECHNICAL_INTERVIEW, base.minusDays(1), "recruiter-1","Technical round today")
                ),
                List.of(
                    new RecruiterNote("N008", "A007", "Real-time payment processing experience is very relevant. Tracking closely.", "recruiter-1", "Jane Smith", base.minusDays(6))
                ));

        List.of(a001, a002, a003, a004, a005, a006, a007)
                .forEach(a -> store.put(a.id(), a));
    }

    public Optional<Application> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Application> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Application::id))
                .collect(Collectors.toList());
    }

    public List<Application> findByCandidate(String candidateId) {
        return store.values().stream()
                .filter(a -> a.candidateId().equals(candidateId))
                .sorted(Comparator.comparing(Application::appliedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Application> findByJob(String jobId) {
        return store.values().stream()
                .filter(a -> a.jobId().equals(jobId))
                .sorted(Comparator.comparing(Application::appliedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Application> findByStatus(ApplicationStatus status) {
        return store.values().stream()
                .filter(a -> a.status() == status)
                .sorted(Comparator.comparing(Application::id))
                .collect(Collectors.toList());
    }

    public Optional<Application> updateStatus(String id, ApplicationStatus newStatus,
                                               String changedBy, String reason) {
        return findById(id).map(a -> {
            List<StatusHistoryEntry> history = new ArrayList<>(a.statusHistory());
            history.add(new StatusHistoryEntry(newStatus, LocalDateTime.now(), changedBy, reason));
            int round = newStatus == ApplicationStatus.TECHNICAL_INTERVIEW ? a.currentInterviewRound() + 1
                      : newStatus == ApplicationStatus.FINAL_INTERVIEW     ? a.currentInterviewRound() + 1
                      : a.currentInterviewRound();
            Application updated = new Application(a.id(), a.candidateId(), a.jobId(),
                    newStatus, a.source(), a.appliedAt(), round, history, a.notes());
            store.put(id, updated);
            return updated;
        });
    }

    public Optional<Application> addNote(String applicationId, String note,
                                          String authorId, String authorName) {
        return findById(applicationId).map(a -> {
            List<RecruiterNote> notes = new ArrayList<>(a.notes());
            notes.add(new RecruiterNote(
                    "N" + noteIdSeq.incrementAndGet(), applicationId,
                    note, authorId, authorName, LocalDateTime.now()));
            Application updated = new Application(a.id(), a.candidateId(), a.jobId(),
                    a.status(), a.source(), a.appliedAt(),
                    a.currentInterviewRound(), a.statusHistory(), notes);
            store.put(applicationId, updated);
            return updated;
        });
    }

    public Map<ApplicationStatus, Long> pipelineStats() {
        return store.values().stream()
                .collect(Collectors.groupingBy(Application::status, Collectors.counting()));
    }

    /** Applications that have been sitting in a non-terminal stage for more than thresholdDays. */
    public List<Application> findStuck(int thresholdDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(thresholdDays);
        return store.values().stream()
                .filter(a -> !isTerminal(a.status()))
                .filter(a -> lastStatusChange(a).isBefore(cutoff))
                .sorted(Comparator.comparing(Application::id))
                .collect(Collectors.toList());
    }

    /** Number of days the application has been in its current status. */
    public long daysInCurrentStage(Application a) {
        return java.time.temporal.ChronoUnit.DAYS.between(lastStatusChange(a), LocalDateTime.now());
    }

    public LocalDateTime lastStatusChange(Application a) {
        return a.statusHistory().stream()
                .map(StatusHistoryEntry::changedAt)
                .max(Comparator.naturalOrder())
                .orElse(a.appliedAt());
    }

    private static boolean isTerminal(ApplicationStatus s) {
        return s == ApplicationStatus.HIRED || s == ApplicationStatus.REJECTED
                || s == ApplicationStatus.WITHDRAWN || s == ApplicationStatus.OFFER_DECLINED;
    }
}
