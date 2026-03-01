package com.example.mcpserver.dto.jobsync;

/**
 * Bonus structure for compensation package.
 */
public record BonusStructure(
        Boolean hasSigningBonus,
        Integer signingBonusAmount,
        Boolean hasPerformanceBonus,
        String performanceBonusRange,  // "10-20%"
        Boolean hasEquity,
        String equityDescription
) {
}
