package com.example.mcpserver.model;

import java.time.LocalDateTime;
import java.util.List;

public record Candidate(
        String id,
        String name,
        String email,
        String phone,
        String location,
        List<String> skills,
        int yearsOfExperience,
        String currentRole,
        String currentCompany,
        CandidateStatus status,
        String summary,
        String linkedinUrl,
        LocalDateTime createdAt
) {}
