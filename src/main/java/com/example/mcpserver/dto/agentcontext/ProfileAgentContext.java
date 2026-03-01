package com.example.mcpserver.dto.agentcontext;

import com.example.mcpserver.dto.common.EducationSummary;
import com.example.mcpserver.dto.common.SkillEndorsement;
import com.example.mcpserver.dto.talentprofile.JobPreferences;
import com.example.mcpserver.dto.talentprofile.LocationPreferences;
import com.example.mcpserver.dto.talentprofile.WorkStylePreferences;
import com.example.mcpserver.model.AssessmentType;
import com.example.mcpserver.model.CandidateStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Layer 1 projection of CandidateProfileV2 for agent consumption.
 * PII-stripped with aggregated assessment metrics.
 * Excludes: nationalId, ssnLast4, homeAddress, personalEmail, personalPhone,
 *           dateOfBirth, emergencyContact, Cosmos metadata.
 */
public record ProfileAgentContext(
        String candidateId,
        String displayName,
        String location,
        Integer yearsOfExperience,
        String currentRole,
        String currentCompany,
        EducationSummary education,
        List<SkillEndorsement> skills,
        CandidateStatus status,
        Integer totalAssessmentsCompleted,
        Map<AssessmentType, Integer> averagePercentilesByType,
        LocationPreferences locationPreferences,
        JobPreferences jobPreferences,
        WorkStylePreferences workStylePreferences,
        Boolean questionnaireCompleted,
        LocalDateTime questionnaireCompletedAt
) {
}
