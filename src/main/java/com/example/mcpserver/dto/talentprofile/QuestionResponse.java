package com.example.mcpserver.dto.talentprofile;

import java.util.Map;

/**
 * Single response to a questionnaire question.
 */
public record QuestionResponse(
        String questionId,
        String questionText,
        String responseType,
        Object response,
        Map<String, Object> metadata
) {
}
