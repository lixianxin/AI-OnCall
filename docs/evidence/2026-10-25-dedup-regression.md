# P1-2 双重检索修复回归报告（2026-10-25，mock 上游离线回归）

> 验证对象：commit bd0c210 的 P1-2 改动（SearchTool 经 ToolResult 带回检索结果，编排层复用，消除每请求重复检索 + debug 日志第三次检索）。
> 方法：`evaluate.py` 全量回放 diagnosis_v2（200 条，固定数据集），服务连本地 mock 上游（`scripts/mock_llm_upstream.py`，scenario=ok），全程离线、无真实 API key。
> 结果快照：`results/dedup_regression_mock_20261025.json.json`。

## 口径声明（先说清楚这次回归能证明什么、不能证明什么）

- 意图/工具/规避/降级四项指标的判定链路（规则路由 + SemanticFallbackRouter + ToolExecutor）**完全不依赖 LLM 输出**（SemanticFallbackRouter 为本地 n-gram+IDF，确定性），mock 不影响其有效性；
- **根因判定依赖 LLM 生成内容**（evaluate.py 判定口径：`tool_summary + LLM content` 子串匹配，设计动机是 LLM 常用中文转述根因）——mock 不产生真实答案，**该项离线不可验证**，全量确认需要真实 API key 重跑；
- 延迟指标同样不可比（mock 响应 ≈0ms vs 真实 GLM ≈14s），本报告不引用延迟对比。

## 结果对照（diagnosis_v2，200 条，基线 = results/latest.json 20260908 真实 GLM 回放）

| 指标 | 基线（真实 GLM） | 本次（mock，throttle 2.5s） | 判定 |
|---|---|---|---|
| Intent Accuracy | 89.5% | **89.5%** | ✅ 持平 |
| Tool Selection Accuracy | 88.33% | **88.33%** | ✅ 持平 |
| No-Tool Avoidance | 100.0% | **100.0%** | ✅ 持平 |
| Tool Routing（组合口径） | 89.5% | **89.5%** | ✅ 持平 |
| Fallback Rate | 0.0% | **0.0%** | ✅ 持平 |
| Root Cause Accuracy | 95.0% | N/A（mock 伪影，见口径声明） | ⏳ 待真实 key 确认 |
| 本次工具执行 | — | analyze_log:success 99 / generate:success 50 / search:success 30，无 rate_limited | ✅ |

**结论：双重检索修复未改变路由行为**——四项路由链路指标与基线逐位一致。检索复用改动（SearchTool/ToolResult/编排层）正是作用于该链路，回归通过；根因指标的全量确认遗留至真实 key 可用时。

## 过程中发现的问题（已处理 / 已记录）

1. **限流伪影**：无节流直跑 200 条仅 ~5s（mock 下 25ms/条），打爆工具限流（analyze_log 20/min、generate 10/min），出现 `analyze_log:rate_limited 79`、Fallback 59.5%、Root Cause 归零——**这是回放速度伪影，不是回归失败**。首次无节流快照保留在 `results/` 外的本次运行日志中，正式快照以 `--throttle 2.5` 重跑为准。
   - 处置：正式回归统一使用 `--throttle 2.5`（按数据集分布与各工具限流上限计算的安全值，analyze_log 滑动窗 ≈14.4 次/分 < 20）；
   - 已在 `eval/README.md` 补充节流指引，避免后人踩同一坑。
2. **附带收获**：首次 mock 快照意外构成了一次"限流降级路径"的全量验证（79 次 rate_limited 全部被治理层正确降级、无崩溃、SSE 正常终态）。

## 遗留

- [ ] 真实 API key 可用时：`evaluate.py --dataset datasets/diagnosis_v2.jsonl`（真实 LLM 延迟天然避开限流，无需节流）确认 Root Cause 95.0 无回归。
