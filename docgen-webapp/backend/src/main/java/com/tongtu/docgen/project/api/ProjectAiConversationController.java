package com.tongtu.docgen.project.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.project.service.ProjectAiConversationService;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.system.security.AccessGuard;
import com.tongtu.docgen.system.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/ai/conversations")
@Tag(name = "project-ai-conversation", description = "Project AI conversation")
public class ProjectAiConversationController {
    private final ProjectAiConversationService projectAiConversationService;

    public ProjectAiConversationController(ProjectAiConversationService projectAiConversationService) {
        this.projectAiConversationService = projectAiConversationService;
    }

    public record SourceMaterialRequest(
            @Size(max = 16) String materialType,
            @Size(max = 256) String title,
            @Size(max = 1000) String sourceUri,
            @Size(max = 20000) String rawContent
    ) {
    }

    public record StartConversationRequest(
            @Size(max = 128) String projectName,
            @Size(max = 4000) String description,
            List<SourceMaterialRequest> materials
    ) {
    }

    public record ChatRequest(
            @NotBlank @Size(max = 8000) String message
    ) {
    }

    public record SaveMaterialsRequest(
            List<SourceMaterialRequest> materials
    ) {
    }

    public record CreateProjectFromConversationRequest(
            @NotBlank @Size(max = 64) String projectKey,
            @Size(max = 128) String projectName,
            @Size(max = 32) String projectType,
            @Size(max = 16) String priority,
            @Size(max = 32) String visibility,
            Long ownerUserId,
            LocalDate startDate,
            LocalDate targetDate,
            @Size(max = 512) String tags
    ) {
    }

    @PostMapping
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Start project AI conversation")
    public ApiResponse<Object> startConversation(@RequestBody StartConversationRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.startConversation(
                traceId,
                req.projectName(),
                req.description(),
                toMaterialInputs(req.materials()),
                ctx
        ));
    }

    @PostMapping("/{sessionId}/chat")
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Continue project AI conversation")
    public ApiResponse<Object> chat(@PathVariable("sessionId") Long sessionId, @RequestBody ChatRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.continueConversation(traceId, sessionId, req.message(), ctx));
    }

    @PostMapping("/{sessionId}/materials")
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Save project source materials")
    public ApiResponse<Object> saveMaterials(@PathVariable("sessionId") Long sessionId, @RequestBody SaveMaterialsRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.addMaterials(sessionId, toMaterialInputs(req.materials()), ctx));
    }

    @GetMapping("/{sessionId}")
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Get project AI conversation")
    public ApiResponse<Object> getConversation(@PathVariable("sessionId") Long sessionId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.getConversation(sessionId));
    }

    @PostMapping("/{sessionId}/create-project")
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Create project from AI conversation")
    public ApiResponse<Object> createProject(@PathVariable("sessionId") Long sessionId,
                                             @RequestBody CreateProjectFromConversationRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.createProjectFromConversation(
                sessionId,
                req.projectKey(),
                req.projectName(),
                req.projectType(),
                req.priority(),
                req.visibility(),
                req.ownerUserId(),
                ctx
        ));
    }

    private List<ProjectAiConversationService.SourceMaterialInput> toMaterialInputs(List<SourceMaterialRequest> materials) {
        if (materials == null) {
            return List.of();
        }
        return materials.stream()
                .map(item -> new ProjectAiConversationService.SourceMaterialInput(
                        item.materialType(),
                        item.title(),
                        item.sourceUri(),
                        item.rawContent()
                ))
                .toList();
    }
}
