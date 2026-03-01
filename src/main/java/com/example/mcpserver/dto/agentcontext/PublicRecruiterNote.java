package com.example.mcpserver.dto.agentcontext;

import java.time.LocalDateTime;

/**
 * PII-stripped recruiter note with author ID removed.
 * Only author name is retained.
 */
public record PublicRecruiterNote(
        LocalDateTime createdAt,
        String note,
        String authorName
) {
}
