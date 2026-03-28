package com.tongtu.docgen.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongtu.docgen.llm.AgentClient;
import com.tongtu.docgen.project.mapper.ProjectChatMessageMapper;
import com.tongtu.docgen.project.mapper.ProjectChatSessionMapper;
import com.tongtu.docgen.project.mapper.ProjectSourceMaterialMapper;
import com.tongtu.docgen.project.model.entity.ProjectChatMessageEntity;
import com.tongtu.docgen.project.model.entity.ProjectChatSessionEntity;
import com.tongtu.docgen.project.model.entity.ProjectSourceMaterialEntity;
import com.tongtu.docgen.project.model.entity.ProjectEntity;
import com.tongtu.docgen.system.model.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProjectAiConversationService {
    private final ProjectChatSessionMapper sessionMapper;
    private final ProjectChatMessageMapper messageMapper;
    private final ProjectSourceMaterialMapper materialMapper;
    private final AgentClient agentClient;
    private final ObjectMapper objectMapper;
    private final ProjectService projectService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeDocumentProcessingService knowledgeDocumentProcessingService;
    private final KnowledgeFileStorageService knowledgeFileStorageService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;

    public ProjectAiConversationService(ProjectChatSessionMapper sessionMapper,
                                        ProjectChatMessageMapper messageMapper,
                                        ProjectSourceMaterialMapper materialMapper,
                                        AgentClient agentClient,
                                        ObjectMapper objectMapper,
                                        ProjectService projectService,
                                        KnowledgeDocumentService knowledgeDocumentService,
                                        KnowledgeDocumentProcessingService knowledgeDocumentProcessingService,
                                        KnowledgeFileStorageService knowledgeFileStorageService,
                                        KnowledgeRetrievalService knowledgeRetrievalService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.materialMapper = materialMapper;
        this.agentClient = agentClient;
        this.objectMapper = objectMapper;
        this.projectService = projectService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.knowledgeDocumentProcessingService = knowledgeDocumentProcessingService;
        this.knowledgeFileStorageService = knowledgeFileStorageService;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    public record SourceMaterialInput(
            String materialType,
            String title,
            String sourceUri,
            String rawContent
    ) {
    }

    public record ChatMessageView(
            Long id,
            String role,
            String messageType,
            String content,
            Integer seqNo
    ) {
    }

    public record SourceMaterialView(
            Long id,
            String materialType,
            String title,
            String sourceUri,
            String rawContent,
            String aiExtractedSummary
    ) {
    }

    public record StructuredInfo(
            String projectName,
            String description,
            String projectBackground,
            String similarProducts,
            String targetCustomerGroups,
            String commercialValue,
            String coreProductValue,
            String businessKnowledgeSummary
    ) {
    }

    public record ConversationView(
            Long sessionId,
            String jobId,
            String status,
            String assistantSummary,
            String businessKnowledgeSummary,
            StructuredInfo structuredInfo,
            List<ChatMessageView> messages,
            List<SourceMaterialView> materials,
            boolean readyToCreate
    ) {
    }

    public record ConversationTurnResult(
            Long sessionId,
            String jobId,
            String assistantMessage,
            List<String> followUpQuestions,
            StructuredInfo structuredInfo,
            boolean readyToCreate
    ) {
    }

    public record MaterialSaveResult(
            Long sessionId,
            int materialCount
    ) {
    }

    public record CreateProjectFromConversationResult(
            Long sessionId,
            Long projectId
    ) {
    }

    public record KnowledgePreviewView(
            String query,
            List<KnowledgeRetrievalService.RetrievalItem> items,
            String contextText
    ) {
    }

    @Transactional
    public ConversationTurnResult startConversation(String traceId,
                                                    String projectName,
                                                    String description,
                                                    String initialMessage,
                                                    List<SourceMaterialInput> materials,
                                                    UserContext operator) {
        String bootstrapDescription = mergeBootstrapDescription(description, initialMessage);
        ProjectChatSessionEntity session = new ProjectChatSessionEntity();
        session.setProjectId(null);
        session.setJobId("proj-chat-" + UUID.randomUUID().toString().replace("-", ""));
        session.setStatus("ACTIVE");
        session.setAssistantSummary(null);
        session.setBusinessKnowledgeSummary(null);
        session.setStructuredInfoJson(writeStructuredInfo(new StructuredInfo(
                normalize(projectName),
                normalize(bootstrapDescription),
                "",
                "",
                "",
                "",
                "",
                ""
        )));
        session.setReadyToCreate(false);
        session.setCreatedBy(operator.getUserId());
        sessionMapper.insert(session);

        int seqNo = 0;
        String initialUserContent = buildInitialUserContent(projectName, description, initialMessage);
        if (!initialUserContent.isBlank()) {
            seqNo = appendMessage(session.getId(), ++seqNo, "user", "chat", initialUserContent);
        }
        if (materials != null && !materials.isEmpty()) {
            for (SourceMaterialInput item : materials) {
                saveMaterial(session.getId(), null, item, operator.getUserId());
                String materialMessage = formatMaterialMessage(item);
                if (!materialMessage.isBlank()) {
                    seqNo = appendMessage(session.getId(), ++seqNo, "user", "material", materialMessage);
                }
            }
        }

        StructuredInfo current = readStructuredInfo(session.getStructuredInfoJson());
        AgentClient.ProjectProductGuideResult aiResult = runGuide(traceId, current, session.getId());
        StructuredInfo next = mergeStructuredInfo(current, aiResult);
        updateSessionAfterAi(session, aiResult, next);
        appendAssistantMessages(session.getId(), seqNo, aiResult);
        return toTurnResult(session, aiResult.assistantMessage(), aiResult.followUpQuestions(), next);
    }

    public ConversationTurnResult continueConversation(String traceId,
                                                       Long sessionId,
                                                       String message,
                                                       UserContext operator) {
        ProjectChatSessionEntity session = requireSession(sessionId);
        StructuredInfo current = readStructuredInfo(session.getStructuredInfoJson());
        int seqNo = messageMapper.findMaxSeqNo(sessionId);
        if (message != null && !message.isBlank()) {
            appendMessage(sessionId, ++seqNo, "user", "chat", message.trim());
        }
        AgentClient.ProjectProductGuideResult aiResult = runGuide(traceId, current, sessionId);
        StructuredInfo next = mergeStructuredInfo(current, aiResult);
        updateSessionAfterAi(session, aiResult, next);
        appendAssistantMessages(sessionId, seqNo, aiResult);
        return toTurnResult(session, aiResult.assistantMessage(), aiResult.followUpQuestions(), next);
    }

    @Transactional
    public MaterialSaveResult addMaterials(Long sessionId,
                                           List<SourceMaterialInput> materials,
                                           UserContext operator) {
        requireSession(sessionId);
        if (materials == null || materials.isEmpty()) {
            return new MaterialSaveResult(sessionId, 0);
        }
        int seqNo = messageMapper.findMaxSeqNo(sessionId);
        int count = 0;
        for (SourceMaterialInput item : materials) {
            saveMaterial(sessionId, null, item, operator.getUserId());
            String materialMessage = formatMaterialMessage(item);
            if (!materialMessage.isBlank()) {
                appendMessage(sessionId, ++seqNo, "user", "material", materialMessage);
            }
            count++;
        }
        return new MaterialSaveResult(sessionId, count);
    }

    @Transactional
    public void deleteMaterial(Long materialId, UserContext operator) {
        ProjectSourceMaterialEntity material = materialMapper.findById(materialId);
        if (material == null) {
            throw new IllegalArgumentException("Project source material not found.");
        }
        requireSession(material.getSessionId());
        knowledgeDocumentService.deleteBySourceMaterialId(materialId);
        if (materialMapper.deleteById(materialId) <= 0) {
            throw new IllegalArgumentException("Project source material not found.");
        }
    }

    @Transactional
    public ConversationView resumeProjectConversation(String traceId, Long projectId, UserContext operator) {
        ProjectEntity project = projectService.getById(projectId);
        ProjectChatSessionEntity session = sessionMapper.findLatestByProjectId(projectId);
        if (session != null) {
            syncSessionWithProjectSnapshot(session, project);
            return getConversation(session.getId());
        }

        session = new ProjectChatSessionEntity();
        session.setProjectId(projectId);
        session.setJobId("proj-edit-" + UUID.randomUUID().toString().replace("-", ""));
        session.setStatus("ACTIVE");
        session.setAssistantSummary(null);
        session.setBusinessKnowledgeSummary(normalize(project.getBusinessKnowledgeSummary()));
        session.setStructuredInfoJson(writeStructuredInfo(toStructuredInfo(project)));
        session.setReadyToCreate(false);
        session.setCreatedBy(operator.getUserId());
        sessionMapper.insert(session);

        int seqNo = 0;
        String snapshotMessage = buildProjectSnapshotMessage(project);
        if (!snapshotMessage.isBlank()) {
            seqNo = appendMessage(session.getId(), ++seqNo, "user", "snapshot", snapshotMessage);
        }

        StructuredInfo current = readStructuredInfo(session.getStructuredInfoJson());
        AgentClient.ProjectProductGuideResult aiResult = runGuide(traceId, current, session.getId());
        StructuredInfo next = mergeStructuredInfo(current, aiResult);
        updateSessionAfterAi(session, aiResult, next);
        appendAssistantMessages(session.getId(), seqNo, aiResult);
        return getConversation(session.getId());
    }

    public ConversationTurnResult continueProjectConversation(String traceId,
                                                              Long projectId,
                                                              Long sessionId,
                                                              String message,
                                                              UserContext operator) {
        ProjectChatSessionEntity session = requireProjectSession(projectId, sessionId);
        return continueConversation(traceId, session.getId(), message, operator);
    }

    public KnowledgePreviewView previewProjectKnowledgeContext(String traceId,
                                                               Long projectId,
                                                               Long sessionId,
                                                               String query) {
        ProjectChatSessionEntity session = requireProjectSession(projectId, sessionId);
        return previewKnowledgeContext(traceId, session.getId(), query);
    }

    @Transactional
    public MaterialSaveResult uploadFileMaterial(Long sessionId,
                                                 String title,
                                                 MultipartFile file,
                                                 UserContext operator) {
        ProjectChatSessionEntity session = requireSession(sessionId);
        KnowledgeFileStorageService.StoredFile storedFile = knowledgeFileStorageService.storeProjectConversationFile(sessionId, file);

        ProjectSourceMaterialEntity entity = new ProjectSourceMaterialEntity();
        entity.setSessionId(sessionId);
        entity.setProjectId(session.getProjectId());
        entity.setMaterialType("FILE");
        entity.setTitle(firstNonBlank(title, storedFile.originalFilename()));
        entity.setSourceUri(storedFile.storageKey());
        entity.setRawContent(null);
        entity.setAiExtractedSummary("Uploaded file: " + storedFile.originalFilename());
        entity.setCreatedBy(operator.getUserId());
        materialMapper.insert(entity);

        int seqNo = messageMapper.findMaxSeqNo(sessionId);
        appendMessage(sessionId, ++seqNo, "user", "material", formatFileMaterialMessage(entity.getTitle(), storedFile));
        syncKnowledgeDocument(entity, null, operator.getUserId(), storedFile, true);
        return new MaterialSaveResult(sessionId, 1);
    }

    public ConversationView getConversation(Long sessionId) {
        ProjectChatSessionEntity session = requireSession(sessionId);
        StructuredInfo structuredInfo = readStructuredInfo(session.getStructuredInfoJson());
        List<ChatMessageView> messages = messageMapper.listBySessionId(sessionId).stream()
                .map(item -> new ChatMessageView(item.getId(), item.getRole(), item.getMessageType(), item.getContent(), item.getSeqNo()))
                .toList();
        List<SourceMaterialView> materials = materialMapper.listBySessionId(sessionId).stream()
                .map(item -> new SourceMaterialView(item.getId(), item.getMaterialType(), item.getTitle(), item.getSourceUri(), item.getRawContent(), item.getAiExtractedSummary()))
                .toList();
        return new ConversationView(
                session.getId(),
                session.getJobId(),
                session.getStatus(),
                defaultString(session.getAssistantSummary()),
                defaultString(session.getBusinessKnowledgeSummary()),
                structuredInfo,
                messages,
                materials,
                Boolean.TRUE.equals(session.getReadyToCreate())
        );
    }

    public KnowledgePreviewView previewKnowledgeContext(String traceId, Long sessionId, String query) {
        ProjectChatSessionEntity session = requireSession(sessionId);
        StructuredInfo current = readStructuredInfo(session.getStructuredInfoJson());
        List<ProjectSourceMaterialEntity> materials = materialMapper.listBySessionId(sessionId);
        List<Long> sourceMaterialIds = materials.stream()
                .map(ProjectSourceMaterialEntity::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        String retrievalQuery = query == null || query.isBlank()
                ? buildKnowledgeQuery(current, sessionId)
                : query.trim();
        KnowledgeRetrievalService.RetrievalContext context = knowledgeRetrievalService.searchSourceMaterialKnowledge(
                traceId,
                sourceMaterialIds,
                retrievalQuery,
                5
        );
        return new KnowledgePreviewView(context.query(), context.items(), context.contextText());
    }

    public CreateProjectFromConversationResult createProjectFromConversation(Long sessionId,
                                                                            String projectKey,
                                                                            String projectName,
                                                                            String projectType,
                                                                            String priority,
                                                                            String visibility,
                                                                            Long ownerUserId,
                                                                            UserContext operator) {
        ProjectChatSessionEntity session = requireSession(sessionId);
        StructuredInfo structuredInfo = readStructuredInfo(session.getStructuredInfoJson());
        String finalProjectName = firstNonBlank(projectName, structuredInfo.projectName());
        if (finalProjectName.isBlank()) {
            throw new IllegalArgumentException("Project name is required.");
        }
        Long projectId = projectService.createProjectFromAiConversation(
                projectKey,
                finalProjectName,
                blankToNull(structuredInfo.description()),
                blankToNull(structuredInfo.projectBackground()),
                blankToNull(structuredInfo.similarProducts()),
                blankToNull(structuredInfo.targetCustomerGroups()),
                blankToNull(structuredInfo.commercialValue()),
                blankToNull(structuredInfo.coreProductValue()),
                blankToNull(structuredInfo.businessKnowledgeSummary()),
                blankToNull(projectType),
                blankToNull(priority),
                null,
                null,
                null,
                ownerUserId,
                blankToNull(visibility),
                operator
        );
        session.setProjectId(projectId);
        session.setStatus("COMPLETED");
        session.setReadyToCreate(true);
        session.setBusinessKnowledgeSummary(structuredInfo.businessKnowledgeSummary());
        session.setStructuredInfoJson(writeStructuredInfo(structuredInfo));
        sessionMapper.update(session);
        materialMapper.bindProjectIdBySessionId(sessionId, projectId);
        knowledgeDocumentService.bindProjectDocumentsBySessionId(sessionId, projectId);
        return new CreateProjectFromConversationResult(sessionId, projectId);
    }

    private AgentClient.ProjectProductGuideResult runGuide(String traceId, StructuredInfo current, Long sessionId) {
        String retrievalContext = loadKnowledgeContext(traceId, current, sessionId);
        String descriptionWithMaterials = mergeDescriptionAndMaterials(current.description(), sessionId, retrievalContext);
        return agentClient.guideProjectProductInfo(
                traceId,
                current.projectName(),
                descriptionWithMaterials,
                current.projectBackground(),
                current.similarProducts(),
                current.targetCustomerGroups(),
                current.commercialValue(),
                current.coreProductValue(),
                buildAnswers(sessionId)
        );
    }

    private List<AgentClient.ProjectProductAnswer> buildAnswers(Long sessionId) {
        List<AgentClient.ProjectProductAnswer> answers = new ArrayList<>();
        for (ProjectChatMessageEntity item : messageMapper.listBySessionId(sessionId)) {
            if (!"user".equalsIgnoreCase(item.getRole())) {
                continue;
            }
            answers.add(new AgentClient.ProjectProductAnswer("", item.getContent()));
        }
        return answers;
    }

    private StructuredInfo mergeStructuredInfo(StructuredInfo current, AgentClient.ProjectProductGuideResult aiResult) {
        String background = choose(aiResult.projectBackground(), current.projectBackground());
        String similarProducts = choose(aiResult.similarProducts(), current.similarProducts());
        String targetCustomers = choose(aiResult.targetCustomerGroups(), current.targetCustomerGroups());
        String commercialValue = choose(aiResult.commercialValue(), current.commercialValue());
        String coreValue = choose(aiResult.coreProductValue(), current.coreProductValue());
        String businessKnowledgeSummary = summarizeKnowledge(current.projectName(), current.description(), background, targetCustomers, commercialValue, coreValue);
        return new StructuredInfo(
                current.projectName(),
                current.description(),
                background,
                similarProducts,
                targetCustomers,
                commercialValue,
                coreValue,
                businessKnowledgeSummary
        );
    }

    private void updateSessionAfterAi(ProjectChatSessionEntity session,
                                      AgentClient.ProjectProductGuideResult aiResult,
                                      StructuredInfo next) {
        boolean readyToCreate = isReadyToCreate(next);
        session.setStatus(readyToCreate ? "READY_TO_CREATE" : "ACTIVE");
        session.setAssistantSummary(aiResult.assistantMessage());
        session.setBusinessKnowledgeSummary(next.businessKnowledgeSummary());
        session.setStructuredInfoJson(writeStructuredInfo(next));
        session.setReadyToCreate(readyToCreate);
        sessionMapper.update(session);
    }

    private void appendAssistantMessages(Long sessionId, int currentSeqNo, AgentClient.ProjectProductGuideResult aiResult) {
        int seqNo = currentSeqNo;
        if (aiResult.assistantMessage() != null && !aiResult.assistantMessage().isBlank()) {
            appendMessage(sessionId, ++seqNo, "assistant", "chat", aiResult.assistantMessage());
        }
        if (aiResult.followUpQuestions() != null) {
            for (String question : aiResult.followUpQuestions()) {
                if (question == null || question.isBlank()) {
                    continue;
                }
                appendMessage(sessionId, ++seqNo, "assistant", "question", question.trim());
            }
        }
    }

    private int appendMessage(Long sessionId, int seqNo, String role, String messageType, String content) {
        ProjectChatMessageEntity entity = new ProjectChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setSeqNo(seqNo);
        entity.setRole(role);
        entity.setMessageType(messageType);
        entity.setContent(content);
        messageMapper.insert(entity);
        return seqNo;
    }

    private void saveMaterial(Long sessionId, Long projectId, SourceMaterialInput input, Long userId) {
        if (input == null) {
            return;
        }
        ProjectChatSessionEntity session = requireSession(sessionId);
        ProjectSourceMaterialEntity entity = new ProjectSourceMaterialEntity();
        entity.setSessionId(sessionId);
        entity.setProjectId(projectId != null ? projectId : session.getProjectId());
        entity.setMaterialType(normalizeMaterialType(input.materialType()));
        entity.setTitle(normalize(input.title()));
        entity.setSourceUri(normalize(input.sourceUri()));
        entity.setRawContent(normalize(input.rawContent()));
        entity.setAiExtractedSummary(buildMaterialSummary(input));
        entity.setCreatedBy(userId);
        materialMapper.insert(entity);
        syncKnowledgeDocument(entity, input, userId, null, true);
    }

    private void syncKnowledgeDocument(ProjectSourceMaterialEntity material,
                                       SourceMaterialInput input,
                                       Long userId,
                                       KnowledgeFileStorageService.StoredFile storedFile,
                                       boolean autoProcessTask) {
        UserContext operator = new UserContext();
        operator.setUserId(userId);

        Long documentId = knowledgeDocumentService.createDocument(
                new KnowledgeDocumentService.CreateDocumentCommand(
                        material.getProjectId(),
                        null,
                        material.getId(),
                        normalizeMaterialType(input == null ? material.getMaterialType() : input.materialType()),
                        normalize(input == null ? material.getSourceUri() : input.sourceUri()),
                        normalize(input == null ? material.getTitle() : input.title()),
                        normalize(input == null ? material.getRawContent() : input.rawContent()),
                        null,
                        input == null ? material.getAiExtractedSummary() : buildMaterialSummary(input),
                        null,
                        null,
                        null,
                        1,
                        false
                ),
                operator
        );

        if (storedFile != null) {
            knowledgeDocumentService.addAsset(
                    documentId,
                    new KnowledgeDocumentService.DocumentAssetCommand(
                            "ORIGINAL",
                            storedFile.storageBucket(),
                            storedFile.storageKey(),
                            storedFile.mimeType(),
                            storedFile.sizeBytes(),
                            storedFile.sha256()
                    )
            );
        }

        Long taskId = knowledgeDocumentService.createTask(
                documentId,
                new KnowledgeDocumentService.DocumentTaskCommand(
                        resolveInitialTaskType(material.getMaterialType()),
                        "PENDING",
                        0,
                        null,
                        null,
                        null
                )
        );
        if (autoProcessTask) {
            knowledgeDocumentProcessingService.processTask(taskId, operator);
        }
    }

    private String resolveInitialTaskType(String materialType) {
        return switch (normalizeMaterialType(materialType)) {
            case "URL" -> "FETCH_URL";
            case "FILE" -> "PARSE_FILE";
            default -> "SUMMARIZE";
        };
    }

    private String buildMaterialSummary(SourceMaterialInput input) {
        if (input == null) {
            return null;
        }
        if (input.rawContent() != null && !input.rawContent().isBlank()) {
            String content = input.rawContent().trim();
            return content.length() <= 500 ? content : content.substring(0, 500);
        }
        return normalize(input.sourceUri());
    }

    private String formatMaterialMessage(SourceMaterialInput input) {
        if (input == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (input.title() != null && !input.title().isBlank()) {
            parts.add("Title: " + input.title().trim());
        }
        if (input.sourceUri() != null && !input.sourceUri().isBlank()) {
            parts.add("URL: " + input.sourceUri().trim());
        }
        if (input.rawContent() != null && !input.rawContent().isBlank()) {
            parts.add("Content: " + input.rawContent().trim());
        }
        return String.join("\n", parts);
    }

    private String formatFileMaterialMessage(String title, KnowledgeFileStorageService.StoredFile storedFile) {
        List<String> parts = new ArrayList<>();
        if (title != null && !title.isBlank()) {
            parts.add("Title: " + title.trim());
        }
        parts.add("File: " + storedFile.originalFilename());
        if (storedFile.mimeType() != null && !storedFile.mimeType().isBlank()) {
            parts.add("Type: " + storedFile.mimeType().trim());
        }
        parts.add("Storage Key: " + storedFile.storageKey());
        return String.join("\n", parts);
    }

    private String buildInitialUserContent(String projectName, String description, String initialMessage) {
        List<String> parts = new ArrayList<>();
        if (projectName != null && !projectName.isBlank()) {
            parts.add("Project Name: " + projectName.trim());
        }
        if (description != null && !description.isBlank()) {
            parts.add("Description: " + description.trim());
        }
        if (initialMessage != null && !initialMessage.isBlank()) {
            parts.add("Initial Idea: " + initialMessage.trim());
        }
        return String.join("\n", parts);
    }

    private String mergeBootstrapDescription(String description, String initialMessage) {
        String normalizedDescription = normalize(description);
        String normalizedInitialMessage = normalize(initialMessage);
        if (normalizedDescription == null) {
            return normalizedInitialMessage;
        }
        if (normalizedInitialMessage == null) {
            return normalizedDescription;
        }
        if (normalizedDescription.contains(normalizedInitialMessage)) {
            return normalizedDescription;
        }
        return normalizedDescription + "\n\n" + normalizedInitialMessage;
    }

    private String mergeDescriptionAndMaterials(String description, Long sessionId, String retrievalContext) {
        StringBuilder sb = new StringBuilder(defaultString(description));
        List<ProjectSourceMaterialEntity> materials = materialMapper.listBySessionId(sessionId);
        if (!materials.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Reference Materials:\n");
            for (ProjectSourceMaterialEntity item : materials) {
                if (item.getTitle() != null && !item.getTitle().isBlank()) {
                    sb.append("- ").append(item.getTitle().trim());
                } else {
                    sb.append("- Material");
                }
                if (item.getSourceUri() != null && !item.getSourceUri().isBlank()) {
                    sb.append(" (").append(item.getSourceUri().trim()).append(")");
                }
                if (item.getAiExtractedSummary() != null && !item.getAiExtractedSummary().isBlank()) {
                    sb.append(": ").append(item.getAiExtractedSummary().trim());
                }
                sb.append("\n");
            }
        }
        if (retrievalContext != null && !retrievalContext.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Retrieved Knowledge Context:\n")
                    .append(retrievalContext.trim());
        }
        return sb.toString().trim();
    }

    private String loadKnowledgeContext(String traceId, StructuredInfo current, Long sessionId) {
        List<ProjectSourceMaterialEntity> materials = materialMapper.listBySessionId(sessionId);
        if (materials.isEmpty()) {
            return "";
        }
        List<Long> sourceMaterialIds = materials.stream()
                .map(ProjectSourceMaterialEntity::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (sourceMaterialIds.isEmpty()) {
            return "";
        }
        String retrievalQuery = buildKnowledgeQuery(current, sessionId);
        if (retrievalQuery.isBlank()) {
            return "";
        }
        return knowledgeRetrievalService.searchSourceMaterialKnowledge(traceId, sourceMaterialIds, retrievalQuery, 5)
                .contextText();
    }

    private String buildKnowledgeQuery(StructuredInfo current, Long sessionId) {
        List<String> parts = new ArrayList<>();
        if (current != null) {
            if (current.projectName() != null && !current.projectName().isBlank()) {
                parts.add(current.projectName().trim());
            }
            if (current.description() != null && !current.description().isBlank()) {
                parts.add(current.description().trim());
            }
            if (current.projectBackground() != null && !current.projectBackground().isBlank()) {
                parts.add(current.projectBackground().trim());
            }
            if (current.targetCustomerGroups() != null && !current.targetCustomerGroups().isBlank()) {
                parts.add(current.targetCustomerGroups().trim());
            }
            if (current.commercialValue() != null && !current.commercialValue().isBlank()) {
                parts.add(current.commercialValue().trim());
            }
        }
        for (ProjectChatMessageEntity item : messageMapper.listBySessionId(sessionId)) {
            if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            if (!"user".equalsIgnoreCase(item.getRole())) {
                continue;
            }
            parts.add(item.getContent().trim());
        }
        return String.join("\n", parts).trim();
    }

    private StructuredInfo toStructuredInfo(ProjectEntity project) {
        return new StructuredInfo(
                defaultString(project.getProjectName()),
                defaultString(project.getDescription()),
                defaultString(project.getProjectBackground()),
                defaultString(project.getSimilarProducts()),
                defaultString(project.getTargetCustomerGroups()),
                defaultString(project.getCommercialValue()),
                defaultString(project.getCoreProductValue()),
                defaultString(project.getBusinessKnowledgeSummary())
        );
    }

    private void syncSessionWithProjectSnapshot(ProjectChatSessionEntity session, ProjectEntity project) {
        StructuredInfo current = readStructuredInfo(session.getStructuredInfoJson());
        StructuredInfo merged = new StructuredInfo(
                choose(project.getProjectName(), current.projectName()),
                choose(project.getDescription(), current.description()),
                choose(project.getProjectBackground(), current.projectBackground()),
                choose(project.getSimilarProducts(), current.similarProducts()),
                choose(project.getTargetCustomerGroups(), current.targetCustomerGroups()),
                choose(project.getCommercialValue(), current.commercialValue()),
                choose(project.getCoreProductValue(), current.coreProductValue()),
                choose(project.getBusinessKnowledgeSummary(), current.businessKnowledgeSummary())
        );
        session.setProjectId(project.getId());
        session.setBusinessKnowledgeSummary(blankToNull(merged.businessKnowledgeSummary()));
        session.setStructuredInfoJson(writeStructuredInfo(merged));
        sessionMapper.update(session);
    }

    private ProjectChatSessionEntity requireProjectSession(Long projectId, Long sessionId) {
        projectService.getById(projectId);
        ProjectChatSessionEntity session;
        if (sessionId != null && sessionId > 0) {
            session = requireSession(sessionId);
            if (session.getProjectId() == null || !projectId.equals(session.getProjectId())) {
                throw new IllegalArgumentException("Project AI conversation does not belong to this project.");
            }
            return session;
        }
        session = sessionMapper.findLatestByProjectId(projectId);
        if (session == null) {
            throw new IllegalArgumentException("Project AI conversation not found.");
        }
        return session;
    }

    private String buildProjectSnapshotMessage(ProjectEntity project) {
        String context = projectService.buildProductContext(project);
        if (context.isBlank()) {
            return "";
        }
        return "Current project information:\n" + context;
    }

    private String summarizeKnowledge(String projectName,
                                      String description,
                                      String projectBackground,
                                      String targetCustomers,
                                      String commercialValue,
                                      String coreValue) {
        List<String> sections = new ArrayList<>();
        if (projectName != null && !projectName.isBlank()) {
            sections.add("项目名称：" + projectName.trim());
        }
        if (description != null && !description.isBlank()) {
            sections.add("项目概述：" + description.trim());
        }
        if (projectBackground != null && !projectBackground.isBlank()) {
            sections.add("项目背景：" + projectBackground.trim());
        }
        if (targetCustomers != null && !targetCustomers.isBlank()) {
            sections.add("目标客户群体：" + targetCustomers.trim());
        }
        if (commercialValue != null && !commercialValue.isBlank()) {
            sections.add("商业价值：" + commercialValue.trim());
        }
        if (coreValue != null && !coreValue.isBlank()) {
            sections.add("产品核心价值：" + coreValue.trim());
        }
        return String.join("\n", sections).trim();
    }

    private boolean isReadyToCreate(StructuredInfo info) {
        int score = 0;
        if (info.projectBackground() != null && !info.projectBackground().isBlank()) score++;
        if (info.targetCustomerGroups() != null && !info.targetCustomerGroups().isBlank()) score++;
        if (info.commercialValue() != null && !info.commercialValue().isBlank()) score++;
        if (info.coreProductValue() != null && !info.coreProductValue().isBlank()) score++;
        if (info.businessKnowledgeSummary() != null && !info.businessKnowledgeSummary().isBlank()) score++;
        return score >= 3;
    }

    private StructuredInfo readStructuredInfo(String json) {
        if (json == null || json.isBlank()) {
            return new StructuredInfo("", "", "", "", "", "", "", "");
        }
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new StructuredInfo(
                    defaultString(map.get("projectName")),
                    defaultString(map.get("description")),
                    defaultString(map.get("projectBackground")),
                    defaultString(map.get("similarProducts")),
                    defaultString(map.get("targetCustomerGroups")),
                    defaultString(map.get("commercialValue")),
                    defaultString(map.get("coreProductValue")),
                    defaultString(map.get("businessKnowledgeSummary"))
            );
        } catch (Exception e) {
            return new StructuredInfo("", "", "", "", "", "", "", "");
        }
    }

    private String writeStructuredInfo(StructuredInfo info) {
        try {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("projectName", defaultString(info.projectName()));
            map.put("description", defaultString(info.description()));
            map.put("projectBackground", defaultString(info.projectBackground()));
            map.put("similarProducts", defaultString(info.similarProducts()));
            map.put("targetCustomerGroups", defaultString(info.targetCustomerGroups()));
            map.put("commercialValue", defaultString(info.commercialValue()));
            map.put("coreProductValue", defaultString(info.coreProductValue()));
            map.put("businessKnowledgeSummary", defaultString(info.businessKnowledgeSummary()));
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize structured project info.", e);
        }
    }

    private ConversationTurnResult toTurnResult(ProjectChatSessionEntity session,
                                                String assistantMessage,
                                                List<String> followUpQuestions,
                                                StructuredInfo structuredInfo) {
        return new ConversationTurnResult(
                session.getId(),
                session.getJobId(),
                defaultString(assistantMessage),
                followUpQuestions == null ? List.of() : followUpQuestions,
                structuredInfo,
                Boolean.TRUE.equals(session.getReadyToCreate())
        );
    }

    private ProjectChatSessionEntity requireSession(Long sessionId) {
        ProjectChatSessionEntity session = sessionMapper.findById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Project AI conversation not found.");
        }
        return session;
    }

    private String normalizeMaterialType(String value) {
        if (value == null || value.isBlank()) {
            return "TEXT";
        }
        String normalized = value.trim().toUpperCase();
        return ("URL".equals(normalized) || "TEXT".equals(normalized) || "FILE".equals(normalized)) ? normalized : "TEXT";
    }

    private String choose(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim() : defaultString(fallback);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
