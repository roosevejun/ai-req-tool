package com.tongtu.docgen.system.service;

import com.tongtu.docgen.system.mapper.SystemPermissionMapper;
import com.tongtu.docgen.system.model.entity.SysPermissionEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemPermissionService {
    private final SystemPermissionMapper systemPermissionMapper;

    public SystemPermissionService(SystemPermissionMapper systemPermissionMapper) {
        this.systemPermissionMapper = systemPermissionMapper;
    }

    public List<SysPermissionEntity> listPermissions() {
        return systemPermissionMapper.listPermissions();
    }

    public Long createPermission(String permCode, String permName, String status) {
        if (systemPermissionMapper.findByCode(permCode) != null) {
            throw new IllegalArgumentException("Permission code already exists.");
        }
        SysPermissionEntity permission = new SysPermissionEntity();
        permission.setPermCode(permCode);
        permission.setPermName(permName);
        permission.setStatus(status == null || status.isBlank() ? "ENABLED" : status);
        systemPermissionMapper.insertPermission(permission);
        return permission.getId();
    }

    public void updatePermission(Long id, String permName, String status) {
        SysPermissionEntity old = systemPermissionMapper.findById(id);
        if (old == null) {
            throw new IllegalArgumentException("Permission not found.");
        }
        SysPermissionEntity patch = new SysPermissionEntity();
        patch.setId(id);
        patch.setPermName(permName == null ? old.getPermName() : permName);
        patch.setStatus(status == null ? old.getStatus() : status);
        systemPermissionMapper.updatePermission(patch);
    }
}


