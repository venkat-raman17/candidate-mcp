package com.example.mcpserver.dto.cxapplications;

import java.util.List;

/**
 * Contains all scheduling information for an application.
 */
public record ScheduleMetadata(
        List<ScheduledEvent> events,
        String calendarLink,
        String timezone
) {
}
