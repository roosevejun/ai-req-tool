package com.tongtu.docgen.system.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.system.security.RequiredPermission;
import com.tongtu.docgen.system.service.SystemRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/roles")
@Tag(name = "system-admin", description = "System admin - roles")
public class SystemRoleController {
    private final SystemRoleService systemRoleService;

    public SystemRoleController(SystemRoleService systemRoleService) {
        this.systemRoleService = systemRoleService;
    }

    public record CreateRoleRequest(
            @NotBlank @Size(max = 64) String roleCode,
            @NotBlank @Size(max = 128) String roleName,
            @Size(max = 32) String status
    ) {
    }

    public record UpdateRoleRequest(
            @Size(max = 128) String roleName,
            @Size(max = 32) String status
    ) {
    }

    public record BindPermissionsRequest(List<Long> permissionIds) {
    }

    @GetMapping
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "List roles")
    public ApiResponse<Object> listRoles() {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.ok(traceId, systemRoleService.listRoles());
    }

    @PostMapping
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Create role")
    public ApiResponse<Object> createRole(@RequestBody CreateRoleRequest req) {
        String traceId = UUID.randomUUID().toString();
        Long id = systemRoleService.createRole(req.roleCode(), req.roleName(), req.status());
        return ApiResponse.ok(traceId, id);
    }

    @PutMapping("/{id}")
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Update role")
    public ApiResponse<Object> updateRole(@PathVariable("id") Long id, @RequestBody UpdateRoleRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemRoleService.updateRole(id, req.roleName(), req.status());
        return ApiResponse.ok(traceId, "OK");
    }

    @PostMapping("/{id}/permissions")
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Bind role permissions")
    public ApiResponse<Object> bindPermissions(@PathVariable("id") Long id, @RequestBody BindPermissionsRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemRoleService.bindPermissions(id, req.permissionIds());
        return ApiResponse.ok(traceId, "OK");
    }
}

