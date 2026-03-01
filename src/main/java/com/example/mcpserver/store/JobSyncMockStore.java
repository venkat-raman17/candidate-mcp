package com.example.mcpserver.store;

import com.example.mcpserver.dto.jobsync.*;
import com.example.mcpserver.model.JobStatus;
import com.example.mcpserver.model.JobType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mock data store for job-sync-service with comprehensive enterprise job scenarios.
 */
public class JobSyncMockStore {

    private static final Map<String, JobRequisitionDocument> JOBS = new HashMap<>();

    static {
        // Job 1: Senior Software Engineer (SRE) - Day Shift - OPEN
        JOBS.put("J001", new JobRequisitionDocument(
                "J001",
                "REQ-2026-001",
                "Senior Software Engineer - Site Reliability",
                "Engineering - SRE",
                "San Francisco, CA",
                JobType.FULL_TIME,
                JobStatus.OPEN,
                "We're looking for an experienced SRE to join our platform team. You'll be responsible for maintaining 99.99% uptime for our mission-critical services.",
                new RequirementSection(
                        List.of("5+ years of experience in SRE or DevOps", "Expert in Kubernetes and container orchestration", "Strong coding skills in Python or Go", "Experience with observability tools (Prometheus, Grafana)"),
                        List.of("Experience with chaos engineering", "Contributions to open-source projects", "On-call rotation experience"),
                        5,
                        com.example.mcpserver.dto.common.enums.EducationLevel.BACHELOR
                ),
                new CompensationDetails(
                        "USD", 150000, 200000,
                        new BonusStructure(true, 10000, true, "10-15%", true, "RSUs vesting over 4 years"),
                        List.of("Equity package", "401k match", "Health insurance")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.DAY, "America/Los_Angeles", "09:00", "17:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY), true, 2),
                new AssessmentCodeMapping(List.of("KUBERNETES_03", "SYS_DESIGN_02", "PYTHON_04"), Map.of("ONCALL_READINESS_01", "On-call readiness assessment (preferred)"), false),
                "MGR001",
                "Sarah Chen",
                LocalDateTime.of(2026, 2, 1, 10, 0),
                null,
                3,
                "CC-ENG-2026",
                "BUDGET-SRE-Q1",
                "Backfill for promoted engineer",
                "j001-partition",
                "etag-j001"
        ));

        // Job 2: Frontend Engineer - Flexible Shift - OPEN
        JOBS.put("J002", new JobRequisitionDocument(
                "J002",
                "REQ-2026-002",
                "Senior Frontend Engineer - React",
                "Engineering - Frontend",
                "Remote (US)",
                JobType.FULL_TIME,
                JobStatus.OPEN,
                "Build beautiful, performant user interfaces for our SaaS platform. Work with a talented team of designers and backend engineers.",
                new RequirementSection(
                        List.of("4+ years of React experience", "TypeScript expert", "Strong CSS and responsive design skills", "Experience with state management (Redux, Zustand)"),
                        List.of("Design system experience", "Accessibility (a11y) knowledge", "WebPerf optimization"),
                        4,
                        com.example.mcpserver.dto.common.enums.EducationLevel.BACHELOR
                ),
                new CompensationDetails(
                        "USD", 130000, 170000,
                        new BonusStructure(true, 8000, true, "8-12%", true, "Stock options vesting over 4 years"),
                        List.of("Equity package", "Unlimited PTO", "Remote work stipend")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.FLEXIBLE, "America/New_York", "10:00", "18:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY), true, 0),
                new AssessmentCodeMapping(List.of("REACT_01", "JAVASCRIPT_02", "SYS_DESIGN_02"), Map.of("CSS_ADVANCED_01", "Advanced CSS assessment (preferred)"), false),
                "MGR002",
                "Michael Rodriguez",
                LocalDateTime.of(2026, 2, 10, 14, 30),
                null,
                2,
                "CC-ENG-2026",
                "BUDGET-FE-Q1",
                "New headcount for platform expansion",
                "j002-partition",
                "etag-j002"
        ));

        // Job 3: Data Engineer - Night Shift - OPEN
        JOBS.put("J003", new JobRequisitionDocument(
                "J003",
                "REQ-2026-003",
                "Data Engineer - ETL Pipeline",
                "Data Engineering",
                "Austin, TX",
                JobType.FULL_TIME,
                JobStatus.OPEN,
                "Design and maintain our data warehouse and ETL pipelines processing 10TB+ daily. Night shift to ensure minimal disruption to production systems.",
                new RequirementSection(
                        List.of("3+ years of data engineering experience", "Expert in Spark, Airflow, and SQL", "Experience with AWS data services (Redshift, Glue, S3)", "Python for data processing"),
                        List.of("Real-time streaming (Kafka, Flink)", "Data quality frameworks (Great Expectations)", "dbt experience"),
                        3,
                        com.example.mcpserver.dto.common.enums.EducationLevel.BACHELOR
                ),
                new CompensationDetails(
                        "USD", 120000, 160000,
                        new BonusStructure(true, 5000, true, "8-10% + night shift differential", true, "RSUs vesting over 4 years"),
                        List.of("Equity package", "401k match", "Night shift premium pay")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.NIGHT, "America/Chicago", "22:00", "06:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY), false, 5),
                new AssessmentCodeMapping(List.of("SQL_ADVANCED_01", "PYTHON_04", "DATA_MODELING_02"), Map.of("SPARK_01", "Apache Spark assessment (preferred)"), false),
                "MGR003",
                "Jennifer Wu",
                LocalDateTime.of(2026, 1, 15, 9, 0),
                null,
                1,
                "CC-DATA-2026",
                "BUDGET-DE-Q1",
                "Pipeline optimization initiative",
                "j003-partition",
                "etag-j003"
        ));

        // Job 4: DevOps Engineer - Rotating Shift - OPEN
        JOBS.put("J004", new JobRequisitionDocument(
                "J004",
                "REQ-2026-004",
                "DevOps Engineer - Infrastructure",
                "Engineering - DevOps",
                "Seattle, WA",
                JobType.FULL_TIME,
                JobStatus.OPEN,
                "Manage our cloud infrastructure across AWS and GCP. Rotating shifts to provide 24/7 coverage for critical systems.",
                new RequirementSection(
                        List.of("4+ years DevOps/Infrastructure experience", "Terraform and Infrastructure as Code", "CI/CD pipelines (GitHub Actions, Jenkins)", "Multi-cloud experience (AWS + GCP)"),
                        List.of("Security certifications (AWS Security Specialty)", "Cost optimization experience", "Disaster recovery planning"),
                        4,
                        com.example.mcpserver.dto.common.enums.EducationLevel.BACHELOR
                ),
                new CompensationDetails(
                        "USD", 140000, 180000,
                        new BonusStructure(true, 10000, true, "12-15% + shift differential", true, "RSUs vesting over 4 years"),
                        List.of("Equity package", "401k match", "Shift rotation premium")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.ROTATING, "America/Los_Angeles", "08:00", "16:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY, java.time.DayOfWeek.SUNDAY), false, 5),
                new AssessmentCodeMapping(List.of("KUBERNETES_03", "TERRAFORM_01", "AWS_ADVANCED_01"), Map.of("GCP_CLOUD_01", "Google Cloud Platform assessment (preferred)"), false),
                "MGR004",
                "David Park",
                LocalDateTime.of(2026, 2, 20, 11, 0),
                null,
                2,
                "CC-DEVOPS-2026",
                "BUDGET-DEVOPS-Q1",
                "24/7 coverage expansion",
                "j004-partition",
                "etag-j004"
        ));

        // Job 5: Junior Software Engineer - Day Shift - CLOSED (filled)
        JOBS.put("J005", new JobRequisitionDocument(
                "J005",
                "REQ-2026-005",
                "Junior Software Engineer - Backend",
                "Engineering - Backend",
                "New York, NY",
                JobType.FULL_TIME,
                JobStatus.CLOSED,
                "Great entry-level role for new grads. Work with senior engineers on microservices architecture.",
                new RequirementSection(
                        List.of("CS degree or equivalent", "Java or Python experience", "Understanding of REST APIs and databases", "Passion for learning and growth"),
                        List.of("Internship experience", "Open-source contributions", "Personal projects"),
                        0,
                        com.example.mcpserver.dto.common.enums.EducationLevel.BACHELOR
                ),
                new CompensationDetails(
                        "USD", 90000, 110000,
                        new BonusStructure(true, 5000, true, "5-8%", true, "Stock options vesting over 4 years"),
                        List.of("Equity package", "401k match", "Learning budget")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.DAY, "America/New_York", "09:00", "17:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY), true, 0),
                new AssessmentCodeMapping(List.of("JAVA_01", "SQL_BASIC_01", "CODING_FUNDAMENTALS_01"), Map.of(), false),
                "MGR002",
                "Michael Rodriguez",
                LocalDateTime.of(2026, 1, 5, 10, 0),
                LocalDateTime.of(2026, 2, 28, 16, 0),
                1,
                "CC-ENG-2026",
                "BUDGET-BE-Q1",
                "Junior hire for team growth",
                "j005-partition",
                "etag-j005"
        ));

        // Job 6: Customer Support Engineer - On-Call Shift - OPEN
        JOBS.put("J006", new JobRequisitionDocument(
                "J006",
                "REQ-2026-006",
                "Customer Support Engineer - Enterprise",
                "Customer Success",
                "Boston, MA",
                JobType.FULL_TIME,
                JobStatus.OPEN,
                "Provide technical support to our enterprise customers. On-call rotation for critical issues.",
                new RequirementSection(
                        List.of("2+ years technical support experience", "Strong communication skills", "SQL and API debugging skills", "Customer-first mindset"),
                        List.of("SaaS product experience", "Zendesk or similar ticketing system", "Previous enterprise support role"),
                        2,
                        com.example.mcpserver.dto.common.enums.EducationLevel.HIGH_SCHOOL
                ),
                new CompensationDetails(
                        "USD", 75000, 95000,
                        new BonusStructure(false, 0, true, "3-5% quarterly", false, null),
                        List.of("401k match", "Health insurance", "On-call compensation")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.ROTATING, "America/New_York", "09:00", "17:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY), false, 3),
                new AssessmentCodeMapping(List.of("SQL_BASIC_01", "COMMUNICATION_01", "PROBLEM_SOLVING_01"), Map.of("ZENDESK_CERT_01", "Zendesk certification (preferred)"), true),
                "MGR005",
                "Amanda Foster",
                LocalDateTime.of(2026, 2, 15, 13, 0),
                null,
                2,
                "CC-CS-2026",
                "BUDGET-CS-Q1",
                "Enterprise tier expansion",
                "j006-partition",
                "etag-j006"
        ));

        // Job 7: Machine Learning Engineer - Day Shift - DRAFT (not published yet)
        JOBS.put("J007", new JobRequisitionDocument(
                "J007",
                "REQ-2026-007",
                "Machine Learning Engineer - Recommendations",
                "AI/ML",
                "San Francisco, CA",
                JobType.FULL_TIME,
                JobStatus.DRAFT,
                "Build and deploy ML models for our recommendation engine. Work with petabyte-scale data.",
                new RequirementSection(
                        List.of("3+ years ML engineering experience", "PyTorch or TensorFlow expert", "Production ML systems experience", "Strong Python and data science skills"),
                        List.of("PhD in ML/AI", "Published research papers", "RecSys experience"),
                        3,
                        com.example.mcpserver.dto.common.enums.EducationLevel.MASTER
                ),
                new CompensationDetails(
                        "USD", 160000, 210000,
                        new BonusStructure(true, 15000, true, "15-20%", true, "RSUs vesting over 4 years"),
                        List.of("Equity package", "401k match", "Research budget")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.DAY, "America/Los_Angeles", "10:00", "18:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY), true, 0),
                new AssessmentCodeMapping(List.of("ML_SYSTEMS_01", "PYTHON_04", "SYS_DESIGN_02"), Map.of("RECSYS_SPECIALIST_01", "Recommendation systems specialist certification (preferred)"), false),
                "MGR006",
                "Dr. Lisa Zhang",
                LocalDateTime.of(2026, 2, 25, 10, 0),
                null,
                1,
                "CC-ML-2026",
                "BUDGET-ML-Q1",
                "Recommendation engine v2 project",
                "j007-partition",
                "etag-j007"
        ));

        // Job 8: Security Engineer - Day Shift - OPEN
        JOBS.put("J008", new JobRequisitionDocument(
                "J008",
                "REQ-2026-008",
                "Security Engineer - AppSec",
                "Security",
                "Remote (US)",
                JobType.FULL_TIME,
                JobStatus.OPEN,
                "Strengthen our application security posture. Conduct security reviews, penetration testing, and implement security tooling.",
                new RequirementSection(
                        List.of("4+ years application security experience", "OWASP Top 10 expert", "Experience with SAST/DAST tools", "Secure SDLC implementation"),
                        List.of("Security certifications (OSCP, CEH)", "Bug bounty experience", "Threat modeling expertise"),
                        4,
                        com.example.mcpserver.dto.common.enums.EducationLevel.BACHELOR
                ),
                new CompensationDetails(
                        "USD", 145000, 185000,
                        new BonusStructure(true, 10000, true, "12-15%", true, "RSUs vesting over 4 years"),
                        List.of("Equity package", "401k match", "Security conference budget")
                ),
                new ShiftDetails(com.example.mcpserver.dto.common.enums.ShiftType.FLEXIBLE, "America/Los_Angeles", "09:00", "17:00", List.of(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY), true, 0),
                new AssessmentCodeMapping(List.of("APPSEC_01", "PENETRATION_TESTING_01", "SECURE_CODING_01"), Map.of("CERT_OSCP_01", "OSCP certification (highly preferred)"), true),
                "MGR007",
                "Robert Kim",
                LocalDateTime.of(2026, 2, 5, 11, 30),
                null,
                1,
                "CC-SEC-2026",
                "BUDGET-SEC-Q1",
                "SOC2 compliance expansion",
                "j008-partition",
                "etag-j008"
        ));
    }

    public static Optional<JobRequisitionDocument> getJob(String jobId) {
        return Optional.ofNullable(JOBS.get(jobId));
    }

    public static List<JobRequisitionDocument> getActiveJobs() {
        return JOBS.values().stream()
                .filter(job -> job.status() == JobStatus.OPEN)
                .toList();
    }

    public static List<JobRequisitionDocument> getJobsByDepartment(String department) {
        return JOBS.values().stream()
                .filter(job -> job.department().equalsIgnoreCase(department))
                .toList();
    }

    public static List<JobRequisitionDocument> getAllJobs() {
        return List.copyOf(JOBS.values());
    }
}
