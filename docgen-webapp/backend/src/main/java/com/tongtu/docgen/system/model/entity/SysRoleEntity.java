package com.tongtu.docgen.system.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysRoleEntity {
    private Long id;
    private String roleCode;
    private String roleName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


