#!/usr/bin/env python3
"""JMeter JTL -> summary.json 标准解析脚本（吞吐/分位数唯一口径）

背景：2026-09-08 压测的 summary.json 中 throughput_reqs 曾为 0.0（一次性脚本字段错位），
与 report/statistics.json 的 0.3967 矛盾。本脚本作为今后唯一的 jtl 汇总入口，杜绝复发。

用法：
    python parse_summary.py results.jtl -o summary.json

口径约定（与 loadtest/README.md 一致）：
- throughput = 成功样本数 / (最后样本时间戳 - 首样本时间戳 + 首样本 elapsed)，全窗口平均
- latency 仅统计成功样本（errorSamples 的 elapsed 无意义）
- 样本量 < 100 时，P99 附带置信度弱标注

JTL CSV 列（JMeter 默认）：timeStamp,elapsed,label,responseCode,success,...
"""
import argparse
import csv
import json
import sys


def percentile(sorted_vals, p):
    """线性插值百分位（与 JMeter report 口径一致）"""
    if not sorted_vals:
        return 0.0
    k = (len(sorted_vals) - 1) * (p / 100.0)
    f = int(k)
    c = min(f + 1, len(sorted_vals) - 1)
    if f == c:
        return float(sorted_vals[f])
    return sorted_vals[f] + (sorted_vals[c] - sorted_vals[f]) * (k - f)


def main():
    ap = argparse.ArgumentParser(description="Parse JMeter JTL into summary.json")
    ap.add_argument("jtl", help="JMeter CSV result file (.jtl)")
    ap.add_argument("-o", "--out", default="summary.json", help="output json path")
    args = ap.parse_args()

    rows = []
    with open(args.jtl, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append(r)

    if not rows:
        print("empty jtl", file=sys.stderr)
        sys.exit(1)

    ok_rows = [r for r in rows if r.get("success", "").strip().lower() == "true"]
    errors = len(rows) - len(ok_rows)

    # 吞吐：全窗口平均。分母 = 末样本时刻 - 首样本发起时刻
    ts = [int(r["timeStamp"]) for r in rows]
    elapsed_total = max(ts) - min(ts)
    if len(rows) > 1 and elapsed_total > 0:
        # 首样本自身耗时也算进窗口
        first_elapsed = int(rows[0]["elapsed"])
        throughput = len(ok_rows) / ((elapsed_total + first_elapsed) / 1000.0)
    else:
        throughput = 0.0

    lat = sorted(int(r["elapsed"]) for r in ok_rows)
    summary = {
        "samples": len(rows),
        "errors": errors,
        "error_rate": round(errors / len(rows), 4),
        "throughput_reqs": round(throughput, 4),
        "avg_ms": round(sum(lat) / len(lat), 1) if lat else 0.0,
        "min_ms": lat[0] if lat else 0,
        "p50_ms": percentile(lat, 50),
        "p90_ms": percentile(lat, 90),
        "p95_ms": percentile(lat, 95),
        "p99_ms": percentile(lat, 99),
        "max_ms": lat[-1] if lat else 0,
    }
    if len(ok_rows) < 100:
        summary["percentile_caveat"] = (
            f"成功样本量 {len(ok_rows)} < 100，P90/P95/P99 统计置信度弱，"
            "仅供趋势参考，不作为容量结论")

    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=1)
    print(json.dumps(summary, ensure_ascii=False, indent=1))


if __name__ == "__main__":
    main()
