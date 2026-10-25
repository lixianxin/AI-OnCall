#!/usr/bin/env python3
"""
Mock LLM 上游服务器（OpenAI 兼容 /chat/completions 流式协议）。

用途：P1-3 LLM 上游故障注入验证。用运行时可控的故障场景替代"真等上游限流"，
配合 DEEPSEEK_BASE_URL=http://localhost:9999 启动 ai-service 即可。

场景（通过 POST /control?scenario=<name> 切换，GET /stats 查看请求计数）：
  ok               正常 200 SSE 流式响应（含 usage 尾帧）
  rate429x2        前两次请求返回 429 + Retry-After: 1，第三次起正常
  always429        永远 429 + Retry-After: 1（验证重试耗尽后的干净终态）
  err500           永远 500（验证 5xx 重试）
  stream_then_break 正常响应头 + 输出 2 个 token 后强行断开连接
                   （验证"流式输出已开始后不重试"语义）

运行：python mock_llm_upstream.py [port]   # 默认 9999
"""
import json
import sys
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

LOCK = threading.Lock()
STATE = {"scenario": "ok", "total_requests": 0, "per_scenario": {}}


def sse_frame(content: str) -> bytes:
    data = json.dumps({"choices": [{"delta": {"content": content}}]}, ensure_ascii=False)
    return f"data: {data}\n\n".encode("utf-8")


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):  # 静默默认访问日志
        pass

    # ---------- 控制 / 观察 ----------
    def do_GET(self):
        if self.path.startswith("/stats"):
            body = json.dumps(STATE, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        else:
            self._plain(404, "use POST /control?scenario=<name> or GET /stats")

    def do_POST(self):
        if self.path.startswith("/control"):
            scenario = self.path.split("scenario=")[-1].split("&")[0]
            with LOCK:
                STATE["scenario"] = scenario
            self._plain(200, f"scenario -> {scenario}")
            return

        # ---------- /chat/completions：注入点 ----------
        length = int(self.headers.get("Content-Length") or 0)
        if length:
            self.rfile.read(length)
        with LOCK:
            STATE["total_requests"] += 1
            sc = STATE["scenario"]
            STATE["per_scenario"][sc] = STATE["per_scenario"].get(sc, 0) + 1
            n = STATE["per_scenario"][sc]
        print(f"[mock] request #{STATE['total_requests']} scenario={sc} seq_in_scenario={n}", flush=True)

        if sc == "rate429x2" and n <= 2:
            self._error(429, retry_after="1", msg=f"rate limited (injection #{n})")
        elif sc == "always429":
            self._error(429, retry_after="1", msg="rate limited (always)")
        elif sc == "err500":
            self._error(500, retry_after=None, msg="internal error (injection)")
        elif sc == "stream_then_break":
            self._stream_then_break()
        else:  # ok 或 rate429x2 的第 3 次起
            self._stream_ok()

    # ---------- 响应实现 ----------
    def _plain(self, code: int, text: str):
        body = text.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _error(self, code: int, retry_after, msg: str):
        body = json.dumps({"error": {"message": msg, "type": "injected"}}).encode("utf-8")
        self.send_response(code)
        if retry_after:
            self.send_header("Retry-After", retry_after)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _stream_ok(self):
        frames = [sse_frame("根据") , sse_frame("知识库，"), sse_frame("建议先查 top。")]
        usage = json.dumps({
            "choices": [],
            "usage": {"prompt_tokens": 128, "completion_tokens": 9},
        }, ensure_ascii=False)
        body = b"".join(frames) + f"data: {usage}\n\ndata: [DONE]\n\n".encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Connection", "close")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _stream_then_break(self):
        # 200 响应头 + 少量 token 后强行断开：客户端已收到部分输出，必须走"不重试"分支
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Connection", "close")
        self.end_headers()
        try:
            self.wfile.write(sse_frame("部分") if False else sse_frame("前半段输出-"))
            self.wfile.flush()
            self.wfile.write(sse_frame("即将断开"))
            self.wfile.flush()
        except Exception:
            pass
        # 强制断开连接（不带 [DONE]，模拟上游中途崩溃）
        self.close_connection = True
        try:
            self.connection.close()
        except Exception:
            pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 9999
    print(f"[mock] LLM upstream mock on :{port} (scenario=ok). "
          f"切换: POST /control?scenario=rate429x2|always429|err500|stream_then_break|ok", flush=True)
    ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
