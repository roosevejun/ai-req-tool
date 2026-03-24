package com.tongtu.docgen.system.mapper;

import com.tongtu.docgen.system.model.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemRoleMapper {
    List<SysRoleEntity> listRoles();

    SysRoleEntity findById(@Param("id") Long id);

    SysRoleEntity findByCode(@Param("roleCode") String roleCode);

    int insertRole(SysRoleEntity role);

    int updateRole(SysRoleEntity role);

    int deleteRolePermissions(@Param("roleId") Long roleId);

    int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    List<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);
}


