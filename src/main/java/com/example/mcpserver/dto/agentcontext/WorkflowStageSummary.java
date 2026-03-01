package com.example.mcpserver.dto.agentcontext;

import java.time.LocalDateTime;

/**
 * PII-stripped summary of workflow stage progression.
 */
public record WorkflowStageSummary(
        String stage,
        LocalDateTime enteredAt,
        Integer daysInStage
) {
}
