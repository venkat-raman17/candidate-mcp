package com.example.mcpserver.dto.common;

import com.example.mcpserver.dto.common.enums.SkillLevel;

/**
 * Represents a skill with proficiency level and certification details.
 * Used in candidate profiles.
 */
public record SkillEndorsement(
        String skill,
        SkillLevel level,
        Integer yearsOfExperience,
        Boolean isCertified,
        String certificationName
) {
}
