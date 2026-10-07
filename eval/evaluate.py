"""AIOps 离线回归评测 runner

对 datasets/*.jsonl 全量回放，逐条调用 /api/chat/stream（SSE），
采集意图 / 工具选择 / 根因 / 延迟 / 降级，与 /metrics 的 token、重试数据合并，
输出 results/latest.json（或 --save 指定名），支持 compare.py 做 Before/After 对比。

指标（与导师建议对齐）：
  Intent Accuracy          意图分类正确率
  Tool Selection Accuracy  工具选择正确率
  Root Cause Accuracy      根因定位正确率（仅诊断类用例）
  Diagnosis Pass Rate      意图+工具+根因全部正确的比例（诊断类）
  Avg / P95 / P99 Latency  单条回放端到端耗时（SSE 首事件 → done）
  First Event Latency      SSE 首事件延迟（均值 + P99）
  Fallback Rate            工具降级/超时/限流比例
  Tool Retry Rate          工具重试次数 / 调用次数（来自 /metrics）
  Token Usage              token 消耗（来自 /metrics）

仅用 Python 标准库，无第三方依赖。
"""
import argparse
import json
import statistics
import sys
import time
import urllib.request
from pathlib import Path

BASE = Path(__file__).resolve().parent
ERROR_STATUSES = {"failed", "timeout", "rate_limited", "fallback"}
REAL_TOOLS = {"analyze_log", "search", "read", "generate"}
NONE_TOKENS = (None, "", "none")


def sse_chat(base_url, message, timeout=60):
    """调一次 /api/chat/stream，返回 (events, first_event_ms, total_ms)"""
    body = json.dumps({"conversationId": f"eval-{int(time.time()*1000)}",
                       "message": message, "messageId": f"m-{time.time()}"}).encode()
    req = urllib.request.Request(
        base_url + "/api/chat/stream", data=body,
        headers={"Content-Type": "application/json", "Accept": "text/event-stream"})
    events, first_ms = [], None
    t0 = time.perf_counter()
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        cur_event, cur_data = None, ""
        for raw in resp:
            line = raw.decode("utf-8", "replace").rstrip("\n").rstrip("\r")
            if first_ms is None and line:
                first_ms = (time.perf_counter() - t0) * 1000
            if line.startswith("event:"):
                cur_event = line.split(":", 1)[1].strip()
            elif line.startswith("data:"):
                cur_data += line.split(":", 1)[1].strip()
            elif not line and cur_event:
                events.append({"event": cur_event, "data": cur_data})
                # done 为服务端终态事件；其后仅剩 keepalive 注释行（连接不关闭），
                # 继续等待 EOF 会导致评测挂死（LLM 成功路径 2026-09-08 修复）
                if cur_event == "done":
                    break
                cur_event, cur_data = None, ""
        if cur_event:  # 最后一个事件可能无空行结尾
            events.append({"event": cur_event, "data": cur_data})
    return events, first_ms or 0.0, (time.perf_counter() - t0) * 1000


def fetch_metrics(base_url):
    try:
        with urllib.request.urlopen(base_url + "/metrics", timeout=10) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        print(f"[warn] /metrics 不可用: {e}", file=sys.stderr)
        return {}


def metrics_delta(pre, post):
    """after-before 差值 → 本次回放专属的计数（/metrics 是服务生命周期累计值，必须做差值）"""
    p = (pre or {}).get("metrics", {})
    q = (post or {}).get("metrics", {})

    def diff_map(a, b):
        keys = set(a) | set(b)
        out = {}
        for k in keys:
            d = b.get(k, 0) - a.get(k, 0)
            if d:
                out[k] = d
        return out

    delta = {}
    if q:
        delta["calls_by_status"] = diff_map(p.get("calls_by_status", {}), q.get("calls_by_status", {}))
        delta["errors_by_tool"] = diff_map(p.get("errors_by_tool", {}), q.get("errors_by_tool", {}))
        per = {}
        for name in set(p.get("per_tool", {})) | set(q.get("per_tool", {})):
            a = p.get("per_tool", {}).get(name, {})
            b = q.get("per_tool", {}).get(name, {})
            d = {k: b.get(k, 0) - a.get(k, 0) for k in set(a) | set(b) if isinstance(b.get(k), (int, float))}
            d = {k: v for k, v in d.items() if v}
            if d:
                per[name] = d
        if per:
            delta["per_tool"] = per
    return delta


