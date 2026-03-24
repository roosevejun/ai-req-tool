package com.tongtu.docgen.project.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.project.service.ProjectService;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.system.security.AccessGuard;
import com.tongtu.docgen.system.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

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
            @Size(max = 32) String visibility
    ) {
    }

    public record UpdateProjectRequest(
            @Size(max = 128) String projectName,
            @Size(max = 2000) String description,
            @Size(max = 32) String visibility,
            @Size(max = 32) String status,
            Long ownerUserId
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
        Long id = projectService.createProject(req.projectKey(), req.projectName(), req.description(), req.visibility(), ctx);
        return ApiResponse.ok(traceId, id);
    }

    @PutMapping("/{id}")
    @RequiredPermission("PROJECT:EDIT")
    @Operation(summary = "Update project")
    public ApiResponse<Object> updateProject(@PathVariable("id") Long id, @RequestBody UpdateProjectRequest req) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        projectService.updateProject(id, req.projectName(), req.description(), req.visibility(), req.status(), req.ownerUserId(), ctx);
        return ApiResponse.ok(traceId, "OK");
    }
}

