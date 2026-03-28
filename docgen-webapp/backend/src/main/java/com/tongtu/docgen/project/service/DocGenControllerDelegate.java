package com.tongtu.docgen.project.service;

import com.tongtu.docgen.api.DocGenController;
import com.tongtu.docgen.llm.AgentClient;
import com.tongtu.docgen.service.DocGenService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DocGenControllerDelegate {
    private final DocGenService docGenService;

    public DocGenControllerDelegate(DocGenService docGenService) {
        this.docGenService = docGenService;
    }

    public DocGenController.CreateJobResponse createJob(String traceId,
                                                        String businessDescription,
                                                        String previousPrdMarkdown,
                                                        String templateMarkdown) {
        return docGenService.createJob(traceId, businessDescription, previousPrdMarkdown, null, null, templateMarkdown);
    }

    public DocGenController.CreateJobResponse getJob(String traceId, String jobId) {
        return docGenService.getJob(traceId, jobId);
    }

    public DocGenController.ChatResponse chat(String traceId, String jobId, String message) {
        return docGenService.chat(traceId, jobId, message);
    }

    public AgentClient.ConversationTurn chatWithHistory(String traceId,
                                                        String businessDescription,
                                                        List<DocGenController.ChatMessage> history,
                                                        String pendingQuestion,
                                                        String basePrdMarkdown,
                                                        String templateMarkdown) {
        return docGenService.continueConversationWithHistory(
                traceId,
                businessDescription,
                history,
                pendingQuestion,
                basePrdMarkdown,
                templateMarkdown
        );
    }

    public DocGenController.ChatResponse generate(String traceId, String jobId) {
        return docGenService.generatePrdFromChat(traceId, jobId);
    }

    public String generateWithHistory(String traceId,
                                      String businessDescription,
                                      List<DocGenController.ChatMessage> history,
                                      String basePrdMarkdown,
                                      List<String> unconfirmedItems,
                                      String templateMarkdown) {
        return docGenService.generatePrdFromHistory(
                traceId,
                businessDescription,
                history,
                basePrdMarkdown,
                unconfirmedItems,
                templateMarkdown
        );
    }

    public String loadDefaultTemplate() {
        return docGenService.loadDefaultTemplate();
    }

    public byte[] exportMarkdown(String jobId) {
        String md = docGenService.getPrdMarkdownForExport(jobId);
        return md.getBytes(StandardCharsets.UTF_8);
    }
}
