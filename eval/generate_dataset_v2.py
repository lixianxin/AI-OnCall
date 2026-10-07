"""故障回放数据集生成器 v2 —— easy / normal / hard 三档分层

按导师要求改进（对照 v1）：
  1. expected_root_cause 只是标注，不再以「用户口述异常名」的方式直接暴露在 input 中；
  2. 诊断类分三档：
       easy   —— 用户直接说出异常名（v1 风格）
       normal —— 证据藏在日志片段/堆栈里，用户不点名
       hard   —— 纯自然语言症状 + 间接证据，且刻意避开全部意图关键词
                 （错误/异常/失败/exception/npe/超时/timeout/黑屏/ssl），专测路由天花板
  3. 每条用例附 root_keywords（同义词组），Root Cause 匹配口径更公平；
  4. 每条用例附 difficulty 字段，evaluate.py 按档位输出分档指标。

总量与 v1 对齐（200 条），保证 Before/After 可比。

用法：python generate_dataset_v2.py > datasets/diagnosis_v2.jsonl
"""
import itertools
import json
import random

random.seed(42)

SUFFIXES = ["", "（联调环境）", "（线上环境）", "怎么办？", "如何排查？", "灰度期间出现的",
            "（复现步骤见附件）", "，急", "（昨天开始出现）", "求帮助"]

# ── 诊断类：8 类根因 × 3 档难度，每档 3 个模板（扩充到每档 5 条）──────────────
# (input 模板, difficulty)；root_keywords 由外层 root 统一给出
DIAG = {
    "NullPointerException": {
        "root_keywords": ["NullPointerException", "NPE", "空指针", "NullPointer"],
        "easy": [
            "调用 {obj} 接口报 NullPointerException，帮忙排查一下",
            "App 启动日志里出现 NPE 异常，怎么回事",
            "线上异常：null pointer 崩溃了",
        ],
        "normal": [
            "用户反馈保存草稿失败，日志贴出来是一段 Caused by: java.lang.NullPointerException 的堆栈",
            "灰度机器疯狂打堆栈，头部是 java.lang.NullPointerException，帮忙看下",
            "联调时点保存就挂，attach 的日志里有 NullPointer 相关堆栈",
        ],
        "hard": [
            "调用窗口接口后服务退出了，日志末尾一大段堆栈指向 WindowService.java:88",
            "点保存之后页面卡死，开发说堆栈里有个 NullPointer 什么的东西",
            "昨天上线后偶发闪退，日志被截断了只看到 NullPointer 开头的一行",
        ],
    },
    "SSLException": {
        "root_keywords": ["SSLException", "SSLHandshakeException", "SSL", "TLS 握手", "证书"],
        "easy": [
            "HTTPS 握手失败，报 SSLException",
            "SSL 证书校验异常 SSLHandshakeException 怎么解决",
            "请求报 certificate verify 错误",
        ],
        "normal": [
            "灰度用户反馈 HTTPS 请求全部失败，日志：javax.net.ssl.SSLHandshakeException: handshake_failure",
            "抓包看 TLS Client Hello 之后直接被 RST，日志里有 ssl 相关堆栈",
            "升级域名证书之后 App 端请求全挂，attach 日志：certificate verify failed",
        ],
        "hard": [
            "新装机的用户连不上网，抓包看 TLS 握手那一步就被服务端断了",
            "测试环境换了张证书之后 App 一直转圈，开发说像是握手环节的问题",
            "部分用户反馈 HTTPS 打不开，运维怀疑是证书链的问题",
        ],
    },
    "ConnectTimeout": {
        "root_keywords": ["ConnectTimeout", "ConnectException", "Connection refused", "connect"],
        "easy": [
            "连接超时，日志提示 Connection refused",
            "客户端 ConnectException 连不上服务端",
            "报 no route to host 异常",
        ],
        "normal": [
            "服务间调用不通，日志里有 java.net.ConnectException: Connection refused",
            "容器重启后上游一直报 connect 被拒，帮忙看下端口",
            "跨机房调用全挂，日志片段：java.net.ConnectException",
        ],
        "hard": [
            "A 机房调 B 机房突然全不通了，telnet 端口直接被拒",
            "发布之后上游说我们端口没开，telnet 一下确实不通",
            "有台机器ping得通但端口死活连不上，帮忙看看",
        ],
    },
    "SocketTimeout": {
        "root_keywords": ["SocketTimeout", "Read timed out", "ReadTimeout", "超时未响应"],
        "easy": [
            "接口读取超时，Read timed out",
            "日志有 SocketTimeoutException，请求挂起",
            "下游返回太慢导致 ReadTimeout 异常",
        ],
        "normal": [
            "下游接口偶发拿不到响应，日志：java.net.SocketTimeoutException: Read timed out",
            "大促压测时批量读超时，日志头部全是 socket timeout 相关堆栈",
            "弱网用户反馈等不到响应，客户端日志：SocketTimeoutException",
        ],
        "hard": [
            "下游接口经常等不到响应，客户端等够 5 秒就把连接挂断了",
            "晚上高峰批量查询总卡到超时限制才返回空，帮忙看看下游",
            "数据导出功能跑到一半就停了，日志最后一句是 read 超时限制触发",
        ],
    },
    "SQLException": {
        "root_keywords": ["SQLException", "SQL", "Duplicate entry", "死锁", "数据库"],
        "easy": [
            "执行 SQL 报 SQLException 死锁",
            "数据库写入失败 ConstraintViolation",
            "MySQL 报 Duplicate entry 异常",
        ],
        "normal": [
            "订单写入失败，日志：java.sql.SQLException: Duplicate entry 'xxx' for key 'PRIMARY'",
            "定时任务跑一半挂了，日志里有 sql 死锁堆栈",
            "库存扣减接口报数据库约束冲突，attach 的是一段 SQLException 堆栈",
        ],
        "hard": [
            "下单时那条记录写不进去，提示主键重复，帮忙看看",
            "库存偶尔扣不动，DBA 说日志里有死锁记录",
            "定时任务到一半就断了，运维说是数据库那边拦的",
        ],
    },
    "黑屏": {
        "root_keywords": ["黑屏", "black screen", "渲染"],
        "easy": [
            "Android 模拟器黑屏了",
            "运行后黑屏，GPU 渲染好像有问题",
            "emulator black screen 怎么处理",
        ],
        "normal": [
            "模拟器起来之后画面全黑，logcat 显示系统服务一切正常",
            "冷启动卡在启动页之后整块屏幕黑掉，渲染管线好像没起来",
            "真机预览一直 black screen，但进程活着",
        ],
        "hard": [
            "模拟器能起来但画面全黑，logcat 里 system_server 一切正常",
            "启动之后屏幕整块黑着不动，进程和日志都看起来正常",
            "新环境跑起来是一片黑，硬件加速开着也不行",
        ],
    },
    "SSE连接": {
        "root_keywords": ["SSE", "event-stream", "流式", "断开"],
        "easy": [
            "SSE 连接断开，回答中断",
            "流式响应老是断开连接",
            "sse 通道超时断开，日志无报错",
        ],
        "normal": [
            "回答生成到一半就停了，抓包看是 text/event-stream 的连接被服务端断开",
            "网关层显示 sse 通道频繁重建，客户端频繁触发重连",
            "长回答必现中断，Wireshark 里 event-stream 连接提前 FIN",
        ],
        "hard": [
            "AI 回答生成一半就停了，前端等不到后续内容只能手动刷新",
            "长文本输出总是中途停止，网络面板里那条流式请求变成灰色",
            "回答到 30 秒左右必停，重试也一样",
        ],
    },
    "DeepSeek API Error": {
        "root_keywords": ["DeepSeek API", "deepseek", "api key", "401", "402", "api.error"],
        "easy": [
            "DeepSeek API 返回 401",
            "LLM 调用失败，提示 api key 无效",
            "deepseek 请求报 api.error 异常",
        ],
        "normal": [
            "生成功能时好时坏，日志显示 HTTP 401 from api.deepseek.com",
            "模型网关报上游鉴权失效，抓到响应体里是 deepseek 的 invalid_request_error",
            "流式补全批量失败，响应头 X-Db-Error 无、状态码 401，上游是 deepseek",
        ],
        "hard": [
            "AI 生成功能集体罢工，日志里有 401 字样",
            "问答功能挂了半小时，运维说是大模型供应商那边拒了",
            "今天上午开始所有回答都出不来，日志里有 api.deepseek.com 的拒绝记录",
        ],
    },
}

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

