#!/usr/bin/env bash
# P1-3 LLM 上游故障注入测试脚本
# 前置：mock 上游 (scripts/mock_llm_upstream.py :9999) 与 ai-service (:8081) 均已启动
# 用法：bash scripts/run_retry_injection.sh [输出目录]
set -uo pipefail

OUT="${1:-docs/evidence/retry-injection-raw}"
mkdir -p "$OUT"
BASE=http://localhost:8081
MOCK=http://localhost:9999

send_chat() { # $1=conversationId  $2=输出文件
  curl -sN --max-time 90 -X POST "$BASE/api/chat/stream" \
    -H "Content-Type: application/json" \
    -H "X-Trace-Id: inj-$1" \
    -d "{\"conversationId\":\"$1\",\"messageId\":\"$1\",\"message\":\"服务器 CPU 使用率持续 95%，如何排查？\"}" \
    -o "$2" -w "http=%{http_code} time_total=%{time_total}s\n"
}

metrics_snapshot() { curl -s "$BASE/metrics" ; }

echo "===== S1: 429x2 后恢复（预期：重试 2 次成功，耗时含 2 次 Retry-After 退避） ====="
curl -s -X POST "$MOCK/control?scenario=rate429x2" >/dev/null
send_chat "inj-s1-$RANDOM" "$OUT/s1_429x2_then_ok.sse"
echo "--- mock stats ---"; curl -s "$MOCK/stats"; echo
echo "--- /metrics llm_retries ---"; metrics_snapshot | grep -i "retry" || echo "(no retry metrics line)"
echo

echo "===== S2: 持续 429（预期：3 次尝试后干净终态 error，耗时 ~3s+） ====="
curl -s -X POST "$MOCK/control?scenario=always429" >/dev/null
send_chat "inj-s2-$RANDOM" "$OUT/s2_always429.sse"
echo "--- mock stats ---"; curl -s "$MOCK/stats"; echo
echo "--- /metrics llm_retries ---"; metrics_snapshot | grep -i "retry" || echo "(no retry metrics line)"
echo

echo "===== S3: 流式中途断开（预期：不重试，直接 error 终态，mock 只收到 1 次请求） ====="
curl -s -X POST "$MOCK/control?scenario=stream_then_break" >/dev/null
send_chat "inj-s3-$RANDOM" "$OUT/s3_stream_break.sse"
echo "--- mock stats ---"; curl -s "$MOCK/stats"; echo
echo

echo "===== S4: 连接失败（端口 9998 无服务；需 ai-service 以 DEEPSEEK_BASE_URL=http://localhost:9998 启动） ====="
echo "(此场景需重启服务切换 base-url，单独执行，见证据文档)"
echo

echo "===== 最终 /metrics 快照 ====="
metrics_snapshot | tee "$OUT/final_metrics.txt"
echo "raw outputs saved to $OUT/"
