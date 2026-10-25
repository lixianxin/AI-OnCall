# P2-2 Micrometer → Prometheus 指标暴露验证报告（2026-10-25）

> 目标：把已有 AgentMetrics 业务指标以 Prometheus 标准格式暴露，补齐可观测性短板
> （三层评估中"线上可观测性 2/4"的主要缺口：无标准监控端点）。
> 验证方式：mock 上游 + 429×2 注入 + 抓取 /actuator/prometheus 核对指标值与注入行为一致。

## 改动内容（最小侵入式）

| 文件 | 改动 |
|---|---|
| `build.gradle` | + `spring-boot-starter-actuator`、`micrometer-registry-prometheus`（版本由 Boot BOM 管理，源走阿里云仓库——官方 Maven Central 本机不可达） |
| `AgentMetrics.java` | 双写：原 JSON `/metrics` 保持不变（人工排查用），同时以 Counter/Timer 写入 Micrometer 全局 Registry。用静态门面而非注入 MeterRegistry，避免破坏单测直接实例化 |
| `DeepSeekClient.java` | 成功的 LLM 调用记录端到端耗时 → `oncall_llm_call_latency`（区分"模型等待"与"本地处理"，这是评估报告点名的分段能力） |
| `application.yml` | 暴露 `/actuator/prometheus`；注释明确**标签只用低基数字段**（tool/status/reason），trace_id 归日志不归指标 |

## 指标清单（抓取样本：`prometheus_scrape_sample.txt`）

| Prometheus 指标 | 类型 | 用途 |
|---|---|---|
| `oncall_tool_calls_total{tool,status}` | Counter | 各工具调用与错误率 |
| `oncall_tool_latency_seconds{tool}` + quantile/sum/max | Timer | 工具执行瓶颈定位（P50/P95/P99） |
| `oncall_tool_retries_total{tool}` | Counter | 工具重试观察 |
| `oncall_llm_retries_total{reason}` | Counter | 上游限流/异常（reason ∈ 429/5xx/connect_error，固定枚举） |
| `oncall_llm_call_latency_seconds` | Timer | LLM 等待分段（成功尝试，含流式接收全程） |
| `oncall_tokens_total{source}` | Counter | Token 用量/成本估算 |
| `http_server_requests_seconds{uri,status,...}` | Spring Boot 自动直方图 | 请求级 P50/P95/P99（免费获得） |

## 验证结果

1. **端点可用**：`GET /actuator/prometheus` 返回标准文本格式（样本文件为实际抓取内容）。
2. **指标值与注入行为一致**：429×2 注入场景后，`oncall_llm_retries_total{reason="429"} 2.0`
   ——与 mock 收到的请求数（3 = 2 次 429 + 1 次成功）和 JSON `/metrics` 的 `llm_retries{"429":2}`
   三方互相印证。
3. **分段计时生效**：`oncall_llm_call_latency_seconds_count=1`（成功尝试），
   `oncall_tool_latency_seconds` 分位数输出正常。
4. **兼容性**：自定义 `/metrics` JSON 输出不变；`/health` 不变。

## 测试与验证的诚实口径

- `./gradlew test` 在本机**无法运行**（存量环境问题：Gradle Test Worker 在中文用户路径 +
  GBK 编码下 `ClassNotFoundException: GradleWorkerMain`，与本次改动无关——仓库此前仅做过
  compileJava 验证，同一限制）。本次验证依赖：编译通过 + 运行时行为核对 + 全量回放回归。
- 路由链路回归：diagnosis_v2 全量 200 条（mock 上游，throttle 2.5s），
  Intent/Tool/No-Tool/Fallback 预期与基线逐位一致（改动纯增量，不触碰路由代码）——
  结果见 `eval/results/p2_prometheus_regression_20261025.json.json`。

## Grafana 面板建议（回答三个问题即可，不堆曲线）

1. 请求是否持续失败：`rate(http_server_requests_seconds_count{status=~"5.."}[5m])` + `oncall_tool_calls_total{status!="success"}`
2. 延迟在哪一段：`oncall_llm_call_latency_seconds` vs `oncall_tool_latency_seconds` vs 总耗时
3. 修复是否有效：修复前后的 `oncall_llm_retries_total` 增速与 `http_server_requests` 错误率对比

## 遗留

- [ ] 告警阈值与实际告警通道（Prometheus Server + Alertmanager 部署）随云服务器一起做；
  本报告完成的是"可被抓取"这一前置条件。
- [ ] `test` 任务的 Windows 环境问题（中文路径）待在有干净 CI 环境时修复。
