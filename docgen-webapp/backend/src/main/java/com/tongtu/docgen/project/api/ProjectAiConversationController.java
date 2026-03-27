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
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
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

    public record ProjectChatRequest(
            Long sessionId,
            @NotBlank @Size(max = 8000) String message
    ) {
    }

    public record KnowledgePreviewRequest(
            @Size(max = 4000) String query
    ) {
    }

    public record SaveMaterialsRequest(
            List<SourceMaterialRequest> materials
    ) {
    }

    public record UploadFileMaterialResponse(
            Long sessionId,
            int materialCount
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

    @PostMapping(value = "/{sessionId}/materials/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Upload project file material")
    public ApiResponse<Object> uploadMaterial(@PathVariable("sessionId") Long sessionId,
                                              @RequestParam(value = "title", required = false) String title,
                                              @RequestPart("file") MultipartFile file) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        ProjectAiConversationService.MaterialSaveResult result = projectAiConversationService.uploadFileMaterial(sessionId, title, file, ctx);
        return ApiResponse.ok(traceId, new UploadFileMaterialResponse(result.sessionId(), result.materialCount()));
    }

    @GetMapping("/{sessionId}")
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Get project AI conversation")
    public ApiResponse<Object> getConversation(@PathVariable("sessionId") Long sessionId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.getConversation(sessionId));
    }

    @GetMapping("/{sessionId}/knowledge-preview")
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Preview retrieved knowledge context for current conversation")
    public ApiResponse<Object> previewKnowledge(@PathVariable("sessionId") Long sessionId,
                                                @RequestParam(value = "query", required = false) String query) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.previewKnowledgeContext(traceId, sessionId, query));
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

    @DeleteMapping("/materials/{materialId}")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Delete project source material from AI conversation")
    public ApiResponse<Object> deleteMaterial(@PathVariable("materialId") Long materialId) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        projectAiConversationService.deleteMaterial(materialId, ctx);
        return ApiResponse.ok(traceId, "OK");
    }

    @PostMapping("/projects/{projectId}/resume")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Resume or create project edit AI conversation")
    public ApiResponse<Object> resumeProjectConversation(@PathVariable("projectId") Long projectId) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.resumeProjectConversation(traceId, projectId, ctx));
    }

    @PostMapping("/projects/{projectId}/chat")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Continue project edit AI conversation")
    public ApiResponse<Object> chatProjectConversation(@PathVariable("projectId") Long projectId,
                                                       @RequestBody ProjectChatRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.continueProjectConversation(
                traceId,
                projectId,
                req.sessionId(),
                req.message(),
                ctx
        ));
    }

    @GetMapping("/projects/{projectId}/knowledge-preview")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Preview retrieved knowledge context for project edit AI conversation")
    public ApiResponse<Object> previewProjectKnowledge(@PathVariable("projectId") Long projectId,
                                                       @RequestParam(value = "sessionId", required = false) Long sessionId,
                                                       @RequestParam(value = "query", required = false) String query) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectAiConversationService.previewProjectKnowledgeContext(
                traceId,
                projectId,
                sessionId,
                query
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
