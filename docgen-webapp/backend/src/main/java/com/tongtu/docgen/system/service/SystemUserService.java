package com.tongtu.docgen.system.service;

import com.tongtu.docgen.system.mapper.SystemRoleMapper;
import com.tongtu.docgen.system.mapper.SystemUserMapper;
import com.tongtu.docgen.system.model.entity.SysUserEntity;
import com.tongtu.docgen.system.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SystemUserService {
    private final SystemUserMapper systemUserMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final PasswordUtil passwordUtil;

    public SystemUserService(SystemUserMapper systemUserMapper, SystemRoleMapper systemRoleMapper, PasswordUtil passwordUtil) {
        this.systemUserMapper = systemUserMapper;
        this.systemRoleMapper = systemRoleMapper;
        this.passwordUtil = passwordUtil;
    }

    public List<UserView> listUsers() {
        List<SysUserEntity> users = systemUserMapper.listUsers();
        List<UserView> out = new ArrayList<>();
        for (SysUserEntity u : users) {
            UserView v = new UserView();
            v.setId(u.getId());
            v.setUsername(u.getUsername());
            v.setDisplayName(u.getDisplayName());
            v.setStatus(u.getStatus());
            v.setRoleCodes(systemUserMapper.findRoleCodesByUserId(u.getId()));
            out.add(v);
        }
        return out;
    }

    @Transactional
    public Long createUser(String username, String displayName, String password, String status, List<Long> roleIds) {
        if (systemUserMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists.");
        }
        SysUserEntity user = new SysUserEntity();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setStatus(status == null || status.isBlank() ? "ENABLED" : status);
        user.setPasswordHash(passwordUtil.hash(password));
        systemUserMapper.insertUser(user);

        if (roleIds != null && !roleIds.isEmpty()) {
            bindRoles(user.getId(), roleIds);
        }
        return user.getId();
    }

    public void updateUser(Long id, String displayName, String status) {
        SysUserEntity old = systemUserMapper.findById(id);
        if (old == null) {
            throw new IllegalArgumentException("User not found.");
        }
        SysUserEntity patch = new SysUserEntity();
        patch.setId(id);
        patch.setDisplayName(displayName == null ? old.getDisplayName() : displayName);
        patch.setStatus(status == null ? old.getStatus() : status);
        systemUserMapper.updateUser(patch);
    }

    public void updatePassword(Long id, String newPassword) {
        SysUserEntity old = systemUserMapper.findById(id);
        if (old == null) {
            throw new IllegalArgumentException("User not found.");
        }
        systemUserMapper.updatePassword(id, passwordUtil.hash(newPassword));
    }

    @Transactional
    public void bindRoles(Long userId, List<Long> roleIds) {
        SysUserEntity old = systemUserMapper.findById(userId);
        if (old == null) {
            throw new IllegalArgumentException("User not found.");
        }
        systemUserMapper.deleteUserRoles(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            if (systemRoleMapper.findById(roleId) == null) {
                throw new IllegalArgumentException("Role not found: " + roleId);
            }
            systemUserMapper.insertUserRole(userId, roleId);
        }
    }

    public static class UserView {
        private Long id;
        private String username;
        private String displayName;
        private String status;
        private List<String> roleCodes;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<String> getRoleCodes() {
            return roleCodes;
        }

        public void setRoleCodes(List<String> roleCodes) {
            this.roleCodes = roleCodes;
        }
    }
}


