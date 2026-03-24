package com.tongtu.docgen.system.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysPermissionEntity {
    private Long id;
    private String permCode;
    private String permName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


