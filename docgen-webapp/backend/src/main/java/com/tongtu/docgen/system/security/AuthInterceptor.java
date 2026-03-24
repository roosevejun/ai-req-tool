package com.tongtu.docgen.system.security;

import com.tongtu.docgen.system.exception.ForbiddenException;
import com.tongtu.docgen.system.exception.UnauthorizedException;
import com.tongtu.docgen.system.model.AuthProfile;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.system.model.UserContextHolder;
import com.tongtu.docgen.system.service.SystemAuthService;
import com.tongtu.docgen.system.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final TokenUtil tokenUtil;
    private final SystemAuthService systemAuthService;

    public AuthInterceptor(TokenUtil tokenUtil, SystemAuthService systemAuthService) {
        this.tokenUtil = tokenUtil;
        this.systemAuthService = systemAuthService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (isPublicApi(handlerMethod)) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing Authorization token.");
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        TokenUtil.TokenPayload payload = tokenUtil.verify(token);
        AuthProfile profile = systemAuthService.loadProfile(payload.userId());
        if (!"ENABLED".equalsIgnoreCase(profile.getStatus())) {
            throw new UnauthorizedException("Current user is disabled.");
        }

        UserContext ctx = new UserContext();
        ctx.setUserId(profile.getUserId());
        ctx.setUsername(profile.getUsername());
        ctx.setDisplayName(profile.getDisplayName());
        ctx.setRoleCodes(profile.getRoleCodes());
        ctx.setPermissionCodes(profile.getPermissionCodes());
        UserContextHolder.set(ctx);

        validatePermissionsIfRequired(handlerMethod, ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private void validatePermissionsIfRequired(HandlerMethod handlerMethod, UserContext ctx) {
        List<String> required = collectRequiredPermissions(handlerMethod);
        if (required.isEmpty()) {
            return;
        }
        if (hasRole(ctx, "ADMIN")) {
            return;
        }
        for (String permission : required) {
            if (permission == null || permission.isBlank()) {
                continue;
            }
            if (permission.startsWith("ROLE:")) {
                String roleCode = permission.substring("ROLE:".length());
                if (!hasRole(ctx, roleCode)) {
                    throw new ForbiddenException("No permission: " + permission);
                }
                continue;
            }
            if (!hasPermission(ctx, permission)) {
                throw new ForbiddenException("No permission: " + permission);
            }
        }
    }

    private boolean isPublicApi(HandlerMethod handlerMethod) {
        PublicApi classAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PublicApi.class);
        if (classAnno != null) {
            return true;
        }
        PublicApi methodAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PublicApi.class);
        return methodAnno != null;
    }

    private List<String> collectRequiredPermissions(HandlerMethod handlerMethod) {
        List<String> out = new ArrayList<>();
        RequiredPermission classAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequiredPermission.class);
        RequiredPermission methodAnno = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequiredPermission.class);
        if (classAnno != null && classAnno.value() != null) {
            for (String p : classAnno.value()) {
                out.add(p);
            }
        }
        if (methodAnno != null && methodAnno.value() != null) {
            for (String p : methodAnno.value()) {
                out.add(p);
            }
        }
        return out;
    }

    private boolean hasRole(UserContext ctx, String roleCode) {
        if (ctx.getRoleCodes() == null) return false;
        return ctx.getRoleCodes().stream().anyMatch(it -> roleCode.equalsIgnoreCase(it));
    }

    private boolean hasPermission(UserContext ctx, String permissionCode) {
        if (ctx.getPermissionCodes() == null) return false;
        return ctx.getPermissionCodes().stream().anyMatch(it -> permissionCode.equalsIgnoreCase(it));
    }
}

