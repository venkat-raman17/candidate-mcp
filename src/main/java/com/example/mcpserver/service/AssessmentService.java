package com.example.mcpserver.service;

import com.example.mcpserver.model.AssessmentResult;
import com.example.mcpserver.model.AssessmentType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    private final Map<String, AssessmentResult> store = new ConcurrentHashMap<>();

    public AssessmentService() {
        LocalDateTime base = LocalDateTime.now();
        List.of(
            // C001 / A001 — Senior SWE candidate
            new AssessmentResult("AS001", "C001", "A001", AssessmentType.CODING_CHALLENGE,
                85, 100, 88, base.minusDays(22),
                "Solved 3/3 problems. Optimal solution for two, brute-force for the third.",
                Map.of("problemsSolved", 3, "optimalSolutions", 2, "timeUsedMinutes", 72,
                        "languages", List.of("Java"))),
            new AssessmentResult("AS002", "C001", "A001", AssessmentType.SYSTEM_DESIGN,
                78, 100, 75, base.minusDays(16),
                "Designed a URL shortener at scale. Good understanding of caching and sharding, missed CDN edge cases.",
                Map.of("designScore", 80, "scalabilityScore", 82, "reliabilityScore", 70,
                        "communicationScore", 85, "missingTopics", List.of("CDN", "Global failover"))),

            // C002 / A002 — ML Engineer
            new AssessmentResult("AS003", "C002", "A002", AssessmentType.TECHNICAL_SCREENING,
                90, 100, 94, base.minusDays(10),
                "Exceptional ML fundamentals. Deep knowledge of transformer architectures and MLOps.",
                Map.of("mlTheoryScore", 95, "mlOpsScore", 88, "codingScore", 87,
                        "papersDiscussed", List.of("Attention is All You Need", "BERT", "GPT-4 Technical Report"))),

            // C003 / A003 — Full Stack
            new AssessmentResult("AS004", "C003", "A003", AssessmentType.CODING_CHALLENGE,
                72, 100, 65, base.minusDays(5),
                "Completed 2/3 problems. Strong frontend skills but backend algorithm weak.",
                Map.of("problemsSolved", 2, "optimalSolutions", 1, "timeUsedMinutes", 85,
                        "languages", List.of("TypeScript", "Node.js"))),

            // C004 / A004 — Lead Architect
            new AssessmentResult("AS005", "C004", "A004", AssessmentType.CODING_CHALLENGE,
                92, 100, 97, base.minusDays(27),
                "Flawless. All 3 problems solved optimally with detailed complexity analysis.",
                Map.of("problemsSolved", 3, "optimalSolutions", 3, "timeUsedMinutes", 55,
                        "languages", List.of("Java"), "bonus", "Provided two alternative solutions for problem 2")),
            new AssessmentResult("AS006", "C004", "A004", AssessmentType.SYSTEM_DESIGN,
                94, 100, 98, base.minusDays(20),
                "Outstanding system design for a distributed job scheduler. Covered partitioning, replication, consensus, observability.",
                Map.of("designScore", 96, "scalabilityScore", 95, "reliabilityScore", 94,
                        "communicationScore", 92, "standoutTopics", List.of("Raft consensus", "Back-pressure", "Circuit breaker"))),
            new AssessmentResult("AS007", "C004", "A004", AssessmentType.BEHAVIORAL,
                88, 100, 85, base.minusDays(15),
                "Strong leadership examples. Clear ownership mentality. Handled conflict scenarios well.",
                Map.of("leadershipScore", 90, "communicationScore", 88, "problemSolvingScore", 86,
                        "starStoriesProvided", 5)),

            // C005 / A005 — Platform Engineer (HIRED)
            new AssessmentResult("AS008", "C005", "A005", AssessmentType.CODING_CHALLENGE,
                88, 100, 91, base.minusDays(75),
                "Solved all problems in Go. Particularly strong on concurrency patterns.",
                Map.of("problemsSolved", 3, "optimalSolutions", 3, "timeUsedMinutes", 60,
                        "languages", List.of("Go", "Rust"))),
            new AssessmentResult("AS009", "C005", "A005", AssessmentType.TAKE_HOME_PROJECT,
                95, 100, 99, base.minusDays(68),
                "Built a fully functional CI/CD pipeline tool in Go. Clean code, comprehensive tests, excellent docs.",
                Map.of("codeQuality", 96, "testCoverage", 94, "documentation", 98,
                        "repoLink", "github.com/emmadavis/pipeline-demo")),

            // C006 / A007 — Software Engineer
            new AssessmentResult("AS010", "C006", "A007", AssessmentType.CODING_CHALLENGE,
                78, 100, 72, base.minusDays(2),
                "Solved 2/3 problems optimally. Showed strong Java concurrency knowledge.",
                Map.of("problemsSolved", 3, "optimalSolutions", 2, "timeUsedMinutes", 80,
                        "languages", List.of("Java")))
        ).forEach(a -> store.put(a.id(), a));
    }

    public Optional<AssessmentResult> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<AssessmentResult> findByCandidate(String candidateId) {
        return store.values().stream()
                .filter(a -> a.candidateId().equals(candidateId))
                .sorted(Comparator.comparing(AssessmentResult::completedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<AssessmentResult> findByApplication(String applicationId) {
        return store.values().stream()
                .filter(a -> a.applicationId().equals(applicationId))
                .sorted(Comparator.comparing(AssessmentResult::completedAt).reversed())
                .collect(Collectors.toList());
    }

    public Optional<AssessmentResult> findByCandidateAndType(String candidateId, AssessmentType type) {
        return store.values().stream()
                .filter(a -> a.candidateId().equals(candidateId) && a.type() == type)
                .max(Comparator.comparing(AssessmentResult::completedAt));
    }

    public OptionalDouble averageScorePercent(String candidateId) {
        return store.values().stream()
                .filter(a -> a.candidateId().equals(candidateId))
                .mapToDouble(AssessmentResult::scorePercent)
                .average();
    }
}
