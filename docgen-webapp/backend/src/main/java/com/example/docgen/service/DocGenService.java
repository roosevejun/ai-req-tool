package com.example.docgen.service;

import com.example.docgen.api.DocGenController;
import com.example.docgen.llm.AgentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocGenService {

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

    public DocGenController.CreateJobResponse createJob(String traceId, String businessDescription, String previousPrdMarkdown) {
        String jobId = UUID.randomUUID().toString();
        JobState s = new JobState(jobId, businessDescription, "PENDING");
        s.basePrdMarkdown = (previousPrdMarkdown == null || previousPrdMarkdown.isBlank()) ? null : previousPrdMarkdown;
        jobs.put(jobId, s);

        String template = loadPrdTemplate();
        AgentClient.ConversationTurn first = agentClient.startConversation(traceId, template, businessDescription, s.basePrdMarkdown);
        s.confirmedItems = new ArrayList<>(first.confirmedItems());
        s.unconfirmedItems = new ArrayList<>(first.unconfirmedItems());
        s.readyToGenerate = s.unconfirmedItems.isEmpty();
        s.status = s.readyToGenerate ? "READY" : "CLARIFYING";
        s.pendingQuestion = first.pendingQuestion();
        s.chatHistory.add(new ChatEntry("assistant", first.assistantMessage()));
        return toCreateJobResponse(s);
    }

    public DocGenController.CreateJobResponse submitClarifyAndMaybeGenerate(String traceId, String jobId, Map<String, String> answers) {
        JobState s = requireJob(jobId);
        if (!"CLARIFYING".equalsIgnoreCase(s.status)) {
            throw new IllegalArgumentException("当前任务不是澄清状态，无法提交答案。");
        }
        s.answers.putAll(answers == null ? Map.of() : answers);

        String template = loadPrdTemplate();
        String prdMarkdown = agentClient.generatePrd(traceId, template, s.businessDescription, s.clarifyQuestions, s.answers);

        s.status = "COMPLETED";
        s.prdMarkdown = prdMarkdown;
        return toCreateJobResponse(s);
    }

    public DocGenController.CreateJobResponse getJob(String traceId, String jobId) {
        JobState s = requireJob(jobId);
        return toCreateJobResponse(s);
    }

    public DocGenController.ChatResponse chat(String traceId, String jobId, String userMessage) {
        JobState s = requireJob(jobId);
        if ("COMPLETED".equalsIgnoreCase(s.status)) {
            return toChatResponse(s, "文档已生成完成。你可以查看结果，或新建任务继续讨论。");
        }
        s.chatHistory.add(new ChatEntry("user", userMessage));
        String template = loadPrdTemplate();
        AgentClient.ConversationTurn turn = agentClient.continueConversation(
                traceId,
                template,
                s.businessDescription,
                toClientHistory(s.chatHistory),
                s.pendingQuestion,
                s.basePrdMarkdown
        );
        s.chatHistory.add(new ChatEntry("assistant", turn.assistantMessage()));
        s.confirmedItems = new ArrayList<>(turn.confirmedItems());
        s.unconfirmedItems = new ArrayList<>(turn.unconfirmedItems());
        s.readyToGenerate = s.unconfirmedItems.isEmpty();
        s.pendingQuestion = turn.pendingQuestion();
        s.status = s.readyToGenerate ? "READY" : "CLARIFYING";
        return toChatResponse(s, turn.assistantMessage());
    }

    public DocGenController.ChatResponse generatePrdFromChat(String traceId, String jobId) {
        JobState s = requireJob(jobId);
        if (!s.unconfirmedItems.isEmpty()) {
            String guidance = buildUnconfirmedGuidancePrompt(s.unconfirmedItems);
            throw new IllegalArgumentException(
                    "仍存在未确认项，禁止生成PRD。请先补齐： "
                            + String.join("；", s.unconfirmedItems)
                            + "\n\n你可以直接按下面模板回复给AI：\n"
                            + guidance
            );
        }
        String template = loadPrdTemplate();
        String prdMarkdown = agentClient.generatePrdFromChat(
                traceId,
                template,
                s.businessDescription,
                toClientHistory(s.chatHistory),
                s.basePrdMarkdown
        );
        s.prdMarkdown = prdMarkdown;
        s.status = "COMPLETED";
        return toChatResponse(s, "已根据确认后的需求生成 PRD 文档。");
    }

    public String getPrdMarkdownForExport(String jobId) {
        JobState s = requireJob(jobId);
        if (s.prdMarkdown == null || s.prdMarkdown.isBlank()) {
            throw new IllegalArgumentException("当前任务还没有生成PRD，无法导出。");
        }
        return s.prdMarkdown;
    }

    private JobState requireJob(String jobId) {
        JobState s = jobs.get(jobId);
        if (s == null) {
            throw new IllegalArgumentException("jobId 不存在：" + jobId);
        }
        return s;
    }

    private DocGenController.CreateJobResponse toCreateJobResponse(JobState s) {
        Object clarify = s.clarifyQuestions;
        String md = s.prdMarkdown;
        return new DocGenController.CreateJobResponse(s.jobId, s.status, clarify, md);
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
                s.basePrdMarkdown
        );
    }

    private List<AgentClient.ChatMessage> toClientHistory(List<ChatEntry> entries) {
        List<AgentClient.ChatMessage> out = new ArrayList<>();
        for (ChatEntry e : entries) {
            out.add(new AgentClient.ChatMessage(e.role, e.content));
        }
        return out;
    }

    private String buildUnconfirmedGuidancePrompt(List<String> unconfirmedItems) {
        StringBuilder sb = new StringBuilder();
        for (String item : unconfirmedItems) {
            if (item.contains("用户与角色")) {
                sb.append("【用户与角色】\n")
                  .append("- 角色1：\n")
                  .append("  - 职责：\n")
                  .append("  - 关键操作：\n")
                  .append("- 角色2：\n")
                  .append("  - 职责：\n")
                  .append("  - 关键操作：\n\n");
            } else if (item.contains("核心业务流程")) {
                sb.append("【核心业务流程】\n")
                  .append("- 步骤1：输入来源是...\n")
                  .append("- 步骤2：系统处理...\n")
                  .append("- 步骤3：输出结果...\n\n");
            } else if (item.contains("输入输出与边界")) {
                sb.append("【输入输出与边界】\n")
                  .append("- 输入：字段A/字段B（必填/可选）\n")
                  .append("- 输出：页面展示/文件导出/API返回\n")
                  .append("- 边界：不处理...\n\n");
            } else if (item.contains("验收标准")) {
                sb.append("【验收标准】\n")
                  .append("- 功能验收：\n")
                  .append("- 权限/安全验收：\n")
                  .append("- 性能验收（如p95）：\n\n");
            } else {
                sb.append("【").append(item).append("】\n")
                  .append("- 请补充具体内容...\n\n");
            }
        }
        return sb.toString().trim();
    }

    private String loadPrdTemplate() {
        Path dir = Paths.get(templateDir);
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("找不到模板目录： " + dir.toAbsolutePath());
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, templateGlob)) {
            for (Path p : ds) {
                // 取第一个匹配项即可（试用阶段）。
                return Files.readString(p, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取模板失败：" + e.getMessage(), e);
        }
        throw new IllegalArgumentException("模板未找到：目录=" + dir.toAbsolutePath() + " glob=" + templateGlob);
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

        JobState(String jobId, String businessDescription, String status) {
            this.jobId = jobId;
            this.businessDescription = businessDescription;
            this.status = status;
        }
    }

    public record ClarifyQuestion(String id, String question, String whyNeeded, boolean required) {}
    private record ChatEntry(String role, String content) {}
}