DIFFICULTIES = ["easy", "normal", "hard"]


def expand(templates, root, difficulty, n_target, prefix, start):
    cases = []
    for i, (tpl, suffix) in enumerate(itertools.islice(
            itertools.product(templates, SUFFIXES), n_target)):
        cases.append({
            "id": f"{prefix}_{start + i:03d}",
            "input": tpl.format(obj="窗口") + suffix,
            "difficulty": difficulty,
        })
        if root is not None:
            cases[-1]["expected_root_cause"] = root
    return cases


def main():
    cases = []
    # 120 条诊断：8 根因 × (5 easy + 5 normal + 5 hard)
    idx = 0
    for root, spec in DIAG.items():
        for diff in DIFFICULTIES:
            for c in expand(spec[diff], root, diff, 5, "diag", idx):
                c.update({
                    "expected_intent": "ERROR_DIAGNOSIS",
                    "expected_tool": "analyze_log",
                    "root_keywords": spec["root_keywords"],
                })
                cases.append(c)
                idx += 1
    # 30 条协议问答
    for i, c in enumerate(expand(QA_TEMPLATES, None, "normal", 30, "qa", 0)):
        c.update({"expected_intent": "PROTOCOL_QA", "expected_tool": "search"})
        cases.append(c)
    # 30 条代码生成
    for i, c in enumerate(expand(GEN_TEMPLATES, None, "normal", 30, "gen", 0)):
        c.update({"expected_intent": "CODE_GENERATION", "expected_tool": "generate"})
        cases.append(c)
    # 20 条闲聊
    for i, c in enumerate(expand(CHAT_TEMPLATES, None, "easy", 20, "chat", 0)):
        c.update({"expected_intent": "GENERAL_CHAT", "expected_tool": "none"})
        cases.append(c)

    random.shuffle(cases)
    for c in cases:
        print(json.dumps(c, ensure_ascii=False))


if __name__ == "__main__":
    main()
