package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectSourceMaterialEntity {
    private Long id;
    private Long projectId;
    private Long sessionId;
    private String materialType;
    private String title;
    private String sourceUri;
    private String rawContent;
    private String aiExtractedSummary;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
