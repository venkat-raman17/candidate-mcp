package com.example.mcpserver.dto.talentprofile;

import com.example.mcpserver.dto.common.enums.ShiftType;
import com.example.mcpserver.dto.common.enums.WorkMode;

import java.util.List;

/**
 * Candidate's work style and schedule preferences.
 */
public record WorkStylePreferences(
        WorkMode preferredWorkMode,
        Integer preferredOnsiteDays,
        List<ShiftType> acceptableShifts,
        Boolean willingToWorkWeekends,
        Boolean willingToBeOnCall
) {
}
