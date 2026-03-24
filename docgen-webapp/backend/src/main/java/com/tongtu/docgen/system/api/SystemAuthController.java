package com.tongtu.docgen.system.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.system.model.UserContextHolder;
import com.tongtu.docgen.system.security.AccessGuard;
import com.tongtu.docgen.system.security.PublicApi;
import com.tongtu.docgen.system.service.SystemAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/system/auth")
@Tag(name = "system-auth", description = "Authentication")
public class SystemAuthController {
    private final SystemAuthService systemAuthService;

    public SystemAuthController(SystemAuthService systemAuthService) {
        this.systemAuthService = systemAuthService;
    }

    public record LoginRequest(
            @NotBlank @Size(max = 64) String username,
            @NotBlank @Size(max = 128) String password
    ) {
    }

    public record LoginResponse(
            String token,
            Long userId,
            String username,
            String displayName,
            Object roleCodes,
            Object permissionCodes
    ) {
    }

    @PostMapping("/login")
    @PublicApi
    @Operation(summary = "Login")
    @SecurityRequirements
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest req) {
        String traceId = UUID.randomUUID().toString();
        SystemAuthService.LoginResult result = systemAuthService.login(req.username(), req.password());
        LoginResponse data = new LoginResponse(
                result.token(),
                result.profile().getUserId(),
                result.profile().getUsername(),
                result.profile().getDisplayName(),
                result.profile().getRoleCodes(),
                result.profile().getPermissionCodes()
        );
        return ApiResponse.ok(traceId, data);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user")
    public ApiResponse<Object> me() {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        return ApiResponse.ok(traceId, UserContextHolder.get());
    }
}
