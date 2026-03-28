package com.tongtu.docgen.project.service;

import com.tongtu.docgen.project.model.entity.KnowledgeDocumentEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentTaskEntity;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentAssetEntity;
import com.tongtu.docgen.system.model.UserContext;
import com.tongtu.docgen.llm.AgentClient;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeDocumentProcessingService {
    private static final int DEFAULT_PENDING_LIMIT = 20;
    private static final int CHUNK_SIZE = 1000;
    private static final int PDF_OCR_MAX_PAGES = 5;
    private static final float PDF_OCR_DPI = 180f;

    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeFileStorageService knowledgeFileStorageService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final AgentClient agentClient;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public KnowledgeDocumentProcessingService(KnowledgeDocumentService knowledgeDocumentService,
                                              KnowledgeFileStorageService knowledgeFileStorageService,
                                              KnowledgeRetrievalService knowledgeRetrievalService,
                                              AgentClient agentClient) {
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.knowledgeFileStorageService = knowledgeFileStorageService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
        this.agentClient = agentClient;
    }

    @Transactional
    public void processPendingTasks(UserContext operator) {
        processPendingTasks(DEFAULT_PENDING_LIMIT, operator);
    }

    @Transactional
    public void processPendingTasks(int limit, UserContext operator) {
        List<KnowledgeDocumentTaskEntity> tasks = knowledgeDocumentService.listPendingTasks(limit <= 0 ? DEFAULT_PENDING_LIMIT : limit);
        for (KnowledgeDocumentTaskEntity task : tasks) {
            processTask(task.getId(), operator);
        }
    }

    @Transactional
    public void processTask(Long taskId, UserContext operator) {
        KnowledgeDocumentTaskEntity task = knowledgeDocumentService.getTaskById(taskId);
        KnowledgeDocumentEntity document = knowledgeDocumentService.getById(task.getDocumentId());

        knowledgeDocumentService.updateTaskStatus(
                taskId,
                "RUNNING",
                safeAttemptCount(task.getAttemptCount()) + 1,
                null,
                LocalDateTime.now(),
                null
        );

        try {
            ProcessingPayload payload = switch (task.getTaskType()) {
                case "FETCH_URL" -> processUrlDocument(document);
                case "SUMMARIZE" -> processTextDocument(document);
                case "PARSE_FILE" -> processFileDocument(document);
                default -> throw new IllegalArgumentException("Unsupported knowledge document task type: " + task.getTaskType());
            };

            knowledgeDocumentService.updateProcessingResult(
                    document.getId(),
                    new KnowledgeDocumentService.ProcessingResultCommand(
                            "READY",
                            payload.cleanText(),
                            payload.summary(),
                            null,
                            null,
                            payload.contentHash(),
                            true
                    ),
                    operator
            );
            knowledgeDocumentService.replaceChunks(document.getId(), toChunkCommands(payload.cleanText()));
            knowledgeRetrievalService.embedDocumentChunks(buildTraceId(document.getId()), document.getId());
            knowledgeDocumentService.updateTaskStatus(
                    taskId,
                    "SUCCESS",
                    safeAttemptCount(task.getAttemptCount()) + 1,
                    null,
                    task.getStartedAt() == null ? LocalDateTime.now() : task.getStartedAt(),
                    LocalDateTime.now()
            );
        } catch (Exception ex) {
            knowledgeDocumentService.updateProcessingResult(
                    document.getId(),
                    new KnowledgeDocumentService.ProcessingResultCommand(
                            "FAILED",
                            document.getCleanText(),
                            document.getSummary(),
                            null,
                            null,
                            document.getContentHash(),
                            false
                    ),
                    operator
            );
            knowledgeDocumentService.updateTaskStatus(
                    taskId,
                    "FAILED",
                    safeAttemptCount(task.getAttemptCount()) + 1,
                    trimError(ex.getMessage()),
                    task.getStartedAt() == null ? LocalDateTime.now() : task.getStartedAt(),
                    LocalDateTime.now()
            );
        }
    }

    private ProcessingPayload processUrlDocument(KnowledgeDocumentEntity document) throws IOException, InterruptedException {
        String sourceUri = requireText(document.getSourceUri(), "Document URL is required.");
        HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUri))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "docgen-webapp/knowledge-fetcher")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Fetch URL failed with status " + response.statusCode() + ".");
        }
        String cleanText = normalizeBlank(stripHtml(response.body()));
        if (cleanText == null) {
            throw new IllegalStateException("Fetched page content is empty.");
        }
        return new ProcessingPayload(cleanText, summarize(cleanText), Integer.toHexString(cleanText.hashCode()));
    }

    private ProcessingPayload processTextDocument(KnowledgeDocumentEntity document) {
        String cleanText = normalizeBlank(document.getRawText());
        if (cleanText == null) {
            throw new IllegalStateException("Document raw text is empty.");
        }
        return new ProcessingPayload(cleanText, summarize(cleanText), Integer.toHexString(cleanText.hashCode()));
    }

    private ProcessingPayload processFileDocument(KnowledgeDocumentEntity document) throws IOException {
        KnowledgeDocumentAssetEntity asset = knowledgeDocumentService.getPrimaryAsset(document.getId());
        String cleanText;
        if (isPdf(asset)) {
            cleanText = normalizeBlank(extractPdfText(asset));
        } else if (isDocx(asset)) {
            cleanText = normalizeBlank(extractDocxText(asset));
        } else if (isImage(asset)) {
            cleanText = normalizeBlank(extractImageText(document, asset));
        } else if (isTextLike(asset)) {
            cleanText = normalizeBlank(knowledgeFileStorageService.readUtf8(asset.getStorageBucket(), asset.getStorageKey()));
        } else {
            throw new IllegalStateException("Current file parser only supports PDF, DOCX, image OCR, text, markdown, and JSON files.");
        }
        if (cleanText == null) {
            throw new IllegalStateException("Uploaded file content is empty.");
        }
        return new ProcessingPayload(cleanText, summarize(cleanText), Integer.toHexString(cleanText.hashCode()));
    }

    private String extractPdfText(KnowledgeDocumentAssetEntity asset) throws IOException {
        byte[] bytes = knowledgeFileStorageService.readBytes(asset.getStorageBucket(), asset.getStorageKey());
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extracted = normalizeBlank(stripper.getText(document));
            if (extracted != null) {
                return extracted;
            }
            return extractScannedPdfText(document, asset.getStorageKey());
        }
    }

    private String extractDocxText(KnowledgeDocumentAssetEntity asset) throws IOException {
        byte[] bytes = knowledgeFileStorageService.readBytes(asset.getStorageBucket(), asset.getStorageKey());
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractImageText(KnowledgeDocumentEntity document, KnowledgeDocumentAssetEntity asset) {
        byte[] bytes = knowledgeFileStorageService.readBytes(asset.getStorageBucket(), asset.getStorageKey());
        String filename = knowledgeFileStorageService.suggestFilename(asset.getStorageKey());
        return agentClient.extractTextFromImage(buildTraceId(document.getId()), bytes, asset.getMimeType(), filename);
    }

    private String extractScannedPdfText(PDDocument document, String storageKey) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        int pageCount = Math.min(document.getNumberOfPages(), PDF_OCR_MAX_PAGES);
        List<String> pageTexts = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, PDF_OCR_DPI, ImageType.RGB);
            byte[] pngBytes = renderPng(image);
            String pageText = normalizeBlank(agentClient.extractTextFromImage(
                    buildPdfOcrTraceId(storageKey, i + 1),
                    pngBytes,
                    "image/png",
                    buildPdfOcrFilename(storageKey, i + 1)
            ));
            if (pageText != null) {
                pageTexts.add("Page " + (i + 1) + ":\n" + pageText);
            }
        }
        return pageTexts.isEmpty() ? null : String.join("\n\n", pageTexts);
    }

    private byte[] renderPng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<KnowledgeDocumentService.DocumentChunkCommand> toChunkCommands(String cleanText) {
        List<KnowledgeDocumentService.DocumentChunkCommand> chunks = new ArrayList<>();
        if (cleanText == null || cleanText.isBlank()) {
            return chunks;
        }
        String normalized = cleanText.trim();
        int chunkNo = 1;
        for (int start = 0; start < normalized.length(); start += CHUNK_SIZE) {
            int end = Math.min(normalized.length(), start + CHUNK_SIZE);
            String item = normalized.substring(start, end).trim();
            if (item.isEmpty()) {
                continue;
            }
            chunks.add(new KnowledgeDocumentService.DocumentChunkCommand(
                    chunkNo++,
                    item,
                    item.length(),
                    item.length() <= 200 ? item : item.substring(0, 200),
                    "PENDING",
                    null
            ));
        }
        return chunks;
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return html
                .replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String summarize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
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

    private Integer safeAttemptCount(Integer attemptCount) {
        return attemptCount == null || attemptCount < 0 ? 0 : attemptCount;
    }

    private String trimError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown processing error.";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private String buildTraceId(Long documentId) {
        return documentId == null ? "knowledge-doc" : "knowledge-doc-" + documentId;
    }

    private String buildPdfOcrTraceId(String storageKey, int pageNo) {
        String safeKey = storageKey == null ? "pdf" : storageKey.replaceAll("[^a-zA-Z0-9-_./]", "_");
        return "knowledge-pdf-ocr-" + safeKey + "-page-" + pageNo;
    }

    private String buildPdfOcrFilename(String storageKey, int pageNo) {
        String base = storageKey == null || storageKey.isBlank() ? "document" : knowledgeFileStorageService.suggestFilename(storageKey);
        return base + "-page-" + pageNo + ".png";
    }

    private boolean isTextLike(KnowledgeDocumentAssetEntity asset) {
        String mimeType = asset.getMimeType() == null ? "" : asset.getMimeType().trim().toLowerCase();
        String storageKey = asset.getStorageKey() == null ? "" : asset.getStorageKey().trim().toLowerCase();
        if (mimeType.startsWith("text/")) {
            return true;
        }
        return storageKey.endsWith(".txt")
                || storageKey.endsWith(".md")
                || storageKey.endsWith(".markdown")
                || "application/json".equals(mimeType);
    }

    private boolean isPdf(KnowledgeDocumentAssetEntity asset) {
        String mimeType = asset.getMimeType() == null ? "" : asset.getMimeType().trim().toLowerCase();
        String storageKey = asset.getStorageKey() == null ? "" : asset.getStorageKey().trim().toLowerCase();
        return "application/pdf".equals(mimeType) || storageKey.endsWith(".pdf");
    }

    private boolean isDocx(KnowledgeDocumentAssetEntity asset) {
        String mimeType = asset.getMimeType() == null ? "" : asset.getMimeType().trim().toLowerCase();
        String storageKey = asset.getStorageKey() == null ? "" : asset.getStorageKey().trim().toLowerCase();
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mimeType)
                || storageKey.endsWith(".docx");
    }

    private boolean isImage(KnowledgeDocumentAssetEntity asset) {
        String mimeType = asset.getMimeType() == null ? "" : asset.getMimeType().trim().toLowerCase();
        String storageKey = asset.getStorageKey() == null ? "" : asset.getStorageKey().trim().toLowerCase();
        if (mimeType.startsWith("image/")) {
            return true;
        }
        return storageKey.endsWith(".png")
                || storageKey.endsWith(".jpg")
                || storageKey.endsWith(".jpeg")
                || storageKey.endsWith(".webp");
    }

    private record ProcessingPayload(
            String cleanText,
            String summary,
            String contentHash
    ) {
    }
}
