package com.example.mcpserver.dto.cxapplications;

import com.example.mcpserver.dto.common.enums.EventType;
import com.example.mcpserver.dto.common.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a scheduled interview or assessment event.
 */
public record ScheduledEvent(
        String eventId,
        EventType type,
        LocalDateTime scheduledAt,
        Integer durationMinutes,
        String location,
        List<String> interviewerIds,
        List<String> interviewerNames,
        EventStatus status
) {
}
