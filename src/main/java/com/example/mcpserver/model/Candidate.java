package com.example.mcpserver.model;

import java.util.List;

public record Candidate(
        String id,
        String name,
        String email,
        String phone,
        List<String> skills,
        int yearsOfExperience,
        String currentRole,
        CandidateStatus status,
        String summary
) {}
