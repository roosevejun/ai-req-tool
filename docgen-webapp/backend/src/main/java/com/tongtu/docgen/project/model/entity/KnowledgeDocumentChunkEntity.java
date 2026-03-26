package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentChunkEntity {
    private Long id;
    private Long documentId;
    private Integer chunkNo;
    private String chunkText;
    private Integer tokenCount;
    private String summary;
    private String embeddingStatus;
    private String vectorRef;
    private LocalDateTime createdAt;
}
