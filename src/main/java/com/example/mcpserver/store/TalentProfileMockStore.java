package com.example.mcpserver.store;

import com.example.mcpserver.dto.common.EducationSummary;
import com.example.mcpserver.dto.common.SkillEndorsement;
import com.example.mcpserver.dto.common.enums.EducationLevel;
import com.example.mcpserver.dto.common.enums.ShiftType;
import com.example.mcpserver.dto.common.enums.SkillLevel;
import com.example.mcpserver.dto.common.enums.WorkMode;
import com.example.mcpserver.dto.talentprofile.*;
import com.example.mcpserver.model.AssessmentType;
import com.example.mcpserver.model.CandidateStatus;
import com.example.mcpserver.model.JobType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mock data store for talent-profile-service with comprehensive candidate profiles.
 *
 * Candidates:
 * - C001: 7-year experienced SRE/DevOps engineer
 * - C002: Recent CS graduate (new grad)
 * - C003: 8-year senior security engineer (OSCP certified)
 * - C004: 5-year backend engineer
 * - C005: 6-year frontend engineer (React specialist)
 */
public class TalentProfileMockStore {

    private static final Map<String, CandidateProfileV2> PROFILES = new HashMap<>();

    static {
        LocalDateTime now = LocalDateTime.now();

        // ========== CANDIDATE C001: Experienced SRE/DevOps Engineer ==========
        PROFILES.put("C001", new CandidateProfileV2(
                "C001",
                new BaseProfile(
                        "Alex Thompson",
                        "alex.thompson.work@gmail.com",
                        "https://linkedin.com/in/alexthompson-sre",
                        "San Francisco, CA",
                        7,
                        "Senior Site Reliability Engineer",
                        "CloudScale Inc",
                        new EducationSummary(
                                EducationLevel.BACHELOR,
                                "Computer Science",
                                "University of California, Berkeley",
                                2017
                        ),
                        List.of(
                                new SkillEndorsement("Kubernetes", SkillLevel.EXPERT, 12, true, "CKA - Certified Kubernetes Administrator"),
                                new SkillEndorsement("Python", SkillLevel.EXPERT, 15, false, null),
                                new SkillEndorsement("Terraform", SkillLevel.ADVANCED, 8, false, null),
                                new SkillEndorsement("Prometheus", SkillLevel.EXPERT, 10, false, null),
                                new SkillEndorsement("AWS", SkillLevel.ADVANCED, 9, true, "AWS Certified Solutions Architect"),
                                new SkillEndorsement("Docker", SkillLevel.EXPERT, 14, false, null),
                                new SkillEndorsement("Go", SkillLevel.INTERMEDIATE, 5, false, null),
                                new SkillEndorsement("CI/CD", SkillLevel.ADVANCED, 11, false, null)
                        ),
                        CandidateStatus.ACTIVE
                ),
                new AssessmentResults(
                        List.of(
                                new AssessmentResult("ASM001", "KUBERNETES_03", AssessmentType.CODING_CHALLENGE, 92, 100, 95, now.minusDays(30), true, "Excellent understanding of K8s architecture, networking, and troubleshooting"),
                                new AssessmentResult("ASM002", "SYS_DESIGN_02", AssessmentType.SYSTEM_DESIGN, 88, 100, 90, now.minusDays(28), true, "Strong system design skills. Designed a distributed caching system with clear trade-offs"),
                                new AssessmentResult("ASM003", "PYTHON_04", AssessmentType.CODING_CHALLENGE, 85, 100, 82, now.minusDays(25), true, "Solid Python skills. Implemented efficient algorithms and clean code structure"),
                                new AssessmentResult("ASM004", "ONCALL_READINESS_01", AssessmentType.BEHAVIORAL, 90, 100, 88, now.minusDays(20), true, "Experienced with on-call rotations, incident response, and post-mortems")
                        ),
                        Map.of(
                                "TECHNICAL", new PercentileScore(AssessmentType.CODING_CHALLENGE, 89.0, 1250),
                                "DESIGN", new PercentileScore(AssessmentType.SYSTEM_DESIGN, 90.0, 980),
                                "BEHAVIORAL", new PercentileScore(AssessmentType.BEHAVIORAL, 88.0, 1100)
                        )
                ),
                new Preferences(
                        new LocationPreferences(
                                List.of("San Francisco", "Seattle", "Austin"),
                                List.of("California", "Washington", "Texas"),
                                true,
                                false,
                                45
                        ),
                        new JobPreferences(
                                List.of("Site Reliability Engineer", "DevOps Engineer", "Platform Engineer"),
                                List.of("Engineering - SRE", "Engineering - DevOps", "Engineering - Platform"),
                                List.of(JobType.FULL_TIME),
                                false,
                                false,
                                LocalDate.of(2026, 4, 1)
                        ),
                        new CompensationExpectations(
                                "USD", 150000, 200000, true, true, List.of("Health insurance", "401k match", "Equity")
                        ),
                        new WorkStylePreferences(
                                WorkMode.HYBRID,
                                2,
                                List.of(ShiftType.DAY, ShiftType.FLEXIBLE),
                                false,
                                true
                        )
                ),
                new QuestionnaireResponses(
                        "v2.0",
                        now.minusDays(35),
                        List.of(
                                new QuestionResponse("Q1", "Why are you looking for a new role?", "TEXT", "Seeking new challenges in platform engineering at scale", Map.of()),
                                new QuestionResponse("Q2", "What's your biggest technical achievement?", "TEXT", "Led migration to Kubernetes, reducing infrastructure costs by 40%", Map.of()),
                                new QuestionResponse("Q3", "Preferred team size?", "TEXT", "5-10 engineers, cross-functional team", Map.of())
                        )
                ),
                // PII fields (will be stripped in Layer 1)
                "USA-123456",
                "6789",
                "123 Market Street, Apt 4B, San Francisco, CA 94103",
                "alex.thompson.personal@gmail.com",
                "+1-415-555-0123",
                LocalDate.of(1995, 6, 15),
                "Emergency: Jane Thompson (spouse) +1-415-555-0124",
                // Cosmos metadata
                "c001-partition",
                "etag-c001"
        ));

        // ========== CANDIDATE C002: Recent Graduate ==========
        PROFILES.put("C002", new CandidateProfileV2(
                "C002",
                new BaseProfile(
                        "Maya Patel",
                        "maya.patel@mit.edu",
                        "https://linkedin.com/in/mayapatel-cs",
                        "Cambridge, MA",
                        0,
                        "Recent Graduate",
                        "MIT (Student)",
                        new EducationSummary(
                                EducationLevel.BACHELOR,
                                "Computer Science",
                                "Massachusetts Institute of Technology",
                                2026
                        ),
                        List.of(
                                new SkillEndorsement("Java", SkillLevel.INTERMEDIATE, 4, false, null),
                                new SkillEndorsement("Python", SkillLevel.INTERMEDIATE, 5, false, null),
                                new SkillEndorsement("Algorithms", SkillLevel.ADVANCED, 3, false, null),
                                new SkillEndorsement("React", SkillLevel.BEGINNER, 2, false, null),
                                new SkillEndorsement("SQL", SkillLevel.INTERMEDIATE, 3, false, null),
                                new SkillEndorsement("Git", SkillLevel.INTERMEDIATE, 4, false, null)
                        ),
                        CandidateStatus.ACTIVE
                ),
                new AssessmentResults(
                        List.of(
                                new AssessmentResult("ASM005", "JAVA_01", AssessmentType.CODING_CHALLENGE, 78, 100, 70, now.minusDays(15), true, "Good understanding of Java fundamentals and OOP principles"),
                                new AssessmentResult("ASM006", "SQL_BASIC_01", AssessmentType.CODING_CHALLENGE, 82, 100, 75, now.minusDays(12), true, "Solid SQL knowledge. Wrote efficient queries and understood joins"),
                                new AssessmentResult("ASM007", "CODING_FUNDAMENTALS_01", AssessmentType.CODING_CHALLENGE, 85, 100, 80, now.minusDays(10), true, "Strong algorithmic thinking. Solved medium-difficulty problems efficiently")
                        ),
                        Map.of(
                                "TECHNICAL", new PercentileScore(AssessmentType.CODING_CHALLENGE, 75.0, 890)
                        )
                ),
                new Preferences(
                        new LocationPreferences(
                                List.of("Boston", "New York", "San Francisco"),
                                List.of("Massachusetts", "New York", "California"),
                                true,
                                false,
                                60
                        ),
                        new JobPreferences(
                                List.of("Software Engineer", "Junior Developer", "Backend Engineer"),
                                List.of("Engineering - Backend", "Engineering - Frontend"),
                                List.of(JobType.FULL_TIME),
                                false,
                                true,
                                LocalDate.of(2026, 5, 1)
                        ),
                        new CompensationExpectations(
                                "USD", 90000, 120000, false, false, List.of("Health insurance")
                        ),
                        new WorkStylePreferences(
                                WorkMode.HYBRID,
                                3,
                                List.of(ShiftType.DAY, ShiftType.FLEXIBLE),
                                false,
                                false
                        )
                ),
                new QuestionnaireResponses(
                        "v2.0",
                        now.minusDays(20),
                        List.of(
                                new QuestionResponse("Q1", "Why software engineering?", "TEXT", "Passionate about building systems that solve real-world problems", Map.of()),
                                new QuestionResponse("Q2", "Favorite project?", "TEXT", "Built a distributed task scheduler as senior thesis project", Map.of()),
                                new QuestionResponse("Q3", "Career goals?", "TEXT", "Grow into a full-stack engineer, work on scalable systems", Map.of())
                        )
                ),
                // PII fields
                null,
                null,
                "MIT Dorm, Building 10, Room 203, Cambridge, MA 02139",
                "maya.patel.personal@gmail.com",
                "+1-617-555-0234",
                LocalDate.of(2003, 11, 8),
                "Emergency: Raj Patel (father) +1-617-555-0235",
                // Cosmos metadata
                "c002-partition",
                "etag-c002"
        ));

        // ========== CANDIDATE C003: Senior Security Engineer ==========
        PROFILES.put("C003", new CandidateProfileV2(
                "C003",
                new BaseProfile(
                        "Jordan Rivera",
                        "jordan.rivera.sec@protonmail.com",
                        "https://linkedin.com/in/jordanrivera-appsec",
                        "Remote (San Diego, CA)",
                        8,
                        "Senior Application Security Engineer",
                        "SecureCloud Systems",
                        new EducationSummary(
                                EducationLevel.MASTER,
                                "Cybersecurity",
                                "Carnegie Mellon University",
                                2016
                        ),
                        List.of(
                                new SkillEndorsement("Application Security", SkillLevel.EXPERT, 20, true, "CISSP"),
                                new SkillEndorsement("Penetration Testing", SkillLevel.EXPERT, 18, true, "OSCP"),
                                new SkillEndorsement("OWASP Top 10", SkillLevel.EXPERT, 22, false, null),
                                new SkillEndorsement("Secure Coding", SkillLevel.EXPERT, 19, false, null),
                                new SkillEndorsement("Threat Modeling", SkillLevel.ADVANCED, 15, false, null),
                                new SkillEndorsement("Python", SkillLevel.ADVANCED, 12, false, null),
                                new SkillEndorsement("Burp Suite", SkillLevel.EXPERT, 17, false, null),
                                new SkillEndorsement("SAST/DAST Tools", SkillLevel.ADVANCED, 14, false, null)
                        ),
                        CandidateStatus.HIRED
                ),
                new AssessmentResults(
                        List.of(
                                new AssessmentResult("ASM008", "APPSEC_01", AssessmentType.CODING_CHALLENGE, 95, 100, 98, now.minusDays(40), true, "Exceptional AppSec knowledge. Identified 15+ vulnerabilities in code review exercise"),
                                new AssessmentResult("ASM009", "PENETRATION_TESTING_01", AssessmentType.CODING_CHALLENGE, 93, 100, 96, now.minusDays(38), true, "Expert penetration testing skills. Demonstrated complete attack chain"),
                                new AssessmentResult("ASM010", "SECURE_CODING_01", AssessmentType.CODING_CHALLENGE, 90, 100, 94, now.minusDays(35), true, "Strong secure coding practices. Wrote defensive code with proper input validation"),
                                new AssessmentResult("ASM011", "CERT_OSCP_01", AssessmentType.TECHNICAL_SCREENING, 100, 100, 100, now.minusDays(365), true, "OSCP Certified - Offensive Security Certified Professional")
                        ),
                        Map.of(
                                "TECHNICAL", new PercentileScore(AssessmentType.CODING_CHALLENGE, 96.0, 2100),
                                "CERTIFICATION", new PercentileScore(AssessmentType.TECHNICAL_SCREENING, 100.0, 450)
                        )
                ),
                new Preferences(
                        new LocationPreferences(
                                List.of("Remote", "San Diego", "San Francisco"),
                                List.of("California"),
                                false,
                                false,
                                null
                        ),
                        new JobPreferences(
                                List.of("Application Security Engineer", "Security Architect", "Principal Security Engineer"),
                                List.of("Security", "Engineering - Security"),
                                List.of(JobType.FULL_TIME),
                                false,
                                false,
                                LocalDate.of(2026, 4, 1)
                        ),
                        new CompensationExpectations(
                                "USD", 165000, 195000, true, true, List.of("Health insurance", "401k", "Security conference budget")
                        ),
                        new WorkStylePreferences(
                                WorkMode.REMOTE,
                                0,
                                List.of(ShiftType.FLEXIBLE),
                                false,
                                false
                        )
                ),
                new QuestionnaireResponses(
                        "v2.0",
                        now.minusDays(45),
                        List.of(
                                new QuestionResponse("Q1", "Why security?", "TEXT", "Passionate about protecting users and building secure systems", Map.of()),
                                new QuestionResponse("Q2", "Biggest security achievement?", "TEXT", "Led AppSec program at current company, reduced vulnerabilities by 80%", Map.of()),
                                new QuestionResponse("Q3", "Bug bounty experience?", "TEXT", "Active bug bounty hunter, $50k+ in rewards from HackerOne", Map.of())
                        )
                ),
                // PII fields
                "USA-987654",
                "4321",
                "456 Ocean View Drive, San Diego, CA 92109",
                "jordan.rivera.personal@protonmail.com",
                "+1-619-555-0345",
                LocalDate.of(1992, 3, 22),
                "Emergency: Sam Rivera (partner) +1-619-555-0346",
                // Cosmos metadata
                "c003-partition",
                "etag-c003"
        ));

        // ========== CANDIDATE C004: Backend Engineer (SLA breach scenario) ==========
        PROFILES.put("C004", new CandidateProfileV2(
                "C004",
                new BaseProfile(
                        "Chris Martinez",
                        "chris.martinez.dev@outlook.com",
                        "https://linkedin.com/in/chrismartinez-backend",
                        "Austin, TX",
                        5,
                        "Backend Software Engineer",
                        "DataFlow Technologies",
                        new EducationSummary(
                                EducationLevel.BACHELOR,
                                "Software Engineering",
                                "University of Texas at Austin",
                                2019
                        ),
                        List.of(
                                new SkillEndorsement("Python", SkillLevel.ADVANCED, 10, false, null),
                                new SkillEndorsement("Django", SkillLevel.ADVANCED, 8, false, null),
                                new SkillEndorsement("PostgreSQL", SkillLevel.ADVANCED, 9, false, null),
                                new SkillEndorsement("REST APIs", SkillLevel.ADVANCED, 11, false, null),
                                new SkillEndorsement("Docker", SkillLevel.INTERMEDIATE, 6, false, null),
                                new SkillEndorsement("Redis", SkillLevel.INTERMEDIATE, 5, false, null),
                                new SkillEndorsement("Celery", SkillLevel.INTERMEDIATE, 4, false, null)
                        ),
                        CandidateStatus.ACTIVE
                ),
                new AssessmentResults(
                        List.of(
                                new AssessmentResult("ASM012", "PYTHON_04", AssessmentType.CODING_CHALLENGE, 80, 100, 77, now.minusDays(20), true, "Good Python skills. Solved most coding problems efficiently"),
                                new AssessmentResult("ASM013", "SQL_ADVANCED_01", AssessmentType.CODING_CHALLENGE, 75, 100, 70, now.minusDays(18), true, "Solid SQL knowledge. Could optimize queries but missed some edge cases")
                        ),
                        Map.of(
                                "TECHNICAL", new PercentileScore(AssessmentType.CODING_CHALLENGE, 74.0, 650)
                        )
                ),
                new Preferences(
                        new LocationPreferences(
                                List.of("Austin", "Dallas", "San Antonio"),
                                List.of("Texas"),
                                false,
                                false,
                                30
                        ),
                        new JobPreferences(
                                List.of("Backend Engineer", "Software Engineer", "API Developer"),
                                List.of("Engineering - Backend"),
                                List.of(JobType.FULL_TIME),
                                false,
                                false,
                                LocalDate.of(2026, 3, 15)
                        ),
                        new CompensationExpectations(
                                "USD", 110000, 140000, false, true, List.of("Health insurance", "401k match")
                        ),
                        new WorkStylePreferences(
                                WorkMode.HYBRID,
                                3,
                                List.of(ShiftType.DAY),
                                false,
                                false
                        )
                ),
                new QuestionnaireResponses(
                        "v2.0",
                        now.minusDays(25),
                        List.of(
                                new QuestionResponse("Q1", "Why looking for change?", "TEXT", "Current company not offering growth opportunities", Map.of()),
                                new QuestionResponse("Q2", "Favorite tech stack?", "TEXT", "Python/Django with PostgreSQL and Redis", Map.of()),
                                new QuestionResponse("Q3", "Team preference?", "TEXT", "Mid-size team (10-15), collaborative environment", Map.of())
                        )
                ),
                // PII fields
                "USA-456789",
                "1234",
                "789 Congress Avenue, Apt 12C, Austin, TX 78701",
                "chris.martinez.personal@outlook.com",
                "+1-512-555-0456",
                LocalDate.of(1997, 8, 10),
                "Emergency: Maria Martinez (mother) +1-512-555-0457",
                // Cosmos metadata
                "c004-partition",
                "etag-c004"
        ));

        // ========== CANDIDATE C005: Frontend Engineer (withdrawn scenario) ==========
        PROFILES.put("C005", new CandidateProfileV2(
                "C005",
                new BaseProfile(
                        "Taylor Kim",
                        "taylor.kim.frontend@gmail.com",
                        "https://linkedin.com/in/taylorkim-react",
                        "New York, NY",
                        6,
                        "Senior Frontend Engineer",
                        "DesignSystems Co",
                        new EducationSummary(
                                EducationLevel.BACHELOR,
                                "Design + Computer Science",
                                "New York University",
                                2018
                        ),
                        List.of(
                                new SkillEndorsement("React", SkillLevel.EXPERT, 18, false, null),
                                new SkillEndorsement("TypeScript", SkillLevel.EXPERT, 16, false, null),
                                new SkillEndorsement("CSS/Sass", SkillLevel.EXPERT, 20, false, null),
                                new SkillEndorsement("Webpack", SkillLevel.ADVANCED, 12, false, null),
                                new SkillEndorsement("Redux", SkillLevel.ADVANCED, 14, false, null),
                                new SkillEndorsement("Accessibility (a11y)", SkillLevel.ADVANCED, 10, true, "IAAP WAS Certification"),
                                new SkillEndorsement("Design Systems", SkillLevel.EXPERT, 15, false, null),
                                new SkillEndorsement("Jest/Testing Library", SkillLevel.ADVANCED, 11, false, null)
                        ),
                        CandidateStatus.INACTIVE
                ),
                new AssessmentResults(
                        List.of(
                                new AssessmentResult("ASM014", "REACT_01", AssessmentType.CODING_CHALLENGE, 94, 100, 92, now.minusDays(25), true, "Exceptional React knowledge. Built complex component with hooks and performance optimization"),
                                new AssessmentResult("ASM015", "JAVASCRIPT_02", AssessmentType.CODING_CHALLENGE, 88, 100, 85, now.minusDays(23), true, "Strong JavaScript fundamentals. Solved async/await challenges elegantly"),
                                new AssessmentResult("ASM016", "CSS_ADVANCED_01", AssessmentType.CODING_CHALLENGE, 91, 100, 89, now.minusDays(20), true, "Expert CSS skills. Implemented responsive design with modern CSS features")
                        ),
                        Map.of(
                                "TECHNICAL", new PercentileScore(AssessmentType.CODING_CHALLENGE, 89.0, 1450)
                        )
                ),
                new Preferences(
                        new LocationPreferences(
                                List.of("New York", "Brooklyn", "Jersey City"),
                                List.of("New York", "New Jersey"),
                                false,
                                false,
                                45
                        ),
                        new JobPreferences(
                                List.of("Frontend Engineer", "UI Engineer", "Design Systems Engineer"),
                                List.of("Engineering - Frontend", "Design"),
                                List.of(JobType.FULL_TIME),
                                false,
                                false,
                                LocalDate.of(2026, 3, 1)
                        ),
                        new CompensationExpectations(
                                "USD", 140000, 170000, true, true, List.of("Health insurance", "Unlimited PTO", "Remote work stipend")
                        ),
                        new WorkStylePreferences(
                                WorkMode.HYBRID,
                                2,
                                List.of(ShiftType.FLEXIBLE),
                                false,
                                false
                        )
                ),
                new QuestionnaireResponses(
                        "v2.0",
                        now.minusDays(30),
                        List.of(
                                new QuestionResponse("Q1", "Why frontend?", "TEXT", "Love the intersection of design and engineering", Map.of()),
                                new QuestionResponse("Q2", "Design system experience?", "TEXT", "Built component library used by 50+ engineers at current company", Map.of()),
                                new QuestionResponse("Q3", "Accessibility focus?", "TEXT", "Passionate about a11y. All my components are WCAG 2.1 AA compliant", Map.of())
                        )
                ),
                // PII fields
                "USA-654321",
                "5678",
                "321 Broadway, Apt 8D, New York, NY 10007",
                "taylor.kim.personal@gmail.com",
                "+1-212-555-0567",
                LocalDate.of(1996, 2, 28),
                "Emergency: Jamie Kim (sibling) +1-212-555-0568",
                // Cosmos metadata
                "c005-partition",
                "etag-c005"
        ));
    }

    public static Optional<CandidateProfileV2> getProfileV2(String candidateId) {
        return Optional.ofNullable(PROFILES.get(candidateId));
    }

    public static List<CandidateProfileV2> getAllProfiles() {
        return List.copyOf(PROFILES.values());
    }
}
