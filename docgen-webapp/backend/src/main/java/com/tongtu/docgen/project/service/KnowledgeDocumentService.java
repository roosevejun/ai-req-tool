package com.tongtu.docgen.project.service;

import com.tongtu.docgen.project.mapper.KnowledgeDocumentAssetMapper;
import com.tongtu.docgen.project.mapper.KnowledgeDocumentChunkMapper;
import com.tongtu.docgen.project.mapper.KnowledgeDocumentMapper;
import com.tongtu.docgen.project.mapper.KnowledgeDocumentTaskMapper;
import com.tongtu.docgen.project.mapper.ProjectMapper;
import com.tongtu.docgen.project.mapper.ProjectSourceMaterialMapper;
import com.tongtu.docgen.project.mapper.RequirementMapper;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentAssetEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentChunkEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentTaskEntity;
import com.tongtu.docgen.system.model.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeDocumentService {
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeDocumentAssetMapper knowledgeDocumentAssetMapper;
    private final KnowledgeDocumentChunkMapper knowledgeDocumentChunkMapper;
    private final KnowledgeDocumentTaskMapper knowledgeDocumentTaskMapper;
    private final ProjectMapper projectMapper;
    private final RequirementMapper requirementMapper;
    private final ProjectSourceMaterialMapper projectSourceMaterialMapper;

    public KnowledgeDocumentService(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                    KnowledgeDocumentAssetMapper knowledgeDocumentAssetMapper,
                                    KnowledgeDocumentChunkMapper knowledgeDocumentChunkMapper,
                                    KnowledgeDocumentTaskMapper knowledgeDocumentTaskMapper,
                                    ProjectMapper projectMapper,
                                    RequirementMapper requirementMapper,
                                    ProjectSourceMaterialMapper projectSourceMaterialMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeDocumentAssetMapper = knowledgeDocumentAssetMapper;
        this.knowledgeDocumentChunkMapper = knowledgeDocumentChunkMapper;
        this.knowledgeDocumentTaskMapper = knowledgeDocumentTaskMapper;
        this.projectMapper = projectMapper;
        this.requirementMapper = requirementMapper;
        this.projectSourceMaterialMapper = projectSourceMaterialMapper;
    }

    public record CreateDocumentCommand(
            Long projectId,
            Long requirementId,
            Long sourceMaterialId,
            String documentType,
            String sourceUri,
            String title,
            String rawText,
            String cleanText,
            String summary,
            String keywordsJson,
            String tags,
            String contentHash,
            Integer versionNo,
            Boolean retrievable
    ) {
    }

    public record ProcessingResultCommand(
            String status,
            String cleanText,
            String summary,
            String keywordsJson,
            String tags,
            String contentHash,
            Boolean retrievable
    ) {
    }

    public record DocumentAssetCommand(
            String assetRole,
            String storageBucket,
            String storageKey,
            String mimeType,
            Long sizeBytes,
            String sha256
    ) {
    }

    public record DocumentChunkCommand(
            Integer chunkNo,
            String chunkText,
            Integer tokenCount,
            String summary,
            String embeddingStatus,
            String vectorRef
    ) {
    }

    public record DocumentTaskCommand(
            String taskType,
            String status,
            Integer attemptCount,
            String lastError,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
    }

    public List<KnowledgeDocumentEntity> listByProjectId(Long projectId) {
        ensureProject(projectId);
        return knowledgeDocumentMapper.listByProjectId(projectId);
    }

    public List<KnowledgeDocumentEntity> listByRequirementId(Long requirementId) {
        ensureRequirement(requirementId);
        return knowledgeDocumentMapper.listByRequirementId(requirementId);
    }

    public List<KnowledgeDocumentEntity> listBySourceMaterialId(Long sourceMaterialId) {
        ensureSourceMaterial(sourceMaterialId);
        return knowledgeDocumentMapper.listBySourceMaterialId(sourceMaterialId);
    }

    public void bindProjectDocumentsBySessionId(Long sessionId, Long projectId) {
        if (sessionId == null || sessionId <= 0 || projectId == null || projectId <= 0) {
            return;
        }
        ensureProject(projectId);
        knowledgeDocumentMapper.bindProjectIdBySessionId(sessionId, projectId);
    }

    public void deleteBySourceMaterialId(Long sourceMaterialId) {
        if (sourceMaterialId == null || sourceMaterialId <= 0) {
            return;
        }
        ensureSourceMaterial(sourceMaterialId);
        knowledgeDocumentMapper.deleteBySourceMaterialId(sourceMaterialId);
    }

    public KnowledgeDocumentEntity getById(Long id) {
        KnowledgeDocumentEntity document = knowledgeDocumentMapper.findById(id);
        if (document == null) {
            throw new IllegalArgumentException("Knowledge document not found.");
        }
        return document;
    }

    @Transactional
    public Long createDocument(CreateDocumentCommand command, UserContext operator) {
        validateScope(command.projectId(), command.requirementId(), command.sourceMaterialId());
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setProjectId(command.projectId());
        entity.setRequirementId(command.requirementId());
        entity.setSourceMaterialId(command.sourceMaterialId());
        entity.setDocumentType(requireText(command.documentType(), "Document type is required."));
        entity.setSourceUri(normalizeBlank(command.sourceUri()));
        entity.setTitle(normalizeBlank(command.title()));
        entity.setStatus("PENDING");
        entity.setRawText(normalizeBlank(command.rawText()));
        entity.setCleanText(normalizeBlank(command.cleanText()));
        entity.setSummary(normalizeBlank(command.summary()));
        entity.setKeywordsJson(normalizeBlank(command.keywordsJson()));
        entity.setTags(normalizeBlank(command.tags()));
        entity.setContentHash(normalizeBlank(command.contentHash()));
        entity.setVersionNo(command.versionNo() == null || command.versionNo() < 1 ? 1 : command.versionNo());
        entity.setIsLatest(true);
        entity.setRetrievable(Boolean.TRUE.equals(command.retrievable()));
        entity.setCreatedBy(operator.getUserId());
        entity.setUpdatedBy(operator.getUserId());
        knowledgeDocumentMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void updateProcessingResult(Long documentId, ProcessingResultCommand command, UserContext operator) {
        KnowledgeDocumentEntity document = getById(documentId);
        document.setStatus(requireText(command.status(), "Document status is required."));
        document.setCleanText(command.cleanText() == null ? document.getCleanText() : normalizeBlank(command.cleanText()));
        document.setSummary(command.summary() == null ? document.getSummary() : normalizeBlank(command.summary()));
        document.setKeywordsJson(command.keywordsJson() == null ? document.getKeywordsJson() : normalizeBlank(command.keywordsJson()));
        document.setTags(command.tags() == null ? document.getTags() : normalizeBlank(command.tags()));
        document.setContentHash(command.contentHash() == null ? document.getContentHash() : normalizeBlank(command.contentHash()));
        document.setRetrievable(command.retrievable() == null ? document.getRetrievable() : command.retrievable());
        document.setUpdatedBy(operator.getUserId());
        knowledgeDocumentMapper.update(document);
    }

    public List<KnowledgeDocumentAssetEntity> listAssets(Long documentId) {
        getById(documentId);
        return knowledgeDocumentAssetMapper.listByDocumentId(documentId);
    }

    public KnowledgeDocumentAssetEntity getPrimaryAsset(Long documentId) {
        List<KnowledgeDocumentAssetEntity> assets = listAssets(documentId);
        if (assets.isEmpty()) {
            throw new IllegalArgumentException("Knowledge document asset not found.");
        }
        for (KnowledgeDocumentAssetEntity item : assets) {
            if ("ORIGINAL".equalsIgnoreCase(item.getAssetRole())) {
                return item;
            }
        }
        return assets.get(0);
    }

    public List<KnowledgeDocumentChunkEntity> listChunks(Long documentId) {
        getById(documentId);
        return knowledgeDocumentChunkMapper.listByDocumentId(documentId);
    }

    public List<KnowledgeDocumentTaskEntity> listTasks(Long documentId) {
        getById(documentId);
        return knowledgeDocumentTaskMapper.listByDocumentId(documentId);
    }

    @Transactional
    public Long addAsset(Long documentId, DocumentAssetCommand command) {
        getById(documentId);
        KnowledgeDocumentAssetEntity entity = new KnowledgeDocumentAssetEntity();
        entity.setDocumentId(documentId);
        entity.setAssetRole(requireText(command.assetRole(), "Asset role is required."));
        entity.setStorageBucket(requireText(command.storageBucket(), "Storage bucket is required."));
        entity.setStorageKey(requireText(command.storageKey(), "Storage key is required."));
        entity.setMimeType(normalizeBlank(command.mimeType()));
        entity.setSizeBytes(command.sizeBytes());
        entity.setSha256(normalizeBlank(command.sha256()));
        knowledgeDocumentAssetMapper.insert(entity);
        return entity.getId();
    }

    @Transactional
    public void replaceChunks(Long documentId, List<DocumentChunkCommand> chunks) {
        getById(documentId);
        knowledgeDocumentChunkMapper.deleteByDocumentId(documentId);
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        int fallbackNo = 0;
        for (DocumentChunkCommand item : chunks) {
            KnowledgeDocumentChunkEntity entity = new KnowledgeDocumentChunkEntity();
            entity.setDocumentId(documentId);
            entity.setChunkNo(item.chunkNo() == null ? ++fallbackNo : item.chunkNo());
            entity.setChunkText(requireText(item.chunkText(), "Chunk text is required."));
            entity.setTokenCount(item.tokenCount());
            entity.setSummary(normalizeBlank(item.summary()));
            entity.setEmbeddingStatus(defaultIfBlank(item.embeddingStatus(), "PENDING"));
            entity.setVectorRef(normalizeBlank(item.vectorRef()));
            knowledgeDocumentChunkMapper.insert(entity);
            fallbackNo = entity.getChunkNo();
        }
    }

    @Transactional
    public Long createTask(Long documentId, DocumentTaskCommand command) {
        getById(documentId);
        KnowledgeDocumentTaskEntity entity = new KnowledgeDocumentTaskEntity();
        entity.setDocumentId(documentId);
        entity.setTaskType(requireText(command.taskType(), "Task type is required."));
        entity.setStatus(defaultIfBlank(command.status(), "PENDING"));
        entity.setAttemptCount(command.attemptCount() == null || command.attemptCount() < 0 ? 0 : command.attemptCount());
        entity.setLastError(normalizeBlank(command.lastError()));
        entity.setStartedAt(command.startedAt());
        entity.setFinishedAt(command.finishedAt());
        knowledgeDocumentTaskMapper.insert(entity);
        return entity.getId();
    }

    public KnowledgeDocumentTaskEntity getTaskById(Long taskId) {
        KnowledgeDocumentTaskEntity task = knowledgeDocumentTaskMapper.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Knowledge document task not found.");
        }
        return task;
    }

    public List<KnowledgeDocumentTaskEntity> listPendingTasks(int limit) {
        return knowledgeDocumentTaskMapper.listPendingTasks(limit <= 0 ? 20 : limit);
    }

    @Transactional
    public void updateTaskStatus(Long taskId,
                                 String status,
                                 Integer attemptCount,
                                 String lastError,
                                 LocalDateTime startedAt,
                                 LocalDateTime finishedAt) {
        KnowledgeDocumentTaskEntity task = getTaskById(taskId);
        task.setStatus(requireText(status, "Task status is required."));
        task.setAttemptCount(attemptCount == null || attemptCount < 0 ? task.getAttemptCount() : attemptCount);
        task.setLastError(normalizeBlank(lastError));
        task.setStartedAt(startedAt);
        task.setFinishedAt(finishedAt);
        knowledgeDocumentTaskMapper.updateStatus(task);
    }

    private void validateScope(Long projectId, Long requirementId, Long sourceMaterialId) {
        if (projectId != null) {
            ensureProject(projectId);
        }
        if (requirementId != null) {
            ensureRequirement(requirementId);
        }
        if (sourceMaterialId != null) {
            ensureSourceMaterial(sourceMaterialId);
        }
        if (projectId == null && requirementId == null && sourceMaterialId == null) {
            throw new IllegalArgumentException("Knowledge document scope is required.");
        }
    }

    private void ensureProject(Long projectId) {
        if (projectMapper.findById(projectId) == null) {
            throw new IllegalArgumentException("Project not found.");
        }
    }

    private void ensureRequirement(Long requirementId) {
        if (requirementMapper.findById(requirementId) == null) {
            throw new IllegalArgumentException("Requirement not found.");
        }
    }

    private void ensureSourceMaterial(Long sourceMaterialId) {
        if (projectSourceMaterialMapper.findById(sourceMaterialId) == null) {
            throw new IllegalArgumentException("Source material not found.");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
