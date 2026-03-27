package com.tongtu.docgen.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongtu.docgen.api.DocGenController;
import com.tongtu.docgen.llm.AgentClient;
import com.tongtu.docgen.project.mapper.RequirementChatMessageMapper;
import com.tongtu.docgen.project.mapper.RequirementDocgenMapper;
import com.tongtu.docgen.project.model.entity.ProjectEntity;
import com.tongtu.docgen.project.model.entity.RequirementChatMessageEntity;
import com.tongtu.docgen.project.model.entity.RequirementChatSessionEntity;
import com.tongtu.docgen.project.model.entity.RequirementEntity;
import com.tongtu.docgen.project.model.entity.RequirementVersionEntity;
import com.tongtu.docgen.system.model.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class RequirementDocgenService {
    public record KnowledgePreviewView(
            String query,
            KnowledgeRetrievalService.RetrievalContext requirementKnowledge,
            KnowledgeRetrievalService.RetrievalContext projectKnowledge,
            String mergedContext
    ) {
    }

    private final DocGenControllerDelegate docGenDelegate;
    private final RequirementChatMessageMapper requirementChatMessageMapper;
    private final RequirementDocgenMapper requirementDocgenMapper;
    private final RequirementService requirementService;
    private final ProjectService projectService;
    private final AgentClient agentClient;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequirementDocgenService(DocGenControllerDelegate docGenDelegate,
                                    RequirementChatMessageMapper requirementChatMessageMapper,
                                    RequirementDocgenMapper requirementDocgenMapper,
                                    RequirementService requirementService,
                                    ProjectService projectService,
                                    AgentClient agentClient,
                                    KnowledgeRetrievalService knowledgeRetrievalService) {
        this.docGenDelegate = docGenDelegate;
        this.requirementChatMessageMapper = requirementChatMessageMapper;
        this.requirementDocgenMapper = requirementDocgenMapper;
        this.requirementService = requirementService;
        this.projectService = projectService;
        this.agentClient = agentClient;
        this.knowledgeRetrievalService = knowledgeRetrievalService;
    }

    @Transactional
    public DocGenController.CreateJobResponse createRequirementJob(String traceId,
                                                                   Long requirementId,
                                                                   String businessDescription,
                                                                   String previousPrdMarkdown,
                                                                   UserContext operator) {
        RequirementEntity requirement = requirementService.getById(requirementId);
        String baseDescription = resolveInitialBusinessDescription(requirement, businessDescription);
        DocGenController.CreateJobResponse resp = docGenDelegate.createJob(traceId, baseDescription, previousPrdMarkdown);

        RequirementChatSessionEntity session = new RequirementChatSessionEntity();
        session.setRequirementId(requirementId);
        session.setJobId(resp.jobId());
        session.setStatus(resp.status());
        session.setPendingQuestion(resp.pendingQuestion());
        session.setConfirmedItemsJson(toJson(resp.confirmedItems()));
        session.setUnconfirmedItemsJson(toJson(resp.unconfirmedItems()));
        session.setReadyToGenerate(resp.readyToGenerate());
        session.setCreatedBy(operator.getUserId());
        requirementDocgenMapper.insert(session);

        insertMessageIfPresent(session.getId(), "user", baseDescription);
        insertMessageIfPresent(session.getId(), "assistant", findLastAssistantMessage(resp.chatHistory()));
        return resp;
    }

    public DocGenController.CreateJobResponse getRequirementJob(String traceId, Long requirementId, String jobId) {
        RequirementChatSessionEntity session = ensureSession(requirementId, jobId);
        RequirementEntity requirement = requirementService.getById(requirementId);
        List<RequirementChatMessageEntity> messages = requirementChatMessageMapper.listBySessionId(session.getId());
        return buildCreateJobResponse(session, requirement, messages);
    }

    @Transactional
    public DocGenController.ChatResponse chat(String traceId, Long requirementId, String jobId, String message) {
        RequirementChatSessionEntity session = ensureSession(requirementId, jobId);
        RequirementEntity requirement = requirementService.getById(requirementId);

        insertMessageIfPresent(session.getId(), "user", message);
        List<RequirementChatMessageEntity> messages = requirementChatMessageMapper.listBySessionId(session.getId());
        AgentClient.ConversationTurn turn = docGenDelegate.chatWithHistory(
                traceId,
                resolveBusinessDescription(requirement, messages),
                toChatHistory(messages),
                session.getPendingQuestion(),
                resolveBasePrdMarkdown(requirement)
        );
        updateSessionFromChat(session, turn);
        insertMessageIfPresent(session.getId(), "assistant", turn.assistantMessage());

        List<RequirementChatMessageEntity> refreshedMessages = requirementChatMessageMapper.listBySessionId(session.getId());
        return buildChatResponse(session, turn, requirement, refreshedMessages, null);
    }

    @Transactional
    public DocGenController.ChatResponse generate(String traceId, Long requirementId, String jobId, UserContext operator) {
        RequirementChatSessionEntity session = ensureSession(requirementId, jobId);
        RequirementEntity requirement = requirementService.getById(requirementId);
        List<RequirementChatMessageEntity> messages = requirementChatMessageMapper.listBySessionId(session.getId());

        String prdMarkdown = docGenDelegate.generateWithHistory(
                traceId,
                resolveBusinessDescription(requirement, messages),
                toChatHistory(messages),
                resolveBasePrdMarkdown(requirement),
                fromJsonStringList(session.getUnconfirmedItemsJson())
        );

        String assistantMessage = "Generated PRD successfully. You can keep chatting to revise and generate a newer version.";
        insertMessageIfPresent(session.getId(), "assistant", assistantMessage);

        session.setStatus("COMPLETED");
        session.setPendingQuestion("");
        session.setReadyToGenerate(true);
        requirementDocgenMapper.update(session);

        String versionNo = requirementService.nextVersionNo(requirementId);
        requirementService.addVersion(
                requirementId,
                versionNo,
                prdMarkdown,
                "Generated from AI workbench",
                jobId,
                operator
        );
        projectService.mergeTagsByAi(
                requirement.getProjectId(),
                agentClient.suggestProjectTags(traceId, requirement.getTitle(), requirement.getSummary(), prdMarkdown),
                operator
        );

        RequirementEntity refreshedRequirement = requirementService.getById(requirementId);
        List<RequirementChatMessageEntity> refreshedMessages = requirementChatMessageMapper.listBySessionId(session.getId());
        return buildCompletedChatResponse(session, refreshedRequirement, refreshedMessages, prdMarkdown, assistantMessage);
    }

    public byte[] export(String traceId, Long requirementId, String jobId) {
        ensureSession(requirementId, jobId);
        RequirementVersionEntity version = findVersionByJobId(requirementId, jobId);
        if (version != null && version.getPrdMarkdown() != null && !version.getPrdMarkdown().isBlank()) {
            return version.getPrdMarkdown().getBytes(StandardCharsets.UTF_8);
        }
        return docGenDelegate.exportMarkdown(jobId);
    }

    public KnowledgePreviewView previewKnowledgeContext(String traceId, Long requirementId, String query) {
        RequirementEntity requirement = requirementService.getById(requirementId);
        String retrievalQuery = query == null || query.isBlank()
                ? buildRetrievalQuery(
                requirement,
                requirement.getTitle() + "\n\n" + (requirement.getSummary() == null ? "" : requirement.getSummary()),
                null
        )
                : query.trim();
        KnowledgeRetrievalService.RetrievalContext requirementKnowledge = knowledgeRetrievalService.searchRequirementKnowledge(
                traceId,
                requirementId,
                retrievalQuery,
                3
        );
        KnowledgeRetrievalService.RetrievalContext projectKnowledge = knowledgeRetrievalService.searchProjectKnowledge(
                traceId,
                requirement.getProjectId(),
                retrievalQuery,
                4
        );
        String mergedContext = appendKnowledgeContext(
                joinKnowledgeContexts(requirementKnowledge.contextText(), projectKnowledge.contextText()),
                ""
        );
        return new KnowledgePreviewView(retrievalQuery, requirementKnowledge, projectKnowledge, mergedContext);
    }

    private RequirementChatSessionEntity ensureSession(Long requirementId, String jobId) {
        requirementService.getById(requirementId);
        RequirementChatSessionEntity session = requirementDocgenMapper.findByRequirementIdAndJobId(requirementId, jobId);
        if (session == null) {
            throw new IllegalArgumentException("Requirement chat session not found.");
        }
        return session;
    }

    private DocGenController.CreateJobResponse buildCreateJobResponse(RequirementChatSessionEntity session,
                                                                      RequirementEntity requirement,
                                                                      List<RequirementChatMessageEntity> messages) {
        RequirementVersionEntity version = findVersionByJobId(requirement.getId(), session.getJobId());
        return new DocGenController.CreateJobResponse(
                session.getJobId(),
                session.getStatus(),
                List.of(),
                version == null ? null : version.getPrdMarkdown(),
                Boolean.TRUE.equals(session.getReadyToGenerate()),
                session.getPendingQuestion(),
                fromJsonStringList(session.getConfirmedItemsJson()),
                fromJsonStringList(session.getUnconfirmedItemsJson()),
                toChatHistory(messages),
                resolveBasePrdMarkdown(requirement),
                currentVersionCount(requirement.getId())
        );
    }

    private DocGenController.ChatResponse buildChatResponse(RequirementChatSessionEntity session,
                                                            AgentClient.ConversationTurn turn,
                                                            RequirementEntity requirement,
                                                            List<RequirementChatMessageEntity> messages,
                                                            String prdMarkdown) {
        List<String> confirmedItems = new ArrayList<>(turn.confirmedItems());
        List<String> unconfirmedItems = new ArrayList<>(turn.unconfirmedItems());
        boolean readyToGenerate = unconfirmedItems.isEmpty();
        String pendingQuestion = normalizePendingQuestion(turn.pendingQuestion(), unconfirmedItems);
        return new DocGenController.ChatResponse(
                session.getJobId(),
                readyToGenerate ? "READY" : "CLARIFYING",
                turn.assistantMessage(),
                readyToGenerate,
                pendingQuestion,
                confirmedItems,
                unconfirmedItems,
                toChatHistory(messages),
                prdMarkdown,
                resolveBasePrdMarkdown(requirement),
                currentVersionCount(requirement.getId())
        );
    }

    private DocGenController.ChatResponse buildCompletedChatResponse(RequirementChatSessionEntity session,
                                                                     RequirementEntity requirement,
                                                                     List<RequirementChatMessageEntity> messages,
                                                                     String prdMarkdown,
                                                                     String assistantMessage) {
        return new DocGenController.ChatResponse(
                session.getJobId(),
                session.getStatus(),
                assistantMessage,
                true,
                session.getPendingQuestion(),
                fromJsonStringList(session.getConfirmedItemsJson()),
                fromJsonStringList(session.getUnconfirmedItemsJson()),
                toChatHistory(messages),
                prdMarkdown,
                resolveBasePrdMarkdown(requirement),
                currentVersionCount(requirement.getId())
        );
    }

    private void updateSessionFromChat(RequirementChatSessionEntity session, AgentClient.ConversationTurn turn) {
        List<String> unconfirmedItems = turn.unconfirmedItems() == null ? List.of() : turn.unconfirmedItems();
        session.setStatus(unconfirmedItems.isEmpty() ? "READY" : "CLARIFYING");
        session.setPendingQuestion(normalizePendingQuestion(turn.pendingQuestion(), unconfirmedItems));
        session.setConfirmedItemsJson(toJson(turn.confirmedItems()));
        session.setUnconfirmedItemsJson(toJson(unconfirmedItems));
        session.setReadyToGenerate(unconfirmedItems.isEmpty());
        requirementDocgenMapper.update(session);
    }

    private List<DocGenController.ChatMessage> toChatHistory(List<RequirementChatMessageEntity> messages) {
        List<DocGenController.ChatMessage> history = new ArrayList<>();
        if (messages == null) {
            return history;
        }
        for (RequirementChatMessageEntity message : messages) {
            history.add(new DocGenController.ChatMessage(message.getRole(), message.getContent()));
        }
        return history;
    }

    private String resolveInitialBusinessDescription(RequirementEntity requirement, String businessDescription) {
        String requirementText = businessDescription != null && !businessDescription.isBlank()
                ? businessDescription
                : requirement.getTitle() + "\n\n" + (requirement.getSummary() == null ? "" : requirement.getSummary());
        ProjectEntity project = projectService.getById(requirement.getProjectId());
        String baseDescription = mergeProjectContext(projectService.buildProductContext(project), requirementText);
        return appendKnowledgeContext(
                buildKnowledgeContext("requirement-init-" + requirement.getId(), requirement, requirementText, null),
                baseDescription
        );
    }

    private String resolveBusinessDescription(RequirementEntity requirement, List<RequirementChatMessageEntity> messages) {
        ProjectEntity project = projectService.getById(requirement.getProjectId());
        String projectContext = projectService.buildProductContext(project);
        String latestUserMessage = "";
        if (messages != null) {
            for (RequirementChatMessageEntity message : messages) {
                if (message != null
                        && "user".equalsIgnoreCase(message.getRole())
                        && message.getContent() != null
                        && !message.getContent().isBlank()) {
                    latestUserMessage = message.getContent().trim();
                    break;
                }
            }
        }
        String requirementText = latestUserMessage.isBlank()
                ? requirement.getTitle() + "\n\n" + (requirement.getSummary() == null ? "" : requirement.getSummary())
                : latestUserMessage;
        String baseDescription = mergeProjectContext(projectContext, requirementText);
        return appendKnowledgeContext(
                buildKnowledgeContext("requirement-chat-" + requirement.getId(), requirement, requirementText, messages),
                baseDescription
        );
    }

    private String resolveBasePrdMarkdown(RequirementEntity requirement) {
        if (requirement.getCurrentVersionId() == null) {
            return null;
        }
        RequirementVersionEntity version = requirementService.getVersionById(requirement.getId(), requirement.getCurrentVersionId());
        return version == null ? null : version.getPrdMarkdown();
    }

    private RequirementVersionEntity findVersionByJobId(Long requirementId, String jobId) {
        for (RequirementVersionEntity version : requirementService.listVersions(requirementId)) {
            if (version != null && jobId.equals(version.getSourceJobId())) {
                return version;
            }
        }
        return null;
    }

    private int currentVersionCount(Long requirementId) {
        return requirementService.listVersions(requirementId).size();
    }

    private List<String> fromJsonStringList(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
        } catch (Exception e) {
            return List.of();
        }
    }

    private String normalizePendingQuestion(String pendingQuestion, List<String> unconfirmedItems) {
        String normalized = pendingQuestion == null ? "" : pendingQuestion.trim();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (unconfirmedItems == null || unconfirmedItems.isEmpty()) {
            return "";
        }
        return "璇峰厛琛ュ厖骞剁‘璁わ細" + unconfirmedItems.get(0);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String findLastAssistantMessage(List<DocGenController.ChatMessage> chatHistory) {
        if (chatHistory == null || chatHistory.isEmpty()) {
            return "";
        }
        for (int i = chatHistory.size() - 1; i >= 0; i--) {
            DocGenController.ChatMessage message = chatHistory.get(i);
            if (message != null && "assistant".equalsIgnoreCase(message.role())) {
                return message.content();
            }
        }
        return "";
    }

    private void insertMessageIfPresent(Long sessionId, String role, String content) {
        if (sessionId == null || role == null || role.isBlank() || content == null || content.isBlank()) {
            return;
        }
        RequirementChatMessageEntity entity = new RequirementChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setRole(role);
        entity.setContent(content.trim());
        entity.setSeqNo(requirementChatMessageMapper.findMaxSeqNo(sessionId) + 1);
        requirementChatMessageMapper.insert(entity);
    }

    private String mergeProjectContext(String projectContext, String requirementText) {
        String req = requirementText == null ? "" : requirementText.trim();
        String ctx = projectContext == null ? "" : projectContext.trim();
        if (req.contains("【当前需求】")) {
            return req;
        }
        if (ctx.isEmpty()) {
            return req;
        }
        if (req.isEmpty()) {
            return ctx;
        }
        return ctx + "\n\n【当前需求】\n" + req;
    }

    private String buildKnowledgeContext(String traceId,
                                         RequirementEntity requirement,
                                         String requirementText,
                                         List<RequirementChatMessageEntity> messages) {
        String retrievalQuery = buildRetrievalQuery(requirement, requirementText, messages);
        if (retrievalQuery.isBlank()) {
            return "";
        }

        String requirementKnowledge = knowledgeRetrievalService.searchRequirementKnowledge(
                traceId,
                requirement.getId(),
                retrievalQuery,
                3
        ).contextText();
        String projectKnowledge = knowledgeRetrievalService.searchProjectKnowledge(
                traceId,
                requirement.getProjectId(),
                retrievalQuery,
                4
        ).contextText();
        return joinKnowledgeContexts(requirementKnowledge, projectKnowledge);
    }

    private String buildRetrievalQuery(RequirementEntity requirement,
                                       String requirementText,
                                       List<RequirementChatMessageEntity> messages) {
        List<String> parts = new ArrayList<>();
        if (requirement != null) {
            if (requirement.getTitle() != null && !requirement.getTitle().isBlank()) {
                parts.add(requirement.getTitle().trim());
            }
            if (requirement.getSummary() != null && !requirement.getSummary().isBlank()) {
                parts.add(requirement.getSummary().trim());
            }
        }
        if (requirementText != null && !requirementText.isBlank()) {
            parts.add(requirementText.trim());
        }
        if (messages != null) {
            for (RequirementChatMessageEntity message : messages) {
                if (message == null || message.getContent() == null || message.getContent().isBlank()) {
                    continue;
                }
                if (!"user".equalsIgnoreCase(message.getRole())) {
                    continue;
                }
                parts.add(message.getContent().trim());
            }
        }
        return String.join("\n", parts).trim();
    }

    private String appendKnowledgeContext(String knowledgeContext, String baseDescription) {
        String base = baseDescription == null ? "" : baseDescription.trim();
        String knowledge = knowledgeContext == null ? "" : knowledgeContext.trim();
        if (knowledge.isEmpty()) {
            return base;
        }
        if (base.isEmpty()) {
            return "Knowledge Context:\n" + knowledge;
        }
        return base + "\n\nKnowledge Context:\n" + knowledge;
    }

    private String joinKnowledgeContexts(String requirementKnowledge, String projectKnowledge) {
        List<String> sections = new ArrayList<>();
        if (requirementKnowledge != null && !requirementKnowledge.isBlank()) {
            sections.add("Requirement Knowledge:\n" + requirementKnowledge.trim());
        }
        if (projectKnowledge != null && !projectKnowledge.isBlank()) {
            sections.add("Project Knowledge:\n" + projectKnowledge.trim());
        }
        return String.join("\n\n", sections).trim();
    }
}
