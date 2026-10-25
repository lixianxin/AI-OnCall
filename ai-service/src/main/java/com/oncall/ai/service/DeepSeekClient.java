package com.oncall.ai.service;

import com.oncall.ai.model.Conversation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oncall.ai.model.Message;
import com.oncall.ai.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DeepSeek（OpenAI 兼容协议）流式客户端，带上游错误分级退避重试。
 *
 * <p>重试语义（2026-10-25，针对 5 并发压测触发 GLM 免费档 429 限流的真实故障）：
 * <ul>
 *   <li><b>429</b>：优先遵循上游 Retry-After 头；无头则指数退避（base → 2× → 4×，封顶 8s）</li>
 *   <li><b>5xx</b>：请求未产生任何输出 token，可安全重试</li>
 *   <li><b>连接超时 / 连接失败（IOException）</b>：请求未送达，可安全重试</li>
 *   <li><b>其他 4xx（401/402/参数错误等）</b>：终态，不重试——盲目重试只会重复失败</li>
 *   <li><b>流式输出已开始后的异常</b>：不重试——客户端已收到部分文本，重放会造成重复内容；
 *       直接 error 终止，由 SSE 错误事件机制兜底</li>
 * </ul>
 *
 * <p>两级保护：
 * <ol>
 *   <li>最大尝试次数（{@code deepseek.retry.max-attempts}，含首次）</li>
 *   <li>总时间预算（{@code deepseek.retry.budget-ms}）：所有重试共享，防止"每次重试都吃满
 *       单请求超时"导致一个请求无限期占用资源</li>
 * </ol>
 *
 * <p>与 ToolExecutor 治理层的关系：ToolExecutor 的重试针对<b>工具执行</b>，
 * 本类重试针对<b>LLM 上游调用</b>，两层互不叠加（编排链路上每个环节只经过一层重试）。
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private static final long MAX_BACKOFF_MS = 8_000;

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    /** 备用模型链路（分级降级第二级）：主链路重试耗尽后切换。为空 = 未配置备用链路 */
    private final String backupBaseUrl;
    private final String backupModelName;
    private final String backupApiKey;
    private final HttpClient httpClient;
    private final AgentMetrics agentMetrics;

    private final int retryMaxAttempts;
    private final long retryBudgetMs;
    private final long retryBaseDelayMs;

    public DeepSeekClient(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String modelName,
            @Value("${deepseek.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${deepseek.retry.budget-ms:30000}") long retryBudgetMs,
            @Value("${deepseek.retry.base-delay-ms:1000}") long retryBaseDelayMs,
            @Value("${deepseek.backup.base-url:}") String backupBaseUrl,
            @Value("${deepseek.backup.model:deepseek-chat}") String backupModelName,
            @Value("${deepseek.backup.api-key:}") String backupApiKey,
            AgentMetrics agentMetrics) {
        this.apiKey = apiKey;
        this.baseUrl = normalize(baseUrl);
        this.modelName = modelName;
        this.backupBaseUrl = backupBaseUrl == null || backupBaseUrl.isBlank()
                ? null : normalize(backupBaseUrl);
        this.backupModelName = backupModelName;
        this.backupApiKey = backupApiKey == null || backupApiKey.isBlank() ? apiKey : backupApiKey;
        this.retryMaxAttempts = Math.max(1, retryMaxAttempts);
        this.retryBudgetMs = retryBudgetMs;
        this.retryBaseDelayMs = Math.max(0, retryBaseDelayMs);
        this.agentMetrics = agentMetrics;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    private static String normalize(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean hasBackup() { return backupBaseUrl != null; }

    public Flux<String> stream(String systemPrompt, Conversation conversation, String userMessage, String detailData) {
        String prompt = buildPrompt(systemPrompt, conversation, userMessage, detailData);
        // include_usage: 流式最后一帧携带 usage（prompt/completion tokens），用于可观测性统计
        String requestBody = String.format("""
                {"model":"%s","stream":true,"stream_options":{"include_usage":true},"messages":%s}
                """, modelName, prompt).stripIndent().strip();

        log.debug("DeepSeek request: model={}, promptLen={}", modelName, requestBody.length());
        log.debug("DeepSeek body (first 500): {}", requestBody.length() > 500 ? requestBody.substring(0, 500) : requestBody);

        // MDC 是 ThreadLocal，响应式链路切线程不自动继承；捕获入口上下文带入重试线程
        Map<String, String> mdcCtx = MDC.getCopyOfContextMap();

        return Flux.<String>create(sink -> {
                    if (mdcCtx != null) {
                        MDC.setContextMap(mdcCtx);
                    }
                    try {
                        executeWithRetry(sink, requestBody, baseUrl, apiKey, modelName,
                                retryMaxAttempts, retryBudgetMs);
                    } finally {
                        MDC.clear();
                    }
                })
                // 重试循环含同步 HTTP 与退避 sleep，放到 boundedElastic，避免阻塞事件循环线程
                .subscribeOn(Schedulers.boundedElastic());
    }

    /** 分级退避重试主循环。sink 只在确定不再重试时发出 error / complete。 */
    private void executeWithRetry(reactor.core.publisher.FluxSink<String> sink, String requestBody,
                                  String baseUrl, String apiKey, String modelName,
                                  int maxAttempts, long budgetMs) {
        String traceId = MDC.get("traceId") != null ? MDC.get("traceId") : "n/a";
        long deadline = System.currentTimeMillis() + budgetMs;
        long backoffMs = retryBaseDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/chat/completions"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "text/event-stream")
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<java.io.InputStream> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
                int status = response.statusCode();
                long llmCallStartNs = System.nanoTime();

                // ---- 可重试类：429 限流 / 5xx 服务端错误（尚未输出任何 token）----
                if (status == 429 || status >= 500) {
                    String errorBody = readAndClose(response);
                    long waitMs = Math.max(backoffMs, parseRetryAfterMs(response));
                    // 时间预算保护：等待后必须仍预留至少一次尝试的空间，否则视为预算耗尽
                    boolean canRetry = attempt < maxAttempts
                            && System.currentTimeMillis() + waitMs <= deadline;
                    if (canRetry) {
                        agentMetrics.recordLlmRetry(status == 429 ? "429" : "5xx");
                        log.warn("[traceId={}] DeepSeek 上游 {}（attempt {}/{}），{}ms 后重试{}",
                                traceId, status, attempt, maxAttempts, waitMs,
                                waitMs == backoffMs ? "" : "（遵循 Retry-After）");
                        Thread.sleep(waitMs);
                        backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                        continue;
                    }
                    log.error("[traceId={}] DeepSeek 上游 {} 重试耗尽或超预算（attempt {}/{}）: {}",
                            traceId, status, attempt, maxAttempts, abbreviate(errorBody));
                    failOverToBackupOrError(sink, requestBody, traceId, baseUrl,
                            status + " 重试耗尽",
                            new RuntimeException("DeepSeek API returned " + status + ": " + errorBody));
                    return;
                }

                // ---- 终态类：其他 4xx（鉴权/余额/参数错误），重试无意义 ----
                if (status != 200) {
                    String errorBody = readAndClose(response);
                    log.error("[traceId={}] DeepSeek API error（终态，不重试）: status={}, body={}",
                            traceId, status, abbreviate(errorBody));
                    // 未产生任何输出 token；备用链路通常持有独立凭据/额度，值得一试
                    failOverToBackupOrError(sink, requestBody, traceId, baseUrl,
                            status + " 终态错误",
                            new RuntimeException("DeepSeek API returned " + status + ": " + errorBody));
                    return;
                }

                // ---- 200：流式读取。此点之后已可能向客户端输出 token，
                //      任何异常都直接 error（不重试），避免重放造成重复文本 ----
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    // [DONE] 哨兵：区分"上游正常结束"与"上游中途断开"。
                    // 没有 [DONE] 就到达 EOF = 流被截断（注入测试 2026-10-25 发现：
                    // 旧实现把截断当正常完成，客户端收到 done，半截答案被静默接受）
                    boolean sawDone = false;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                sawDone = true;
                                break;
                            }
                            recordUsageIfPresent(data);
                            String content = extractDeltaContent(data);
                            if (content != null && !content.isEmpty()) {
                                sink.next(content);
                            }
                        }
                    }
                    if (sawDone) {
                        // LLM 上游调用端到端耗时（仅成功尝试）：区分"模型等待"与"本地处理"
                        agentMetrics.recordLlmLatency((System.nanoTime() - llmCallStartNs) / 1_000_000);
                        sink.complete();
                    } else {
                        // EOF 且未收到 [DONE]：流被截断。已输出部分内容，不重试（避免重复文本），
                        // 以 error 终态交给 SSE 错误事件机制
                        log.error("[traceId={}] DeepSeek 流在上游被截断（EOF 未收到 [DONE]，已输出部分内容，不重试）", traceId);
                        sink.error(new IllegalStateException("upstream stream truncated before [DONE]"));
                    }
                } catch (Exception e) {
                    log.error("[traceId={}] DeepSeek 流式读取中断（已输出部分内容，不重试）", traceId, e);
                    sink.error(e);
                }
                return;

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                sink.error(ie);
                return;
            } catch (IOException e) {
                // 请求未送达（连接超时 / 连接失败 / HttpTimeoutException）：
                // 尚未产生任何输出 token，与 429/5xx 同属可重试类
                boolean canRetry = attempt < maxAttempts
                        && System.currentTimeMillis() + backoffMs <= deadline;
                if (canRetry) {
                    agentMetrics.recordLlmRetry("connect_error");
                    log.warn("[traceId={}] DeepSeek 连接失败（attempt {}/{}）：{}，{}ms 后重试",
                            traceId, attempt, maxAttempts, e.getMessage(), backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        sink.error(ie);
                        return;
                    }
                    backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                    continue;
                }
                log.error("[traceId={}] DeepSeek request failed（重试耗尽或超预算）", traceId, e);
                failOverToBackupOrError(sink, requestBody, traceId, baseUrl,
                        "连接失败重试耗尽", e);
                return;
            } catch (Exception e) {
                sink.error(e);
                return;
            }
        }
    }

    /**
     * 分级降级第二级：主链路在任何输出之前失败（可重试耗尽 / 终态 4xx / 连接失败），
     * 且配置了备用链路 → 切换备用模型重走一次（独立预算，最多 2 次尝试，防降级放大）；
     * 已在备用链路上或未配置备用 → sink.error（降级第三级"部分诊断结论"由编排层兜底）。
     */
    private void failOverToBackupOrError(reactor.core.publisher.FluxSink<String> sink, String requestBody,
                                         String traceId, String currentBaseUrl,
                                         String reason, Exception cause) {
        if (hasBackup() && !backupBaseUrl.equals(currentBaseUrl)) {
            agentMetrics.recordLlmDegrade("backup_model");
            log.warn("[traceId={}] LLM 主链路失败（{}），降级至备用模型 {} @ {}",
                    traceId, reason, backupModelName, backupBaseUrl);
            executeWithRetry(sink, requestBody, backupBaseUrl, backupApiKey, backupModelName,
                    Math.min(retryMaxAttempts, 2), retryBudgetMs);
            return;
        }
        sink.error(cause);
    }

    /** 读取错误响应体并关闭流 */
    private String readAndClose(HttpResponse<java.io.InputStream> response) {
        try (java.io.InputStream is = response.body()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    /** 解析 Retry-After 头（秒），非法/缺失返回 0（回落到指数退避） */
    private long parseRetryAfterMs(HttpResponse<java.io.InputStream> response) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter == null || retryAfter.isBlank()) {
            return 0;
        }
        try {
            return Math.min(Long.parseLong(retryAfter.trim()) * 1000L, retryBudgetMs);
        } catch (NumberFormatException e) {
            return 0; // HTTP-date 形式暂不支持，回落指数退避
        }
    }

    private String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 流式最后一帧（choices 为空、携带 usage）时记录 token 用量到指标层 */
    private void recordUsageIfPresent(String jsonData) {
        try {
            var root = objectMapper.readTree(jsonData);
            var usage = root.get("usage");
            if (usage == null || usage.isNull()) return;
            long promptTokens = usage.path("prompt_tokens").asLong(0);
            long completionTokens = usage.path("completion_tokens").asLong(0);
            if (promptTokens > 0) agentMetrics.recordTokens("llm_prompt", promptTokens);
            if (completionTokens > 0) agentMetrics.recordTokens("llm_completion", completionTokens);
            log.debug("DeepSeek usage recorded: prompt={}, completion={}", promptTokens, completionTokens);
        } catch (Exception e) {
            log.debug("Failed to parse usage frame", e);
        }
    }

    private String extractDeltaContent(String jsonData) {
        try {
            var root = objectMapper.readTree(jsonData);
            var choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) return null;
            var delta = choices.get(0).get("delta");
            if (delta == null) return null;
            // skip reasoning_content, only return actual content
            var contentNode = delta.get("content");
            if (contentNode == null || contentNode.isNull()) return null;
            String content = contentNode.asText();
            return content.isEmpty() ? null : content;
        } catch (Exception e) {
            log.warn("Failed to parse delta: {}", jsonData.substring(0, Math.min(200, jsonData.length())), e);
            return null;
        }
    }

    private String buildPrompt(String systemPrompt, Conversation conversation, String userMessage, String detailData) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        // Use systemPrompt as-is (already contains knowledge base content + metrics)
        String fullSystemPrompt = systemPrompt;
        sb.append(String.format("""
                {"role":"system","content":"%s"}
                """, escapeJson(fullSystemPrompt)).stripIndent());

        // Conversation history
        for (Message msg : conversation.getRecentMessages(10)) {
            String role = msg.role() == Role.USER ? "user" : "assistant";
            sb.append(",");
            sb.append(String.format("""
                    {"role":"%s","content":"%s"}
                    """, role, escapeJson(msg.content())).stripIndent());
        }

        // Current user message
        sb.append(",");
        sb.append(String.format("""
                {"role":"user","content":"%s"}
                """, escapeJson(userMessage)).stripIndent());

        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        /* skip control chars */;
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
