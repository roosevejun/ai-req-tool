package com.tongtu.docgen.project.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.project.service.RequirementService;
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
@Tag(name = "requirement", description = "Requirement management")
public class RequirementController {
    private final RequirementService requirementService;

    public RequirementController(RequirementService requirementService) {
        this.requirementService = requirementService;
    }

    public record CreateRequirementRequest(
            @NotBlank @Size(max = 256) String title,
            @Size(max = 4000) String summary,
            @Size(max = 16) String priority,
            @Size(max = 32) String status,
            Long assigneeUserId
    ) {
    }

    public record UpdateRequirementRequest(
            @Size(max = 256) String title,
            @Size(max = 4000) String summary,
            @Size(max = 16) String priority,
            @Size(max = 32) String status,
            Long assigneeUserId
    ) {
    }

    @GetMapping("/api/projects/{projectId}/requirements")
    @Operation(summary = "List requirements in project")
    public ApiResponse<Object> listRequirements(@PathVariable("projectId") Long projectId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, requirementService.listByProjectId(projectId));
    }

    @PostMapping("/api/projects/{projectId}/requirements")
    @RequiredPermission("REQUIREMENT:CREATE")
    @Operation(summary = "Create requirement")
    public ApiResponse<Object> createRequirement(@PathVariable("projectId") Long projectId, @RequestBody CreateRequirementRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        Long id = requirementService.createRequirement(projectId, req.title(), req.summary(), req.priority(), req.status(), req.assigneeUserId(), ctx);
        return ApiResponse.ok(traceId, id);
    }

    @GetMapping("/api/requirements/{id}")
    @Operation(summary = "Get requirement detail")
    public ApiResponse<Object> getRequirement(@PathVariable("id") Long id) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, requirementService.getById(id));
    }

    @PutMapping("/api/requirements/{id}")
    @RequiredPermission("REQUIREMENT:EDIT")
    @Operation(summary = "Update requirement")
    public ApiResponse<Object> updateRequirement(@PathVariable("id") Long id, @RequestBody UpdateRequirementRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        requirementService.updateRequirement(id, req.title(), req.summary(), req.priority(), req.status(), req.assigneeUserId(), ctx);
        return ApiResponse.ok(traceId, "OK");
    }

    @GetMapping("/api/requirements/{id}/versions")
    @RequiredPermission("REQUIREMENT:VERSION_VIEW")
    @Operation(summary = "List requirement versions")
    public ApiResponse<Object> listVersions(@PathVariable("id") Long id) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, requirementService.listVersions(id));
    }

    @GetMapping("/api/requirements/{id}/versions/{versionId}")
    @RequiredPermission("REQUIREMENT:VERSION_VIEW")
    @Operation(summary = "Get requirement version detail")
    public ApiResponse<Object> getVersion(@PathVariable("id") Long id,
                                          @PathVariable("versionId") Long versionId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, requirementService.getVersionById(id, versionId));
    }

    @GetMapping("/api/requirements/{id}/versions/{versionId}/export")
    @RequiredPermission("REQUIREMENT:VERSION_VIEW")
    @Operation(summary = "Download requirement version markdown")
    public ResponseEntity<byte[]> exportVersion(@PathVariable("id") Long id,
                                                @PathVariable("versionId") Long versionId) {
        AccessGuard.requireLogin();
        var version = requirementService.getVersionById(id, versionId);
        String fileName = "01-PRD-Agent-Requirement-v" + version.getVersionNo() + ".md";
        String safeAsciiFileName = "requirement-" + id + "-version-" + versionId + ".md";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + safeAsciiFileName + "\"; filename*=UTF-8''" + encodedFileName;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .body(version.getPrdMarkdown().getBytes(StandardCharsets.UTF_8));
    }
}
