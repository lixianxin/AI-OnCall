# Baseline 评测与失败样本分析报告

> 2026-09-07 运行 · 数据集 `eval/datasets/diagnosis_v1.jsonl`（200 条）· 服务 `AI-OnCall ai-service`（本机 8081）
> 结果文件：`eval/results/baseline.json` · 指标快照：`eval/results/metrics_snapshot_baseline.json`
> 2026-09-08 **口径修正**：`baseline.json` 已按新口径重聚合（per-case 原始数据未变，见
> `results/baseline_raw_20260907_archive.json`）；口径定义与 `evaluate.py` 同步。
> 2026-09-08 **复现认证**：同数据集第二次独立回放 `results/rerun_20260908.json`，核心指标 Delta 全 ±0（见文末）。

---

## 〇、运行上下文（先于一切指标说清楚）

本次 baseline 是在 **DeepSeek API 断供（402 Insufficient Balance）** 期间跑完的：

- `llm_error_rate = 1.0`：200/200 条回放均收到 SSE `error` 事件（content 生成阶段失败）。
- 但 **Intent / Tool 判定不依赖本次断供的 LLM**——编排器走本地关键词规则路由
  （`classifyIntent` → intent→tool 1:1），因此这两类指标在 402 下仍有效，
  语义是**「本地路由 + 工具治理链路（降级路径）」的成绩，不是「LLM Agent 全链路」的成绩**。
- Root Cause / Diagnosis Pass / Token Usage 依赖 LLM 输出，**本次无有效数据**。

**结论：本次数字全部是 degraded-mode（降级模式）baseline；全链路指标必须等 LLM 恢复后重跑再冻结。**

## 一、Baseline 结果（真实运行产出，口径 2026-09-08 修正）

| 指标 | 数值 | 说明 |
| --- | ---: | --- |
| Intent Accuracy | **92.5%**（185/200） | 本地关键词规则路由（诊断类 15 条误判为闲聊） |
| Tool Selection Accuracy | **91.7%**（165/180） | 分母=应调真实工具的 180 条；analyze_log 105/120、search 30/30、generate 30/30 |
| No-Tool Avoidance | **100%**（20/20） | 闲聊样本 0 条误触发工具（独立口径，不混入上者分母） |
| 组合路由 Accuracy（含规避） | **92.5%**（185/200） | = 165 工具任务正确 + 20 规避正确；旧口径的等价换算 |
| Root Cause Accuracy | **N/A** | 120/120 诊断样本全遇 LLM 402，无有效诊断输出 → 语义 N/A，**不是模型能力 0%** |
| Diagnosis Pass Rate | N/A | 同上，随 LLM 恢复重跑 |
| Fallback Rate | **39.0%**（78/200）→ 9/8 复现 38.0%（76/200） | 工具调用以治理终态 `failed` 收尾的样本占比（含限流/失败） |
| 限流拦截 | 9/8 **差值法：76 次**（analyze_log 65 + generate 11） | ✅ 回放前后 `/metrics` 快照差值 = 本次回放专属；与 per-case failed 76 完全对账；9/7 的 155 是服务生命周期累计（含冒烟），仅历史参考 |
| First Event Avg / P99 | **28.0–29.6ms / 45.0–105.3ms** | 两次回放区间；SSE 编排链路首包（intent 事件）延迟，**不含 LLM 推理** |
| Avg / P95 / P99 Latency | ~~不含 LLM~~ ❌ | **含 LLM 调用失败等待**（402 ~400ms/条、个别超时数秒），受断供污染，**不作为有效性能数据** |
| Token Usage | 空 | `DeepSeekClient` 已接入 usage 解析；402 无 usage 返回 → 待 LLM 可用出数 |

## 二、失败样本逐类归因

### ① 意图误分类 15 条 —— 全部是「诊断类无错误关键词」
- 模式：expected=ERROR_DIAGNOSIS → actual=GENERAL_CHAT，如 diag_018/019/021/042/043/045/066/069/090/115…
- 特征：输入为自然语言描述，不含 错误/异常/失败/exception/npe/超时/timeout/黑屏/ssl 任一关键词 → 规则路由兜底到闲聊。
- 归因：**纯规则路由的天花板**，正是 v2 hard 档「0 关键词泄漏」要逼出的实验点。
- 改进方向（下一轮 Before/After 的实验点）：Embedding 相似度兜底路由 或 低置信度时 LLM 路由（服务恢复后可测）。

