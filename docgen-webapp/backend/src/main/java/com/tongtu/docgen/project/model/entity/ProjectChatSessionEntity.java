package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectChatSessionEntity {
    private Long id;
    private Long projectId;
    private String jobId;
    private String status;
    private String assistantSummary;
    private String businessKnowledgeSummary;
    private String structuredInfoJson;
    private Boolean readyToCreate;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
