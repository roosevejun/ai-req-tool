package com.tongtu.docgen.system.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysTemplateVersionEntity {
    private Long id;
    private Long templateId;
    private Integer versionNo;
    private String versionLabel;
    private String contentMarkdown;
    private String variablesJson;
    private String notes;
    private String status;
    private Boolean isPublished;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
