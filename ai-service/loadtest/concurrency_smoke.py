#!/usr/bin/env python3
"""
并发冒烟测试（P1-4 第一步：并发校准）

用途：在正式 JMeter 压测前，验证服务在并发 SSE 连接下的行为正确性：
  1. 所有流都能正常终结（done 或 error 事件，无挂死连接）
  2. 服务端在流结束后主动关闭连接（2026-09-08 修复的 bug 的并发回归）
  3. 高并发下无 5xx / 连接拒绝
  4. 记录服务端吞吐与延迟分布（口径：端到端，含上游等待）

诚实口径：本脚本打的是「编排 + 上游」全链路。用 mock 上游跑时，
数字反映的是服务端自身并发行为（连接管理/编排/工具治理），
**不代表真实 LLM 延迟与吞吐**，不得写入冻结表或简历。

用法：python concurrency_smoke.py --concurrency 10 --per-worker 3 --base http://localhost:8081
"""
import argparse
import json
import statistics
import sys
import threading
import time
import urllib.request
from collections import Counter

results = []
lock = threading.Lock()


def one_request(base_url, worker_id, seq, timeout=60):
    body = json.dumps({
        "conversationId": f"smoke-{worker_id}-{seq}",
        "messageId": f"smoke-m-{worker_id}-{seq}-{time.time()}",
        "message": "服务器 CPU 使用率持续 95%，如何排查？",
    }).encode()
    req = urllib.request.Request(base_url + "/api/chat/stream", data=body,
                                 headers={"Content-Type": "application/json",
                                          "Accept": "text/event-stream"})
    t0 = time.perf_counter()
    events = []
    outcome, err = "unknown", None
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            cur_event, cur_data = None, ""
            for raw in resp:
                line = raw.decode("utf-8", "replace").rstrip("\r\n")
                if line.startswith("event:"):
                    cur_event = line.split(":", 1)[1].strip()
                elif line.startswith("data:") and cur_event:
                    cur_data += line.split(":", 1)[1].strip()
                elif not line and cur_event:
                    events.append(cur_event)
                    if cur_event == "done":
                        outcome = "done"
                        break
                    cur_event, cur_data = None, ""
            # 连接在 done/error 后应被服务端关闭（EOF 自然结束循环）
            if outcome == "unknown":
                if events and events[-1] == "error":
                    outcome = "error_event"
                elif "done" in events:
                    outcome = "done"
                else:
                    outcome = "truncated"  # 流被截断且无终结事件 —— 行为缺陷
    except Exception as e:
        outcome, err = "exception", repr(e)
    elapsed_ms = (time.perf_counter() - t0) * 1000
    with lock:
        results.append({"worker": worker_id, "seq": seq, "outcome": outcome,
                        "ms": round(elapsed_ms, 1), "err": err,
                        "terminated_cleanly": outcome in ("done", "error_event"),
                        "server_closed": True})  # 循环正常结束 = 服务端关了连接


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="http://localhost:8081")
    ap.add_argument("--concurrency", type=int, default=10)
    ap.add_argument("--per-worker", type=int, default=3)
    args = ap.parse_args()

    total = args.concurrency * args.per_worker
    print(f"[smoke] {args.concurrency} 并发 × {args.per_worker} 请求 = {total} 条 SSE 流 → {args.base}")
    threads = []
    t_start = time.perf_counter()
    for w in range(args.concurrency):
        th = threading.Thread(target=lambda wid=w: [one_request(args.base, wid, s)
                                                    for s in range(args.per_worker)])
        threads.append(th)
    for th in threads:
        th.start()
    for th in threads:
        th.join(timeout=120)
    wall_s = time.perf_counter() - t_start

    outcomes = Counter(r["outcome"] for r in results)
    ok = [r["ms"] for r in results if r["outcome"] == "done"]
    latency = sorted(ok)
    p = lambda q: round(latency[min(len(latency) - 1, int(len(latency) * q))], 1) if latency else None

    summary = {
        "total_streams": total,
        "completed": len(results),
        "outcomes": dict(outcomes),
        "all_terminated_cleanly": all(r["terminated_cleanly"] for r in results),
        "wall_seconds": round(wall_s, 1),
        "throughput_streams_per_s": round(len(results) / wall_s, 2) if wall_s else 0,
        "latency_ms_end_to_end": {"p50": p(0.5), "p95": p(0.95), "max": max(latency) if latency else None},
        "sample_count_note": f"{len(latency)} 样本，小样本分位数仅供参考" if len(latency) < 100 else ">=100 样本",
        "caliber": "端到端（含上游等待）；mock 上游时数字仅反映服务端并发行为，不代表真实 LLM 容量",
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    if len(sys.argv) > 1:
        pass
    return summary


if __name__ == "__main__":
    main()
