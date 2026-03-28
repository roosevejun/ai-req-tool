package com.tongtu.docgen.api;

import com.tongtu.docgen.service.DocGenService;
import com.tongtu.docgen.system.security.PublicApi;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/docgen")
@Tag(name = "docgen", description = "AI requirement conversation workspace")
public class DocGenController {

    private final DocGenService docGenService;

    public DocGenController(DocGenService docGenService) {
        this.docGenService = docGenService;
    }

    @GetMapping("/health")
    @PublicApi
    @Operation(summary = "Health check")
    public ApiResponse<Object> health(@RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        return ApiResponse.ok(tid, Map.of("status", "UP"));
    }

    public record CreateJobRequest(
            @NotBlank @Size(max = 20000) String businessDescription,
            // Optional: previous PRD markdown for iterative refinement
            @Size(max = 200000) String previousPrdMarkdown,
            Long templateId,
            Long templateVersionId
    ) {
    }

    public record TemplateSelection(
            Long templateId,
            Long templateVersionId,
            String templateVersionLabel
    ) {
    }

    public record CreateJobResponse(
            String jobId,
            String status,
            Object clarifyQuestions,
            String prdMarkdown,
            boolean readyToGenerate,
            String pendingQuestion,
            List<String> confirmedItems,
            List<String> unconfirmedItems,
            List<ChatMessage> chatHistory,
            String basePrdMarkdown,
            int currentVersion,
            TemplateSelection templateSelection
    ) {
    }

    public record ChatMessage(String role, String content) {
    }

    public record ChatResponse(
            String jobId,
            String status,
            String assistantMessage,
            boolean readyToGenerate,
            String pendingQuestion,
            List<String> confirmedItems,
            List<String> unconfirmedItems,
            List<ChatMessage> chatHistory,
            String prdMarkdown,
            String basePrdMarkdown,
            int currentVersion,
            TemplateSelection templateSelection
    ) {
    }

    public record ClarifyQuestion(
            String id,
            String question,
            String whyNeeded,
            boolean required
    ) {
    }

    public record SubmitClarifyRequest(
            @NotBlank String jobId,
            Map<String, String> answers
    ) {
    }

    @PostMapping("/jobs")
    @Operation(summary = "Create conversation job")
    public ApiResponse<CreateJobResponse> createJob(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody CreateJobRequest req
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        CreateJobResponse data = docGenService.createJob(
                tid,
                req.businessDescription(),
                req.previousPrdMarkdown(),
                req.templateId(),
                req.templateVersionId(),
                null
        );
        return ApiResponse.ok(tid, data);
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get conversation job status")
    public ApiResponse<CreateJobResponse> getJob(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @PathVariable("jobId") String jobId
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        CreateJobResponse data = docGenService.getJob(tid, jobId);
        return ApiResponse.ok(tid, data);
    }

    @PostMapping("/jobs/{jobId}/clarify")
    @Operation(summary = "Submit clarify answers")
    public ApiResponse<CreateJobResponse> submitClarify(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @PathVariable("jobId") String jobId,
            @RequestBody Map<String, String> answers
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        CreateJobResponse data = docGenService.submitClarifyAndMaybeGenerate(tid, jobId, answers);
        return ApiResponse.ok(tid, data);
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String message) {
    }

    @PostMapping("/jobs/{jobId}/chat")
    @Operation(summary = "Continue conversation")
    public ApiResponse<ChatResponse> chat(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @PathVariable("jobId") String jobId,
            @RequestBody ChatRequest req
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        ChatResponse data = docGenService.chat(tid, jobId, req.message());
        return ApiResponse.ok(tid, data);
    }

    @PostMapping("/jobs/{jobId}/generate")
    @Operation(summary = "Generate PRD from conversation")
    public ApiResponse<ChatResponse> generate(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @PathVariable("jobId") String jobId
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        ChatResponse data = docGenService.generatePrdFromChat(tid, jobId);
        return ApiResponse.ok(tid, data);
    }

    @GetMapping("/jobs/{jobId}/export")
    @Operation(summary = "Export PRD markdown")
    public ResponseEntity<byte[]> exportPrd(@PathVariable("jobId") String jobId) {
        String markdown = docGenService.getPrdMarkdownForExport(jobId);
        String fileName = docGenService.getExportFileName(jobId);
        String safeAsciiFileName = "prd-" + jobId + ".md";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + safeAsciiFileName + "\"; filename*=UTF-8''" + encodedFileName;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(markdown.getBytes(StandardCharsets.UTF_8));
    }
}
