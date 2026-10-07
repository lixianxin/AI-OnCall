# -*- coding: utf-8 -*-
"""口径一致性校验（离线，不调服务）。

职责：从 results/*.json 的 per_case 逐条重算指标，断言与文件顶层汇总一致，
防止未来修改 evaluate.py 后忘记重聚合，导致“表/文件/代码”三处口径漂移。

用法：python recheck_baseline_caliber.py [results/baseline.json ...]
退出码：全部通过 0；任一不一致 1。
"""
import json
import sys
from pathlib import Path

BASE = Path(__file__).resolve().parent
REAL_TOOLS = {"analyze_log", "search", "read", "generate"}
ERROR_STATUSES = {"failed", "timeout", "rate_limited", "fallback"}


def main():
    files = sys.argv[1:] or ["results/baseline.json"]
    failed = False
    for f in files:
        rep = json.loads((BASE / f).read_text(encoding="utf-8"))
        cases = rep["per_case"]
        n = len(cases)
        tool_tasks = [r for r in cases if r["expected"]["tool"] in REAL_TOOLS]
        no_tool = [r for r in cases if r["expected"]["tool"] not in REAL_TOOLS]
        diag = [r for r in cases if r["expected"]["root_cause"]]

        def pct(sub, key):
            return round(100.0 * sum(1 for r in sub if r[key]) / len(sub), 2) if sub else None

        expect = {
            "intent_accuracy": pct(cases, "intent_ok"),
            "tool_task_samples": len(tool_tasks),
            "tool_selection_accuracy": pct(tool_tasks, "tool_ok"),
            "no_tool_samples": len(no_tool),
            "no_tool_accuracy": pct(no_tool, "tool_ok"),
            "tool_routing_accuracy": pct(cases, "tool_ok"),
            "fallback_count": sum(1 for r in cases if r["actual"]["tool_status"] in ERROR_STATUSES),
            "diagnosis_samples": len(diag),
            "diag_llm_error_samples": sum(1 for r in diag if r.get("llm_error")),
        }
        ok = True
        for k, v in expect.items():
            got = rep.get(k)
            if v != got:
                ok = False
                print(f"[MISMATCH] {f} {k}: 文件={got} 重算={v}")
        print(f"{'PASS' if ok else 'FAIL'}  {f}  (samples={n}, tool_task={expect['tool_task_samples']}, "
              f"tool={expect['tool_selection_accuracy']}%, no_tool={expect['no_tool_accuracy']}%, "
              f"fallback={expect['fallback_count']}/{n}={100.0*expect['fallback_count']/n:.1f}%)")
        failed = failed or not ok
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
