package com.example.mcpserver.dto.cxapplications;

import com.example.mcpserver.dto.common.enums.OfferStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Contains all offer-related information for an application.
 */
public record OfferMetadata(
        String offerId,
        LocalDateTime offerExtendedAt,
        LocalDateTime offerExpiresAt,
        LocalDateTime candidateRespondedAt,
        OfferStatus offerStatus,
        CompensationOffer compensation,
        List<NegotiationRound> negotiationHistory,
        String offerLetterUrl
) {
}
