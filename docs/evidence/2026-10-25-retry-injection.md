# P1-3 LLM 上游故障注入验证报告（2026-10-25）

> 验证对象：`DeepSeekClient` 分级退避重试（commit bd0c210 引入）+ 本次发现的流截断缺陷修复。
> 方法：本地 mock 上游（`scripts/mock_llm_upstream.py`，OpenAI 兼容流式协议，场景运行时切换），
> `DEEPSEEK_BASE_URL` 指向 mock，不需要真实 API key。原始输出见 `retry-injection-raw/`。

## 环境与口径

| 项 | 值 |
|---|---|
| 被测服务 | ai-service（Spring Boot 3.4.5 / JDK 21.0.10 / Windows 本机） |
| 重试配置 | max-attempts=3（含首次）、budget-ms=30000、base-delay-ms=1000（application.yml 默认值） |
| mock 场景切换 | `POST /control?scenario=<name>`，请求计数 `GET /stats` |
| 测试请求 | `POST /api/chat/stream`，固定消息「服务器 CPU 使用率持续 95%，如何排查？」 |
| 延迟口径 | curl time_total，端到端（含退避等待） |

## 场景结果

### S1：429×2 后恢复（scenario=rate429x2）
| 项 | 结果 | 预期 | 判定 |
|---|---|---|---|
| mock 收到请求 | 3 次（429、429、200） | 3 | ✅ |
| 客户端结果 | 正常 content 流 + `done`，对用户透明 | 成功 | ✅ |
| 端到端耗时 | 3.26s | ≈1s(Retry-After)+2s(退避)+处理 | ✅ |
| 日志 | `attempt 1/3，1000ms 后重试`（遵循 Retry-After）→ `attempt 2/3，2000ms 后重试` | — | ✅ |

### S2：持续 429（scenario=always429）
| 项 | 结果 | 预期 | 判定 |
|---|---|---|---|
| mock 收到请求 | 3 次 | 3（重试耗尽） | ✅ |
| 客户端结果 | 干净 `event:error`（STREAM_ERROR），连接正常关闭 | error 终态 | ✅ |
| 端到端耗时 | 3.05s（未触及 30s 预算） | < budget | ✅ |
| 指标 | `llm_retries{"429": 4}`（S1 贡献 2 + S2 贡献 2） | — | ✅ |

### S3：流式中途断开（scenario=stream_then_break）——**发现缺陷并修复**
- **缺陷**：修复前，上游输出 2 个 token 后断开连接（EOF 无 `[DONE]`），旧实现把 EOF 当正常结束，
  客户端收到 **`done`**——半截答案被静默接受，用户无感知。（raw：`s3_stream_break.sse` 尾部为 done）
- **根因**：`DeepSeekClient` 流式读取循环以 `readLine()==null` 退出后无条件 `sink.complete()`，
  未校验 `[DONE]` 哨兵。
- **修复**：跟踪 `sawDone`；EOF 且未收到 `[DONE]` → `sink.error`（输出已开始，不重试，避免重复文本），
  由 SSE 错误事件机制兜底。
- **修复后复测**（raw：`s3fix_stream_break.sse`）：客户端收到 `event:error` ✅；mock 仍只收到 1 次请求
  （确认"输出已开始不重试"语义未破坏）✅。

### S4：连接失败（DEEPSEEK_BASE_URL 指向无服务端口 9998）
| 项 | 结果 | 预期 | 判定 |
|---|---|---|---|
| 重试行为 | 连接失败重试 2 次（`connect_error: 2`），1s+2s 退避 | 重试后退避升级 | ✅ |
| 客户端结果 | 干净 `event:error` 终态 | error 终态 | ✅ |
| 端到端耗时 | 3.23s | ≈3s 退避 | ✅ |

## 结论

1. 分级重试语义（429 遵循 Retry-After / 重试耗尽干净终态 / 连接失败可重试 / 流式输出开始后不重试）
   四场景全部按设计工作，指标计数（`llm_retries` 按 reason）与日志（traceId 全程贯通）可交叉印证。
2. 注入测试发现 1 个代码审查未发现的缺陷（截断静默当完成），已修复并回归验证——
   这是"故障注入"方法本身的价值证明，可写入面试故事。

## 遗留

- `[DONE]` 严格校验依赖上游遵守 OpenAI 兼容协议（正规 API 均满足）；若未来接入不发 `[DONE]` 的上游，需按 provider 适配。
- S2/S4 的 error 终态目前不区分错误码（统一 STREAM_ERROR），前端如需展示限流/断连差异，可后续把 reason 透传到 SSE 事件体。
