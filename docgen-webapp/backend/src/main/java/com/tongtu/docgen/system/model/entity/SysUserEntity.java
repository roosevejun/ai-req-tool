package com.tongtu.docgen.system.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUserEntity {
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


