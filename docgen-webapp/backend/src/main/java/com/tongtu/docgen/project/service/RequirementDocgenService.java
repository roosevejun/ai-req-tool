package com.tongtu.docgen.project.service;

import com.tongtu.docgen.api.DocGenController;
import com.tongtu.docgen.project.mapper.RequirementDocgenMapper;
import com.tongtu.docgen.project.model.entity.RequirementChatSessionEntity;
import com.tongtu.docgen.project.model.entity.RequirementEntity;
import com.tongtu.docgen.system.model.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequirementDocgenService {
    private final DocGenControllerDelegate docGenDelegate;
    private final RequirementDocgenMapper requirementDocgenMapper;
    private final RequirementService requirementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RequirementDocgenService(DocGenControllerDelegate docGenDelegate,
                                    RequirementDocgenMapper requirementDocgenMapper,
                                    RequirementService requirementService) {
        this.docGenDelegate = docGenDelegate;
        this.requirementDocgenMapper = requirementDocgenMapper;
        this.requirementService = requirementService;
    }

    @Transactional
    public DocGenController.CreateJobResponse createRequirementJob(String traceId, Long requirementId, String businessDescription, String previousPrdMarkdown, UserContext operator) {
        RequirementEntity req = requirementService.getById(requirementId);
        String base = (businessDescription == null || businessDescription.isBlank())
                ? req.getTitle() + "\n\n" + (req.getSummary() == null ? "" : req.getSummary())
                : businessDescription;
        DocGenController.CreateJobResponse resp = docGenDelegate.createJob(traceId, base, previousPrdMarkdown);
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
        return resp;
    }

    public DocGenController.CreateJobResponse getRequirementJob(String traceId, Long requirementId, String jobId) {
        ensureSession(requirementId, jobId);
        return docGenDelegate.getJob(traceId, jobId);
    }

    public DocGenController.ChatResponse chat(String traceId, Long requirementId, String jobId, String message) {
        RequirementChatSessionEntity session = ensureSession(requirementId, jobId);
        DocGenController.ChatResponse resp = docGenDelegate.chat(traceId, jobId, message);
        updateSessionFromChat(session, resp);
        return resp;
    }

    @Transactional
    public DocGenController.ChatResponse generate(String traceId, Long requirementId, String jobId, UserContext operator) {
        RequirementChatSessionEntity session = ensureSession(requirementId, jobId);
        DocGenController.ChatResponse resp = docGenDelegate.generate(traceId, jobId);
        updateSessionFromChat(session, resp);
        String versionNo = requirementService.nextVersionNo(requirementId);
        requirementService.addVersion(requirementId, versionNo, resp.prdMarkdown(), "Generated from AI workbench", jobId, operator);
        return resp;
    }

    public byte[] export(String traceId, Long requirementId, String jobId) {
        ensureSession(requirementId, jobId);
        return docGenDelegate.exportMarkdown(jobId);
    }

    private RequirementChatSessionEntity ensureSession(Long requirementId, String jobId) {
        requirementService.getById(requirementId);
        RequirementChatSessionEntity session = requirementDocgenMapper.findByRequirementIdAndJobId(requirementId, jobId);
        if (session == null) {
            throw new IllegalArgumentException("Requirement chat session not found.");
        }
        return session;
    }

    private void updateSessionFromChat(RequirementChatSessionEntity session, DocGenController.ChatResponse resp) {
        session.setStatus(resp.status());
        session.setPendingQuestion(resp.pendingQuestion());
        session.setConfirmedItemsJson(toJson(resp.confirmedItems()));
        session.setUnconfirmedItemsJson(toJson(resp.unconfirmedItems()));
        session.setReadyToGenerate(resp.readyToGenerate());
        requirementDocgenMapper.update(session);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}