### ② 工具选择失败 15 条 = 同一批意图误联动的连带失败
- 混淆矩阵只有一类：analyze_log → 无工具事件（15 条，即误判为闲聊后跳过工具）。
- 本版本意图与工具选型是 1:1 映射（架构事实），故 Tool Selection 失败数与 Intent 失败数一致，面试可如实讲。

### ③ Fallback 39.0%（9/7）→ 38.0%（76/200，9/8 复现）= 治理层真实工作
- 9/8 rerun 的 per-case：`done 89 / failed 76 / 无工具事件 35`。
- **归因闭环**：9/8 用差值法（回放前后 `/metrics` 差值）测得回放专属 `rate_limited = 76` 次
  （analyze_log 65 + generate 11），与 per-case `failed 76` **一一对应** → 治理终态样本几乎全部可归因于限流。
- 9/7 报告的 155 次是 `/metrics` 自服务启动以来的累计值（含冒烟），**不可表述为「本次回放拦截 155 次」**，仅作历史参考。
- 待改进：正式实验加 `--throttle 3.0`（analyze_log 20/min → 每条 ≥3s）以隔离限流变量。

### ④ LLM 断供 100%（120/120 诊断类无 content）
- 证据：`DeepSeek API error: status=402 Insufficient Balance`。
- 连带：Root Cause / Diagnosis Pass / Token Usage 全部无有效值。

## 三、本轮评测真跑出来的 bug（均已修复）

| # | bug | 根因 | 修复 |
| --- | --- | --- | --- |
| 1 | **SSE tool 事件 status 恒为 `done`** | 编排器硬编码 `"done"`，治理结果对客户端不可见 | 改为透传 `toolResult.getStatus()` |
| 2 | **system prompt 中文乱码**（`銆愭渶杩?` 等） | `Conversation.java` 历史编码事故（UTF-8→GBK 错编）污染每次 LLM 调用 | 4 处字面量修复 |
| 3 | **评测把「闲聊无工具」计为失败** | 编排器对 GENERAL_CHAT 不发 tool 事件，actual=None 与 "none" 不匹配 | 归一化以期望值为准（None→none），与 intent 判定解耦 |
| 4 | **`args.throttle` 未定义（evaluate.py 潜在崩溃）** | argparse 未声明该参数但 main 引用了它 | 补 `--throttle`（默认 0，建议 3.0） |
| 5 | **v2 分档统计恒为空** | per-case 结果未携带 `difficulty` 字段 | `evaluate_case` 透传 difficulty |

> 口径修正工具：`eval/recheck_baseline_caliber.py` 可从 per_case 重算并断言与汇总文件一致，防口径漂移。
> 审计增强：`evaluate.py` 现内置回放前后 `/metrics` 差值（`metrics_delta` 字段），
> 「累计口径」自动升级为「本次回放专属」计数（2026-09-08）。

## 四、复现认证（2026-09-08，同数据集第二次独立回放）

协议：`diagnosis_v1.jsonl` 200 条 · 同一服务实例 · 无 throttle（复刻 9/7 条件）· `results/rerun_20260908.json`

```
                  Before(9/7)    After(9/8)   Delta
Intent Accuracy       92.5%        92.5%        ±0
Tool Selection       91.67%       91.67%        ±0
No-Tool Avoidance    100.0%       100.0%        ±0
Routing (Total)       92.5%        92.5%        ±0
Fallback Rate         39.0%        38.0%       −1.0
First Event P99       45.0ms      105.3ms     波动（均值 28→29.6ms 稳定）
```

**结论**：质量指标两次独立回放 Delta 全 ±0 → 可复现性成立；
Fallback ±1 与首事件 P99 波动来自「限流窗口对齐 / 运行瞬态」→ 这两类不作为简历主数字（口径纪律，非掩盖）。

## 五、给简历的结论（冻结，2026-09-08）

