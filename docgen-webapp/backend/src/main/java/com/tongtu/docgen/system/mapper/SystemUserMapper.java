package com.tongtu.docgen.system.mapper;

import com.tongtu.docgen.system.model.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemUserMapper {
    SysUserEntity findByUsername(@Param("username") String username);

    SysUserEntity findById(@Param("id") Long id);

    List<SysUserEntity> listUsers();

    int insertUser(SysUserEntity user);

    int updateUser(SysUserEntity user);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    List<String> findPermissionCodesByUserId(@Param("userId") Long userId);

    int deleteUserRoles(@Param("userId") Long userId);

    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}


