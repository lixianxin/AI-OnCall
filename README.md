# AI-OnCall — 智能运维诊断 Agent 平台

面向应用运维故障诊断场景的 LLM Agent 工程化实践。目标不是「让大模型调通接口」，而是把 Agent 做成
**可治理（Tool Runtime）、可评测（Evaluation Pipeline）、可观测（trace_id / metrics）** 的应用系统。

## 项目背景

LLM Agent 落地运维场景存在四类工程障碍，本项目逐项对应解决：

| 障碍 | 具体表现 | 对应模块 |
| --- | --- | --- |
| 工具调用不可控 | 多工具直接暴露给 LLM，参数异常、超时、重试、失败处理逻辑分散在各 Tool 内部 | Tool Runtime（`ToolRegistry` + `ToolExecutor`） |
| 迭代无法回归 | 改 Prompt / Tool / Router 后缺少固定基准，无法判断是否引入回归 | Evaluation Pipeline（200-case 回放） |
| 关键词路由泛化差 | 自然语言故障描述关键词 0 命中即被误路由 | Semantic Fallback Router（离线 n-gram + IDF） |
| 异步链路难定位 | 多工具异步执行后 Trace 丢失，问题无法定位到具体调用 | Observability（`trace_id` + MDC + SSE 事件） |

## 仓库结构

```text
AI-OnCall/
├── ai-service/                  # Agent Runtime 服务（Spring Boot WebFlux）
│   ├── src/main/java/com/oncall/ai/
│   │   ├── controller/          # SSE 流式对话 / 指标端点
│   │   └── service/             # ToolRegistry / ToolExecutor / SemanticFallbackRouter / AgentOrchestrator
│   ├── src/test/java/           # JUnit（ToolExecutor 9 + SemanticFallbackRouter 6）
│   ├── docs/                    # 知识库语料与 API / SSE 协议文档
│   └── loadtest/                # JMeter 压测脚本与结果
├── eval/                        # 离线回放评测闭环（Python）
│   ├── evaluate.py              # 回放评测主程序
│   ├── compare.py               # Before/After 指标差值对比
│   ├── datasets/                # diagnosis_v1 / v2（各 200 条，固定种子可复现）
│   └── results/                 # 冻结的评测结果
├── client-android/              # Android 客户端（Compose，基于开源 chat 工程改造）
├── docs/                        # 设计 / 答辩文档（含历史版本）
├── tools/legacy/                # 早期一次性修补脚本（归档，不参与构建）
├── docker-compose.yml           # 一键起服务
└── 数字冻结表.md                 # 指标口径冻结记录
```

## 核心能力

- **Tool Runtime 治理链**：业务工具只实现 `Tool#execute`，注册 / 参数校验 / 固定窗口限流 / 超时（CompletableFuture）/
  重试 / 降级由 `ToolExecutor` 统一承担；超时为终态不重试，避免重试挂死的工具继续消耗时间预算；Tool 状态统一透传 SSE。
- **LLM Diagnostic Loop**：故障理解 → 工具选择 → 工具执行 → 证据回收 → 根因判断 → 修复建议，全流程可追踪。
- **Semantic Fallback Router**：关键词快路径未命中时兜底判定。特征为 CJK unigram + bigram + ASCII token 三元特征，
  权重按意图原型语料 IDF 计算（unigram 降权 ×0.4），度量为加权包含度 `containment`，阈值 0.30 于独立 DEV 集冻结；
  纯 JDK 实现，离线、确定性、无外部模型依赖，保证实验单一变量。
- **Evaluation Pipeline**：样本按任务类型分类，并进一步按 easy / normal / hard 三档难度分层；统一记录期望工具、
  实际路由与执行结果，支持指标重聚合、Before/After 对比与 `/metrics` 差值校验。
- **可观测性**：`trace_id` 贯穿请求 → ToolExecutor → SSE 事件，MDC 上下文拷贝解决异步线程 Trace 丢失。

## 评测结果（GLM-4-Flash 实测，2026-09-08 冻结）

