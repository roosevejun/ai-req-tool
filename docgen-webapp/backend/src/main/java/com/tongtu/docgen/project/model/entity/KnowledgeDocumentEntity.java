package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentEntity {
    private Long id;
    private Long projectId;
    private Long requirementId;
    private Long sourceMaterialId;
    private String documentType;
    private String sourceUri;
    private String title;
    private String status;
    private String rawText;
    private String cleanText;
    private String summary;
    private String keywordsJson;
    private String tags;
    private String contentHash;
    private Integer versionNo;
    private Boolean isLatest;
    private Boolean retrievable;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
