package com.example.mcpserver.model;

import java.time.LocalDateTime;
import java.util.List;

public record Application(
        String id,
        String candidateId,
        String jobId,
        ApplicationStatus status,
        ApplicationSource source,
        LocalDateTime appliedAt,
        int currentInterviewRound,
        List<StatusHistoryEntry> statusHistory,
        List<RecruiterNote> notes
) {}