- **可写（必须带限定语）**：
  - **本地规则路由 Tool Selection Accuracy 91.7%（165/180）**（主指标），200 条故障回放，注明 LLM 断供期间（降级路径）测得；
  - 意图路由 92.5%（185/200）、闲聊 No-Tool Avoidance 100%（20/20），组合路由 92.5%（185/200）作补充口径；
  - SSE 编排链路首事件 **P99 45–105ms / 均值 28–30ms（不含 LLM 推理）**（两次运行区间）。
- **仅面试叙事、不进简历正文**：限流治理真实生效（9/8 差值法回放专属 76 次，与 per-case failed 76 对账）；「限流阈值 × 回放吞吐耦合」是评测方法论发现；「规则路由在自然语言故障描述上的关键词依赖」已由 Semantic Fallback 受控实验量化解决（见下节六）。
- **不可写**：Root Cause Accuracy（N/A，等 LLM 恢复）；Fallback Rate 38–39%（限流+402 环境产物）；全链路 Avg/P95/P99（受 402 污染）；QPS/P99 压测（未做）；15K→4K（未做）。
- **口径红线**：任何一次重跑后，必须 `recheck_baseline_caliber.py` 对账，再更新冻结表与 baseline.json——「表 / 结果文件 / evaluate.py」三处永远同源。

---

## 六、Semantic Fallback Before/After（2026-09-08 完成）

> 唯一产品改动 = Intent Router；工具 / 治理链 / Prompt / 评测脚本 / 数据集全部冻结。

### 6.1 问题（v2 分层压力集量化）

`diagnosis_v2.jsonl` 的 hard 档刻意避开全部 9 个诊断关键词；在旧 Router（纯 Keyword Fast Path）下
**120 条诊断样本 65 条被误送**：45 → GENERAL_CHAT（无关键词的自然语言故障：端口被拒/服务退出/写不进去/画面全黑…），
20 → CODE_GENERATION（**歧义词「生成」抢先命中**："AI 回答生成一半就停了"、"生成功能时好时坏"…）。
v2 全量 Before：Tool Selection **63.89%（115/180）**、Intent 67.5%——比 v1 的 91.7% 更彻底暴露关键词天花板。

### 6.2 方案与实现

Keyword Fast Path 未命中时启用 **Semantic Fallback**（不改变关键词优先级）：
`SemanticFallbackRouter.java` = 纯 JDK 离线确定性组件，CJK unigram+bigram+ASCII token 三元特征，
IDF 按 4 意图原型语料降权公共字（unigram 再降权 0.4），度量 = **加权包含度**（查询特征被该意图原型覆盖的比例），
阈值 0.30。原型措辞独立编写（不取自数据集模板防泄漏）；阈值/降权在独立 DEV 集（29 句）冻结，不在评测集上迭代。
配套：`build.gradle` 强制 UTF-8 编译（防 Windows GBK 中文乱码）、`SemanticFallbackRouterTest.java` 6 用例
（自然故障→诊断 / 闲聊零误升 / 代码/协议意图 / 空输入 / 防跨意图串扰 / 确定性），JUnit 共 **15/15**。

### 6.3 结果（同集真实回放 + 离线预演对照）

| 指标 | 数据集 | Before | After | Delta | 校验 |
| --- | --- | ---: | ---: | ---: | --- |
| Tool Selection | v2 压力集 | 63.89%（115/180） | **88.33%（159/180）** | **+24.44** | 与离线预演 88.33% 一致 |
| Intent Accuracy | v2 | 67.5% | 89.5% | +22.00 | |
| No-Tool Avoidance | v2 | 100%（20/20） | **100%（20/20）** | ±0 | 闲聊零误升 |
| Tool Selection | v1 通用集（回归） | 91.67%（165/180，冻结） | **99.44%（179/180）** | +7.77 | 主指标不回退 |
| Intent Accuracy | v1 | 92.5% | 99.5% | +7.00 | |
| 首事件 P99 | v2 | 47.1ms | 60.3ms | 运行瞬态，不入主指标 | |
| Fallback Rate | v2 | 22.5%（45/200） | 60.0%（120/200） | 限流窗口产物，不入主指标 | |

两结果文件 `recheck_baseline_caliber.py` 均 PASS；`evaluate.py` 已修复 difficulty 透传 bug
（此前 per-case 缺字段，v1 无 difficulty 掩盖；v2 分档由此前恒空变为可用）。

