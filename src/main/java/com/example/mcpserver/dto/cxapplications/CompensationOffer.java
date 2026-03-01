package com.example.mcpserver.dto.cxapplications;

import java.util.List;

/**
 * Details of the compensation package in an offer.
 */
public record CompensationOffer(
        Integer baseSalary,
        Integer signingBonus,
        Integer equityShares,
        String currency,
        String equityValue,
        String startDate,
        List<String> benefits
) {
}
