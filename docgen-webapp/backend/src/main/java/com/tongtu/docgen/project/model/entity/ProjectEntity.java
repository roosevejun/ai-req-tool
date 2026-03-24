package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectEntity {
    private Long id;
    private String projectKey;
    private String projectName;
    private String description;
    private String visibility;
    private String status;
    private Long ownerUserId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


