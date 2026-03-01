package com.example.mcpserver.dto.agentcontext;

import com.example.mcpserver.dto.common.enums.OfferStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PII-stripped offer summary with internal negotiation details removed.
 */
public record OfferSummary(
        LocalDateTime offerExtendedAt,
        LocalDateTime offerExpiresAt,
        OfferStatus status,
        String salaryRangeDisplay,
        LocalDate startDate
) {
}
