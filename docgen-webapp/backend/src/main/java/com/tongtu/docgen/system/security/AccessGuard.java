package com.tongtu.docgen.system.security;

import com.tongtu.docgen.system.exception.ForbiddenException;
import com.tongtu.docgen.system.exception.UnauthorizedException;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.system.model.UserContextHolder;

public class AccessGuard {
    private AccessGuard() {
    }

    public static UserContext requireLogin() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null) {
            throw new UnauthorizedException("Please login first.");
        }
        return ctx;
    }

    public static void requireRole(String roleCode) {
        UserContext ctx = requireLogin();
        if (ctx.getRoleCodes() == null || ctx.getRoleCodes().stream().noneMatch(roleCode::equalsIgnoreCase)) {
            throw new ForbiddenException("No permission: required role " + roleCode);
        }
    }
}


