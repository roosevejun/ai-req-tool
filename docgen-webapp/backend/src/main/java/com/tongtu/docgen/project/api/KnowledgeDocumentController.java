package com.tongtu.docgen.project.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentAssetEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentChunkEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentTaskEntity;
import com.tongtu.docgen.project.service.KnowledgeDocumentService;
import com.tongtu.docgen.system.security.AccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge-documents")
@Tag(name = "knowledge-document", description = "Knowledge document management")
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(KnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    public record KnowledgeDocumentListItem(
            Long id,
            Long projectId,
            Long requirementId,
            Long sourceMaterialId,
            String documentType,
            String sourceUri,
            String title,
            String status,
            String summary,
            String tags,
            String contentHash,
            Integer versionNo,
            Boolean isLatest,
            Boolean retrievable,
            String latestTaskStatus,
            String latestTaskError
    ) {
    }

    public record KnowledgeDocumentDetail(
            KnowledgeDocumentEntity document,
            List<KnowledgeDocumentAssetEntity> assets,
            List<KnowledgeDocumentChunkEntity> chunks,
            List<KnowledgeDocumentTaskEntity> tasks
    ) {
    }

    @GetMapping("/projects/{projectId}")
    @Operation(summary = "List knowledge documents in project")
    public ApiResponse<Object> listByProject(@PathVariable("projectId") Long projectId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        List<KnowledgeDocumentListItem> items = knowledgeDocumentService.listByProjectId(projectId).stream()
                .map(this::toListItem)
                .toList();
        return ApiResponse.ok(traceId, items);
    }

    @GetMapping("/requirements/{requirementId}")
    @Operation(summary = "List knowledge documents in requirement")
    public ApiResponse<Object> listByRequirement(@PathVariable("requirementId") Long requirementId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        List<KnowledgeDocumentListItem> items = knowledgeDocumentService.listByRequirementId(requirementId).stream()
                .map(this::toListItem)
                .toList();
        return ApiResponse.ok(traceId, items);
    }

    @GetMapping("/source-materials/{sourceMaterialId}")
    @Operation(summary = "List knowledge documents by source material")
    public ApiResponse<Object> listBySourceMaterial(@PathVariable("sourceMaterialId") Long sourceMaterialId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        List<KnowledgeDocumentListItem> items = knowledgeDocumentService.listBySourceMaterialId(sourceMaterialId).stream()
                .map(this::toListItem)
                .toList();
        return ApiResponse.ok(traceId, items);
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get knowledge document detail")
    public ApiResponse<Object> getDetail(@PathVariable("documentId") Long documentId) {
        String traceId = UUID.randomUUID().toString();
        AccessGuard.requireLogin();
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(documentId);
        return ApiResponse.ok(traceId, new KnowledgeDocumentDetail(
                document,
                knowledgeDocumentService.listAssets(documentId),
                knowledgeDocumentService.listChunks(documentId),
                knowledgeDocumentService.listTasks(documentId)
        ));
    }

    private KnowledgeDocumentListItem toListItem(KnowledgeDocumentEntity item) {
        List<KnowledgeDocumentTaskEntity> tasks = knowledgeDocumentService.listTasks(item.getId());
        KnowledgeDocumentTaskEntity latestTask = tasks.isEmpty() ? null : tasks.get(tasks.size() - 1);
        return new KnowledgeDocumentListItem(
                item.getId(),
                item.getProjectId(),
                item.getRequirementId(),
                item.getSourceMaterialId(),
                item.getDocumentType(),
                item.getSourceUri(),
                item.getTitle(),
                item.getStatus(),
                item.getSummary(),
                item.getTags(),
                item.getContentHash(),
                item.getVersionNo(),
                item.getIsLatest(),
                item.getRetrievable(),
                latestTask == null ? null : latestTask.getStatus(),
                latestTask == null ? null : latestTask.getLastError()
        );
    }
}
