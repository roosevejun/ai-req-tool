package com.tongtu.docgen.project.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentAssetEntity {
    private Long id;
    private Long documentId;
    private String assetRole;
    private String storageBucket;
    private String storageKey;
    private String mimeType;
    private Long sizeBytes;
    private String sha256;
    private LocalDateTime createdAt;
}
