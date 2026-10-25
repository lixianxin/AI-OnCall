# 工程加固记录：2026-10-25（P0-2 / P0-3 / P1-2 / P1-3）

> 本文档是本轮改造的证据产物索引。每项改动标注验证状态：✅ 已验证 / ⏳ 待运行时验证。

## 1. P0-2 仓库卫生

| 改动 | 说明 | 状态 |
|---|---|---|
| 删除 `client-android/compose_chat-master/doc/key.jks` | 签名密钥误提交，移除追踪；`.gitignore` 补 `*.jks / *.keystore / *.p12 / *.pem / .env` | ✅ 文件已删。⚠️ git 历史仍含该文件（早期 commit），如含真实签名信息需 history rewrite 或作废该 keystore |
| 根目录 30+ 个 `fix*.py` / `check.py` 移入 `scripts/archive/` | 与 README "legacy 已归档" 声明对齐 | ✅ |
| 新增 `LICENSE`（MIT） | 两个核心仓库此前均无 License | ✅ |
| 修复 `application.yml` 乱码注释 | GBK→UTF-8 编码问题导致的 mojibake | ✅ |
| `eval/README.md` 状态更新 | 原文称"真实评测未跑"，与 `results/` 中 20260908 真实快照矛盾；已更新为事实状态并列出快照清单 | ✅ |

## 2. P0-3 压测吞吐量口径修复

- **问题**：`p1_glm_20260908/summary.json` 的 `throughput_reqs=0.0`，与 JMeter report `statistics.json` 的 0.3967 矛盾（一次性解析脚本字段错位）。
- **修复**：
  1. `summary.json` 按 `statistics.json` 勘误为 0.3967，附 `throughput_errata` 勘误说明与口径定义（全窗口平均吞吐）；
  2. 新增标准解析脚本 `loadtest/parse_summary.py`（吞吐 = 成功样本 / 全窗口；延迟仅统计成功样本；样本 <100 自动附 P99 置信度弱标注），作为今后 jtl 汇总唯一入口；
  3. `loadtest/README.md` 压测记录补充勘误条目。
- **验证**：✅ 脚本逻辑人工审查通过（线性插值百分位与 JMeter report 口径一致）；⏳ 下次压测时用脚本实际生成 summary 验证。

## 3. P1-2 消除重复检索

- **问题确认**：旧链路中 `SearchTool` 内部执行 `searchWithScores(query, 5)`（query=message、TOP_K=5、同一 DocumentStore、BM25 确定性打分），编排层 `DocumentAgentOrchestrator` 又用完全相同参数再搜一次；RAG debug 日志开启时还会第三次检索（top-3）。BM25 为确定性打分，同参数结果必然一致 → 纯冗余 I/O。
- **修复**：
  1. `ToolResult` 新增 `searchResults` 字段，`SearchTool`（含空结果分支）带回原始检索结果；
  2. 编排层优先复用 `toolResult.getSearchResults()`；工具未返回结果（降级/超时/非检索类工具，`searchResults=null`）时才补检索，与旧行为兼容；
  3. debug 日志复用 `rawResults.limit(3)`，第三次检索一并消除。
- **等价性依据**：`ToolContext(message, ...)` 构建 → `context.getQuery()==message`，与编排层原直查参数逐项相同。
- **验证**：✅ 参数等价性代码审查通过；⏳ 待固定测试集回归（eval 200-case，指标不得下降）+ 前后耗时对照（预期检索调用次数减半）。

## 4. P1-3 LLM 上游 429 分级退避重试

- **背景**：5 并发压测曾触发 GLM 免费档 429 限流；`DeepSeekClient` 原实现无任何重试。
- **实现语义**（`DeepSeekClient.java`）：

| 错误类型 | 行为 |
|---|---|
| `429` | 优先遵循 `Retry-After` 头（封顶 budget）；无头则指数退避 1s→2s→4s（封顶 8s） |
| `5xx` | 重试（尚未输出任何 token，安全） |
| 连接超时 / 连接失败（IOException） | 重试（请求未送达） |
| 其他 4xx（鉴权/余额/参数） | 终态，不重试 |
| 流式输出开始后的异常 | 不重试（客户端已收到部分文本，重放会重复），直接 error 由 SSE 错误事件兜底 |

- **两级保护**：最大尝试次数（`deepseek.retry.max-attempts`，默认 3 含首次）+ 总时间预算（`deepseek.retry.budget-ms`，默认 30s，所有重试共享）。
- **与 ToolExecutor 的关系**：本重试只针对 LLM 上游调用；ToolExecutor 重试针对工具执行，链路每环节只经过一层重试，不叠加。
- **配套**：`AgentMetrics.recordLlmRetry(reason)`（reason ∈ {429, 5xx, connect_error}，固定枚举无高基数风险），`/metrics` 输出 `llm_retries`；MDC traceId 跨线程传播（boundedElastic）。
- **验证**：✅ 编译验证（见 §5）；✅ mock 上游故障注入验证已完成（2026-10-25，见
  [2026-10-25-retry-injection.md](2026-10-25-retry-injection.md)：429×2 恢复 / 持续 429 终态 /
  连接失败重试 / 流式中途断开四场景全部按语义工作；
  **注入还发现并修复了"截断流被静默当正常完成"缺陷**——EOF 未收到 [DONE] 时旧实现向客户端发 done，
  现改为 error 终态不重试）。

## 5. 编译验证

- ✅ `./gradlew compileJava` **BUILD SUCCESSFUL**（2026-10-25，Gradle 9.5.1，JDK 21.0.10，Windows），
  覆盖本轮全部 Java 改动（DeepSeekClient / AgentMetrics / DocumentAgentOrchestrator / SearchTool / ToolResult）。
- 备注：本机首次构建时 `services.gradle.org` 直连超时，临时切换腾讯镜像下载 Gradle 发行包，
  `gradle-wrapper.properties` 已还原，未入库任何镜像改动。

## 遗留与下一步

1. **历史中的 key.jks**：确认 keystore 是否为真实签名密钥；是 → 作废重签 + 考虑 history rewrite；否 → 记录结论即可。
2. **P1-2 回归**：✅ 路由链路已回归（2026-10-25，mock 上游离线跑通 200 条，Intent/Tool/No-Tool/Fallback 与基线逐位一致，
   见 [2026-10-25-dedup-regression.md](2026-10-25-dedup-regression.md)）；
   ⏳ 根因指标（95.0）判定口径含 LLM 生成内容，离线 mock 不可验证，待真实 API key 重跑全量确认。
3. ~~**P1-3 注入测试**~~ ✅ 已完成（2026-10-25，[retry-injection 报告](2026-10-25-retry-injection.md)，含新缺陷发现与修复）。
4. **P1-1 / P1-4**：干净环境部署验收、多档压测——见任务清单。
