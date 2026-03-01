package com.example.mcpserver.transformer;

import com.example.mcpserver.dto.agentcontext.ProfileAgentContext;
import com.example.mcpserver.dto.talentprofile.AssessmentResult;
import com.example.mcpserver.dto.talentprofile.AssessmentResults;
import com.example.mcpserver.dto.talentprofile.CandidateProfileV2;
import com.example.mcpserver.dto.talentprofile.QuestionnaireResponses;
import com.example.mcpserver.model.AssessmentType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transformer that converts CandidateProfileV2 into agent-safe ProfileAgentContext.
 * <p>
 * This transformer implements Layer 1 data processing with the following operations:
 * </p>
 * <h3>Removed Fields (PII - CRITICAL SECURITY)</h3>
 * <ul>
 *   <li>{@code nationalId} - National identification number</li>
 *   <li>{@code ssnLast4} - Social Security Number (last 4 digits)</li>
 *   <li>{@code dateOfBirth} - Birth date</li>
 *   <li>{@code homeAddress} - Full home address</li>
 *   <li>{@code personalEmail} - Personal email address</li>
 *   <li>{@code personalPhone} - Personal phone number</li>
 *   <li>{@code emergencyContact} - Emergency contact information</li>
 *   <li>{@code _cosmosPartitionKey} - Database implementation detail</li>
 *   <li>{@code _etag} - Concurrency control metadata</li>
 * </ul>
 *
 * <h3>Derived Fields (Computed for Agent Use)</h3>
 * <ul>
 *   <li>{@code totalAssessmentsCompleted} - Count of completed assessments</li>
 *   <li>{@code averagePercentilesByType} - Average percentile score by assessment type</li>
 *   <li>{@code questionnaireCompleted} - Boolean flag indicating completion status</li>
 * </ul>
 *
 * <h3>Retained Fields (Safe for Agent Use)</h3>
 * <ul>
 *   <li>Candidate ID (for cross-referencing)</li>
 *   <li>Display name (from base profile)</li>
 *   <li>Professional location (city, state)</li>
 *   <li>Years of experience</li>
 *   <li>Current role and company</li>
 *   <li>Education summary</li>
 *   <li>Skills with proficiency levels</li>
 *   <li>Assessment results (scores, percentiles)</li>
 *   <li>Preferences (location, job, work style)</li>
 *   <li>Profile status</li>
 * </ul>
 *
 * <p>
 * <strong>Privacy Note:</strong> This transformer is the critical layer for PII protection.
 * Any changes to this class must be reviewed for privacy compliance.
 * </p>
 *
 * @see CandidateProfileV2
 * @see ProfileAgentContext
 * @since 1.0
 */
@Component
public class ProfileTransformer implements AgentContextTransformer<CandidateProfileV2, ProfileAgentContext> {

    /**
     * Transforms a candidate profile into an agent-safe context.
     * <p>
     * This method strips all PII and sensitive data, retaining only information
     * necessary for candidate evaluation and matching.
     * </p>
     *
     * @param source the raw candidate profile from TalentProfile system
     * @return agent-safe profile context, or null if source is null
     */
    @Override
    public ProfileAgentContext transform(CandidateProfileV2 source) {
        if (source == null) {
            return null;
        }

        var baseProfile = source.baseProfile();
        var assessments = source.assessments();
        var questionnaire = source.questionnaires();

        return new ProfileAgentContext(
                source.candidateId(),
                baseProfile != null ? baseProfile.displayName() : "Unknown",
                baseProfile != null ? baseProfile.location() : null,
                baseProfile != null ? baseProfile.yearsOfExperience() : null,
                baseProfile != null ? baseProfile.currentRole() : null,
                baseProfile != null ? baseProfile.currentCompany() : null,
                baseProfile != null ? baseProfile.education() : null,
                baseProfile != null ? baseProfile.skills() : List.of(),
                baseProfile != null ? baseProfile.status() : null,
                calculateTotalAssessments(assessments),
                calculateAveragePercentilesByType(assessments),
                source.preferences() != null ? source.preferences().location() : null,
                source.preferences() != null ? source.preferences().job() : null,
                source.preferences() != null ? source.preferences().workStyle() : null,
                questionnaire != null && questionnaire.completedAt() != null,
                questionnaire != null ? questionnaire.completedAt() : null
        );
    }

    /**
     * Calculates total number of completed assessments.
     *
     * @param assessments the assessment results
     * @return count of assessments completed
     */
    private int calculateTotalAssessments(AssessmentResults assessments) {
        if (assessments == null || assessments.results() == null) {
            return 0;
        }
        return assessments.results().size();
    }

    /**
     * Calculates average percentile scores grouped by assessment type.
     * <p>
     * This helps agents understand candidate strengths across different skill areas:
     * <ul>
     *   <li>CODING_CHALLENGE: Average of all coding assessments</li>
     *   <li>SYSTEM_DESIGN: Average of system design assessments</li>
     *   <li>TECHNICAL_SCREENING: Average of technical knowledge assessments</li>
     *   <li>BEHAVIORAL: Average of behavioral assessments</li>
     * </ul>
     * </p>
     *
     * @param assessments the assessment results
     * @return map of assessment type to average percentile
     */
    private Map<AssessmentType, Integer> calculateAveragePercentilesByType(AssessmentResults assessments) {
        if (assessments == null || assessments.results() == null || assessments.results().isEmpty()) {
            return Map.of();
        }

        Map<AssessmentType, List<Integer>> groupedPercentiles = new HashMap<>();

        for (AssessmentResult result : assessments.results()) {
            if (result.percentile() != null) {
                groupedPercentiles
                        .computeIfAbsent(result.type(), k -> new java.util.ArrayList<>())
                        .add(result.percentile());
            }
        }

        Map<AssessmentType, Integer> averages = new HashMap<>();
        for (Map.Entry<AssessmentType, List<Integer>> entry : groupedPercentiles.entrySet()) {
            int average = (int) entry.getValue().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            averages.put(entry.getKey(), average);
        }

        return averages;
    }
}
