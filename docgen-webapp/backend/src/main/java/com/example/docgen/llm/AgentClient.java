package com.example.docgen.llm;

import com.example.docgen.service.DocGenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class AgentClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${agent.llm.openai.baseUrl:https://api.openai.com/v1}")
    private String openaiBaseUrl;
    @Value("${agent.llm.openai.model:gpt-4o-mini}")
    private String openaiModel;
    @Value("${agent.llm.openai.timeoutMs:30000}")
    private int openaiTimeoutMs;
    @Value("${agent.llm.mockOnly:false}")
    private boolean mockOnly;

    public record ChatMessage(String role, String content) {}
    public record ConversationTurn(
            String assistantMessage,
            List<String> confirmedItems,
            List<String> unconfirmedItems,
            String pendingQuestion
    ) {}

    public List<DocGenService.ClarifyQuestion> generateClarifyQuestions(String traceId, String prdTemplate, String businessDescription) {
        return List.of(
                new DocGenService.ClarifyQuestion("q1", "这个系统面向哪些角色？", "用于明确用户与权限边界", true),
                new DocGenService.ClarifyQuestion("q2", "核心业务流程有哪些步骤？", "用于明确主流程与接口边界", true),
                new DocGenService.ClarifyQuestion("q3", "你最关心的验收指标是什么？", "用于形成可测试验收标准", true)
        );
    }

    public String generatePrd(String traceId, String prdTemplate, String businessDescription,
                              List<DocGenService.ClarifyQuestion> clarifyQuestions,
                              Map<String, String> answers) {
        return mockGeneratePrdFromChat(businessDescription, List.of(
                new ChatMessage("assistant", "请补充角色信息"),
                new ChatMessage("user", answers.toString())
        ));
    }

    public ConversationTurn startConversation(String traceId, String prdTemplate, String businessDescription, String basePrdMarkdown) {
        if (mockOnly || getApiKey().isEmpty()) {
            return mockStartConversation(basePrdMarkdown);
        }
        String systemPrompt = """
你是“需求分析与评审助手”。
你要基于《01-PRD-Agent需求文档.md》的信息完整度标准，进行“分析-确认-补齐”。
你必须只输出 JSON，格式如下：
{
  "assistantMessage": "先给出你对当前需求的分析与结论，再提出一个最关键的确认问题",
  "confirmedItems": ["已确认项1", "已确认项2"],
  "unconfirmedItems": ["未确认项1", "未确认项2"],
  "pendingQuestion": "本轮要用户回答的单个问题"
}
规则：
1) 每次只问一个问题（pendingQuestion）；
2) confirmedItems / unconfirmedItems 必须同时输出；
3) unconfirmedItems 为空时，表示信息已完整，可以生成PRD；
4) 中文输出，措辞可落地（可影响接口/字段/验收）。
""";
        String userPrompt = "模板:\n" + prdTemplate
                + "\n\n上一版PRD（如为空表示首次编写）:\n" + (basePrdMarkdown == null ? "" : basePrdMarkdown)
                + "\n\n本次新增/调整需求:\n" + businessDescription
                + "\n\n请优先识别“相对上一版”的变化点，并从最关键的未确认项开始提问。";
        return parseConversationTurn(callOpenAiForText(systemPrompt, userPrompt));
    }

    public ConversationTurn continueConversation(String traceId, String prdTemplate, String businessDescription,
                                                 List<ChatMessage> history, String pendingQuestion, String basePrdMarkdown) {
        if (mockOnly || getApiKey().isEmpty()) {
            return mockContinueConversation(history, basePrdMarkdown);
        }
        String systemPrompt = """
你是“需求分析与评审助手”。
根据历史对话，先确认用户刚才回答的要点，并更新“已确认项/未确认项”。
你必须只输出 JSON：
{
  "assistantMessage": "先确认用户回答，再给结论与下一步",
  "confirmedItems": ["..."],
  "unconfirmedItems": ["..."],
  "pendingQuestion": "如果仍有未确认项，就只问一个最关键问题；否则空字符串"
}
规则：
1) 一次只问一个问题；
2) unconfirmedItems 为空时，pendingQuestion 必须为空；
3) confirmedItems/unconfirmedItems 必须与 PRD 可落地内容相关（角色/范围/流程/输入输出/验收等）。
""";
        String userPrompt = "模板:\n" + prdTemplate
                + "\n\n上一版PRD（如为空表示首次编写）:\n" + (basePrdMarkdown == null ? "" : basePrdMarkdown)
                + "\n\n本次新增/调整需求:\n" + businessDescription
                + "\n\n历史对话JSON:\n" + objectToJsonSafe(history);
        return parseConversationTurn(callOpenAiForText(systemPrompt, userPrompt));
    }

    public String generatePrdFromChat(String traceId, String prdTemplate, String businessDescription, List<ChatMessage> history, String basePrdMarkdown) {
        if (mockOnly || getApiKey().isEmpty()) {
            return mockGeneratePrdFromChat(businessDescription, history);
        }
        String systemPrompt = """
你是通用需求整理Agent。根据模板 + 上一版PRD + 确认后的对话，生成“修订后的”PRD Markdown。
要求：
1) 如果提供了上一版PRD，必须在保持其合理结构的基础上进行修订（补充/修改/删除都允许），避免无关重写；
2) 以确认后的对话为准；
3) 输出仍需符合模板章节结构；
只输出JSON：
{"prdMarkdown":"..."}
""";
        String userPrompt = "模板:\n" + prdTemplate
                + "\n\n上一版PRD（如为空表示首次编写）:\n" + (basePrdMarkdown == null ? "" : basePrdMarkdown)
                + "\n\n本次新增/调整需求:\n" + businessDescription
                + "\n\n确认对话JSON:\n" + objectToJsonSafe(history);
        String json = callOpenAiForText(systemPrompt, userPrompt);
        return parsePrdOutput(json);
    }

    private String getApiKey() {
        String v = System.getenv("OPENAI_API_KEY");
        return v == null ? "" : v.trim();
    }

    private ConversationTurn parseConversationTurn(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            String msg = root.path("assistantMessage").asText();
            String pending = root.path("pendingQuestion").asText("");
            List<String> confirmed = toStringList(root.path("confirmedItems"));
            List<String> unconfirmed = toStringList(root.path("unconfirmedItems"));
            if (msg == null || msg.isBlank()) throw new IllegalArgumentException("assistantMessage为空");
            return new ConversationTurn(msg, confirmed, unconfirmed, pending);
        } catch (Exception e) {
            throw new RuntimeException("解析会话结果失败: " + e.getMessage(), e);
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
            JsonNode root = objectMapper.readTree(jsonText);
            String md = root.path("prdMarkdown").asText();
            if (md == null || md.isBlank()) throw new IllegalArgumentException("prdMarkdown为空");
            return md;
        } catch (Exception e) {
            throw new RuntimeException("解析PRD失败: " + e.getMessage(), e);
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
        if (apiKey.isEmpty()) throw new IllegalStateException("OPENAI_API_KEY 未配置");
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
            throw new RuntimeException("构造请求失败: " + e.getMessage(), e);
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
                throw new RuntimeException("LLM调用失败 status=" + resp.statusCode() + " body=" + resp.body());
            }
            JsonNode root = objectMapper.readTree(resp.body());
            return root.path("choices").get(0).path("message").path("content").asText("").trim();
        } catch (Exception e) {
            throw new RuntimeException("调用LLM失败: " + e.getMessage(), e);
        }
    }

    private ConversationTurn mockStartConversation(String basePrdMarkdown) {
        boolean hasBase = basePrdMarkdown != null && !basePrdMarkdown.isBlank();
        return new ConversationTurn(
                (hasBase
                        ? "我已收到上一版PRD，将以它为基线做“增量修订”。我会先确认变化点，再补齐未确认项。\n\n第一个关键问题：这次相对上一版，你新增/调整/删除了哪些功能点？请用要点列出。"
                        : "我先分析你的需求：你想要一个Web应用，通过页面输入需求，AI逐步分析与确认，最后生成标准PRD。为保证评审严谨，我会同时给出“已确认项/未确认项”。\n\n第一个关键问题：这个系统有哪些用户角色？每个角色能做什么？"),
                hasBase
                        ? List.of("已提供上一版PRD（已确认）", "目标：对话式确认需求并生成PRD（已确认）")
                        : List.of("目标：对话式确认需求并生成PRD（已确认）"),
                List.of("用户与角色（未确认）", "核心业务流程（未确认）", "输入输出与边界（未确认）", "验收标准口径（未确认）"),
                hasBase
                        ? "请列出相对上一版PRD的变化点（新增/调整/删除）。"
                        : "请说明系统角色及职责（例如：管理员/普通用户/审核人员分别能做什么）。"
        );
    }

    private ConversationTurn mockContinueConversation(List<ChatMessage> history, String basePrdMarkdown) {
        int userCount = 0;
        for (ChatMessage m : history) if ("user".equalsIgnoreCase(m.role())) userCount++;
        if (userCount == 1) {
            return new ConversationTurn(
                    "收到。我先确认主流程，确保变化点能落到交互与接口边界。",
                    List.of("目标（已确认）", basePrdMarkdown != null && !basePrdMarkdown.isBlank() ? "上一版PRD（已确认）" : "上一版PRD（无）"),
                    List.of("核心业务流程（未确认）", "用户与角色（未确认）", "验收标准口径（未确认）"),
                    "请按“步骤1/2/3”描述核心业务流程（输入来源、关键处理、输出结果），并标注哪些步骤是本次变更影响到的。"
            );
        }
        if (userCount == 2) {
            return new ConversationTurn(
                    "好的，主流程已明确。下一步需要关键数据对象，以便落到接口与数据表。",
                    List.of("目标（已确认）", "用户与角色（已确认）", "核心业务流程（已确认）"),
                    List.of("关键数据对象与字段（未确认）", "验收标准口径（未确认）"),
                    "请列出关键数据对象（例如：用户/订单/文档/任务等）以及每个对象的核心字段。"
            );
        }
        if (userCount == 3) {
            return new ConversationTurn(
                    "关键数据对象已补齐。最后需要验收标准，确保需求可测试。",
                    List.of("目标（已确认）", "用户与角色（已确认）", "核心业务流程（已确认）", "关键数据对象与字段（已确认）"),
                    List.of("验收标准口径（未确认）"),
                    "请给出验收标准：至少包含功能验收点、权限/安全要求、性能目标（如p95耗时）。"
            );
        }
        return new ConversationTurn(
                "信息已经完整，未确认项为空。你现在可以生成 PRD 文档。",
                List.of("目标（已确认）", "用户与角色（已确认）", "核心业务流程（已确认）", "关键数据对象与字段（已确认）", "验收标准口径（已确认）"),
                List.of(),
                ""
        );
    }

    private String mockGeneratePrdFromChat(String businessDescription, List<ChatMessage> history) {
        StringBuilder dialog = new StringBuilder();
        for (ChatMessage m : history) dialog.append("- ").append(m.role()).append(": ").append(m.content()).append("\n");
        return """
## Agent需求文档（通用模板）

**项目名称**：自动生成需求文档  
**文档版本**：v0.2  
**日期**：%s  
**负责人**：待定  

### 1. 目标与范围
- 基于对话逐步确认需求后输出PRD。

### 2. 用户与角色
- 根据对话确认。

### 3. 核心场景（用例）
- 输入需求 -> AI分析确认 -> 逐步细化 -> 生成PRD。

### 4. 功能性需求（模块拆分）
- 对话分析、任务状态、文档生成。

### 5. 非功能性需求
- 可追溯、可解释、可定位错误。

### 6. 数据与权限
- 保存对话历史和生成结果。

### 7. 验收标准索引
- 完成从分析到PRD的闭环。

---
### 对话记录
%s
""".formatted(new Date().toString(), dialog.toString());
    }
}

