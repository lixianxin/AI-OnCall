# AI-OnCall SSE 压测（JMeter）

冻结表 QPS / P99(压测) 两行的正式数据来源。

## 前置条件（缺一不可，否则数字无意义）

1. **LLM 可用**（`/health` UP）。压测打的是「编排 + 真实 LLM」全链路，LLM 不可用/断供下跑出的吞吐/延迟不冻结。
   2026-09-08 首轮实测用 **GLM-4-Flash 免费档**（DeepSeek 402 期间），结果见 `results/p1_glm_20260908/`，
   冻结表 QPS/P99(压测) 两行；DeepSeek 恢复后可直接重压补「DeepSeek 实测」标签。
2. 服务运行在 `http://127.0.0.1:8081`（可用 `-Jhost/-Jport` 覆盖）。
3. 本机有 JMeter 5.x。CLI 示例（本机无 PATH 时）：
   `cd /d/tools/apache-jmeter-5.6.3/bin && java -jar ApacheJMeter.jar -n -t <jmx> ...`
4. **免费档并发天花板**：上游限流很紧，实测 **5 并发即触发 429（code 1302 账户速率限制）**。
   默认 20 并发参数只适用于不限流/高配额供应商；跑免费档必须 `-Jthreads=2`（实测 2 并发 48 样本 0 错误），
   先做并发校准（python 并发冒烟）再正式跑，防止 error 主导的无效数字。

## 运行

```bash
# 默认：20 并发、10s 爬坡、持续 120s、闲聊消息（纯 LLM 链路，不触发工具限流）
jmeter -n -t chat-stream-load.jmx -l results.jtl -e -o report/

# 示例：50 并发 60s、诊断类消息（混合负载，工具限流会介入）
jmeter -n -t chat-stream-load.jmx -Jthreads=50 -Jduration=60 \
       -Jmessage="线上服务报 null pointer 异常，怎么排查？" \
       -l results_diag.jtl -e -o report_diag/
```

跑完看 `report/index.html`（吞吐量 ≈ QPS、Avg/P95/P99 延迟）。
若无 GUI report，用仓库内标准脚本 `parse_summary.py` 汇总 jtl（吞吐/分位数唯一口径，勿再手写一次性脚本）：

```bash
python parse_summary.py results.jtl -o summary.json
```

## 压测记录

- **2026-10-25 并发冒烟（并发校准，mock 上游）**：10×3 / 20×3 / 50×2 共 190 条 SSE 流
  **100% 干净终结**（done，无挂死、无 5xx、无 ERROR 级日志）；50 并发下工具限流按设计降级
  （WARN 记录，客户端无感知）。脚本：`concurrency_smoke.py`（正式压测前的并发校准步骤）。
  **口径声明：mock 上游数字仅反映服务端并发行为（连接管理/编排/治理），不代表真实 LLM 延迟与
  吞吐，不冻结、不进简历。** 原始输出：`docs/evidence/smoke_*.json`。
- **2026-09-08 P1（GLM-4-Flash 实测）**：2 并发 × 120s 纯 LLM 闲聊链路 → 48 样本 0 错误，
  0.40 req/s，P50 4.4s / P90 9.1s / P95 13.2s / P99 16.9s；窗口内工具调用 Δ=0（纯度验证）。
  归档：`results/p1_glm_20260908/`。附带修复：`chat-stream-load.jmx` HeaderManager 缺空 `<hashTree/>`。
  服务端 SSE 在 `done` 后连接不关闭的 bug 由本次压测暴露并已修复（见评测报告 §8.3）。
  **勘误（2026-10-25）**：该目录 summary.json 的 `throughput_reqs` 原为 0.0（一次性解析脚本字段错位），
  已按 `report/statistics.json`（0.3967）勘误；样本 48 的 P99 置信度弱已在 summary 内标注。
  此后一律用 `parse_summary.py` 生成 summary，杜绝手工口径漂移。

## 口径纪律（务必遵守）

- **默认闲聊场景**测的是纯 LLM 全链路（无工具），QPS/P99 语义干净；工具治理类负载请单独跑
  诊断消息场景，并在 `/metrics` 观察限流——工具配额（analyze_log 20/min 等）会在高并发下被击穿，
  这是治理层在起作用，不是压测缺陷。
- **限流计数用差值法**：压测前与压测后各取一次 `/metrics`，`after - before` 才是本次压测的调用数。

```bash
curl -s http://127.0.0.1:8081/metrics > metrics_before.json
# ... 跑 jmeter ...
curl -s http://127.0.0.1:8081/metrics > metrics_after.json
```

- 结果冻结流程：把 `report/`（或 jtl）与两次 metrics 快照归档 → 更新数字冻结表 → 再谈简历。
- 首事件延迟（不含 LLM）不是本脚本的测量对象；本脚本的 latency = SSE 从发起到 `done` 的**全链路**时长。
