package com.example.mcpserver.dto.common;

import com.example.mcpserver.dto.common.enums.EducationLevel;

/**
 * Summary of candidate's education background.
 * PII note: GPA and transcripts are NOT included unless candidate opts in.
 */
public record EducationSummary(
        EducationLevel highestDegree,
        String major,
        String institution,
        Integer graduationYear
) {
}
