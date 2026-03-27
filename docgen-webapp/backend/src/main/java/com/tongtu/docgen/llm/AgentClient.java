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
    @Value("${agent.llm.openai.embeddingModel:text-embedding-3-small}")
    private String openaiEmbeddingModel;
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

    public record ProjectProductAnswer(
            String question,
            String answer
    ) {}

    public record ProjectProductGuideResult(
            String assistantMessage,
            List<String> followUpQuestions,
            String projectBackground,
            String similarProducts,
            String targetCustomerGroups,
            String commercialValue,
            String coreProductValue
    ) {}

    public List<Float> embedText(String traceId, String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (mockOnly || getApiKey().isEmpty()) {
            return mockEmbedding(normalized, 1536);
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", openaiEmbeddingModel);
            body.put("input", normalized);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(openaiBaseUrl) + "/embeddings"))
                    .timeout(Duration.ofMillis(openaiTimeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Embedding request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode items = root.path("data");
            if (!items.isArray() || items.isEmpty()) {
                throw new IllegalStateException("Embedding response data is empty");
            }
            JsonNode embeddingNode = items.get(0).path("embedding");
            if (!embeddingNode.isArray() || embeddingNode.isEmpty()) {
                throw new IllegalStateException("Embedding array is empty");
            }

            List<Float> out = new ArrayList<>();
            for (JsonNode item : embeddingNode) {
                out.add((float) item.asDouble());
            }
            return out;
        } catch (Exception e) {
            log.warn("Embedding fallback activated, traceId={}, reason={}", traceId, e.getMessage());
            return mockEmbedding(normalized, 1536);
        }
    }

    public String embeddingModel() {
        return openaiEmbeddingModel;
    }

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

    public ProjectProductGuideResult guideProjectProductInfo(String traceId,
                                                             String projectName,
                                                             String description,
                                                             String projectBackground,
                                                             String similarProducts,
                                                             String targetCustomerGroups,
                                                             String commercialValue,
                                                             String coreProductValue,
                                                             List<ProjectProductAnswer> answers) {
        String context = buildProjectProductGuideContext(
                projectName,
                description,
                projectBackground,
                similarProducts,
                targetCustomerGroups,
                commercialValue,
                coreProductValue,
                answers
        );
        if (mockOnly || getApiKey().isEmpty()) {
            return fallbackProjectProductGuide(
                    projectName,
                    description,
                    projectBackground,
                    similarProducts,
                    targetCustomerGroups,
                    commercialValue,
                    coreProductValue,
                    answers
            );
        }

        String systemPrompt = """
You are a product planning assistant for software project intake.
Your job is to help complete project product information with guided clarification.
Return strict JSON only:
{
  "assistantMessage":"...",
  "followUpQuestions":["..."],
  "projectBackground":"...",
  "similarProducts":"...",
  "targetCustomerGroups":"...",
  "commercialValue":"...",
  "coreProductValue":"..."
}
Rules:
1) Ask at most 3 targeted follow-up questions.
2) Questions should focus on missing critical product context, not generic filler.
3) Always return best-effort completion suggestions for all fields, even if some are assumptions.
4) Keep suggestions concrete, concise, and business-relevant.
5) If answers are provided, incorporate them and reduce repeated questions.
6) Do not output markdown or prose outside JSON.
""";
        String userPrompt = "Current project intake context:\n" + context;
        try {
            JsonNode root = objectMapper.readTree(sanitizeJsonText(callOpenAiForText(systemPrompt, userPrompt)));
            return new ProjectProductGuideResult(
                    root.path("assistantMessage").asText("已基于当前输入生成补全建议。"),
                    limitQuestions(toStringList(root.path("followUpQuestions"))),
                    normalizeLongText(root.path("projectBackground").asText(projectBackground == null ? "" : projectBackground)),
                    normalizeLongText(root.path("similarProducts").asText(similarProducts == null ? "" : similarProducts)),
                    normalizeShortText(root.path("targetCustomerGroups").asText(targetCustomerGroups == null ? "" : targetCustomerGroups), 1000),
                    normalizeLongText(root.path("commercialValue").asText(commercialValue == null ? "" : commercialValue)),
                    normalizeLongText(root.path("coreProductValue").asText(coreProductValue == null ? "" : coreProductValue))
            );
        } catch (Exception ignored) {
            return fallbackProjectProductGuide(
                    projectName,
                    description,
                    projectBackground,
                    similarProducts,
                    targetCustomerGroups,
                    commercialValue,
                    coreProductValue,
                    answers
            );
        }
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

    private List<Float> mockEmbedding(String text, int dimensions) {
        int safeDimensions = Math.max(8, dimensions);
        List<Float> vector = new ArrayList<>(safeDimensions);
        int seed = text.hashCode();
        for (int i = 0; i < safeDimensions; i++) {
            seed = 31 * seed + i + 17;
            float value = ((seed & 0x7fffffff) % 2000) / 1000.0f - 1.0f;
            vector.add(value);
        }
        return normalizeVector(vector);
    }

    private List<Float> normalizeVector(List<Float> vector) {
        double norm = 0.0d;
        for (Float item : vector) {
            if (item == null) continue;
            norm += item * item;
        }
        if (norm <= 0.0d) {
            return vector;
        }
        double divisor = Math.sqrt(norm);
        List<Float> normalized = new ArrayList<>(vector.size());
        for (Float item : vector) {
            normalized.add(item == null ? 0.0f : (float) (item / divisor));
        }
        return normalized;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private ProjectProductGuideResult fallbackProjectProductGuide(String projectName,
                                                                  String description,
                                                                  String projectBackground,
                                                                  String similarProducts,
                                                                  String targetCustomerGroups,
                                                                  String commercialValue,
                                                                  String coreProductValue,
                                                                  List<ProjectProductAnswer> answers) {
        String seed = firstNonBlank(description, projectBackground, projectName, "该项目");
        String answerContext = flattenAnswers(answers);
        List<String> questions = new ArrayList<>();
        if (isBlank(projectBackground)) {
            questions.add("这个项目最初是因为什么业务问题或客户诉求启动的？");
        }
        if (isBlank(targetCustomerGroups)) {
            questions.add("这个项目主要服务哪类客户、角色或使用群体？");
        }
        if (isBlank(commercialValue)) {
            questions.add("这个项目的商业价值更偏向增收、降本、提效，还是增强竞争力？");
        }
        questions = limitQuestions(questions);

        String mergedBackground = !isBlank(projectBackground)
                ? projectBackground
                : seed + "，当前处于项目早期定义阶段，需要进一步明确业务痛点、目标用户与价值定位。"
                + (isBlank(answerContext) ? "" : " 已补充信息：" + answerContext);
        String mergedSimilarProducts = !isBlank(similarProducts)
                ? similarProducts
                : inferSimilarProducts(seed);
        String mergedTargetCustomers = !isBlank(targetCustomerGroups)
                ? targetCustomerGroups
                : inferTargetCustomers(seed, answerContext);
        String mergedCommercialValue = !isBlank(commercialValue)
                ? commercialValue
                : inferCommercialValue(seed, answerContext);
        String mergedCoreValue = !isBlank(coreProductValue)
                ? coreProductValue
                : inferCoreProductValue(seed, answerContext);

        return new ProjectProductGuideResult(
                questions.isEmpty() ? "当前信息已经比较完整，我已基于已有内容生成项目产品信息建议。" : "我先根据当前输入补全了一版建议，并补了几条关键追问，方便你继续收敛。",
                questions,
                normalizeLongText(mergedBackground),
                normalizeLongText(mergedSimilarProducts),
                normalizeShortText(mergedTargetCustomers, 1000),
                normalizeLongText(mergedCommercialValue),
                normalizeLongText(mergedCoreValue)
        );
    }

    private String buildProjectProductGuideContext(String projectName,
                                                   String description,
                                                   String projectBackground,
                                                   String similarProducts,
                                                   String targetCustomerGroups,
                                                   String commercialValue,
                                                   String coreProductValue,
                                                   List<ProjectProductAnswer> answers) {
        StringBuilder sb = new StringBuilder();
        appendSection(sb, "项目名称", projectName);
        appendSection(sb, "项目描述", description);
        appendSection(sb, "项目背景", projectBackground);
        appendSection(sb, "类似产品参考", similarProducts);
        appendSection(sb, "目标客户群体", targetCustomerGroups);
        appendSection(sb, "商业价值", commercialValue);
        appendSection(sb, "产品核心价值", coreProductValue);
        if (answers != null && !answers.isEmpty()) {
            sb.append("\n【追问回答】\n");
            for (ProjectProductAnswer answer : answers) {
                if (answer == null) {
                    continue;
                }
                String q = answer.question() == null ? "" : answer.question().trim();
                String a = answer.answer() == null ? "" : answer.answer().trim();
                if (q.isEmpty() && a.isEmpty()) {
                    continue;
                }
                sb.append("- 问题: ").append(q).append("\n");
                sb.append("  回答: ").append(a).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private void appendSection(StringBuilder sb, String title, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("\n\n");
        }
        sb.append("【").append(title).append("】\n").append(value.trim());
    }

    private List<String> limitQuestions(List<String> questions) {
        List<String> result = new ArrayList<>();
        if (questions == null) {
            return result;
        }
        for (String question : questions) {
            if (question == null || question.isBlank()) {
                continue;
            }
            result.add(question.trim());
            if (result.size() >= 3) {
                break;
            }
        }
        return result;
    }

    private String normalizeLongText(String text) {
        return normalizeShortText(text, 4000);
    }

    private String normalizeShortText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String flattenAnswers(List<ProjectProductAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (ProjectProductAnswer answer : answers) {
            if (answer == null || isBlank(answer.answer())) {
                continue;
            }
            if (!isBlank(answer.question())) {
                parts.add(answer.question().trim() + "：" + answer.answer().trim());
            } else {
                parts.add(answer.answer().trim());
            }
        }
        return String.join("；", parts);
    }

    private String inferSimilarProducts(String seed) {
        return "可参考同类的行业需求管理、项目立项分析或产品规划工具，并结合 " + seed + " 的业务场景做差异化设计。";
    }

    private String inferTargetCustomers(String seed, String answerContext) {
        String base = "项目发起方、业务负责人、产品经理、需求分析人员";
        if (!isBlank(answerContext)) {
            return base + "；补充线索：" + answerContext;
        }
        if (!isBlank(seed)) {
            return base + "；重点围绕“" + seed + "”相关业务使用方。";
        }
        return base;
    }

    private String inferCommercialValue(String seed, String answerContext) {
        String base = "通过更早梳理项目背景、用户对象和价值主张，减少前期沟通成本、缩短需求收敛周期，并提升后续需求文档质量。";
        if (!isBlank(answerContext)) {
            return base + " 结合补充信息，预计还能进一步支持业务评估与项目优先级判断。";
        }
        if (!isBlank(seed)) {
            return base + " 对“" + seed + "”相关场景尤其有利。";
        }
        return base;
    }

    private String inferCoreProductValue(String seed, String answerContext) {
        String base = "把分散的项目想法、业务诉求与价值判断，沉淀成结构化、可复用、可继续传递到需求阶段的项目产品信息。";
        if (!isBlank(answerContext)) {
            return base + " AI 可进一步基于补充回答做引导式完善。";
        }
        if (!isBlank(seed)) {
            return base + " 并围绕“" + seed + "”形成更清晰的立项共识。";
        }
        return base;
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