### 6.4 残差分析（实验边界，如实记录）

- v2 After 仅剩 **21 条**误判 = 20 条 analyze_log→generate（全部含歧义词「生成」，关键词误命中而非 miss，
  **超出本次实验范围** → 下一点：关键词歧义仲裁）+ 1 条边界短样本（diag_108，无足够语义信号 → chat）。
- v1 After 仅剩 **1 条**（diag_093）。
- 即：65 条关键词依赖误判中，语义兜底救回 44 条；剩余 20 条属「兼类词误命中」，是规则路由的**第二类缺陷**，
  需 Keyword↔Semantic 二次仲裁（新一轮实验点，2026-09-08 已登记冻结表）。

### 6.5 实验结论

同一数据集、单一变量（Router）、统一评测脚本下：**v2 压力集 Tool Selection 63.89% → 88.33%（+24.44pt）**，
v1 通用集 **不回退且提升至 99.44%**，闲聊规避保持 100% → 「工程改动 → 指标变化」因果链成立。
语义兜底为**离线字符 n-gram + IDF 加权包含度**（无外部模型/LLM/网络），表述时如实说明，不冒称 embedding API。
Root Cause / Diagnosis Pass / Token / 全链路延迟仍 N/A —— 等 DeepSeek 恢复后按 v2 + `--throttle 3.0` 全链路复现并冻结真实值。

---

## 七、P0 真实 LLM 全链路（2026-09-08，GLM-4-Flash 实测）

> DeepSeek 持续 402 期间，用户提供智谱 key，仅改环境变量切 `glm-4-flash`（OpenAI 兼容协议，零代码改动），
> 打通 LLM Diagnostic Loop 并冻结**首次真实值**。本节数字一律标注「GLM-4-Flash 实测」，与 DeepSeek 叙事分开。

### 7.1 运行条件

- 数据集 `diagnosis_v2.jsonl` 200 条 × 真实 LLM × `--throttle 3.0`，47min，**llm_error 0**。
- 服务：当前 jar（含乱码修复），`/health` UP，4 工具注册；`recheck_baseline_caliber.py` PASS。
- 结果文件：`results/p0_glm_20260908.json`（per_case 全留痕）；`latest.json` 同步。

### 7.2 结果（首次真实冻结）

| 指标 | 值 | 备注 |
| --- | ---: | --- |
| Intent Accuracy | 89.5%（179/200） | 与路由实验冻结值一致 → 路由确定性、LLM 无关 |
| Tool Selection | 88.33%（159/180） | 与 v2 After 冻结值一致 → 真实 LLM 全链路不退化 |
| No-Tool Avoidance | 100%（20/20） | 一致 |
| **Root Cause Accuracy** | **95.0%（114/120）** | llm_error 0；判定 = 标签字面量 OR `root_keywords` |
| **Diagnosis Pass Rate** | **77.5%（93/120）** | 失败分解 = 21 intent（20「生成」兼类 + 1 短样本）+ 6 root 真性未命中 + 0 tool |
| Token Usage | prompt 117,869 / completion 59,011 | 200 条全覆盖（include_usage 回传） |
| 全链路延迟 | Avg 14,109ms / P95 25,377ms / P99 29,213ms | LLM 上游真实耗时（免费档），供应商标注 |
| SSE 首事件 | avg 33.4ms / P99 61.4ms | 不含 LLM，与前两次回放同区间 |

分档（by_difficulty）：easy 60（root 100%）/ normal 100（root 100%）/ hard 40（root 85%，intent 75%）——
**hard 档是剩余错误集中地**，与 v2 构造意图一致（hard = 无关键词泄漏 + 自然语言故障）。

### 7.3 打通全链路同步修的 3 个工程 bug（不动 Router / 数据集 / 冻结值）

1. **`evaluate.py` 成功路径挂死**：服务端 `done` 事件后持续发 `:keepalive` 永不关连接 → 评测器等 EOF 永久阻塞
   （402 时代 error 路径会关连接所以从未暴露）。修复：读到 `event:done` 即 break。
2. **`Conversation.java:136` 源码乱码**：字面量「鐢ㄦ埛」=「用户」编码事故残留，每次请求把乱码角色标签喂给 LLM。
   修复 + 全仓 java 同类排查 0。