| 指标 | 结果 | 数据集 / 说明 |
| --- | --- | --- |
| Tool Selection Accuracy | **63.89% → 88.33%（+24.44pt）** | v2 分层集，冻结变量仅替换路由层 |
| v1 Tool Selection Accuracy | **99.44%** | diagnosis_v1（200 条） |
| No-Tool 准确率 | **100%**（未回退） | v2 |
| Root Cause Accuracy | **95.0%（114/120）** | P0 真实 LLM 全链路；基于预定义 ground truth 的自动规则评测（标签/别名匹配） |
| Diagnosis Pass Rate | **77.5%（93/120）** | 端到端口径 = 意图 AND 工具 AND 根因三关全过 |

> **链路稳定性验证（非性能结论）**：JMeter 2 线程 / 120s，48 请求 **0 错误**、P99 16.9s。
> 该结果用于验证链路稳定性与容量瓶颈归属，端到端延迟主要受上游 LLM 推理与**免费档并发上限**影响
> （压测期间触发供应商限流），不构成服务端吞吐结论。详见 `ai-service/loadtest/results/`。

> 口径与 `数字冻结表.md` 一致；评测脚本、数据集与结果文件随仓库提供，可按下方命令复现。

## 已知边界（Known Limitations）

主动记录，避免被当作"生产就绪"过度解读：

| 边界 | 现状 | 若要生产化 |
| --- | --- | --- |
| Tool 执行线程池 | 使用 `CompletableFuture.supplyAsync` 默认的 `ForkJoinPool.commonPool` | 当前工具为本地检索/文档读取，延迟可控且有 `timeoutMs` 兜底；若接入数据库、HTTP、SSH 等阻塞型 IO Tool，应改为**专用 ExecutorService**，避免占用公共池 |
| 限流策略 | 滑动时间窗口日志，只卡上限、不平滑速率 | 需要平滑时替换为令牌桶（`ToolExecutor` 是唯一入口，替换成本低） |
| 评测自动化程度 | Root Cause / Pass 为**基于预定义 ground truth 的自动规则评测**（标签/别名匹配） | 自动规则测一致性与回归，替代不了人工质量评审；高价值场景可叠加人工抽检 |
| 评测集代表性 | 200-case 为离线构造，含刻意构造的零关键词泄漏 hard 集 | 用于回归门禁与鲁棒性压力测试，**不代表线上分布**；线上效果需另做流量采样 |
| 吞吐结论 | 未做付费档/本地模型压测 | 现压测结果为链路稳定性验证，瓶颈归因于 LLM 供应商免费档并发上限 |

## 快速开始

### 方式一：Docker Compose

```bash
cp .env.example .env        # 填入 DEEPSEEK_API_KEY
docker compose up --build
curl http://localhost:8081/health
```

### 方式二：本地运行

```bash
cd ai-service
./gradlew bootRun           # 默认端口 8081
```

LLM 走 OpenAI 兼容接口，通过环境变量切换供应商（`DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL`）。
上表结果为 GLM-4-Flash 实测（`DEEPSEEK_BASE_URL` 指向兼容端点）。

## 离线评测闭环

```bash
cd eval
python evaluate.py --base http://localhost:8081 --dataset datasets/diagnosis_v2.jsonl --save after_v2
python compare.py results/baseline.json results/after_v2.json    # Before/After 指标差值
```

数据集由固定种子生成、结果落盘可对比，详细说明见 `eval/README.md`。

## 测试与压测

```bash
cd ai-service
./gradlew test                                                  # 15 项 JUnit
jmeter -n -t loadtest/chat-stream-load.jmx -l loadtest/results/run.jtl -e -o loadtest/results/report
```

## 主要端点

| 端点 | 说明 |
| --- | --- |
| `POST /api/chat/stream` | SSE 流式对话（事件：intent / tool / content / done / error） |
| `GET /health` | 健康检查 |
| `GET /metrics` | Prometheus 指标 |
| `GET /api/chat/metrics` | 检索命中率与延迟快照 |

## 文档索引

- `eval/README.md` — 评测闭环与数据集说明
- `ai-service/docs/` — API 与 SSE 协议、知识库语料
- `docs/` — 设计方案与答辩材料（含历史版本）
- `数字冻结表.md` — 指标口径冻结记录
- `tools/legacy/` — 早期一次性修补脚本（归档，仅供追溯）

---

个人项目：用于 AI Agent 应用开发方向的工程实践与面试演示。
