package com.example.mcpserver.dto.jobsync;

import com.example.mcpserver.dto.common.enums.ShiftType;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Shift and work schedule details for a job requisition.
 */
public record ShiftDetails(
        ShiftType type,
        String timeZone,
        String startTime,           // "09:00" format
        String endTime,             // "17:00" format
        List<DayOfWeek> workDays,
        Boolean remoteEligible,
        Integer onsiteDaysPerWeek
) {
    public static ShiftDetails standard() {
        return new ShiftDetails(
                ShiftType.DAY,
                "America/Los_Angeles",
                "09:00",
                "17:00",
                List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
                true,
                0
        );
    }
}
