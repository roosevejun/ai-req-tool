package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectMemberEntity {
    private Long id;
    private Long projectId;
    private Long userId;
    private String username;
    private String displayName;
    private String projectRole;
    private String status;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