def evaluate_case(base_url, case):
    events, first_ms, total_ms = sse_chat(base_url, case["input"])

    intent, tool, tool_status, tool_summary, content = None, None, None, "", ""
    llm_error = False
    for ev in events:
        try:
            data = json.loads(ev["data"]) if ev["data"] else {}
        except json.JSONDecodeError:
            data = {"delta": ev["data"]}
        if ev["event"] == "intent":
            intent = data.get("intent")
        elif ev["event"] == "tool":
            tool, tool_status, tool_summary = data.get("tool"), data.get("status"), data.get("summary", "")
        elif ev["event"] == "error":
            llm_error = True
        elif ev["event"] == "content":
            content += data.get("delta", "")

    exp_intent, exp_tool, root = (case["expected_intent"], case["expected_tool"],
                                  case.get("expected_root_cause"))

    # 无工具样本（expected_tool=none）：编排器本就不发 tool 事件。
    # 归一化以「期望值」为准、与 intent 判定解耦——否则意图误判会连带误伤规避判定。
    if tool is None and exp_tool in NONE_TOKENS:
        tool = "none"

    intent_ok = intent == exp_intent
    tool_ok = tool == exp_tool
    root_ok = None
    if root:
        # 根因判定口径 v2（2026-09-08 P0 启用，仅影响 LLM 可用期的新评测，402 期 N/A 不受影响）：
        #   命中文本 = tool_summary + LLM content；
        #   判定 = 期望根因标签子串 或 数据集自带 root_keywords 子串（ground-truth 别名，非自造同义词表）。
        #   背景：LLM 常以中文转述根因而不回显英文标签（如 SQLException），纯字面匹配系统性低估诊断能力。
        text_low = (tool_summary + content).lower()
        needles = [root] + [k for k in (case.get("root_keywords") or []) if k]
        root_literal = root.lower() in text_low
        root_alias = any(str(k).lower() in text_low for k in needles[1:])
        root_ok = bool(root_literal or root_alias)

    return {
        "id": case["id"], "input": case["input"],
        "difficulty": case.get("difficulty"),
        "expected": {"intent": exp_intent, "tool": exp_tool, "root_cause": root,
                     "root_keywords": case.get("root_keywords")},
        "actual": {"intent": intent, "tool": tool, "tool_status": tool_status},
        "llm_error": llm_error,
        "intent_ok": intent_ok, "tool_ok": tool_ok,
        "root_literal_ok": (root_literal if root else None),
        "root_alias_ok": (root_alias if root else None),
        "root_ok": root_ok, "pass": bool(intent_ok and tool_ok and (root_ok is not False)),
        "first_event_ms": round(first_ms, 1), "total_ms": round(total_ms, 1),
    }


