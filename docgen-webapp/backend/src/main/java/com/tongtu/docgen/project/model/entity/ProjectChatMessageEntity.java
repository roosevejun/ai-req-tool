package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectChatMessageEntity {
    private Long id;
    private Long sessionId;
    private Integer seqNo;
    private String role;
    private String messageType;
    private String content;
    private LocalDateTime createdAt;
}
