package com.example.docgen.api;

import com.example.docgen.service.DocGenService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/docgen")
public class DocGenController {

    private final DocGenService docGenService;

    public DocGenController(DocGenService docGenService) {
        this.docGenService = docGenService;
    }

    @GetMapping("/health")
    public ApiResponse<Object> health(@RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        return ApiResponse.ok(tid, Map.of("status", "UP"));
    }

    public record CreateJobRequest(
            @NotBlank @Size(max = 20000) String businessDescription,
            // 可选：上一版PRD（Markdown），用于“增量修订”场景
            @Size(max = 200000) String previousPrdMarkdown
    ) {}

    public record CreateJobResponse(
            String jobId,
            String status,
            Object clarifyQuestions,
            String prdMarkdown
    ) {}

    public record ChatMessage(String role, String content) {}

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
            String basePrdMarkdown
    ) {}

    public record ClarifyQuestion(
            String id,
            String question,
            String whyNeeded,
            boolean required
    ) {}

    public record SubmitClarifyRequest(
            @NotBlank String jobId,
            Map<String, String> answers
    ) {}

    @PostMapping("/jobs")
    public ApiResponse<CreateJobResponse> createJob(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @RequestBody CreateJobRequest req
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        CreateJobResponse data = docGenService.createJob(tid, req.businessDescription(), req.previousPrdMarkdown());
        return ApiResponse.ok(tid, data);
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<CreateJobResponse> getJob(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @PathVariable("jobId") String jobId
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        CreateJobResponse data = docGenService.getJob(tid, jobId);
        return ApiResponse.ok(tid, data);
    }

    @PostMapping("/jobs/{jobId}/clarify")
    public ApiResponse<CreateJobResponse> submitClarify(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @PathVariable("jobId") String jobId,
            @RequestBody Map<String, String> answers
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        CreateJobResponse data = docGenService.submitClarifyAndMaybeGenerate(tid, jobId, answers);
        return ApiResponse.ok(tid, data);
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String message) {}

    @PostMapping("/jobs/{jobId}/chat")
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
    public ApiResponse<ChatResponse> generate(
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
            @PathVariable("jobId") String jobId
    ) {
        String tid = (traceId == null || traceId.isBlank()) ? UUID.randomUUID().toString() : traceId;
        ChatResponse data = docGenService.generatePrdFromChat(tid, jobId);
        return ApiResponse.ok(tid, data);
    }

    @GetMapping("/jobs/{jobId}/export")
    public ResponseEntity<byte[]> exportPrd(@PathVariable("jobId") String jobId) {
        String markdown = docGenService.getPrdMarkdownForExport(jobId);
        String fileName = "PRD-" + jobId + "-revised.md";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(markdown.getBytes(StandardCharsets.UTF_8));
    }
}

