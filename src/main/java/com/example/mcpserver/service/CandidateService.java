package com.example.mcpserver.service;

import com.example.mcpserver.model.Candidate;
import com.example.mcpserver.model.CandidateStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private final Map<String, Candidate> store = new ConcurrentHashMap<>();

    public CandidateService() {
        List.of(
            new Candidate("C001", "Alice Johnson", "alice.johnson@example.com", "+1-555-0101",
                "San Francisco, CA",
                List.of("Java", "Spring Boot", "AWS", "Kubernetes", "Microservices"),
                8, "Senior Software Engineer", "Acme Corp",
                CandidateStatus.ACTIVE,
                "Experienced Java engineer with cloud-native expertise. Led migration of monolith to microservices for 2M+ user platform.",
                "linkedin.com/in/alice-johnson", LocalDateTime.now().minusDays(30)),
            new Candidate("C002", "Bob Smith", "bob.smith@example.com", "+1-555-0102",
                "New York, NY",
                List.of("Python", "Machine Learning", "TensorFlow", "PyTorch", "MLOps"),
                5, "ML Engineer", "DataDriven Inc",
                CandidateStatus.ACTIVE,
                "ML engineer specializing in production NLP systems. Published researcher with 3 patents in deep learning.",
                "linkedin.com/in/bob-smith-ml", LocalDateTime.now().minusDays(20)),
            new Candidate("C003", "Carol Williams", "carol.w@example.com", "+1-555-0103",
                "Austin, TX",
                List.of("React", "TypeScript", "Node.js", "GraphQL", "PostgreSQL"),
                6, "Full Stack Developer", "StartupXYZ",
                CandidateStatus.ACTIVE,
                "Full-stack developer with modern web stack expertise. Built and shipped 4 SaaS products from 0 to 1.",
                "linkedin.com/in/carol-williams-dev", LocalDateTime.now().minusDays(15)),
            new Candidate("C004", "David Brown", "david.brown@example.com", "+1-555-0104",
                "Seattle, WA",
                List.of("Java", "Microservices", "Kafka", "Docker", "System Design", "Architecture"),
                12, "Lead Software Architect", "BigTech LLC",
                CandidateStatus.ACTIVE,
                "Architect with 12 years designing high-throughput distributed systems. Scaled platform to handle 500K TPS.",
                "linkedin.com/in/david-brown-arch", LocalDateTime.now().minusDays(45)),
            new Candidate("C005", "Emma Davis", "emma.davis@example.com", "+1-555-0105",
                "Remote",
                List.of("Go", "Rust", "Linux", "Terraform", "CI/CD", "Observability"),
                7, "Platform Engineer", "CloudNative Co",
                CandidateStatus.HIRED,
                "Platform engineer who built internal developer platform used by 200+ engineers. SRE background.",
                "linkedin.com/in/emma-davis-platform", LocalDateTime.now().minusDays(60)),
            new Candidate("C006", "Frank Lee", "frank.lee@example.com", "+1-555-0106",
                "Chicago, IL",
                List.of("Java", "Spring Boot", "Kafka", "Redis", "MySQL"),
                4, "Software Engineer", "FinTech Solutions",
                CandidateStatus.ACTIVE,
                "Backend engineer with fintech background. Built real-time payment processing system handling $10M/day.",
                "linkedin.com/in/frank-lee-dev", LocalDateTime.now().minusDays(10))
        ).forEach(c -> store.put(c.id(), c));
    }

    public Optional<Candidate> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Candidate> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Candidate::id))
                .collect(Collectors.toList());
    }

    public List<Candidate> findPage(String afterId, int pageSize) {
        List<Candidate> all = findAll();
        int startIdx = 0;
        if (afterId != null && !afterId.isBlank()) {
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id().equals(afterId)) { startIdx = i + 1; break; }
            }
        }
        return all.subList(startIdx, Math.min(startIdx + pageSize, all.size()));
    }

    public List<Candidate> search(String query, List<String> skills, Integer minExp, String location) {
        String q = query == null ? "" : query.toLowerCase();
        return store.values().stream()
                .filter(c -> q.isEmpty()
                        || c.name().toLowerCase().contains(q)
                        || c.currentRole().toLowerCase().contains(q)
                        || c.summary().toLowerCase().contains(q))
                .filter(c -> skills == null || skills.isEmpty()
                        || skills.stream().anyMatch(s -> c.skills().stream()
                                .anyMatch(cs -> cs.equalsIgnoreCase(s))))
                .filter(c -> minExp == null || c.yearsOfExperience() >= minExp)
                .filter(c -> location == null || location.isBlank()
                        || c.location().toLowerCase().contains(location.toLowerCase()))
                .sorted(Comparator.comparing(Candidate::id))
                .collect(Collectors.toList());
    }

    public List<Candidate> findByStatus(CandidateStatus status) {
        return store.values().stream()
                .filter(c -> c.status() == status)
                .sorted(Comparator.comparing(Candidate::id))
                .collect(Collectors.toList());
    }

    public Candidate add(Candidate candidate) {
        store.put(candidate.id(), candidate);
        return candidate;
    }

    public Optional<Candidate> updateStatus(String id, CandidateStatus status) {
        return findById(id).map(c -> {
            Candidate updated = new Candidate(c.id(), c.name(), c.email(), c.phone(),
                    c.location(), c.skills(), c.yearsOfExperience(), c.currentRole(),
                    c.currentCompany(), status, c.summary(), c.linkedinUrl(), c.createdAt());
            store.put(id, updated);
            return updated;
        });
    }

    public List<String> allSkills() {
        return store.values().stream()
                .flatMap(c -> c.skills().stream())
                .distinct().sorted().collect(Collectors.toList());
    }

    public int totalCount() {
        return store.size();
    }
}
