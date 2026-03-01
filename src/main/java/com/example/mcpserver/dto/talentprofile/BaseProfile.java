package com.example.mcpserver.dto.talentprofile;

import com.example.mcpserver.dto.common.EducationSummary;
import com.example.mcpserver.dto.common.SkillEndorsement;
import com.example.mcpserver.model.CandidateStatus;

import java.util.List;

/**
 * Base candidate profile information.
 * Contains professional identity and current status.
 */
public record BaseProfile(
        String displayName,
        String professionalEmail,
        String linkedinUrl,
        String location,
        Integer yearsOfExperience,
        String currentRole,
        String currentCompany,
        EducationSummary education,
        List<SkillEndorsement> skills,
        CandidateStatus status
) {
}
