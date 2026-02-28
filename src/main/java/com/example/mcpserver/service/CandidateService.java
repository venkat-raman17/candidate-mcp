package com.example.mcpserver.service;

import com.example.mcpserver.model.Candidate;
import com.example.mcpserver.model.CandidateStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private final Map<String, Candidate> store = new ConcurrentHashMap<>();

    public CandidateService() {
        List.of(
            new Candidate("C001", "Alice Johnson", "alice@example.com", "+1-555-0101",
                List.of("Java", "Spring Boot", "AWS", "Kubernetes"), 8,
                "Senior Software Engineer", CandidateStatus.INTERVIEW,
                "Experienced Java developer with strong cloud-native expertise."),
            new Candidate("C002", "Bob Smith", "bob@example.com", "+1-555-0102",
                List.of("Python", "Machine Learning", "TensorFlow", "PyTorch"), 5,
                "ML Engineer", CandidateStatus.SCREENING,
                "ML engineer specializing in NLP and computer vision models."),
            new Candidate("C003", "Carol Williams", "carol@example.com", "+1-555-0103",
                List.of("React", "TypeScript", "Node.js", "GraphQL"), 6,
                "Full Stack Developer", CandidateStatus.APPLIED,
                "Full-stack developer with modern web technology expertise."),
            new Candidate("C004", "David Brown", "david@example.com", "+1-555-0104",
                List.of("Java", "Microservices", "Kafka", "Docker"), 10,
                "Lead Software Architect", CandidateStatus.OFFER,
                "Architect with 10 years building scalable distributed systems."),
            new Candidate("C005", "Emma Davis", "emma@example.com", "+1-555-0105",
                List.of("Go", "Rust", "Linux", "DevOps", "CI/CD"), 7,
                "Platform Engineer", CandidateStatus.HIRED,
                "Platform engineer skilled in systems programming and DevOps.")
        ).forEach(c -> store.put(c.id(), c));
    }

    public Optional<Candidate> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Candidate> findAll() {
        return new ArrayList<>(store.values());
    }

    public List<Candidate> search(String query) {
        String q = query.toLowerCase();
        return store.values().stream()
                .filter(c -> c.name().toLowerCase().contains(q)
                        || c.skills().stream().anyMatch(s -> s.toLowerCase().contains(q))
                        || c.currentRole().toLowerCase().contains(q)
                        || c.summary().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    public List<Candidate> findByStatus(CandidateStatus status) {
        return store.values().stream()
                .filter(c -> c.status() == status)
                .collect(Collectors.toList());
    }

    public Candidate add(Candidate candidate) {
        store.put(candidate.id(), candidate);
        return candidate;
    }

    public Optional<Candidate> updateStatus(String id, CandidateStatus newStatus) {
        return findById(id).map(c -> {
            Candidate updated = new Candidate(c.id(), c.name(), c.email(), c.phone(),
                    c.skills(), c.yearsOfExperience(), c.currentRole(), newStatus, c.summary());
            store.put(id, updated);
            return updated;
        });
    }

    public List<String> allSkills() {
        return store.values().stream()
                .flatMap(c -> c.skills().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
