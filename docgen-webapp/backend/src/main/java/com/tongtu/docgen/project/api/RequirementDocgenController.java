package com.tongtu.docgen.project.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.api.DocGenController;
import com.tongtu.docgen.project.service.RequirementDocgenService;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.system.security.AccessGuard;
import com.tongtu.docgen.system.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/requirements/{requirementId}/docgen")
@Tag(name = "requirement-workbench", description = "Requirement AI workbench")
public class RequirementDocgenController {
    private final RequirementDocgenService requirementDocgenService;

    public RequirementDocgenController(RequirementDocgenService requirementDocgenService) {
        this.requirementDocgenService = requirementDocgenService;
    }

    public record CreateReqDocgenJobRequest(
            @Size(max = 20000) String businessDescription,
            @Size(max = 200000) String previousPrdMarkdown
    ) {
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String message) {
    }

    public record KnowledgePreviewResponse(
            String query,
            Object requirementKnowledge,
            Object projectKnowledge,
            String mergedContext
    ) {
    }

    @PostMapping("/jobs")
    @RequiredPermission("REQUIREMENT:AI_CHAT")
    @Operation(summary = "Create requirement workbench session")
    public ApiResponse<DocGenController.CreateJobResponse> createJob(@PathVariable("requirementId") Long requirementId,
                                                                     @RequestBody CreateReqDocgenJobRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        DocGenController.CreateJobResponse data = requirementDocgenService.createRequirementJob(
                traceId,
                requirementId,
                req.businessDescription(),
                req.previousPrdMarkdown(),
                ctx
        );
        return ApiResponse.ok(traceId, data);
    }

    @GetMapping("/jobs/{jobId}")
    @RequiredPermission("REQUIREMENT:AI_CHAT")
    @Operation(summary = "Get session status")
    public ApiResponse<DocGenController.CreateJobResponse> getJob(@PathVariable("requirementId") Long requirementId,
                                                                  @PathVariable("jobId") String jobId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        DocGenController.CreateJobResponse data = requirementDocgenService.getRequirementJob(traceId, requirementId, jobId);
        return ApiResponse.ok(traceId, data);
    }

    @PostMapping("/jobs/{jobId}/chat")
    @RequiredPermission("REQUIREMENT:AI_CHAT")
    @Operation(summary = "Chat for clarification")
    public ApiResponse<DocGenController.ChatResponse> chat(@PathVariable("requirementId") Long requirementId,
                                                           @PathVariable("jobId") String jobId,
                                                           @RequestBody ChatRequest req) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        DocGenController.ChatResponse data = requirementDocgenService.chat(traceId, requirementId, jobId, req.message());
        return ApiResponse.ok(traceId, data);
    }

    @PostMapping("/jobs/{jobId}/generate")
    @RequiredPermission("REQUIREMENT:GENERATE_PRD")
    @Operation(summary = "Generate and persist PRD")
    public ApiResponse<DocGenController.ChatResponse> generate(@PathVariable("requirementId") Long requirementId,
                                                               @PathVariable("jobId") String jobId) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        DocGenController.ChatResponse data = requirementDocgenService.generate(traceId, requirementId, jobId, ctx);
        return ApiResponse.ok(traceId, data);
    }

    @GetMapping("/jobs/{jobId}/export")
    @RequiredPermission("REQUIREMENT:VERSION_VIEW")
    @Operation(summary = "Export current PRD markdown")
    public ResponseEntity<byte[]> export(@PathVariable("requirementId") Long requirementId,
                                         @PathVariable("jobId") String jobId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        byte[] markdown = requirementDocgenService.export(traceId, requirementId, jobId);
        String fileName = "01-PRD-Agent-Requirement.md";
        String safeAsciiFileName = "prd-" + jobId + ".md";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + safeAsciiFileName + "\"; filename*=UTF-8''" + encodedFileName;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(markdown);
    }

    @GetMapping("/knowledge-preview")
    @RequiredPermission("REQUIREMENT:AI_CHAT")
    @Operation(summary = "Preview retrieved knowledge context for requirement workbench")
    public ApiResponse<Object> previewKnowledge(@PathVariable("requirementId") Long requirementId,
                                                @RequestParam(value = "query", required = false) String query) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        RequirementDocgenService.KnowledgePreviewView data = requirementDocgenService.previewKnowledgeContext(traceId, requirementId, query);
        return ApiResponse.ok(traceId, new KnowledgePreviewResponse(
                data.query(),
                data.requirementKnowledge(),
                data.projectKnowledge(),
                data.mergedContext()
        ));
    }
}
