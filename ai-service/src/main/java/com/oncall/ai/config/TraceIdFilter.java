package com.oncall.ai.config;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * 链路追踪过滤器（WebFlux WebFilter）
 *
 * 职责：
 * - 为每个请求生成或透传 trace_id（X-Trace-Id 头）
 * - 写入 MDC（供全链路日志自动携带 traceId）
 * - 写入 Reactor Context（供响应式链路下游读取）
 * - 回写响应头，便于客户端/上游排查
 *
 * 优先级最高，保证鉴权/异常日志也带 trace_id。
 */
@Component
@Order(-1)
public class TraceIdFilter implements WebFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(MDC_KEY, traceId);
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);

        Context ctx = Context.of(MDC_KEY, traceId);
        return chain.filter(exchange)
                .contextWrite(ctx)
                .doFinally(signal -> MDC.remove(MDC_KEY));
    }
}
