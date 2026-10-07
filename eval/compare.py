"""Before / After 对比：两次评测结果生成 Delta 表

用法：python compare.py results/baseline.json results/latest.json
输出：
                 Before       After       Delta
Tool Accuracy      80.0%       92.0%      +12.0%
...
"""
import json
import sys
from pathlib import Path

METRICS = [
    ("intent_accuracy", "Intent Accuracy", "%"),
    ("tool_selection_accuracy", "Tool Selection", "%"),
    ("no_tool_accuracy", "No-Tool Avoidance", "%"),
    ("tool_routing_accuracy", "Routing (Total)", "%"),
    ("root_cause_accuracy", "Root Cause", "%"),
    ("diagnosis_pass_rate", "Diagnosis Pass", "%"),
    ("avg_latency_ms", "Avg Latency", "ms"),
    ("p95_latency_ms", "P95 Latency", "ms"),
    ("p99_latency_ms", "P99 Latency", "ms"),
    ("first_event_avg_ms", "First Event Avg", "ms"),
    ("first_event_p99_ms", "First Event P99", "ms"),
    ("fallback_rate", "Fallback Rate", "%"),
    ("tool_retry_rate", "Tool Retry Rate", ""),
]


def fmt(v, unit):
    if v is None:
        return "—"
    if unit == "%":
        return f"{v}%"
    return f"{v}{unit}"


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    before = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    after = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))

    print(f"{'':<20}{'Before':>12}{'After':>12}{'Delta':>12}")
    print("-" * 56)
    for key, label, unit in METRICS:
        b, a = before.get(key), after.get(key)
        delta = None if (b is None or a is None) else round(a - b, 2)
        delta_str = "—" if delta is None else (f"{delta:+.2f}" if delta else "±0")
        print(f"{label:<20}{fmt(b, unit):>12}{fmt(a, unit):>12}{delta_str:>12}")
    print(f"\nBefore: {before.get('dataset')} ({before.get('samples')} 条)   "
          f"After: {after.get('dataset')} ({after.get('samples')} 条)")


if __name__ == "__main__":
    main()
