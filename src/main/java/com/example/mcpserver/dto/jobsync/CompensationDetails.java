package com.example.mcpserver.dto.jobsync;

import java.util.List;

/**
 * Compensation package details for a job requisition.
 */
public record CompensationDetails(
        String currency,
        Integer salaryRangeMin,
        Integer salaryRangeMax,
        BonusStructure bonus,
        List<String> benefits
) {
    public String getSalaryRangeDisplay() {
        return String.format("$%,dK-$%,dK", salaryRangeMin / 1000, salaryRangeMax / 1000);
    }
}
