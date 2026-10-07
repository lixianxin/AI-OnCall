# eval/ — 离线回归评测闭环

把"一次性实验"变成**每次改动自动跑、Before/After 可对比**的回归闭环：

```text
改 Prompt / Tool / Retriever → 跑 evaluate.py → 存 results/ → compare.py 对比 Delta
```

## 目录

```
eval/
├── generate_dataset.py          # 数据集生成器（固定种子，可复现）
├── datasets/
│   └── diagnosis_v1.jsonl       # 200 条故障回放（8 类根因 × 4 类意图）
├── evaluate.py                  # 评测 runner：SSE 回放 + /metrics 采集
├── compare.py                   # Before/After Delta 对比表
└── results/
    ├── baseline.example.json    # ⚠️ 仅演示 compare.py 输出格式，非真实结果
    ├── latest.json              # 最近一次真实评测输出
    └── <label>.json             # --save 指定的命名快照（如 baseline.json）
```

## 数据集字段

```json
{
  "id": "diag_001",
  "input": "调用用户接口报 NullPointerException",
  "expected_intent": "ERROR_DIAGNOSIS",
  "expected_tool": "analyze_log",
  "expected_root_cause": "NullPointerException"
}
```

`expected_root_cause` 对齐 `AnalyzeLogTool` 真实可识别的 8 类模式
（NullPointerException / SSLException / ConnectTimeout / SocketTimeout /
SQLException / 黑屏 / SSE连接 / DeepSeek API Error），保证指标可量化。

## 用法

```bash
# 1. 启动服务
cd ai-service && ../gradlew bootRun

# 2. 全量回归评测（200 条）
python eval/evaluate.py --base http://localhost:8081 --save baseline

# 3. 改动后再跑，对比
python eval/evaluate.py --base http://localhost:8081 --save after_v2
python eval/compare.py results/baseline.json results/after_v2.json
```

## 指标体系

| 指标 | 含义 |
| --- | --- |
| Intent Accuracy | 意图分类正确率（intent 事件 vs expected_intent） |
| Tool Selection Accuracy | 工具选择正确率（tool 事件 vs expected_tool） |
| Root Cause Accuracy | 根因定位正确率（仅诊断类用例；根因关键词命中 tool summary 或诊断正文） |
| Diagnosis Pass Rate | 意图+工具+根因全部正确（仅诊断类） |
| Avg / P95 / P99 Latency | 单条回放端到端耗时（请求 → done） |
| First Event Avg / P99 | SSE 首事件延迟 |
| Fallback Rate | 工具降级/超时/限流比例 |
| Tool Retry Rate | 工具重试次数 / 调用次数（来自 /metrics） |
| Token Usage | token 消耗（来自 /metrics） |

> 第一版 Root Cause 用"关键词命中"口径，是可复现的确定性指标；
> 更严谨的做法（LLM-as-judge / 人工标注）作为 v2 迭代项。

## 当前状态

- ✅ 数据集 200 条已生成（固定种子）
- ✅ evaluate.py / compare.py 就绪（纯标准库）
- ⏳ 真实评测未跑（需启动服务）——跑完前，简历对应数字（92% / 87% / QPS / P99 / ≤300ms）一律不写
