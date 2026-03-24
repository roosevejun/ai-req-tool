package com.tongtu.docgen.system.mapper;

import com.tongtu.docgen.system.model.entity.SysPermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemPermissionMapper {
    List<SysPermissionEntity> listPermissions();

    SysPermissionEntity findById(@Param("id") Long id);

    SysPermissionEntity findByCode(@Param("permCode") String permCode);

    int insertPermission(SysPermissionEntity permission);

    int updatePermission(SysPermissionEntity permission);
}


