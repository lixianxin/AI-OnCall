"""故障回放数据集生成器

生成 diagnosis_v1.jsonl（200 条），用例对齐 AnalyzeLogTool 真实可识别的 8 类异常模式
与 DocumentAgentOrchestrator 的 4 类意图（ERROR_DIAGNOSIS / PROTOCOL_QA / CODE_GENERATION / GENERAL_CHAT），
保证 Tool Selection / Root Cause 指标可量化。

字段：
  id                  用例编号
  input               用户输入（回放 query）
  expected_intent     期望意图（IntentEvent 常量）
  expected_tool       期望工具（search / analyze_log / generate / none）
  expected_root_cause 期望根因（对齐 AnalyzeLogTool LogPattern 名；非诊断类为 null）

用法：python generate_dataset.py > datasets/diagnosis_v1.jsonl
"""
import itertools
import json
import random

random.seed(42)  # 固定种子，数据集可复现

# ── 诊断类：8 类根因 × 触发词（与 AnalyzeLogTool.PATTERNS 的正则一致）─────────
DIAG_TEMPLATES = [
    ("调用 {obj} 接口报 NullPointerException，帮忙排查一下", "NullPointerException"),
    ("App 启动日志里出现 NPE 异常，怎么回事", "NullPointerException"),
    ("线上异常：null pointer 崩溃了", "NullPointerException"),
    ("HTTPS 握手失败，报 SSLException", "SSLException"),
    ("SSL 证书校验异常 SSLHandshakeException 怎么解决", "SSLException"),
    ("请求报 certificate verify 错误", "SSLException"),
    ("连接超时，日志提示 Connection refused", "ConnectTimeout"),
    ("客户端 ConnectException 连不上服务端", "ConnectTimeout"),
    ("报 no route to host 异常", "ConnectTimeout"),
    ("接口读取超时，Read timed out", "SocketTimeout"),
    ("日志有 SocketTimeoutException，请求挂起", "SocketTimeout"),
    ("下游返回太慢导致 ReadTimeout 异常", "SocketTimeout"),
    ("执行 SQL 报 SQLException 死锁", "SQLException"),
    ("数据库写入失败 ConstraintViolation", "SQLException"),
    ("MySQL 报 Duplicate entry 异常", "SQLException"),
    ("Android 模拟器黑屏了", "黑屏"),
    ("运行后黑屏，GPU 渲染好像有问题", "黑屏"),
    ("emulator black screen 异常怎么处理", "黑屏"),
    ("SSE 连接断开，回答中断", "SSE连接"),
    ("流式响应老是断开连接", "SSE连接"),
    ("sse 通道超时断开，日志无报错", "SSE连接"),
    ("DeepSeek API 返回 401", "DeepSeek API Error"),
    ("LLM 调用失败，提示 api key 无效", "DeepSeek API Error"),
    ("deepseek 请求报 api.error 异常", "DeepSeek API Error"),
]

QA_TEMPLATES = [
    "tab manifest 里注册路由的协议字段说明",
    "协议文档中权限相关的配置说明",
    "讲一下 manifest 的注册协议",
    "OpenTabContentHost 的路由协议怎么定义",
    "接口协议里 permission 字段的规范",
]

GEN_TEMPLATES = [
    "帮我写一个 Kotlin Repository 实现类",
    "用 Compose 生成一个列表页代码",
    "实现一个分页加载的 Repository",
    "生成一段 kotlin 数据类代码",
    "写一个接口请求的 Repository 模板",
]

CHAT_TEMPLATES = [
    "你好，你是谁呀",
    "今天天气不错，心情很好",
    "谢谢你的帮助",
    "你觉得我该先学什么",
]

SUFFIXES = ["", "（联调环境）", "（线上环境）", "怎么办？", "如何排查？", "灰度期间出现的",
            "（复现步骤见附件）", "，急", "（昨天开始出现）", "求帮助"]


def expand(templates, n_target):
    """模板 × 后缀 组合扩充到目标数量，统一返回 (text, root|None) 元组"""
    cases = []
    for tpl, root in itertools.cycle(templates):
        if len(cases) >= n_target:
            break
        suffix = SUFFIXES[len(cases) % len(SUFFIXES)]
        cases.append((tpl + suffix, root))
    return cases[:n_target]


def main():
    cases = []
    # 120 条诊断（8 类根因 × 15 变体）
    diag = expand([(t, r) for t, r in DIAG_TEMPLATES], 120)
    for i, (text, root) in enumerate(diag):
        cases.append({
            "id": f"diag_{i:03d}",
            "input": text,
            "expected_intent": "ERROR_DIAGNOSIS",
            "expected_tool": "analyze_log",
            "expected_root_cause": root,
        })
    # 30 条协议问答
    qa = expand([(t, None) for t in QA_TEMPLATES], 30)
    for i, (text, _) in enumerate(qa):
        cases.append({
            "id": f"qa_{i:03d}",
            "input": text,
            "expected_intent": "PROTOCOL_QA",
            "expected_tool": "search",
            "expected_root_cause": None,
        })
    # 30 条代码生成
    gen = expand([(t, None) for t in GEN_TEMPLATES], 30)
    for i, (text, _) in enumerate(gen):
        cases.append({
            "id": f"gen_{i:03d}",
            "input": text,
            "expected_intent": "CODE_GENERATION",
            "expected_tool": "generate",
            "expected_root_cause": None,
        })
    # 20 条闲聊
    chat = expand([(t, None) for t in CHAT_TEMPLATES], 20)
    for i, (text, _) in enumerate(chat):
        cases.append({
            "id": f"chat_{i:03d}",
            "input": text,
            "expected_intent": "GENERAL_CHAT",
            "expected_tool": "none",
            "expected_root_cause": None,
        })

    random.shuffle(cases)
    for c in cases:
        print(json.dumps(c, ensure_ascii=False))


if __name__ == "__main__":
    main()
