package com.example.mcpserver.dto.talentprofile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contains all responses to a candidate questionnaire.
 */
public record QuestionnaireResponses(
        String questionnaireVersion,
        LocalDateTime completedAt,
        List<QuestionResponse> responses
) {
}
