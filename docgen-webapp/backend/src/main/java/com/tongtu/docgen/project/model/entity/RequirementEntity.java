package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RequirementEntity {
    private Long id;
    private Long projectId;
    private String requirementNo;
    private String title;
    private String summary;
    private String priority;
    private String status;
    private Long assigneeUserId;
    private Long reporterUserId;
    private Long currentVersionId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


