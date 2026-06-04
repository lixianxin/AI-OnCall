import os

filepath = r"D:\AI-OnCall\client-android\compose_chat-master\app\src\main\java\github\leavesczy\compose_chat\open\ui\OpenTabContentHost.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

idx = content.find("\u5f53\u524d\u8303\u56f4\u4e0b\u6682\u65e0\u53ef\u9009\u53c2\u4e0e\u4eba\u3002")
if idx >= 0:
    start = max(0, idx - 200)
    end = min(len(content), idx + 450)
    print(repr(content[start:end]))
