package com.example.mcpserver.service;

import com.example.mcpserver.model.Candidate;
import com.example.mcpserver.model.JobRequisition;
import com.example.mcpserver.model.JobStatus;
import com.example.mcpserver.model.JobType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class JobService {

    private final Map<String, JobRequisition> store = new ConcurrentHashMap<>();

    public JobService() {
        List.of(
            new JobRequisition("J001", "Senior Software Engineer", "Engineering",
                "San Francisco, CA / Remote", JobType.FULL_TIME, JobStatus.OPEN,
                "Join our platform team to build the next generation of our core product. " +
                "You'll architect and implement scalable backend services handling millions of users.",
                List.of("Java", "Spring Boot", "Microservices", "AWS"),
                List.of("Kafka", "Kubernetes", "System Design"),
                "$160,000 - $210,000", "HM001", "Sarah Connor",
                LocalDateTime.now().minusDays(45)),
            new JobRequisition("J002", "Machine Learning Engineer", "Data Science",
                "New York, NY / Hybrid", JobType.FULL_TIME, JobStatus.OPEN,
                "Build and deploy production ML models that power our recommendation and fraud-detection systems. " +
                "Partner with data scientists to productionize research work.",
                List.of("Python", "Machine Learning", "TensorFlow", "MLOps"),
                List.of("PyTorch", "Spark", "Kubernetes"),
                "$150,000 - $200,000", "HM002", "John Wick",
                LocalDateTime.now().minusDays(30)),
            new JobRequisition("J003", "Platform Engineer", "Infrastructure",
                "Remote", JobType.FULL_TIME, JobStatus.FILLED,
                "Own and evolve our internal developer platform. Drive developer productivity initiatives " +
                "and build the tooling, CI/CD pipelines, and observability stack used by all engineering teams.",
                List.of("Go", "Kubernetes", "Terraform", "CI/CD"),
                List.of("Rust", "Linux", "Observability"),
                "$140,000 - $180,000", "HM003", "Diana Prince",
                LocalDateTime.now().minusDays(90))
        ).forEach(j -> store.put(j.id(), j));
    }

    public Optional<JobRequisition> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<JobRequisition> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(JobRequisition::id))
                .collect(Collectors.toList());
    }

    public List<JobRequisition> findActive() {
        return store.values().stream()
                .filter(j -> j.status() == JobStatus.OPEN)
                .sorted(Comparator.comparing(JobRequisition::id))
                .collect(Collectors.toList());
    }

    public List<JobRequisition> findByDepartment(String department) {
        return store.values().stream()
                .filter(j -> j.department().equalsIgnoreCase(department))
                .sorted(Comparator.comparing(JobRequisition::id))
                .collect(Collectors.toList());
    }

    /** Returns all open jobs sorted by match score descending (inverted candidate match). */
    public List<Map<String, Object>> findMatchingJobs(Candidate candidate, int minScore) {
        return store.values().stream()
                .filter(j -> j.status() == JobStatus.OPEN)
                .map(j -> matchScore(candidate, j))
                .filter(m -> (int) m.get("overallScore") >= minScore)
                .sorted(Comparator.comparingInt(m -> -(int) m.get("overallScore")))
                .collect(Collectors.toList());
    }

    /** Returns a 0-100 match score with a breakdown of matched / missing skills. */
    public Map<String, Object> matchScore(Candidate candidate, JobRequisition job) {
        Set<String> candidateSkills = candidate.skills().stream()
                .map(String::toLowerCase).collect(Collectors.toSet());

        List<String> requiredMatched = job.requiredSkills().stream()
                .filter(s -> candidateSkills.contains(s.toLowerCase()))
                .collect(Collectors.toList());
        List<String> requiredMissing = job.requiredSkills().stream()
                .filter(s -> !candidateSkills.contains(s.toLowerCase()))
                .collect(Collectors.toList());
        List<String> preferredMatched = job.preferredSkills().stream()
                .filter(s -> candidateSkills.contains(s.toLowerCase()))
                .collect(Collectors.toList());

        double requiredScore = job.requiredSkills().isEmpty() ? 100
                : (double) requiredMatched.size() / job.requiredSkills().size() * 70;
        double preferredScore = job.preferredSkills().isEmpty() ? 0
                : (double) preferredMatched.size() / job.preferredSkills().size() * 20;
        double expScore = Math.min(candidate.yearsOfExperience() / 10.0 * 10, 10);
        int totalScore = (int) Math.round(requiredScore + preferredScore + expScore);

        return Map.of(
                "candidateId",       candidate.id(),
                "candidateName",     candidate.name(),
                "jobId",             job.id(),
                "jobTitle",          job.title(),
                "overallScore",      totalScore,
                "requiredMatched",   requiredMatched,
                "requiredMissing",   requiredMissing,
                "preferredMatched",  preferredMatched,
                "yearsOfExperience", candidate.yearsOfExperience(),
                "recommendation",    totalScore >= 70 ? "STRONG_MATCH"
                                   : totalScore >= 50 ? "PARTIAL_MATCH" : "WEAK_MATCH"
        );
    }
}