def summarize(results, dataset_name, metrics_snapshot):
    n = len(results)
    tool_tasks = [r for r in results if r["expected"]["tool"] in REAL_TOOLS]
    no_tool_tasks = [r for r in results if r["expected"]["tool"] not in REAL_TOOLS]
    diag = [r for r in results if r["expected"]["root_cause"]]
    totals = [r["total_ms"] for r in results]
    firsts = [r["first_event_ms"] for r in results]
    fallbacks = [r for r in results if r["actual"]["tool_status"] in ERROR_STATUSES]
    llm_errors = [r for r in results if r.get("llm_error")]
    diag_llm_errors = [r for r in diag if r.get("llm_error")]

    def pct(subset, key):
        return round(100.0 * sum(1 for r in subset if r[key]) / len(subset), 2) if subset else None

    def pctile(arr, p):
        return round(sorted(arr)[min(len(arr) - 1, int(len(arr) * p / 100))], 1) if arr else None

    # 根因 / 诊断通过率：若诊断类样本 100% 遭遇 LLM 错误（如 402 断供），
    # 语义上是「无有效诊断输出」而非「模型能力为 0」→ 记 N/A，避免未来误读。
    def root_pct(subset):
        if not subset:
            return None
        if all(r.get("llm_error") for r in subset):
            return None
        return pct(subset, "root_ok")

    diag_outage = bool(diag) and len(diag_llm_errors) == len(diag)

    tool_retry_rate = None
    per_tool = metrics_snapshot.get("metrics", {}).get("per_tool", {})
    total_calls = sum(v.get("calls", 0) for v in per_tool.values())
    total_retries = sum(v.get("total_retries", 0) for v in per_tool.values())
    if total_calls:
        tool_retry_rate = round(total_retries / total_calls, 4)

    # 分档指标（数据集带 difficulty 字段时输出）：一律按新口径拆分母
    by_diff = {}
    diffs = sorted({r.get("difficulty") for r in results if r.get("difficulty")})
    for d in diffs:
        sub = [r for r in results if r.get("difficulty") == d]
        sub_tool = [r for r in sub if r["expected"]["tool"] in REAL_TOOLS]
        sub_none = [r for r in sub if r["expected"]["tool"] not in REAL_TOOLS]
        sub_diag = [r for r in sub if r["expected"]["root_cause"]]
        by_diff[d] = {
            "samples": len(sub),
            "intent_accuracy": pct(sub, "intent_ok"),
            "tool_task_samples": len(sub_tool),
            "tool_selection_accuracy": pct(sub_tool, "tool_ok"),
            "no_tool_samples": len(sub_none),
            "no_tool_accuracy": pct(sub_none, "tool_ok"),
            "root_cause_accuracy": root_pct(sub_diag),
        }

    return {
        "dataset": dataset_name, "samples": n,
        "intent_accuracy": pct(results, "intent_ok"),
        # --- 工具口径拆分（2026-09-08）---
        "tool_task_samples": len(tool_tasks),
        "tool_selection_accuracy": pct(tool_tasks, "tool_ok"),
        "no_tool_samples": len(no_tool_tasks),
        "no_tool_accuracy": pct(no_tool_tasks, "tool_ok"),
        "tool_routing_accuracy": pct(results, "tool_ok"),  # 组合口径：全量正确（含规避）
        # --- 诊断类 ---
        "diagnosis_samples": len(diag),
        "diag_llm_error_samples": len(diag_llm_errors),
        "root_cause_accuracy": root_pct(diag),
        "root_cause_na": "llm_outage" if diag_outage else None,
        "diagnosis_pass_rate": None if diag_outage else pct(diag, "pass"),
        # --- 延迟 ---
        "avg_latency_ms": round(statistics.mean(totals), 1) if totals else None,
        "p95_latency_ms": pctile(totals, 95),
        "p99_latency_ms": pctile(totals, 99),
        "first_event_avg_ms": round(statistics.mean(firsts), 1) if firsts else None,
        "first_event_p99_ms": pctile(firsts, 99),
        # --- 治理 ---
        "fallback_count": len(fallbacks),
        "fallback_rate": round(100.0 * len(fallbacks) / n, 1) if n else None,  # 单位 %
        "llm_error_rate": round(len(llm_errors) / n, 4) if n else None,
        "tool_retry_rate": tool_retry_rate,
        "token_usage": metrics_snapshot.get("metrics", {}).get("token_usage", {}),
        "by_difficulty": by_diff,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8081")
    ap.add_argument("--dataset", default=str(BASE / "datasets" / "diagnosis_v1.jsonl"))
    ap.add_argument("--throttle", type=float, default=0.0, help="每条回放间隔秒（限流隔离实验用，如 3.0）")
    ap.add_argument("--save", default=None, help="结果另存为 results/<name>.json")
    args = ap.parse_args()

    cases = [json.loads(l) for l in Path(args.dataset).read_text(encoding="utf-8").splitlines() if l.strip()]
    n = len(cases)
    print(f"Dataset: {Path(args.dataset).name}  Samples: {n}")

    # 回放前 metrics 快照：用于 after-before 差值 → 「本次回放专属」调用计数
    metrics_before = fetch_metrics(args.base)

    results = []
    t_last = None
    for i, case in enumerate(cases, 1):
        if args.throttle and t_last is not None:
            wait = args.throttle - (time.perf_counter() - t_last)
            if wait > 0:
                time.sleep(wait)
        t_last = time.perf_counter()
        try:
            results.append(evaluate_case(args.base, case))
        except Exception as e:
            print(f"[warn] {case['id']} 失败: {e}", file=sys.stderr)
            results.append({"id": case["id"], "input": case["input"],
                            "difficulty": case.get("difficulty"),
                            "expected": {"intent": case["expected_intent"],
                                         "tool": case["expected_tool"],
                                         "root_cause": case.get("expected_root_cause")},
                            "actual": {"intent": None, "tool": None, "tool_status": "error"},
                            "llm_error": False,
                            "intent_ok": False, "tool_ok": False, "root_ok": False,
                            "pass": False, "first_event_ms": 0, "total_ms": 0})
        if i % 20 == 0:
            print(f"  ... {i}/{len(cases)}")

    snapshot = fetch_metrics(args.base)
    report = summarize(results, Path(args.dataset).stem, snapshot)
    report["metrics_delta"] = metrics_delta(metrics_before, snapshot)
    report["per_case"] = results

    out = BASE / "results"
    out.mkdir(exist_ok=True)
    latest = out / "latest.json"
    latest.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    if args.save:
        (out / f"{args.save}.json").write_text(
            json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    rc = report["root_cause_accuracy"]
    rc_txt = "N/A（诊断类 100% LLM 断供，无有效输出）" if rc is None else f"{rc}%"
    print(f"""
Intent Accuracy:         {report['intent_accuracy']}%  (all {n})
Tool Selection:          {report['tool_selection_accuracy']}%  (tool_task {report['tool_task_samples']} 条)
No-Tool Avoidance:       {report['no_tool_accuracy']}%  (none {report['no_tool_samples']} 条)
Tool Routing (组合口径):   {report['tool_routing_accuracy']}%
Root Cause Accuracy:     {rc_txt}  (diag {report['diagnosis_samples']} 条, llm_error {report['diag_llm_error_samples']})
Diagnosis Pass Rate:     {report['diagnosis_pass_rate']}%
Avg Latency:             {report['avg_latency_ms']}ms   P95: {report['p95_latency_ms']}ms   P99: {report['p99_latency_ms']}ms
First Event Avg:         {report['first_event_avg_ms']}ms (P99 {report['first_event_p99_ms']}ms, 不含 LLM 推理)
Fallback Rate:           {report['fallback_rate']}% ({report['fallback_count']}/{n})
LLM Error Rate:          {report['llm_error_rate']}
Token Usage:             {report['token_usage']}
Metrics Delta(本次回放): {report['metrics_delta'].get('calls_by_status', {})}
=> 已写入 {latest.name}{'，结果快照 ' + args.save + '.json' if args.save else ''}
""")


if __name__ == "__main__":
    main()
