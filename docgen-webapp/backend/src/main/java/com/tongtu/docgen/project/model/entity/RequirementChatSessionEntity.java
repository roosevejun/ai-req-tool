package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RequirementChatSessionEntity {
    private Long id;
    private Long requirementId;
    private String jobId;
    private String status;
    private String pendingQuestion;
    private String confirmedItemsJson;
    private String unconfirmedItemsJson;
    private Boolean readyToGenerate;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