3. **Root Cause 假阴性**：GLM 用中文转述根因、不回显英文标签（SQLException 案例答对但子串不匹配 → 0%）。
   修复：判定 = 期望标签子串 OR 数据集自带 `root_keywords`（ground-truth 别名，非自造），per-case 留
   `root_literal_ok` / `root_alias_ok`。改判定前的 N/A 无冻结值被破坏，安全。

### 7.4 结论

- 首个完整技术闭环成型：**LLM → diagnosis → Tool → evidence → Root Cause → repair 全链路真实跑通**，
  5 大卖点全部有可复现证据（Tool Runtime / 可观测 / Eval Pipeline / Semantic Routing / LLM Diagnostic Loop）。
- 路由与推理解耦被实测佐证：真实 LLM 在场下路由指标与离线冻结值**逐位一致**。
- 剩余残差（21 intent + 6 root）与路由实验残差同源（「生成」兼类），是已登记的下一实验点，非本轮回溯目标。
- **P1 JMeter 前置条件已满足**；压测数字须标注供应商 + throttle。

---

## 八、P1 JMeter 压测（2026-09-08，GLM-4-Flash 实测）

> 脚本：`ai-service/loadtest/chat-stream-load.jmx`（已修复：HeaderManager 后补空 `<hashTree/>` 结构错误）。
> 归档：`ai-service/loadtest/results/p1_glm_20260908/`（results.jtl + summary.json + metrics_before/after.json）。

### 8.1 运行条件与口径

- **2 并发 × 120s**（ramp 5s），默认闲聊消息（纯 LLM 链路、不触发工具限流），服务 :8081（GLM-4-Flash）。
- metrics 差值法：窗口内工具调用 **Δ=0**、token Δ=prompt 13,152 / completion 4,063（≈274/85 per request）→ 负载纯度验证通过。
- 并发上限依据：**5 并发即触发上游 429（code 1302 账户速率限制）** → 免费档可持续并发 ~2–4；高于此的数字是上游限流产物，不冻结。

### 8.2 结果（48 样本，0 错误，0.00%）

| 指标 | 值 |
| --- | ---: |
| 吞吐 | **0.40 req/s**（2 并发上限内） |
| Avg / Min | 4,970ms / 709ms |
| P50 / P90 | 4,410ms / 9,054ms |
| **P95 / P99** | **13,154ms / 16,926ms** |
| 错误率 | 0.0%（0/48） |

### 8.3 压测暴露的第 4 个工程 bug（服务端 SSE 连接永不关闭）

`ChatController.streamChat` 用 `Flux.merge(mainStream, heartbeat)` 组装 SSE；heartbeat 为无限
`Flux.interval`（15s 一条 `:keepalive` 注释）。mainStream 发完 `done` 后 **heartbeat 仍无限续命 → merged Flux
永不 complete → 连接永不关闭**。evaluate.py 此前已客户端规避（读到 done 即 break），但规范客户端（JMeter/curl）
等 EOF/读超时：JMeter 首轮压测 2 线程全部卡死在首次响应读取上（keepalive 不断重置读超时，永不结束）。

**修复**：`heartbeat.takeUntilOther(mainStream)` —— 主流完成/出错即终止 heartbeat，merged Flux 在 `done` 后
正常 complete → 连接按 SSE 规范关闭。curl 实测：done 后立即 EOF（此前无限挂起）。同时修复同文件 PARSE_ERROR
事件体历史乱码（「闁荤姴娲…」→「请求解析失败」）。**不影响路由/指标口径**：评测在 done 事件层截断，连接关闭语义
与任何冻结值无关。

### 8.4 P1 结论

- 压测链路真实可用（脚本 + JMeter + 差值法闭环），0 错误下 48 样本完整。
- **容量归因**：2 并发下吞吐 0.40 req/s、P95/P99 13.2s/16.9s，**瓶颈全部在上游 LLM 供应商（免费档限流 + 慢生成），
  Runtime/编排层未触顶**（0 错误、无本地超时）。与历史 ~200 QPS / 4.2s P99 无任何可比性，禁止混同。
- DeepSeek 恢复后可用同脚本重压（README 前置①），届时补「DeepSeek 实测」标签。
