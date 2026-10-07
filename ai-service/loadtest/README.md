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
若 jtl 为 CSV 无表头导致 `-e` 告警，直接用 `results/p1_glm_20260908/summary.json` 同款 python 解析 jtl 算百分位即可。

## 压测记录

- **2026-09-08 P1（GLM-4-Flash 实测）**：2 并发 × 120s 纯 LLM 闲聊链路 → 48 样本 0 错误，
  0.40 req/s，P50 4.4s / P90 9.1s / P95 13.2s / P99 16.9s；窗口内工具调用 Δ=0（纯度验证）。
  归档：`results/p1_glm_20260908/`。附带修复：`chat-stream-load.jmx` HeaderManager 缺空 `<hashTree/>`。
  服务端 SSE 在 `done` 后连接不关闭的 bug 由本次压测暴露并已修复（见评测报告 §8.3）。

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
