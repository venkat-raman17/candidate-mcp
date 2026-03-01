package com.example.mcpserver.dto.cxapplications;

import java.time.LocalDateTime;

/**
 * Internal note added by recruiter during application review.
 */
public record RecruiterNote(
        String noteId,
        String applicationId,
        String note,
        String authorId,
        String authorName,
        LocalDateTime createdAt
) {
}
