package com.tongtu.docgen.system.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.system.security.RequiredPermission;
import com.tongtu.docgen.system.service.SystemPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/system/permissions")
@Tag(name = "system-admin", description = "System admin - permissions")
public class SystemPermissionController {
    private final SystemPermissionService systemPermissionService;

    public SystemPermissionController(SystemPermissionService systemPermissionService) {
        this.systemPermissionService = systemPermissionService;
    }

    public record CreatePermissionRequest(
            @NotBlank @Size(max = 64) String permCode,
            @NotBlank @Size(max = 128) String permName,
            @Size(max = 32) String status
    ) {
    }

    public record UpdatePermissionRequest(
            @Size(max = 128) String permName,
            @Size(max = 32) String status
    ) {
    }

    @GetMapping
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "List permissions")
    public ApiResponse<Object> listPermissions() {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.ok(traceId, systemPermissionService.listPermissions());
    }

    @PostMapping
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Create permission")
    public ApiResponse<Object> createPermission(@RequestBody CreatePermissionRequest req) {
        String traceId = UUID.randomUUID().toString();
        Long id = systemPermissionService.createPermission(req.permCode(), req.permName(), req.status());
        return ApiResponse.ok(traceId, id);
    }

    @PutMapping("/{id}")
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Update permission")
    public ApiResponse<Object> updatePermission(@PathVariable("id") Long id, @RequestBody UpdatePermissionRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemPermissionService.updatePermission(id, req.permName(), req.status());
        return ApiResponse.ok(traceId, "OK");
    }
}

