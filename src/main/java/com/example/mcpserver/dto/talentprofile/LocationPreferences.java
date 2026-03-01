package com.example.mcpserver.dto.talentprofile;

import java.util.List;

/**
 * Candidate's location and relocation preferences.
 */
public record LocationPreferences(
        List<String> preferredCities,
        List<String> preferredStates,
        Boolean openToRelocation,
        Boolean requiresVisaSponsorship,
        Integer maxCommuteMinutes
) {
}
