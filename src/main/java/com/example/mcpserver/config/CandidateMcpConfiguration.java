package com.example.mcpserver.config;

import com.example.mcpserver.model.Candidate;
import com.example.mcpserver.model.CandidateStatus;
import com.example.mcpserver.service.CandidateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Registers MCP tools, resources and prompts for the candidate hiring pipeline
 * using the functional stateless API.
 *
 * <p>Each handler receives a {@link McpTransportContext} as its first argument —
 * the "functional context" for stateless HTTP requests. In stateless mode no
 * bidirectional operations (sampling, elicitation, ping) are available; the
 * context is used to read transport-level metadata such as HTTP headers.
 */
@Configuration
public class CandidateMcpConfiguration {

    private final ObjectMapper mapper = new ObjectMapper();

    // ------------------------------------------------------------------ tools

    @Bean
    public List<McpStatelessServerFeatures.SyncToolSpecification> candidateTools(CandidateService service) {
        return List.of(
                getCandidateByIdTool(service),
                listCandidatesTool(service),
                searchCandidatesTool(service),
                getCandidatesByStatusTool(service),
                addCandidateTool(service),
                updateCandidateStatusTool(service)
        );
    }

    private McpStatelessServerFeatures.SyncToolSpecification getCandidateByIdTool(CandidateService service) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("getCandidateById")
                .description("Retrieve full profile for a specific candidate by their ID (e.g. C001).")
                .inputSchema(objectSchema(
                        Map.of("candidateId", prop("string", "Unique candidate identifier, e.g. C001")),
                        List.of("candidateId")))
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((McpTransportContext ctx, McpSchema.CallToolRequest req) -> {
                    String id = (String) req.arguments().get("candidateId");
                    return service.findById(id)
                            .map(c -> success(toJson(c)))
                            .orElse(error("Candidate not found: " + id));
                })
                .build();
    }

    private McpStatelessServerFeatures.SyncToolSpecification listCandidatesTool(CandidateService service) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("listCandidates")
                .description("List all candidates in the hiring pipeline with their current status.")
                .inputSchema(objectSchema(Map.of(), List.of()))
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((McpTransportContext ctx, McpSchema.CallToolRequest req) ->
                        success(toJson(service.findAll())))
                .build();
    }

    private McpStatelessServerFeatures.SyncToolSpecification searchCandidatesTool(CandidateService service) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("searchCandidates")
                .description("Search candidates by name, skill, current role, or summary text.")
                .inputSchema(objectSchema(
                        Map.of("query", prop("string", "Free-text search term (name, skill, role, or summary)")),
                        List.of("query")))
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((McpTransportContext ctx, McpSchema.CallToolRequest req) -> {
                    String query = (String) req.arguments().get("query");
                    List<Candidate> results = service.search(query);
                    if (results.isEmpty()) {
                        return success("No candidates found matching: " + query);
                    }
                    return success(toJson(results));
                })
                .build();
    }

    private McpStatelessServerFeatures.SyncToolSpecification getCandidatesByStatusTool(CandidateService service) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("getCandidatesByStatus")
                .description("Get all candidates at a specific hiring pipeline stage.")
                .inputSchema(objectSchema(
                        Map.of("status", Map.of(
                                "type", "string",
                                "description", "Hiring stage",
                                "enum", List.of("APPLIED", "SCREENING", "INTERVIEW", "OFFER", "HIRED", "REJECTED"))),
                        List.of("status")))
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((McpTransportContext ctx, McpSchema.CallToolRequest req) -> {
                    String raw = (String) req.arguments().get("status");
                    try {
                        CandidateStatus status = CandidateStatus.valueOf(raw.toUpperCase());
                        return success(toJson(service.findByStatus(status)));
                    } catch (IllegalArgumentException e) {
                        return error("Invalid status '" + raw + "'. Use: APPLIED, SCREENING, INTERVIEW, OFFER, HIRED, REJECTED");
                    }
                })
                .build();
    }

    private McpStatelessServerFeatures.SyncToolSpecification addCandidateTool(CandidateService service) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("addCandidate")
                .description("Add a new candidate to the hiring pipeline. Status defaults to APPLIED.")
                .inputSchema(objectSchema(
                        Map.of(
                                "id",               prop("string",  "Unique ID, e.g. C006"),
                                "name",             prop("string",  "Full name"),
                                "email",            prop("string",  "Email address"),
                                "phone",            prop("string",  "Phone number"),
                                "skills",           Map.of("type", "array", "items", Map.of("type", "string"),
                                                        "description", "Technical skills list"),
                                "yearsOfExperience",prop("integer", "Years of professional experience"),
                                "currentRole",      prop("string",  "Current or most recent job title"),
                                "summary",          prop("string",  "Brief professional summary")),
                        List.of("id", "name", "email")))
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((McpTransportContext ctx, McpSchema.CallToolRequest req) -> {
                    Map<String, Object> args = req.arguments();
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> skills = args.get("skills") instanceof List<?> l
                                ? (List<String>) l : List.of();
                        int experience = args.get("yearsOfExperience") instanceof Number n
                                ? n.intValue() : 0;
                        Candidate candidate = new Candidate(
                                (String) args.get("id"),
                                (String) args.get("name"),
                                (String) args.get("email"),
                                (String) args.getOrDefault("phone", ""),
                                skills, experience,
                                (String) args.getOrDefault("currentRole", ""),
                                CandidateStatus.APPLIED,
                                (String) args.getOrDefault("summary", ""));
                        service.add(candidate);
                        return success("Candidate added: " + toJson(candidate));
                    } catch (Exception e) {
                        return error("Failed to add candidate: " + e.getMessage());
                    }
                })
                .build();
    }

    private McpStatelessServerFeatures.SyncToolSpecification updateCandidateStatusTool(CandidateService service) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("updateCandidateStatus")
                .description("Advance or change a candidate's hiring pipeline status.")
                .inputSchema(objectSchema(
                        Map.of(
                                "candidateId", prop("string", "Candidate ID to update"),
                                "status", Map.of(
                                        "type", "string",
                                        "description", "New hiring status",
                                        "enum", List.of("APPLIED", "SCREENING", "INTERVIEW", "OFFER", "HIRED", "REJECTED"))),
                        List.of("candidateId", "status")))
                .build();

        return McpStatelessServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((McpTransportContext ctx, McpSchema.CallToolRequest req) -> {
                    String id     = (String) req.arguments().get("candidateId");
                    String raw    = (String) req.arguments().get("status");
                    try {
                        CandidateStatus status = CandidateStatus.valueOf(raw.toUpperCase());
                        return service.updateStatus(id, status)
                                .map(c -> success("Status updated to " + status + " for " + c.name()))
                                .orElse(error("Candidate not found: " + id));
                    } catch (IllegalArgumentException e) {
                        return error("Invalid status: " + raw);
                    }
                })
                .build();
    }

    // --------------------------------------------------------------- resources

    @Bean
    public List<McpStatelessServerFeatures.SyncResourceSpecification> candidateResources(CandidateService service) {
        return List.of(
                allCandidatesResource(service),
                skillsResource(service)
        );
    }

    private McpStatelessServerFeatures.SyncResourceSpecification allCandidatesResource(CandidateService service) {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("candidates://all")
                .name("All Candidates")
                .description("Complete list of all candidates in the hiring pipeline as JSON.")
                .mimeType("application/json")
                .build();

        return new McpStatelessServerFeatures.SyncResourceSpecification(resource,
                (McpTransportContext ctx, McpSchema.ReadResourceRequest req) ->
                        new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(req.uri(), "application/json",
                                        toJson(service.findAll())))));
    }

    private McpStatelessServerFeatures.SyncResourceSpecification skillsResource(CandidateService service) {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri("candidates://skills")
                .name("Candidate Skills")
                .description("Alphabetically sorted list of all unique technical skills across candidates.")
                .mimeType("application/json")
                .build();

        return new McpStatelessServerFeatures.SyncResourceSpecification(resource,
                (McpTransportContext ctx, McpSchema.ReadResourceRequest req) ->
                        new McpSchema.ReadResourceResult(List.of(
                                new McpSchema.TextResourceContents(req.uri(), "application/json",
                                        toJson(Map.of("skills", service.allSkills()))))));
    }

    // ---------------------------------------------------------------- prompts

    @Bean
    public List<McpStatelessServerFeatures.SyncPromptSpecification> candidatePrompts(CandidateService service) {
        return List.of(
                candidateEvaluationPrompt(service),
                pipelineSummaryPrompt(service)
        );
    }

    private McpStatelessServerFeatures.SyncPromptSpecification candidateEvaluationPrompt(CandidateService service) {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "candidate-evaluation",
                "Generate a structured evaluation report for a specific candidate.",
                List.of(
                        new McpSchema.PromptArgument("candidateId",      "Candidate ID to evaluate (e.g. C001)", true),
                        new McpSchema.PromptArgument("jobRequirements",  "Brief description of the target role requirements", false)
                ));

        return new McpStatelessServerFeatures.SyncPromptSpecification(prompt,
                (McpTransportContext ctx, McpSchema.GetPromptRequest req) -> {
                    String id           = (String) req.arguments().get("candidateId");
                    String requirements = (String) req.arguments().getOrDefault(
                            "jobRequirements", "general software engineering role");

                    String candidateJson = service.findById(id)
                            .map(this::toJson)
                            .orElse("Candidate not found: " + id);

                    String text = """
                            Evaluate the following candidate for the role: %s

                            Candidate Profile:
                            %s

                            Please provide:
                            1. Technical Skills Assessment — match against role requirements
                            2. Experience Level Analysis
                            3. Key Strengths and Development Areas
                            4. Hiring Recommendation: STRONG_YES | YES | MAYBE | NO
                            5. Suggested interview questions tailored to this candidate
                            """.formatted(requirements, candidateJson);

                    return new McpSchema.GetPromptResult(
                            "Candidate evaluation for " + id,
                            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                                    new McpSchema.TextContent(text))));
                });
    }

    private McpStatelessServerFeatures.SyncPromptSpecification pipelineSummaryPrompt(CandidateService service) {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "hiring-pipeline-summary",
                "Generate an executive summary of the current hiring pipeline.",
                List.of());

        return new McpStatelessServerFeatures.SyncPromptSpecification(prompt,
                (McpTransportContext ctx, McpSchema.GetPromptRequest req) -> {
                    String pipelineJson = toJson(service.findAll());

                    String text = """
                            Analyse the following hiring pipeline data and produce a concise summary:

                            Pipeline Data:
                            %s

                            Please include:
                            1. Pipeline Overview — total candidates, stage distribution
                            2. Skills Inventory — most common skills, notable gaps
                            3. Pipeline Health — conversion rates, bottlenecks
                            4. Candidates Ready for Next Stage
                            5. Recommended Actions for the hiring team
                            """.formatted(pipelineJson);

                    return new McpSchema.GetPromptResult(
                            "Hiring pipeline executive summary",
                            List.of(new McpSchema.PromptMessage(McpSchema.Role.USER,
                                    new McpSchema.TextContent(text))));
                });
    }

    // --------------------------------------------------------------- helpers

    /** Creates an "object" JSON Schema with the given property descriptors. */
    private static McpSchema.JsonSchema objectSchema(Map<String, Object> properties, List<String> required) {
        return new McpSchema.JsonSchema("object", properties, required, null, null, null);
    }

    /** Creates a simple string/integer/boolean property descriptor. */
    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private McpSchema.CallToolResult success(String text) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
    }

    private McpSchema.CallToolResult error(String message) {
        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(message)), true);
    }

    private String toJson(Object obj) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
