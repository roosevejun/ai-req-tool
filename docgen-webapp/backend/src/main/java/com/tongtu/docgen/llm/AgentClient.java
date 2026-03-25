package com.tongtu.docgen.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tongtu.docgen.service.DocGenService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AgentClient {
    private static final Logger log = LoggerFactory.getLogger(AgentClient.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${agent.llm.openai.baseUrl:https://api.openai.com/v1}")
    private String openaiBaseUrl;
    @Value("${agent.llm.openai.model:gpt-4o-mini}")
    private String openaiModel;
    @Value("${agent.llm.openai.apiKey:}")
    private String openaiApiKey;
    @Value("${agent.llm.openai.timeoutMs:30000}")
    private int openaiTimeoutMs;
    @Value("${agent.llm.mockOnly:false}")
    private boolean mockOnly;

    @PostConstruct
    public void logRuntimeConfig() {
        ApiKeyResolution key = resolveApiKey();
        log.info("LLM config loaded: baseUrl={}, model={}, mockOnly={}, apiKeySource={}, apiKeyMask={}",
                openaiBaseUrl, openaiModel, mockOnly, key.source(), maskKey(key.key()));
    }

    public record ChatMessage(String role, String content) {}

    public record ConversationTurn(
            String assistantMessage,
            List<String> confirmedItems,
            List<String> unconfirmedItems,
            String pendingQuestion
    ) {}

    public List<DocGenService.ClarifyQuestion> generateClarifyQuestions(String traceId, String prdTemplate, String businessDescription) {
        return List.of(
                new DocGenService.ClarifyQuestion("q1", "核心业务目标是什么？", "例如：想让系统最终解决什么问题。", true),
                new DocGenService.ClarifyQuestion("q2", "关键功能点有哪些？", "例如：实时展示、地图交互、历史回放等。", true),
                new DocGenService.ClarifyQuestion("q3", "验收标准或性能要求是什么？", "例如：刷新频率、响应时间、准确率。", true)
        );
    }

    public String generatePrd(String traceId, String prdTemplate, String businessDescription,
                              List<DocGenService.ClarifyQuestion> clarifyQuestions,
                              Map<String, String> answers) {
        List<ChatMessage> syntheticHistory = new ArrayList<>();
        syntheticHistory.add(new ChatMessage("assistant", "Requirement clarification questions"));
        syntheticHistory.add(new ChatMessage("user", buildClarifyAnswerContext(clarifyQuestions, answers)));

        if (mockOnly || getApiKey().isEmpty()) {
            return mockGeneratePrdFromChat(businessDescription, syntheticHistory);
        }

        String systemPrompt = """
You are a senior product analyst.
Generate one complete PRD markdown from business description and clarified answers.
Requirements:
1) Must reflect concrete user inputs. Do not output generic template-only content.
2) If user gave domain-specific details (e.g. ThingsBoard, map display, history replay), include them explicitly.
3) Keep sections complete and internally consistent.
4) Return strict JSON only: {"prdMarkdown":"..."}
""";
        String userPrompt = "Template:\n" + prdTemplate
                + "\n\nBusiness Description:\n" + businessDescription
                + "\n\nClarification Q&A:\n" + buildClarifyAnswerContext(clarifyQuestions, answers)
                + "\n\nConversation History JSON:\n" + objectToJsonSafe(syntheticHistory);
        return parsePrdOutput(callOpenAiForText(systemPrompt, userPrompt));
    }

    public ConversationTurn startConversation(String traceId, String prdTemplate, String businessDescription, String basePrdMarkdown) {
        if (mockOnly || getApiKey().isEmpty()) {
            return mockStartConversation(basePrdMarkdown);
        }
        String systemPrompt = """
你是需求澄清助手。请根据用户业务描述输出严格 JSON：
{
  "assistantMessage": "...",
  "confirmedItems": ["..."],
  "unconfirmedItems": ["..."],
  "pendingQuestion": "..."
}
要求：如果信息不足，必须给出下一条最关键的问题。
""";
        String userPrompt = "模板:\n" + prdTemplate
                + "\n\n基线PRD:\n" + (basePrdMarkdown == null ? "" : basePrdMarkdown)
                + "\n\n业务描述:\n" + businessDescription;
        return parseConversationTurn(callOpenAiForText(systemPrompt, userPrompt));
    }

    public ConversationTurn continueConversation(String traceId, String prdTemplate, String businessDescription,
                                                 List<ChatMessage> history, String pendingQuestion, String basePrdMarkdown) {
        if (mockOnly || getApiKey().isEmpty()) {
            return mockContinueConversation(history, basePrdMarkdown);
        }
        String systemPrompt = """
你是需求澄清助手。请基于已有对话持续更新 confirmedItems / unconfirmedItems，输出严格 JSON：
{
  "assistantMessage": "...",
  "confirmedItems": ["..."],
  "unconfirmedItems": ["..."],
  "pendingQuestion": "..."
}
""";
        String userPrompt = "模板:\n" + prdTemplate
                + "\n\n基线PRD:\n" + (basePrdMarkdown == null ? "" : basePrdMarkdown)
                + "\n\n业务描述:\n" + businessDescription
                + "\n\n历史对话JSON:\n" + objectToJsonSafe(history)
                + "\n\n当前待确认问题:\n" + (pendingQuestion == null ? "" : pendingQuestion);
        return parseConversationTurn(callOpenAiForText(systemPrompt, userPrompt));
    }

    public String generatePrdFromChat(String traceId, String prdTemplate, String businessDescription,
                                      List<ChatMessage> history, String basePrdMarkdown) {
        if (mockOnly || getApiKey().isEmpty()) {
            return mockGeneratePrdFromChat(businessDescription, history);
        }
        String systemPrompt = """
你是资深产品经理。请基于业务描述和完整对话，生成最终 PRD。
必须体现用户已确认的具体内容，避免泛化模板输出。
返回严格 JSON：{"prdMarkdown":"..."}
""";
        String userPrompt = "模板:\n" + prdTemplate
                + "\n\n基线PRD:\n" + (basePrdMarkdown == null ? "" : basePrdMarkdown)
                + "\n\n业务描述:\n" + businessDescription
                + "\n\n完整对话JSON:\n" + objectToJsonSafe(history);
        return parsePrdOutput(callOpenAiForText(systemPrompt, userPrompt));
    }

    public List<String> suggestProjectTags(String traceId, String requirementTitle, String requirementSummary, String prdMarkdown) {
        String corpus = String.join("\n",
                requirementTitle == null ? "" : requirementTitle,
                requirementSummary == null ? "" : requirementSummary,
                prdMarkdown == null ? "" : prdMarkdown
        );
        if (mockOnly || getApiKey().isEmpty()) {
            return fallbackTags(corpus);
        }

        String systemPrompt = """
You are a software project tagging assistant.
Extract concise project tags from requirement/PRD text.
Return strict JSON only:
{"tags":["..."]}
Rules:
1) 3-8 tags.
2) Prefer product/tech domain terms.
3) Remove duplicates and generic filler words.
4) Keep each tag <= 12 chars.
""";
        String userPrompt = "Requirement/PRD text:\n" + corpus;
        try {
            String result = callOpenAiForText(systemPrompt, userPrompt);
            JsonNode root = objectMapper.readTree(sanitizeJsonText(result));
            List<String> tags = normalizeTags(toStringList(root.path("tags")));
            if (!tags.isEmpty()) {
                return tags;
            }
        } catch (Exception ignored) {
            // fallback below
        }
        return fallbackTags(corpus);
    }

    private String buildClarifyAnswerContext(List<DocGenService.ClarifyQuestion> clarifyQuestions, Map<String, String> answers) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> safeAnswers = answers == null ? Map.of() : answers;
        if (clarifyQuestions != null && !clarifyQuestions.isEmpty()) {
            for (DocGenService.ClarifyQuestion q : clarifyQuestions) {
                String qid = q == null ? "" : q.id();
                String question = q == null ? "" : q.question();
                String answer = safeAnswers.getOrDefault(qid, "");
                sb.append("- ").append(qid).append(": ").append(question).append("\n")
                        .append("  Answer: ").append(answer.isBlank() ? "(empty)" : answer).append("\n");
            }
        } else {
            sb.append("- No predefined clarify questions.\n");
        }

        sb.append("\nAdditional answer map:\n");
        for (Map.Entry<String, String> entry : safeAnswers.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String getApiKey() {
        return resolveApiKey().key();
    }

    private ApiKeyResolution resolveApiKey() {
        String router = System.getenv("AGENT_ROUTER_TOKEN");
        if (router != null && !router.trim().isEmpty()) {
            return new ApiKeyResolution(router.trim(), "env.AGENT_ROUTER_TOKEN");
        }
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.trim().isEmpty()) {
            return new ApiKeyResolution(env.trim(), "env.OPENAI_API_KEY");
        }
        String config = openaiApiKey == null ? "" : openaiApiKey.trim();
        return new ApiKeyResolution(config, "config.agent.llm.openai.apiKey");
    }

    private String maskKey(String key) {
        if (key == null || key.isBlank()) return "(empty)";
        if (key.length() <= 10) return "****";
        return key.substring(0, 6) + "****" + key.substring(key.length() - 4);
    }

    private ConversationTurn parseConversationTurn(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(sanitizeJsonText(jsonText));
            String msg = root.path("assistantMessage").asText();
            String pending = root.path("pendingQuestion").asText("");
            List<String> confirmed = toStringList(root.path("confirmedItems"));
            List<String> unconfirmed = toStringList(root.path("unconfirmedItems"));
            if (msg == null || msg.isBlank()) throw new IllegalArgumentException("assistantMessage is empty");
            return new ConversationTurn(msg, confirmed, unconfirmed, pending);
        } catch (Exception e) {
            throw new RuntimeException("Parse conversation result failed: " + e.getMessage(), e);
        }
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> out = new ArrayList<>();
        for (JsonNode item : node) {
            String v = item.asText();
            if (v != null && !v.isBlank()) out.add(v.trim());
        }
        return out;
    }

    private String parsePrdOutput(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(sanitizeJsonText(jsonText));
            String md = root.path("prdMarkdown").asText();
            if (md == null || md.isBlank()) throw new IllegalArgumentException("prdMarkdown is empty");
            return md;
        } catch (Exception e) {
            throw new RuntimeException("Parse PRD failed: " + e.getMessage(), e);
        }
    }

    private String objectToJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String callOpenAiForText(String systemPrompt, String userPrompt) {
        String apiKey = getApiKey();
        if (apiKey.isEmpty()) throw new IllegalStateException("No API key configured (AGENT_ROUTER_TOKEN / OPENAI_API_KEY)");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openaiModel);
        payload.put("temperature", 0.2);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Build request payload failed: " + e.getMessage(), e);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(openaiBaseUrl + "/chat/completions"))
                .timeout(Duration.ofMillis(openaiTimeoutMs))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("LLM call failed status=" + resp.statusCode() + " body=" + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("LLM response missing choices: " + resp.body());
            }
            String content = extractMessageContent(choices.get(0).path("message").path("content"));
            if (content.isBlank()) {
                throw new RuntimeException("LLM response content is empty: " + resp.body());
            }
            return content.trim();
        } catch (Exception e) {
            throw new RuntimeException("Call LLM failed: " + e.getMessage(), e);
        }
    }

    private String extractMessageContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item == null || item.isNull()) continue;
                String part = item.path("text").asText("");
                if (!part.isBlank()) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(part);
                }
            }
            return sb.toString();
        }
        return contentNode.toString();
    }

    private String sanitizeJsonText(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            text = text.substring(firstBrace, lastBrace + 1);
        }
        return text;
    }

    private List<String> fallbackTags(String corpus) {
        String lower = corpus == null ? "" : corpus.toLowerCase(Locale.ROOT);
        List<String> tags = new ArrayList<>();
        addIfContains(lower, tags, "thingsboard", "ThingsBoard");
        addIfContains(lower, tags, "vehicle", "车辆");
        addIfContains(lower, tags, "map", "地图");
        addIfContains(lower, tags, "websocket", "WebSocket");
        addIfContains(lower, tags, "location", "定位");
        addIfContains(lower, tags, "status", "状态监控");
        addIfContains(lower, tags, "trajectory", "轨迹回放");
        addIfContains(lower, tags, "login", "登录鉴权");
        addIfContains(lower, tags, "rbac", "RBAC");
        addIfContains(lower, tags, "project", "项目管理");
        addIfContains(lower, tags, "requirement", "需求管理");
        addIfContains(lower, tags, "prd", "PRD");
        addIfContains(lower, tags, "api", "API");
        addIfContains(lower, tags, "swagger", "OpenAPI");
        addIfContains(lower, tags, "postgres", "PostgreSQL");
        addIfContains(lower, tags, "mybatis", "MyBatis");
        if (tags.isEmpty()) {
            tags.add("需求管理");
            tags.add("PRD");
        }
        return normalizeTags(tags);
    }

    private void addIfContains(String corpus, List<String> tags, String keyword, String tag) {
        if (corpus.contains(keyword)) {
            tags.add(tag);
        }
    }

    private List<String> normalizeTags(List<String> rawTags) {
        Set<String> unique = new LinkedHashSet<>();
        if (rawTags != null) {
            for (String tag : rawTags) {
                if (tag == null) continue;
                String normalized = tag.trim();
                if (normalized.isEmpty()) continue;
                if (normalized.length() > 12) {
                    normalized = normalized.substring(0, 12);
                }
                unique.add(normalized);
                if (unique.size() >= 8) {
                    break;
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private record ApiKeyResolution(String key, String source) {}

    private ConversationTurn mockStartConversation(String basePrdMarkdown) {
        boolean hasBase = basePrdMarkdown != null && !basePrdMarkdown.isBlank();
        return new ConversationTurn(
                hasBase ? "已读取上一版PRD，请补充本次变更重点。" : "请先描述本次需求的目标和关键功能。",
                hasBase ? List.of("已提供上一版PRD") : List.of(),
                List.of("核心目标", "关键功能", "验收标准"),
                "请先补充核心目标。"
        );
    }

    private ConversationTurn mockContinueConversation(List<ChatMessage> history, String basePrdMarkdown) {
        int userCount = 0;
        for (ChatMessage m : history) if ("user".equalsIgnoreCase(m.role())) userCount++;

        if (userCount <= 1) {
            return new ConversationTurn(
                    "已记录你的输入。请继续补充关键功能点。",
                    List.of("核心目标已确认"),
                    List.of("关键功能", "验收标准"),
                    "请补充关键功能。"
            );
        }
        if (userCount == 2) {
            return new ConversationTurn(
                    "关键功能已记录。请补充验收标准。",
                    List.of("核心目标已确认", "关键功能已确认"),
                    List.of("验收标准"),
                    "请补充验收标准。"
            );
        }
        return new ConversationTurn(
                "信息已完整，可以生成PRD。",
                List.of("核心目标已确认", "关键功能已确认", "验收标准已确认"),
                List.of(),
                ""
        );
    }

    private String mockGeneratePrdFromChat(String businessDescription, List<ChatMessage> history) {
        StringBuilder dialog = new StringBuilder();
        for (ChatMessage m : history) {
            dialog.append("- ").append(m.role()).append(": ").append(m.content()).append("\n");
        }
        return """
## Agent需求文档（通用模板）

**项目名称**：AI需求整理项目
**文档版本**：V1.0.1
**日期**：%s
**负责人**：产品经理

### 1. 目标与范围
- 根据输入需求输出结构化 PRD。

### 2. 输入业务描述
%s

### 3. 对话澄清记录
%s
""".formatted(new Date().toString(), businessDescription, dialog.toString());
    }
}
