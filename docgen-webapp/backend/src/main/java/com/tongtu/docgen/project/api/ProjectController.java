package com.tongtu.docgen.project.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.llm.AgentClient;
import com.tongtu.docgen.project.service.ProjectService;
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
@RequestMapping("/api/projects")
@Tag(name = "project", description = "Project management")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    public record CreateProjectRequest(
            @NotBlank @Size(max = 64) String projectKey,
            @NotBlank @Size(max = 128) String projectName,
            @Size(max = 2000) String description,
            @Size(max = 10000) String projectBackground,
            @Size(max = 10000) String similarProducts,
            @Size(max = 1000) String targetCustomerGroups,
            @Size(max = 4000) String commercialValue,
            @Size(max = 4000) String coreProductValue,
            @Size(max = 32) String projectType,
            @Size(max = 16) String priority,
            LocalDate startDate,
            LocalDate targetDate,
            @Size(max = 512) String tags,
            Long ownerUserId,
            @Size(max = 32) String visibility
    ) {
    }

    public record UpdateProjectRequest(
            @Size(max = 128) String projectName,
            @Size(max = 2000) String description,
            @Size(max = 10000) String projectBackground,
            @Size(max = 10000) String similarProducts,
            @Size(max = 1000) String targetCustomerGroups,
            @Size(max = 4000) String commercialValue,
            @Size(max = 4000) String coreProductValue,
            @Size(max = 32) String projectType,
            @Size(max = 16) String priority,
            LocalDate startDate,
            LocalDate targetDate,
            @Size(max = 512) String tags,
            @Size(max = 32) String visibility,
            @Size(max = 32) String status,
            Long ownerUserId
    ) {
    }

    public record AddProjectMemberRequest(
            Long userId,
            @Size(max = 32) String projectRole
    ) {
    }

    public record ProjectProductAiAnswer(
            @Size(max = 500) String question,
            @Size(max = 4000) String answer
    ) {
    }

    public record GuideProjectProductInfoRequest(
            @Size(max = 128) String projectName,
            @Size(max = 2000) String description,
            @Size(max = 10000) String projectBackground,
            @Size(max = 10000) String similarProducts,
            @Size(max = 1000) String targetCustomerGroups,
            @Size(max = 4000) String commercialValue,
            @Size(max = 4000) String coreProductValue,
            List<ProjectProductAiAnswer> answers
    ) {
    }

    @GetMapping
    @Operation(summary = "List projects")
    public ApiResponse<Object> listProjects() {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectService.listProjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project detail")
    public ApiResponse<Object> getProject(@PathVariable("id") Long id) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectService.getById(id));
    }

    @PostMapping
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Create project")
    public ApiResponse<Object> createProject(@RequestBody CreateProjectRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        Long id = projectService.createProject(
                req.projectKey(),
                req.projectName(),
                req.description(),
                req.projectBackground(),
                req.similarProducts(),
                req.targetCustomerGroups(),
                req.commercialValue(),
                req.coreProductValue(),
                req.projectType(),
                req.priority(),
                req.startDate(),
                req.targetDate(),
                req.tags(),
                req.ownerUserId(),
                req.visibility(),
                ctx
        );
        return ApiResponse.ok(traceId, id);
    }

    @PutMapping("/{id}")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Update project")
    public ApiResponse<Object> updateProject(@PathVariable("id") Long id, @RequestBody UpdateProjectRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        projectService.updateProject(
                id,
                req.projectName(),
                req.description(),
                req.projectBackground(),
                req.similarProducts(),
                req.targetCustomerGroups(),
                req.commercialValue(),
                req.coreProductValue(),
                req.projectType(),
                req.priority(),
                req.startDate(),
                req.targetDate(),
                req.tags(),
                req.visibility(),
                req.status(),
                req.ownerUserId(),
                ctx
        );
        return ApiResponse.ok(traceId, "OK");
    }

    @DeleteMapping("/{id}")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Delete project")
    public ApiResponse<Object> deleteProject(@PathVariable("id") Long id) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        projectService.deleteProject(id);
        return ApiResponse.ok(traceId, "OK");
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List project members")
    public ApiResponse<Object> listProjectMembers(@PathVariable("id") Long id) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, projectService.listMembers(id));
    }

    @PostMapping("/{id}/members")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Add project member")
    public ApiResponse<Object> addProjectMember(@PathVariable("id") Long id, @RequestBody AddProjectMemberRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        projectService.addMember(id, req.userId(), req.projectRole(), ctx);
        return ApiResponse.ok(traceId, "OK");
    }

    @DeleteMapping("/{id}/members/{userId}")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Remove project member")
    public ApiResponse<Object> removeProjectMember(@PathVariable("id") Long id, @PathVariable("userId") Long userId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        projectService.removeMember(id, userId);
        return ApiResponse.ok(traceId, "OK");
    }

    @PostMapping("/ai/product-info/guide")
    @RequiredPermission("PROJECT:CREATE")
    @Operation(summary = "Guide project product info with AI")
    public ApiResponse<Object> guideProjectProductInfo(@RequestBody GuideProjectProductInfoRequest req) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        List<AgentClient.ProjectProductAnswer> answers = req.answers() == null ? List.of() : req.answers().stream()
                .map(item -> new AgentClient.ProjectProductAnswer(item.question(), item.answer()))
                .toList();
        return ApiResponse.ok(traceId, projectService.guideProjectProductInfo(
                traceId,
                req.projectName(),
                req.description(),
                req.projectBackground(),
                req.similarProducts(),
                req.targetCustomerGroups(),
                req.commercialValue(),
                req.coreProductValue(),
                answers
        ));
    }
}
