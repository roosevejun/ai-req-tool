package com.tongtu.docgen.service;

import com.tongtu.docgen.api.DocGenController;
import com.tongtu.docgen.llm.AgentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocGenService {
    private static final ZoneId PRD_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter PRD_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter PRD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/M/d");
    private static final Pattern VERSION_LINE_PATTERN = Pattern.compile("(?m)^(\\*\\*文档版本\\*\\*\\s*[：:]\\s*)(.+)$");
    private static final Pattern DATE_LINE_PATTERN = Pattern.compile("(?m)^(\\*\\*日期\\*\\*\\s*[：:]\\s*)(.+)$");
    private static final Pattern PROJECT_LINE_PATTERN = Pattern.compile("(?m)^\\*\\*项目名称\\*\\*.*$");
    private static final Pattern VERSION_TOKEN_PATTERN = Pattern.compile("(?i)V?\\s*(\\d+(?:\\.\\d+)*)");

    private final AgentClient agentClient;

    // In-memory store for trial. If you need persistence, switch to DB later.
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    @Value("${agent.template.templateDir}")
    private String templateDir;

    @Value("${agent.template.templateGlob}")
    private String templateGlob;

    public DocGenService(AgentClient agentClient) {
        this.agentClient = agentClient;
    }

    public DocGenController.CreateJobResponse createJob(String traceId,
                                                        String businessDescription,
                                                        String previousPrdMarkdown,
                                                        Long templateId,
                                                        Long templateVersionId,
                                                        String templateMarkdown) {
        String jobId = UUID.randomUUID().toString();
        JobState s = new JobState(jobId, businessDescription, "PENDING");
        s.basePrdMarkdown = isBlank(previousPrdMarkdown) ? null : previousPrdMarkdown;
        s.templateId = templateId;
        s.templateVersionId = templateVersionId;
        s.templateMarkdown = isBlank(templateMarkdown) ? null : templateMarkdown;
        jobs.put(jobId, s);

        String template = resolveTemplate(s.templateMarkdown);
        AgentClient.ConversationTurn first = agentClient.startConversation(traceId, template, businessDescription, s.basePrdMarkdown);
        s.confirmedItems = new ArrayList<>(first.confirmedItems());
        s.unconfirmedItems = new ArrayList<>(first.unconfirmedItems());
        s.readyToGenerate = s.unconfirmedItems.isEmpty();
        s.status = s.readyToGenerate ? "READY" : "CLARIFYING";
        s.pendingQuestion = normalizePendingQuestion(first.pendingQuestion(), s.unconfirmedItems);
        addAssistantMessage(s, first.assistantMessage());
        return toCreateJobResponse(s);
    }

    public DocGenController.CreateJobResponse submitClarifyAndMaybeGenerate(String traceId, String jobId, Map<String, String> answers) {
        JobState s = requireJob(jobId);
        if (!"CLARIFYING".equalsIgnoreCase(s.status) && !"READY".equalsIgnoreCase(s.status)) {
            throw new IllegalArgumentException("Current task status does not allow clarify submission.");
        }
        if (answers != null) {
            s.answers.putAll(answers);
        }

        String template = resolveTemplate(s.templateMarkdown);
        String prdMarkdown = agentClient.generatePrd(traceId, template, s.businessDescription, s.clarifyQuestions, s.answers);
        commitGeneratedPrd(s, prdMarkdown);
        String msg = "Generated PRD successfully. You can keep chatting to revise a new version.";
        addAssistantMessage(s, msg);
        return toCreateJobResponse(s);
    }

    public DocGenController.CreateJobResponse getJob(String traceId, String jobId) {
        return toCreateJobResponse(requireJob(jobId));
    }

    public DocGenController.ChatResponse chat(String traceId, String jobId, String userMessage) {
        JobState s = requireJob(jobId);
        if (isBlank(userMessage)) {
            throw new IllegalArgumentException("message cannot be blank.");
        }

        s.chatHistory.add(new ChatEntry("user", userMessage));

        // Iterative cycle support: use latest generated PRD as base context if present.
        String basePrdForContext = !isBlank(s.prdMarkdown) ? s.prdMarkdown : s.basePrdMarkdown;

        String template = resolveTemplate(s.templateMarkdown);
        AgentClient.ConversationTurn turn = agentClient.continueConversation(
                traceId,
                template,
                s.businessDescription,
                toClientHistory(s.chatHistory),
                s.pendingQuestion,
                basePrdForContext
        );

        addAssistantMessage(s, turn.assistantMessage());
        s.confirmedItems = new ArrayList<>(turn.confirmedItems());
        s.unconfirmedItems = new ArrayList<>(turn.unconfirmedItems());
        s.readyToGenerate = s.unconfirmedItems.isEmpty();
        s.pendingQuestion = normalizePendingQuestion(turn.pendingQuestion(), s.unconfirmedItems);
        s.status = s.readyToGenerate ? "READY" : "CLARIFYING";

        return toChatResponse(s, turn.assistantMessage());
    }

    public AgentClient.ConversationTurn continueConversationWithHistory(String traceId,
                                                                        String businessDescription,
                                                                        List<DocGenController.ChatMessage> history,
                                                                        String pendingQuestion,
                                                                        String basePrdMarkdown,
                                                                        String templateMarkdown) {
        if (isBlank(businessDescription)) {
            throw new IllegalArgumentException("businessDescription cannot be blank.");
        }
        String template = resolveTemplate(templateMarkdown);
        return agentClient.continueConversation(
                traceId,
                template,
                businessDescription,
                toClientHistoryFromController(history),
                pendingQuestion,
                basePrdMarkdown
        );
    }

    public DocGenController.ChatResponse generatePrdFromChat(String traceId, String jobId) {
        JobState s = requireJob(jobId);
        if (!s.unconfirmedItems.isEmpty()) {
            String guidance = buildUnconfirmedGuidancePrompt(s.unconfirmedItems);
            throw new IllegalArgumentException(
                    "Still has unconfirmed items. Please complete them before generating PRD: "
                            + String.join("; ", s.unconfirmedItems)
                            + "\n\nSuggested reply template:\n"
                            + guidance
            );
        }

        String template = resolveTemplate(s.templateMarkdown);
        String basePrdForContext = !isBlank(s.prdMarkdown) ? s.prdMarkdown : s.basePrdMarkdown;
        String prdMarkdown = agentClient.generatePrdFromChat(
                traceId,
                template,
                s.businessDescription,
                toClientHistory(s.chatHistory),
                basePrdForContext
        );

        commitGeneratedPrd(s, prdMarkdown);
        String doneMsg = "Generated PRD successfully. You can keep chatting to revise and generate a newer version.";
        addAssistantMessage(s, doneMsg);
        return toChatResponse(s, doneMsg);
    }

    public String generatePrdFromHistory(String traceId,
                                         String businessDescription,
                                         List<DocGenController.ChatMessage> history,
                                         String basePrdMarkdown,
                                         List<String> unconfirmedItems,
                                         String templateMarkdown) {
        if (unconfirmedItems != null && !unconfirmedItems.isEmpty()) {
            String guidance = buildUnconfirmedGuidancePrompt(unconfirmedItems);
            throw new IllegalArgumentException(
                    "Still has unconfirmed items. Please complete them before generating PRD: "
                            + String.join("; ", unconfirmedItems)
                            + "\n\nSuggested reply template:\n"
                            + guidance
            );
        }
        if (isBlank(businessDescription)) {
            throw new IllegalArgumentException("businessDescription cannot be blank.");
        }

        String template = resolveTemplate(templateMarkdown);
        String prdMarkdown = agentClient.generatePrdFromChat(
                traceId,
                template,
                businessDescription,
                toClientHistoryFromController(history),
                basePrdMarkdown
        );
        return normalizePrdDocument(prdMarkdown, basePrdMarkdown);
    }

    public String getPrdMarkdownForExport(String jobId) {
        JobState s = requireJob(jobId);
        if (isBlank(s.prdMarkdown)) {
            throw new IllegalArgumentException("Current task has no generated PRD yet.");
        }
        return s.prdMarkdown;
    }

    public String getExportFileName(String jobId) {
        JobState s = requireJob(jobId);
        int version = Math.max(1, s.currentVersion);
        return "01-PRD-Agent需求文档-v" + version + ".md";
    }

    private void commitGeneratedPrd(JobState s, String prdMarkdown) {
        s.prdMarkdown = normalizePrdDocument(prdMarkdown, s.basePrdMarkdown);
        s.status = "COMPLETED";
        s.readyToGenerate = true;
        s.pendingQuestion = "";
        s.generatedPrdHistory.add(prdMarkdown);
        s.currentVersion = s.generatedPrdHistory.size();
        // The latest PRD becomes baseline for the next revision cycle.
        s.basePrdMarkdown = prdMarkdown;
    }

    private JobState requireJob(String jobId) {
        JobState s = jobs.get(jobId);
        if (s == null) {
            throw new IllegalArgumentException("jobId not found: " + jobId);
        }
        return s;
    }

    private DocGenController.CreateJobResponse toCreateJobResponse(JobState s) {
        List<DocGenController.ChatMessage> history = new ArrayList<>();
        for (ChatEntry e : s.chatHistory) {
            history.add(new DocGenController.ChatMessage(e.role, e.content));
        }
        return new DocGenController.CreateJobResponse(
                s.jobId,
                s.status,
                s.clarifyQuestions,
                s.prdMarkdown,
                s.readyToGenerate,
                s.pendingQuestion,
                s.confirmedItems,
                s.unconfirmedItems,
                history,
                s.basePrdMarkdown,
                s.currentVersion,
                new DocGenController.TemplateSelection(s.templateId, s.templateVersionId, s.templateVersionLabel)
        );
    }

    private DocGenController.ChatResponse toChatResponse(JobState s, String assistantMessage) {
        List<DocGenController.ChatMessage> history = new ArrayList<>();
        for (ChatEntry e : s.chatHistory) {
            history.add(new DocGenController.ChatMessage(e.role, e.content));
        }
        return new DocGenController.ChatResponse(
                s.jobId,
                s.status,
                assistantMessage,
                s.readyToGenerate,
                s.pendingQuestion,
                s.confirmedItems,
                s.unconfirmedItems,
                history,
                s.prdMarkdown,
                s.basePrdMarkdown,
                s.currentVersion,
                new DocGenController.TemplateSelection(s.templateId, s.templateVersionId, s.templateVersionLabel)
        );
    }

    private List<AgentClient.ChatMessage> toClientHistory(List<ChatEntry> entries) {
        List<AgentClient.ChatMessage> out = new ArrayList<>();
        for (ChatEntry e : entries) {
            out.add(new AgentClient.ChatMessage(e.role, e.content));
        }
        return out;
    }

    private List<AgentClient.ChatMessage> toClientHistoryFromController(List<DocGenController.ChatMessage> entries) {
        List<AgentClient.ChatMessage> out = new ArrayList<>();
        if (entries == null) {
            return out;
        }
        for (DocGenController.ChatMessage entry : entries) {
            if (entry == null) {
                continue;
            }
            out.add(new AgentClient.ChatMessage(entry.role(), entry.content()));
        }
        return out;
    }

    private void addAssistantMessage(JobState s, String message) {
        if (!isBlank(message)) {
            s.chatHistory.add(new ChatEntry("assistant", message));
        }
    }

    private String buildUnconfirmedGuidancePrompt(List<String> unconfirmedItems) {
        StringBuilder sb = new StringBuilder();
        for (String item : unconfirmedItems) {
            String lower = item == null ? "" : item.toLowerCase(Locale.ROOT);
            if (lower.contains("user") || lower.contains("role")) {
                sb.append("[User and Roles]\n")
                        .append("- Role 1:\n")
                        .append("  - Responsibilities:\n")
                        .append("  - Key operations:\n")
                        .append("- Role 2:\n")
                        .append("  - Responsibilities:\n")
                        .append("  - Key operations:\n\n");
            } else if (lower.contains("flow") || lower.contains("process")) {
                sb.append("[Core Flow]\n")
                        .append("- Step 1: input source is ...\n")
                        .append("- Step 2: system handles ...\n")
                        .append("- Step 3: output becomes ...\n\n");
            } else if (lower.contains("input") || lower.contains("output") || lower.contains("boundary")) {
                sb.append("[Input/Output and Boundary]\n")
                        .append("- Input: fieldA/fieldB (required/optional)\n")
                        .append("- Output: page view/export/API response\n")
                        .append("- Boundary: out of scope is ...\n\n");
            } else if (lower.contains("acceptance") || lower.contains("sla")) {
                sb.append("[Acceptance Criteria]\n")
                        .append("- Functional acceptance:\n")
                        .append("- Security/permission acceptance:\n")
                        .append("- Performance acceptance (e.g., p95):\n\n");
            } else {
                sb.append("[").append(item).append("]\n")
                        .append("- Please provide concrete details ...\n\n");
            }
        }
        return sb.toString().trim();
    }

    public String loadDefaultTemplate() {
        Path dir = Paths.get(templateDir);
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Template directory not found: " + dir.toAbsolutePath());
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, templateGlob)) {
            for (Path p : ds) {
                // Trial mode: use the first matched template file.
                return Files.readString(p, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template: " + e.getMessage(), e);
        }
        throw new IllegalArgumentException("Template not found: dir=" + dir.toAbsolutePath() + " glob=" + templateGlob);
    }

    private String resolveTemplate(String templateMarkdown) {
        if (!isBlank(templateMarkdown)) {
            return templateMarkdown;
        }
        return loadDefaultTemplate();
    }

    private boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    private String defaultString(String v) {
        return v == null ? "" : v;
    }

    private String normalizeGeneratedAt(String markdown) {
        String text = defaultString(markdown);
        String generatedAt = ZonedDateTime.now(PRD_ZONE).format(PRD_TIME_FORMATTER) + " (" + PRD_ZONE + ")";
        String marker = "> 生成时间：";
        if (text.startsWith(marker)) {
            int newline = text.indexOf('\n');
            if (newline >= 0) {
                return marker + generatedAt + text.substring(newline);
            }
            return marker + generatedAt;
        }
        return marker + generatedAt + "\n\n" + text;
    }

    private String normalizePrdDocument(String markdown, String basePrdMarkdown) {
        String text = normalizeGeneratedAt(markdown);
        String nextVersion = deriveNextDocVersion(basePrdMarkdown, text);
        String currentDate = ZonedDateTime.now(PRD_ZONE).format(PRD_DATE_FORMATTER);

        text = upsertMetadataLine(text, VERSION_LINE_PATTERN, "**文档版本**：" + nextVersion, PROJECT_LINE_PATTERN);
        text = upsertMetadataLine(text, DATE_LINE_PATTERN, "**日期**：" + currentDate, VERSION_LINE_PATTERN);
        return text;
    }

    private String upsertMetadataLine(String text, Pattern linePattern, String newLine, Pattern anchorPattern) {
        Matcher lineMatcher = linePattern.matcher(text);
        if (lineMatcher.find()) {
            return lineMatcher.replaceFirst(Matcher.quoteReplacement(newLine));
        }

        if (anchorPattern != null) {
            Matcher anchorMatcher = anchorPattern.matcher(text);
            if (anchorMatcher.find()) {
                int insertPos = anchorMatcher.end();
                return text.substring(0, insertPos) + "\n" + newLine + text.substring(insertPos);
            }
        }

        return newLine + "\n" + text;
    }

    private String deriveNextDocVersion(String basePrdMarkdown, String generatedMarkdown) {
        String current = extractDocVersion(basePrdMarkdown);
        if (isBlank(current)) {
            current = extractDocVersion(generatedMarkdown);
        }
        if (isBlank(current)) {
            return "V1.0";
        }

        List<Integer> segments = parseVersionSegments(current);
        if (segments.isEmpty()) {
            return "V1.0";
        }
        if (segments.size() == 1) {
            segments.add(1);
        } else if (segments.size() == 2) {
            segments.add(1);
        } else {
            int last = segments.get(segments.size() - 1);
            segments.set(segments.size() - 1, last + 1);
        }
        return "V" + joinSegments(segments);
    }

    private String extractDocVersion(String markdown) {
        if (isBlank(markdown)) return "";
        Matcher lineMatcher = VERSION_LINE_PATTERN.matcher(markdown);
        if (lineMatcher.find()) {
            String raw = defaultString(lineMatcher.group(2));
            Matcher tokenMatcher = VERSION_TOKEN_PATTERN.matcher(raw);
            if (tokenMatcher.find()) {
                return tokenMatcher.group(1);
            }
        }
        return "";
    }

    private List<Integer> parseVersionSegments(String version) {
        List<Integer> out = new ArrayList<>();
        String[] parts = defaultString(version).trim().split("\\.");
        for (String p : parts) {
            if (p.isBlank()) return List.of();
            try {
                out.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException ex) {
                return List.of();
            }
        }
        return out;
    }

    private String joinSegments(List<Integer> segments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) sb.append('.');
            sb.append(segments.get(i));
        }
        return sb.toString();
    }

    private String normalizePendingQuestion(String pendingQuestion, List<String> unconfirmedItems) {
        String normalized = defaultString(pendingQuestion).trim();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (unconfirmedItems == null || unconfirmedItems.isEmpty()) {
            return "";
        }
        String first = unconfirmedItems.get(0);
        return "请先补充并确认：" + first;
    }

    private static class JobState {
        final String jobId;
        final String businessDescription;
        volatile String status;
        volatile List<ClarifyQuestion> clarifyQuestions = List.of();
        final Map<String, String> answers = new HashMap<>();
        final List<ChatEntry> chatHistory = new ArrayList<>();
        volatile boolean readyToGenerate = false;
        volatile String pendingQuestion = "";
        volatile List<String> confirmedItems = new ArrayList<>();
        volatile List<String> unconfirmedItems = new ArrayList<>();
        volatile String prdMarkdown;
        volatile String basePrdMarkdown;
        volatile Long templateId;
        volatile Long templateVersionId;
        volatile String templateVersionLabel;
        volatile String templateMarkdown;
        volatile int currentVersion = 0;
        final List<String> generatedPrdHistory = new ArrayList<>();

        JobState(String jobId, String businessDescription, String status) {
            this.jobId = jobId;
            this.businessDescription = businessDescription;
            this.status = status;
        }
    }

    public record ClarifyQuestion(String id, String question, String whyNeeded, boolean required) {}

    private record ChatEntry(String role, String content) {}
}

