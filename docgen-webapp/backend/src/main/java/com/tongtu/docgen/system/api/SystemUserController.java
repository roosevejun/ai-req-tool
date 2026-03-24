package com.tongtu.docgen.system.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.system.security.RequiredPermission;
import com.tongtu.docgen.system.service.SystemUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/users")
@Tag(name = "system-admin", description = "System admin - users")
public class SystemUserController {
    private final SystemUserService systemUserService;

    public SystemUserController(SystemUserService systemUserService) {
        this.systemUserService = systemUserService;
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 128) String password,
            @Size(max = 128) String displayName,
            @Size(max = 32) String status,
            List<Long> roleIds
    ) {
    }

    public record UpdateUserRequest(
            @Size(max = 128) String displayName,
            @Size(max = 32) String status
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 128) String newPassword
    ) {
    }

    public record BindRolesRequest(List<Long> roleIds) {
    }

    @GetMapping
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "List users")
    public ApiResponse<Object> listUsers() {
        String traceId = UUID.randomUUID().toString();
        return ApiResponse.ok(traceId, systemUserService.listUsers());
    }

    @PostMapping
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Create user")
    public ApiResponse<Object> createUser(@RequestBody CreateUserRequest req) {
        String traceId = UUID.randomUUID().toString();
        Long id = systemUserService.createUser(req.username(), req.displayName(), req.password(), req.status(), req.roleIds());
        return ApiResponse.ok(traceId, id);
    }

    @PutMapping("/{id}")
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Update user profile")
    public ApiResponse<Object> updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemUserService.updateUser(id, req.displayName(), req.status());
        return ApiResponse.ok(traceId, "OK");
    }

    @PutMapping("/{id}/password")
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Reset user password")
    public ApiResponse<Object> resetPassword(@PathVariable("id") Long id, @RequestBody ResetPasswordRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemUserService.updatePassword(id, req.newPassword());
        return ApiResponse.ok(traceId, "OK");
    }

    @PostMapping("/{id}/roles")
    @RequiredPermission("ROLE:ADMIN")
    @Operation(summary = "Bind user roles")
    public ApiResponse<Object> bindRoles(@PathVariable("id") Long id, @RequestBody BindRolesRequest req) {
        String traceId = UUID.randomUUID().toString();
        systemUserService.bindRoles(id, req.roleIds());
        return ApiResponse.ok(traceId, "OK");
    }
}

