package com.tongtu.docgen.system.service;

import com.tongtu.docgen.system.exception.UnauthorizedException;
import com.tongtu.docgen.system.mapper.SystemUserMapper;
import com.tongtu.docgen.system.model.AuthProfile;
import com.tongtu.docgen.system.model.entity.SysUserEntity;
import com.tongtu.docgen.system.util.PasswordUtil;
import com.tongtu.docgen.system.util.TokenUtil;
import org.springframework.stereotype.Service;

@Service
public class SystemAuthService {
    private final SystemUserMapper systemUserMapper;
    private final PasswordUtil passwordUtil;
    private final TokenUtil tokenUtil;

    public SystemAuthService(SystemUserMapper systemUserMapper, PasswordUtil passwordUtil, TokenUtil tokenUtil) {
        this.systemUserMapper = systemUserMapper;
        this.passwordUtil = passwordUtil;
        this.tokenUtil = tokenUtil;
    }

    public LoginResult login(String username, String password) {
        SysUserEntity user = systemUserMapper.findByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("Username or password is incorrect.");
        }
        if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Current user is disabled.");
        }
        if (!passwordUtil.matches(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Username or password is incorrect.");
        }

        String token = tokenUtil.createToken(user.getId(), user.getUsername());
        AuthProfile profile = loadProfile(user.getId());
        return new LoginResult(token, profile);
    }

    public AuthProfile loadProfile(Long userId) {
        SysUserEntity user = systemUserMapper.findById(userId);
        if (user == null) {
            throw new UnauthorizedException("User not found.");
        }
        AuthProfile profile = new AuthProfile();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setDisplayName(user.getDisplayName());
        profile.setStatus(user.getStatus());
        profile.setRoleCodes(systemUserMapper.findRoleCodesByUserId(user.getId()));
        profile.setPermissionCodes(systemUserMapper.findPermissionCodesByUserId(user.getId()));
        return profile;
    }

    public record LoginResult(String token, AuthProfile profile) {
    }
}


