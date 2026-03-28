package com.tongtu.docgen.project.api;

import com.tongtu.docgen.api.ApiResponse;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentAssetEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentChunkEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentTaskEntity;
import com.tongtu.docgen.project.service.KnowledgeDocumentProcessingService;
import com.tongtu.docgen.project.service.KnowledgeDocumentService;
import com.tongtu.docgen.project.service.KnowledgeFileStorageService;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.system.security.AccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge-documents")
@Tag(name = "knowledge-document", description = "Knowledge document management")
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeDocumentProcessingService knowledgeDocumentProcessingService;
    private final KnowledgeFileStorageService knowledgeFileStorageService;

    public KnowledgeDocumentController(KnowledgeDocumentService knowledgeDocumentService,
                                       KnowledgeDocumentProcessingService knowledgeDocumentProcessingService,
                                       KnowledgeFileStorageService knowledgeFileStorageService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.knowledgeDocumentProcessingService = knowledgeDocumentProcessingService;
        this.knowledgeFileStorageService = knowledgeFileStorageService;
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
        return ApiResponse.ok(traceId, buildDetail(documentId));
    }

    @PostMapping("/{documentId}/reprocess")
    @Operation(summary = "Reprocess knowledge document")
    public ApiResponse<Object> reprocess(@PathVariable("documentId") Long documentId) {
        String traceId = UUID.randomUUID().toString();
        UserContext ctx = AccessGuard.requireLogin();
        Long taskId = knowledgeDocumentService.reprocessDocument(documentId, ctx);
        knowledgeDocumentProcessingService.processTask(taskId, ctx);
        return ApiResponse.ok(traceId, buildDetail(documentId));
    }

    @GetMapping("/assets/{assetId}/download")
    @Operation(summary = "Download knowledge document asset")
    public ResponseEntity<ByteArrayResource> downloadAsset(@PathVariable("assetId") Long assetId) {
        AccessGuard.requireLogin();
        return buildAssetResponse(assetId, false);
    }

    @GetMapping("/assets/{assetId}/preview")
    @Operation(summary = "Preview knowledge document asset")
    public ResponseEntity<ByteArrayResource> previewAsset(@PathVariable("assetId") Long assetId) {
        AccessGuard.requireLogin();
        return buildAssetResponse(assetId, true);
    }

    private KnowledgeDocumentListItem toListItem(KnowledgeDocumentEntity item) {
        KnowledgeDocumentTaskEntity latestTask = knowledgeDocumentService.getLatestTask(item.getId());
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

    private KnowledgeDocumentDetail buildDetail(Long documentId) {
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(documentId);
        return new KnowledgeDocumentDetail(
                document,
                knowledgeDocumentService.listAssets(documentId),
                knowledgeDocumentService.listChunks(documentId),
                knowledgeDocumentService.listTasks(documentId)
        );
    }

    private ResponseEntity<ByteArrayResource> buildAssetResponse(Long assetId, boolean inline) {
        KnowledgeDocumentAssetEntity asset = knowledgeDocumentService.getAssetById(assetId);
        byte[] bytes = knowledgeFileStorageService.readBytes(asset.getStorageBucket(), asset.getStorageKey());
        String filename = knowledgeFileStorageService.suggestFilename(asset.getStorageKey());
        MediaType mediaType = resolveMediaType(asset.getMimeType(), filename);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(bytes.length);
        headers.setContentDisposition(ContentDisposition.builder(inline ? "inline" : "attachment")
                .filename(filename, StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(bytes));
    }

    private MediaType resolveMediaType(String mimeType, String filename) {
        if (mimeType != null && !mimeType.isBlank()) {
            try {
                return MediaType.parseMediaType(mimeType.trim());
            } catch (Exception ignore) {
                // fallback below
            }
        }
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return MediaType.TEXT_PLAIN;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
