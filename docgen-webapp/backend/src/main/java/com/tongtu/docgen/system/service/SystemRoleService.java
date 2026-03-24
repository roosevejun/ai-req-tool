package com.tongtu.docgen.system.service;

import com.tongtu.docgen.system.mapper.SystemPermissionMapper;
import com.tongtu.docgen.system.mapper.SystemRoleMapper;
import com.tongtu.docgen.system.model.entity.SysRoleEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemRoleService {
    private final SystemRoleMapper systemRoleMapper;
    private final SystemPermissionMapper systemPermissionMapper;

    public SystemRoleService(SystemRoleMapper systemRoleMapper, SystemPermissionMapper systemPermissionMapper) {
        this.systemRoleMapper = systemRoleMapper;
        this.systemPermissionMapper = systemPermissionMapper;
    }

    public List<SysRoleEntity> listRoles() {
        return systemRoleMapper.listRoles();
    }

    public Long createRole(String roleCode, String roleName, String status) {
        if (systemRoleMapper.findByCode(roleCode) != null) {
            throw new IllegalArgumentException("Role code already exists.");
        }
        SysRoleEntity role = new SysRoleEntity();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setStatus(status == null || status.isBlank() ? "ENABLED" : status);
        systemRoleMapper.insertRole(role);
        return role.getId();
    }

    public void updateRole(Long id, String roleName, String status) {
        SysRoleEntity old = systemRoleMapper.findById(id);
        if (old == null) {
            throw new IllegalArgumentException("Role not found.");
        }
        SysRoleEntity patch = new SysRoleEntity();
        patch.setId(id);
        patch.setRoleName(roleName == null ? old.getRoleName() : roleName);
        patch.setStatus(status == null ? old.getStatus() : status);
        systemRoleMapper.updateRole(patch);
    }

    @Transactional
    public void bindPermissions(Long roleId, List<Long> permissionIds) {
        SysRoleEntity role = systemRoleMapper.findById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found.");
        }
        systemRoleMapper.deleteRolePermissions(roleId);
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        for (Long permissionId : permissionIds) {
            if (systemPermissionMapper.findById(permissionId) == null) {
                throw new IllegalArgumentException("Permission not found: " + permissionId);
            }
            systemRoleMapper.insertRolePermission(roleId, permissionId);
        }
    }
}


