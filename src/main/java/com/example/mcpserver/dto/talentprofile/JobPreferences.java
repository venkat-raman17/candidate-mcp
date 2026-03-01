package com.example.mcpserver.dto.talentprofile;

import com.example.mcpserver.model.JobType;

import java.time.LocalDate;
import java.util.List;

/**
 * Candidate's job role and type preferences.
 */
public record JobPreferences(
        List<String> preferredRoles,
        List<String> preferredDepartments,
        List<JobType> preferredJobTypes,
        Boolean openToContract,
        Boolean openToInternship,
        LocalDate earliestStartDate
) {
}
