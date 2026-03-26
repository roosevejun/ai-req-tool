package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentTaskEntity {
    private Long id;
    private Long documentId;
    private String taskType;
    private String status;
    private Integer attemptCount;
    private String lastError;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
}
