package com.oncall.ai.service;

import com.oncall.ai.model.Conversation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oncall.ai.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final HttpClient httpClient;

    public DeepSeekClient(
            @Value("${deepseek.api-key}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String modelName) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.modelName = modelName;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public Flux<String> stream(String systemPrompt, Conversation conversation, String userMessage, List<DocumentChunk> chunks) {
        String prompt = buildPrompt(systemPrompt, conversation, userMessage, chunks);
        String requestBody = String.format("""
                {"model":"%s","stream":true,"messages":%s}
                """, modelName, prompt).stripIndent().strip();

        log.debug("DeepSeek request: model={}, promptLen={}", modelName, requestBody.length());
        log.debug("DeepSeek body (first 500): {}", requestBody.length() > 500 ? requestBody.substring(0, 500) : requestBody);

        return Flux.create(sink -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/chat/completions"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("Accept", "text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                        .thenAccept(response -> {
                            int statusCode = response.statusCode();
                            if (statusCode != 200) {
                                String errorBody = "";
                                try {
                                    errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                                } catch (Exception ignored) {}
                                log.error("DeepSeek API error: status={}, body={}", statusCode, errorBody);
                                sink.error(new RuntimeException("DeepSeek API returned " + statusCode + ": " + errorBody));
                                return;
                            }

                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("data: ")) {
                                        String data = line.substring(6).trim();
                                        if ("[DONE]".equals(data)) {
                                            break;
                                        }
                                        String content = extractDeltaContent(data);
                                        if (content != null && !content.isEmpty()) {
                                            sink.next(content);
                                        }
                                    }
                                }
                                sink.complete();
                            } catch (Exception e) {
                                sink.error(e);
                            }
                        })
                        .exceptionally(e -> {
                            log.error("DeepSeek request failed", e);
                            sink.error(e);
                            return null;
                        });
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private String buildPrompt(String systemPrompt, Conversation conversation, String userMessage, List<DocumentChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        sb.append(String.format("""
                {"role":"system","content":"%s"}
                """, escapeJson(systemPrompt)).stripIndent());

        if (!chunks.isEmpty()) {
            String docsContext = chunks.stream()
                    .map(c -> "[" + c.getSource() + "] " + c.getContent())
                    .collect(Collectors.joining("\n\n---\n\n"));
            sb.append(String.format("""
                ,{"role":"system","content":"Following are relevant documents. Answer strictly based on them:\\n%s"}
                """, escapeJson(docsContext)).stripIndent());
        }

        if (conversation != null) {
            List<Message> history = conversation.lastMessages(10);
            for (Message msg : history) {
                String role = switch (msg.role()) {
                    case USER -> "user";
                    case ASSISTANT -> "assistant";
                    default -> "system";
                };
                sb.append(String.format("""
                    ,{"role":"%s","content":"%s"}
                    """, role, escapeJson(msg.content())).stripIndent());
            }
        }

        sb.append(String.format("""
                ,{"role":"user","content":"%s"}
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