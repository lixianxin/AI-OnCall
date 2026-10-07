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

## 核心工作（以下四项均为个人独立完成）

- **智能诊断闭环**：面向 NPE、连接超时、SQL 异常等 8 类应用运维故障排查链路长的问题，将人工排查流程拆解为
  故障理解 → 工具选择 → 工具执行 → 证据回收 → 根因判断 → 修复建议的可追踪链路，4 个业务工具由 Agent 统一编排；
  `trace_id` 贯穿请求 → ToolExecutor → SSE 事件，MDC 上下文拷贝解决异步线程 Trace 丢失。
  P0 真实 LLM 全链路 120 条实测 Root Cause Accuracy **95.0%（114/120）**、Diagnosis Pass Rate **77.5%（93/120）**。
- **ToolRuntime 治理**：抽象 `ToolRegistry` + `ToolExecutor` 统一执行入口，业务工具只实现 `Tool#execute`；
  参数校验 / 滑动窗口日志限流 / 超时（CompletableFuture，超时为终态不重试）/ 重试 / 降级统一收口，Tool 状态透传 SSE。
  **15/15 JUnit 治理分支用例通过**（ToolExecutor 9 + SemanticFallbackRouter 6）。
- **评测体系**：构建 200-case 离线回放评测，覆盖 8 类根因，按任务类型分类 + easy（直接点名异常）/ normal（证据藏堆栈）/
  hard（零关键词纯症状）三档分层；统一记录期望工具、实际路由与执行结果，支持指标重聚合、Before/After 对比与
  `/metrics` 差值校验；固定种子生成、结果落盘可复现。
- **轻量路由兜底**：针对关键词快路径在自然语言故障描述下 0 命中即误路由的问题，构造零关键词泄漏 hard 集；
  以 CJK unigram + bigram + ASCII token 三元特征 + IDF 加权（unigram 降权 ×0.4）做加权包含度 `containment` 兜底，
  阈值 0.30 于独立 DEV 集冻结；纯 JDK 实现，离线、确定性、无外部模型依赖——冻结工具 / Prompt / 评测集，仅替换路由层。
  Tool Selection Accuracy **63.89% → 88.33%（+24.44pt）**，No-Tool 保持 **100%** 不回退。

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
