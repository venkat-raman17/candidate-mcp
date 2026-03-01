package com.example.mcpserver.dto.common.enums;

/**
 * Status of an employment offer.
 */
public enum OfferStatus {
    /**
     * Offer extended, awaiting candidate response
     */
    PENDING,

    /**
     * Candidate accepted the offer
     */
    ACCEPTED,

    /**
     * Candidate declined the offer
     */
    DECLINED,

    /**
     * Active negotiation in progress
     */
    NEGOTIATING,

    /**
     * Offer expired before candidate response
     */
    EXPIRED,

    /**
     * Offer withdrawn by company
     */
    WITHDRAWN
}
