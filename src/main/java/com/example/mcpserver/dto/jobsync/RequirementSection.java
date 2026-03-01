package com.example.mcpserver.dto.jobsync;

import com.example.mcpserver.dto.common.enums.EducationLevel;

import java.util.List;

/**
 * Job requirements section.
 */
public record RequirementSection(
        List<String> requiredSkills,
        List<String> preferredSkills,
        Integer minYearsExperience,
        EducationLevel minEducation
) {
}
