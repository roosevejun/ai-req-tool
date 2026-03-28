package com.tongtu.docgen.system.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysTemplateEntity {
    private Long id;
    private String templateCode;
    private String templateName;
    private String templateCategory;
    private String description;
    private String scopeLevel;
    private String status;
    private Integer latestVersionNo;
    private Integer publishedVersionNo;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
