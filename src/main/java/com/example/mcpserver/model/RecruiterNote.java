package com.example.mcpserver.model;

import java.time.LocalDateTime;

public record RecruiterNote(
        String id,
        String applicationId,
        String note,
        String authorId,
        String authorName,
        LocalDateTime createdAt
) {}
