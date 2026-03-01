package com.example.mcpserver.dto.cxapplications;

import java.time.LocalDateTime;

/**
 * Tracks a single round of offer negotiation.
 */
public record NegotiationRound(
        Integer roundNumber,
        LocalDateTime requestedAt,
        LocalDateTime respondedAt,
        String requestedBy,
        String requestType,
        String requestDetails,
        String response
) {
}
