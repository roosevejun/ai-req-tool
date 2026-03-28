package com.tongtu.docgen.system.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.system.security.RequiredPermission;
import com.tongtu.docgen.system.service.SystemTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/system/templates")
@Tag(name = "system-admin", description = "System admin - templates")
public class SystemTemplateController {
    private final SystemTemplateService systemTemplateService;

    public SystemTemplateController(SystemTemplateService systemTemplateService) {
        this.systemTemplateService = systemTemplateService;
    }

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 64) String templateCode,
            @NotBlank @Size(max = 128) String templateName,
            @Size(max = 64) String templateCategory,
            @Size(max = 1000) String description,
            @Size(max = 32) String scopeLevel,
            @Size(max = 32) String status,
            @Size(max = 64) String initialVersionLabel,
            String contentMarkdown,
            String variablesJson,
            @Size(max = 2000) String notes
    ) {
    }

    public record UpdateTemplateRequest(
            @Size(max = 128) String templateName,
            @Size(max = 64) String templateCategory,
            @Size(max = 1000) String description,
            @Size(max = 32) String scopeLevel,
            @Size(max = 32) String status
    ) {
    }

    public record CreateTemplateVersionRequest(
            @Size(max = 64) String versionLabel,
            String contentMarkdown,
            String variablesJson,
            @Size(max = 2000) String notes,
            @Size(max = 32) String status
    ) {
    }

    public record UpdateTemplateVersionRequest(
            @Size(max = 64) String versionLabel,
            String contentMarkdown,
            String variablesJson,
            @Size(max = 2000) String notes,
            @Size(max = 32) String status
    ) {
    }

    @GetMapping
    @RequiredPermission("TEMPLATE:MANAGE")
    @Operation(summary = "List templates")
    public ApiResponse<Object> listTemplates() {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.ok(traceId, systemTemplateService.listTemplates());
    }

    @GetMapping("/{id}")
    @RequiredPermission("TEMPLATE:MANAGE")
    @Operation(summary = "Get template detail")
    public ApiResponse<Object> getTemplateDetail(@PathVariable("id") Long id) {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.ok(traceId, systemTemplateService.getTemplateDetail(id));
    }

    @PostMapping
    @RequiredPermission("TEMPLATE:MANAGE")
    @Operation(summary = "Create template")
    public ApiResponse<Object> createTemplate(@Valid @RequestBody CreateTemplateRequest req) {
        String traceId = UUID.randomUUID().toString();
        Long id = systemTemplateService.createTemplate(
                req.templateCode(),
                req.templateName(),
                req.templateCategory(),
                req.description(),
                req.scopeLevel(),
                req.status(),
                req.initialVersionLabel(),
                req.contentMarkdown(),
                req.variablesJson(),
                req.notes()
        );
        return ApiResponse.ok(traceId, id);
    }

    @PutMapping("/{id}")
    @RequiredPermission("TEMPLATE:MANAGE")
    @Operation(summary = "Update template")
    public ApiResponse<Object> updateTemplate(@PathVariable("id") Long id,
                                              @Valid @RequestBody UpdateTemplateRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemTemplateService.updateTemplate(
                id,
                req.templateName(),
                req.templateCategory(),
                req.description(),
                req.scopeLevel(),
                req.status()
        );
        return ApiResponse.ok(traceId, "OK");
    }

    @PostMapping("/{id}/versions")
    @RequiredPermission("TEMPLATE:MANAGE")
    @Operation(summary = "Create template version")
    public ApiResponse<Object> createTemplateVersion(@PathVariable("id") Long id,
                                                     @Valid @RequestBody CreateTemplateVersionRequest req) {
        String traceId = UUID.randomUUID().toString();
        Long versionId = systemTemplateService.createVersion(
                id,
                req.versionLabel(),
                req.contentMarkdown(),
                req.variablesJson(),
                req.notes(),
                req.status()
        );
        return ApiResponse.ok(traceId, versionId);
    }

    @PutMapping("/{id}/versions/{versionId}")
    @RequiredPermission("TEMPLATE:MANAGE")
    @Operation(summary = "Update template version")
    public ApiResponse<Object> updateTemplateVersion(@PathVariable("id") Long id,
                                                     @PathVariable("versionId") Long versionId,
                                                     @Valid @RequestBody UpdateTemplateVersionRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemTemplateService.updateVersion(
                id,
                versionId,
                req.versionLabel(),
                req.contentMarkdown(),
                req.variablesJson(),
                req.notes(),
                req.status()
        );
        return ApiResponse.ok(traceId, "OK");
    }

    @PostMapping("/{id}/versions/{versionId}/publish")
    @RequiredPermission("TEMPLATE:MANAGE")
    @Operation(summary = "Publish template version")
    public ApiResponse<Object> publishTemplateVersion(@PathVariable("id") Long id,
                                                      @PathVariable("versionId") Long versionId) {
        String traceId = UUID.randomUUID().toString();
        systemTemplateService.publishVersion(id, versionId);
        return ApiResponse.ok(traceId, "OK");
    }
}
