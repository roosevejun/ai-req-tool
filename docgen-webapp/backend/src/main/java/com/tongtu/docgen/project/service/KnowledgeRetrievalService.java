package com.tongtu.docgen.project.service;

import com.tongtu.docgen.llm.AgentClient;
import com.tongtu.docgen.project.mapper.KnowledgeDocumentChunkMapper;
import com.tongtu.docgen.project.model.entity.KnowledgeDocumentChunkEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class KnowledgeRetrievalService {
    private static final int DEFAULT_TOP_K = 5;
    private static final int DEFAULT_EMBED_LIMIT = 20;

    private final KnowledgeDocumentChunkMapper knowledgeDocumentChunkMapper;
    private final AgentClient agentClient;

    public KnowledgeRetrievalService(KnowledgeDocumentChunkMapper knowledgeDocumentChunkMapper,
                                     AgentClient agentClient) {
        this.knowledgeDocumentChunkMapper = knowledgeDocumentChunkMapper;
        this.agentClient = agentClient;
    }

    public record RetrievalItem(
            Long chunkId,
            Long documentId,
            Integer chunkNo,
            String chunkText,
            String summary,
            Double score
    ) {
    }

    public record RetrievalContext(
            String query,
            List<RetrievalItem> items,
            String contextText
    ) {
    }

    @Transactional
    public int embedPendingChunks(String traceId, int limit) {
        List<KnowledgeDocumentChunkEntity> pendingChunks = knowledgeDocumentChunkMapper.listPendingEmbeddingChunks(
                limit <= 0 ? DEFAULT_EMBED_LIMIT : limit
        );
        int successCount = 0;
        for (KnowledgeDocumentChunkEntity item : pendingChunks) {
            if (embedChunk(traceId, item.getId())) {
                successCount++;
            }
        }
        return successCount;
    }

    @Transactional
    public boolean embedChunk(String traceId, Long chunkId) {
        KnowledgeDocumentChunkEntity chunk = knowledgeDocumentChunkMapper.findById(chunkId);
        if (chunk == null || chunk.getChunkText() == null || chunk.getChunkText().isBlank()) {
            return false;
        }

        List<Float> embedding = agentClient.embedText(traceId, chunk.getChunkText());
        if (embedding == null || embedding.isEmpty()) {
            chunk.setEmbeddingStatus("FAILED");
            chunk.setEmbeddedAt(LocalDateTime.now());
            knowledgeDocumentChunkMapper.updateEmbedding(chunk);
            return false;
        }

        chunk.setEmbeddingStatus("READY");
        chunk.setEmbeddingModel(agentClient.embeddingModel());
        chunk.setEmbeddingVectorLiteral(toVectorLiteral(embedding));
        chunk.setVectorRef("pgvector");
        chunk.setEmbeddedAt(LocalDateTime.now());
        knowledgeDocumentChunkMapper.updateEmbedding(chunk);
        return true;
    }

    public RetrievalContext searchProjectKnowledge(String traceId, Long projectId, String query, int topK) {
        String normalizedQuery = normalizeQuery(query);
        if (projectId == null || normalizedQuery == null) {
            return new RetrievalContext(query, List.of(), "");
        }
        String queryVector = toVectorLiteral(agentClient.embedText(traceId, normalizedQuery));
        List<KnowledgeDocumentChunkEntity> rows = knowledgeDocumentChunkMapper.searchProjectChunks(
                projectId,
                queryVector,
                topK <= 0 ? DEFAULT_TOP_K : topK
        );
        return buildContext(normalizedQuery, rows);
    }

    public RetrievalContext searchRequirementKnowledge(String traceId, Long requirementId, String query, int topK) {
        String normalizedQuery = normalizeQuery(query);
        if (requirementId == null || normalizedQuery == null) {
            return new RetrievalContext(query, List.of(), "");
        }
        String queryVector = toVectorLiteral(agentClient.embedText(traceId, normalizedQuery));
        List<KnowledgeDocumentChunkEntity> rows = knowledgeDocumentChunkMapper.searchRequirementChunks(
                requirementId,
                queryVector,
                topK <= 0 ? DEFAULT_TOP_K : topK
        );
        return buildContext(normalizedQuery, rows);
    }

    private RetrievalContext buildContext(String query, List<KnowledgeDocumentChunkEntity> rows) {
        List<RetrievalItem> items = new ArrayList<>();
        Set<Long> uniqueChunkIds = new LinkedHashSet<>();
        StringBuilder context = new StringBuilder();
        int index = 1;
        for (KnowledgeDocumentChunkEntity row : rows) {
            if (row == null || row.getId() == null || !uniqueChunkIds.add(row.getId())) {
                continue;
            }
            RetrievalItem item = new RetrievalItem(
                    row.getId(),
                    row.getDocumentId(),
                    row.getChunkNo(),
                    row.getChunkText(),
                    row.getSummary(),
                    row.getScore()
            );
            items.add(item);
            context.append(index++)
                    .append(". [document#")
                    .append(row.getDocumentId())
                    .append(" chunk#")
                    .append(row.getChunkNo())
                    .append("]\n")
                    .append(trimContextText(row.getChunkText()))
                    .append("\n\n");
        }
        return new RetrievalContext(query, items, context.toString().trim());
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimContextText(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 800 ? trimmed : trimmed.substring(0, 800);
    }

    private String toVectorLiteral(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            float value = vector.get(i) == null ? 0.0f : vector.get(i);
            sb.append(String.format(Locale.US, "%.8f", value));
        }
        sb.append(']');
        return sb.toString();
    }
}
