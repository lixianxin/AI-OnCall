# 简历声明对账 + 分级降级链路补全（2026-10-25）

## 1. 简历声明逐条对账结果

对《李显鑫-17681815141-AI Agent.pdf》"LLM Agent Runtime & AIOps 系统"段全部声明与仓库实现核对：

| # | 简历声明 | 核对结果 | 证据 |
|---|---|---|---|
| 1 | 六段诊断链路 + Tool Registry/Executor（参数校验、限流、超时重试降级） | ✅ 已实现 | ToolRegistry/ToolExecutor/ToolSpec，4 工具注册日志 |
| 2 | 接入 4 个运维工具 | ✅ AnalyzeLog / Search / ReadDoc / GenerateCode | 服务启动日志 4 tools |
| 3 | **写操作负面清单与人工确认** | ❌ **对账时不存在** → 本轮补实现（见 §2.3） | VerifyWriteGuard 4/4 |
| 4 | 三层记忆（存 20/注入 10、异步摘要、工作记忆） | ✅ MAX_HISTORY=20、getRecentMessages(10)、triggerSummary 异步 | Conversation.java:24-26 |
| 5 | 200-case 回放评测（120 诊断/30 文档/30 代码/20 闲聊） | ✅ diagnosis_v2 数据集构成一致 | eval/datasets/diagnosis_v2.jsonl |
| 6 | Root Cause 95.0%（114/120）、Diagnosis Pass 77.5%（93/120） | ✅ 与 latest.json/p0_glm_20260908.json 完全一致（真实 GLM，diag_llm_error=0） | eval/results/ |
| 7 | n-gram+IDF 兜底，工具选择 63.89%→88.33% | ✅ v2_before/v2_after 快照 + 报告 | eval/results/v2_*.json |
| 8 | trace_id/MDC 全链路 + SSE + Prometheus 指标 | ✅（Prometheus 为 2026-10-25 补齐） | docs/evidence/2026-10-25-prometheus-metrics.md |
| 9 | **分级降级链路（重试→备用模型→返回部分诊断结论）** | ❌ **对账时只有重试，备用模型与部分诊断均不存在** → 本轮补实现（见 §2.1/§2.2） | docs/evidence/degrade_*.sse |

**结论**：9 条声明中 7 条有完整实现与数据支撑；2 条（#3、#9）属于"简历先行、实现缺位"，本轮已全部补齐并验证。

## 2. 本轮补实现

### 2.1 备用模型降级（DeepSeekClient，降级第二级）

- 新配置：`deepseek.backup.base-url / model / api-key`（留空=未启用；api-key 留空沿用主 key）
- 触发条件：主链路在任何输出之前失败（429/5xx 重试耗尽、终态 4xx、连接失败重试耗尽）
- 语义：切换备用端点重走一次（独立预算、最多 2 次尝试，防降级放大）；备用再失败交给第三级
- 指标：`oncall_llm_degrades_total{stage="backup_model"}`

### 2.2 部分诊断结论（DocumentAgentOrchestrator，降级第三级）

- 主备均失败且本轮已回收工具证据 → 返回**标注降级的部分诊断结论**（`【降级提示】模型服务暂不可用…`+ 证据前 800 字符），而不是裸 error
- 无证据可回收时维持 error 终态（不编造结论）
- 指标：`oncall_llm_degrades_total{stage="partial_diagnosis"}`

### 2.3 写操作负面清单 + 人工确认（ToolSpec/ToolRegistry/ToolExecutor/ToolContext）

- `ToolSpec.writeOperation` 标志（默认 false，旧构造器兼容）
- `ToolRegistry.WRITE_TOOL_DENYLIST`（9 项高危写操作）：命中名单的写工具**拒绝注册**（启动即失败）
- `ToolContext.writeConfirmed` + `ToolExecutor` 执行前强制门：写操作未确认 → `confirmation_required`（独立状态，非 failed，便于调用方区分拒绝原因并引导补确认）

## 3. 验证结果（全部 2026-10-25 实测）

### 3.1 写操作治理（离线验证，绕过本机损坏的 test worker）

`VerifyWriteGuard`（独立 classpath 编译运行）：**4/4 PASS**
- T1 高危写工具注册被拦截（`restart_service` → IllegalArgumentException）
- T2 合法写工具未确认 → `confirmation_required`，未执行
- T3 确认后正常执行 → success
- T4 只读工具不受确认门影响（回归保护）

### 3.2 降级链路注入测试（mock 双实例，无真实 key）

| 场景 | 注入 | 结果 |
|---|---|---|
| S-A 主挂备活 | 9999=always429, 9998=ok | 主链路 3 次请求耗尽（llm_retries{429}=2）→ 降级 backup_model=1 → 备用 1 次成功 → 完整事件链 done，e2e 3.28s |
| S-B 主备均挂 | 9999/9998=always429 | 备用 2 次尝试耗尽 → partial_diagnosis=1 → 客户端收到【降级提示】+ 工具证据 + done（非裸 error） |

三方印证：mock 请求计数 = JSON /metrics = Prometheus 抓取（`oncall_llm_degrades_total{stage="backup_model"} 2.0, {stage="partial_diagnosis"} 1.0`）。

原始证据：`degrade_a_backup_ok.sse`、`degrade_b_partial.sse`。

### 3.3 路由回归

diagnosis_v2 全量 200 条（throttle 2.5）：结果见 `eval/results/degrade_chain_regression_20261025.json.json`，预期与基线逐位一致（路由判定在 LLM 之前，降级改动不触及）。

## 4. 口径备注

- 降级链路验证使用 mock 上游：证明的是**服务端降级机制的正确性**（切换时机、次数、终态语义、指标），不是真实双模型切换的业务效果——后者需真实备用模型 key，属遗留项；
- 简历可如实表述为"设计并实现三级降级链路，故障注入验证切换语义"，不可写"双模型生产验证"。
