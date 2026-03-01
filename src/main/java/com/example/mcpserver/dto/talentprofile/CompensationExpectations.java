package com.example.mcpserver.dto.talentprofile;

import java.util.List;

/**
 * Candidate's compensation expectations and requirements.
 */
public record CompensationExpectations(
        String currency,
        Integer minBaseSalary,
        Integer targetBaseSalary,
        Boolean requiresEquity,
        Boolean requiresBonus,
        List<String> mustHaveBenefits
) {
}
