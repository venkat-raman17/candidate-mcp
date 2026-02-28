package com.example.mcpserver.model;

import java.time.LocalDateTime;

public record StatusHistoryEntry(
        ApplicationStatus status,
        LocalDateTime changedAt,
        String changedBy,
        String reason
) {}
