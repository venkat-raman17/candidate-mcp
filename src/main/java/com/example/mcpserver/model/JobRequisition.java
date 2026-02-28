package com.example.mcpserver.model;

import java.time.LocalDateTime;
import java.util.List;

public record JobRequisition(
        String id,
        String title,
        String department,
        String location,
        JobType type,
        JobStatus status,
        String description,
        List<String> requiredSkills,
        List<String> preferredSkills,
        String salaryRange,
        String hiringManagerId,
        String hiringManagerName,
        LocalDateTime openedAt
) {}
