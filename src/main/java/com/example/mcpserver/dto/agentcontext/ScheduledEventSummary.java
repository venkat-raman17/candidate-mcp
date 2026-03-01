package com.example.mcpserver.dto.agentcontext;

import com.example.mcpserver.dto.common.enums.EventType;
import com.example.mcpserver.dto.common.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PII-stripped event summary with interviewer IDs removed.
 * Only interviewer names are retained for agent context.
 */
public record ScheduledEventSummary(
        EventType type,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        String location,
        List<String> interviewerNames,
        EventStatus status
) {
}
